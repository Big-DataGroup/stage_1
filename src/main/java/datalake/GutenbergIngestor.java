package datalake;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GutenbergIngestor {

    private static final String START_MARKER = "*** START OF THE PROJECT GUTENBERG EBOOK";
    private static final String END_MARKER = "*** END OF THE PROJECT GUTENBERG EBOOK";

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Resultado de la operación de ingestión para métricas de I/O (Punto 25 y 27).
     */
    public record IngestResult(boolean success, long bytesWritten, int filesCreated) {}

    public static IngestResult downloadBook(int bookId, String baseOutputDir, String strategy) {
        Path outputDir = resolveDatalakePath(Paths.get(baseOutputDir), bookId, strategy);

        Path bodyPath = outputDir.resolve(bookId + ".body.txt");
        Path headerPath = outputDir.resolve(bookId + ".header.txt");

        // Recovery Mode: Si ya existen, no se re-descarga, pero se contabilizan para el estudio de disco
        if (Files.exists(bodyPath) && Files.exists(headerPath)) {
            System.out.println("Skipping book " + bookId + ": Files already exist (Recovery Mode)");
            registerDownloadedBook(bookId); // Asegurar que conste en el control aunque esté en recovery
            try {
                long existingBytes = Files.size(bodyPath) + Files.size(headerPath);
                return new IngestResult(true, existingBytes, 2);
            } catch (IOException e) {
                return new IngestResult(true, 0, 2);
            }
        }

        String url = String.format("https://www.gutenberg.org/cache/epub/%d/pg%d.txt", bookId, bookId);

        try {
            long startTime = System.nanoTime();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Error HTTP " + response.statusCode() + " for the book ID: " + bookId);
                return new IngestResult(false, 0, 0);
            }

            String text = response.body();

            if (!text.contains(START_MARKER) || !text.contains(END_MARKER)) {
                System.err.println("Book markers not found: " + bookId);
                return new IngestResult(false, 0, 0);
            }

            String[] parts1 = text.split(java.util.regex.Pattern.quote(START_MARKER), 2);
            String header = parts1[0];

            String[] parts2 = parts1[1].split(java.util.regex.Pattern.quote(END_MARKER), 2);
            String body = parts2[0];

            Files.createDirectories(outputDir);

            Files.writeString(bodyPath, body.strip());
            Files.writeString(headerPath, header.strip());

            // REGISTRO AUTOMÁTICO EN EL FICHERO DE CONTROL PARA EL PIPELINE
            registerDownloadedBook(bookId);

            long bytesWritten = Files.size(bodyPath) + Files.size(headerPath);
            long elapsedTime = System.nanoTime() - startTime;

            System.out.println("Book " + bookId + " saved using [" + strategy + "] -> "
                    + bytesWritten + " bytes written in " + String.format("%.2f", elapsedTime / 1_000_000.0) + " ms");

            return new IngestResult(true, bytesWritten, 2);

        } catch (IOException | InterruptedException e) {
            System.err.println("Network or disk error while processing the book " + bookId + ": " + e.getMessage());
            Thread.currentThread().interrupt();
            return new IngestResult(false, 0, 0);
        }
    }

    /**
     * Método auxiliar para registrar automáticamente el ID descargado en el fichero de control.
     */
    private static void registerDownloadedBook(int bookId) {
        try {
            Path controlFile = Path.of("data/control/downloaded_books.txt");
            if (controlFile.getParent() != null) {
                Files.createDirectories(controlFile.getParent());
            }
            // Evitar duplicados en el fichero de control si ya estaba escrito
            String idStr = String.valueOf(bookId);
            boolean exists = false;
            if (Files.exists(controlFile)) {
                for (String line : Files.readAllLines(controlFile)) {
                    if (line.trim().equals(idStr)) {
                        exists = true;
                        break;
                    }
                }
            }
            if (!exists) {
                Files.writeString(
                        controlFile,
                        bookId + System.lineSeparator(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                );
            }
        } catch (IOException e) {
            System.err.println("No se pudo registrar el libro en el fichero de control: " + e.getMessage());
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

    public static void downloadBatch(int[] bookIds, String baseOutputDir, String strategy) {
        System.out.println("=== STARTING BATCH DOWNLOAD (" + strategy + ") ===");
        int successCount = 0;
        long totalBytes = 0;
        int totalFiles = 0;

        for (int bookId : bookIds) {
            IngestResult result = downloadBook(bookId, baseOutputDir, strategy);
            if (result.success()) {
                successCount++;
                totalBytes += result.bytesWritten();
                totalFiles += result.filesCreated();
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Batch interrupted.");
                break;
            }
        }
        System.out.println("=== BATCH COMPLETED: " + successCount + "/" + bookIds.length
                + " SUCCESSFUL | Total Bytes: " + totalBytes + " | Total Files: " + totalFiles + " ===");
    }

    public static void main(String[] args) {
        int[] sampleBooks = {1342, 84, 11, 2701, 1661};
        String[] strategies = {"by_book", "by_time", "by_batch"};

        System.out.println("--- Generating Sample Dataset with Metrics ---");

        for (String strategy : strategies) {
            System.out.println("\n-> Executing strategy: " + strategy);
            downloadBatch(sampleBooks, "data/datalake", strategy);
        }
    }
}