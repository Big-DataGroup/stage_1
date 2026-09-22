package DATALAKE;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GutenbergIngestor {

    private static final String START_MARKER = "*** START OF THE PROJECT GUTENBERG EBOOK";
    private static final String END_MARKER = "*** END OF THE PROJECT GUTENBERG EBOOK";

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public static void downloadBook(int bookId, String baseOutputDir, String strategy) {
        String url = String.format("https://www.gutenberg.org/cache/epub/%d/pg%d.txt", bookId, bookId);

        try {
            // Build and send the HTTP GET request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Error HTTP " + response.statusCode() + " for the book ID: " + bookId);
                return;
            }

            String text = response.body();

            // Check if the Gutenberg markers exist in the text
            if (!text.contains(START_MARKER) || !text.contains(END_MARKER)) {
                System.err.println("Book not founded: " + bookId);
                return;
            }

            // Split text to extract header and body
            String[] parts1 = text.split(java.util.regex.Pattern.quote(START_MARKER), 2);
            String header = parts1[0];

            String[] parts2 = parts1[1].split(java.util.regex.Pattern.quote(END_MARKER), 2);
            String body = parts2[0];

            // Resolve the output directory based on the selected strategy
            Path outputDir = resolveDatalakePath(Paths.get(baseOutputDir), bookId, strategy);
            Files.createDirectories(outputDir);

            // Define the file paths using the required nomenclature
            Path bodyPath = outputDir.resolve(bookId + ".body.txt");
            Path headerPath = outputDir.resolve(bookId + ".header.txt");

            // Write the extracted text into the files
            Files.writeString(bodyPath, body.strip());
            Files.writeString(headerPath, header.strip());

            System.out.println("Book " + bookId + " saved using [" + strategy + "]");

        } catch (IOException | InterruptedException e) {
            System.err.println("Network or disk error while processing the book " + bookId + ": " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private static Path resolveDatalakePath(Path baseDir, int bookId, String strategy) {
        switch (strategy.toLowerCase()) {
            case "by_book":
                return baseDir.resolve("by_book").resolve(String.valueOf(bookId));

            case "by_batch":
                int lowerBound = (bookId / 1000) * 1000;
                int upperBound = lowerBound + 999;
                String batchName = lowerBound + "-" + upperBound;
                return baseDir.resolve("by_batch").resolve(batchName);

            case "by_time":
                // Format date and time to match the required YYYYMMDD/HH/ structure
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyyMMdd");
                DateTimeFormatter hourFmt = DateTimeFormatter.ofPattern("HH");

                String dateFolder = now.format(dateFmt);
                String hourFolder = now.format(hourFmt);

                return baseDir.resolve("by_time").resolve(dateFolder).resolve(hourFolder);

            default:
                throw new IllegalArgumentException("Unknown datalake strategy: " + strategy);
        }
    }

    public static void main() {
        // TEST
        downloadBook(1341, "data/datalake", "by_book");
        downloadBook(1341, "data/datalake", "by_batch");
        downloadBook(1341, "data/datalake", "by_time");
    }
}
