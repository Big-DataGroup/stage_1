using System;
using System.IO;
using System.Net.Http;
using System.Threading.Tasks;

public class GutenbergIngestor
{
    private const string StartMarker = "*** START OF THE PROJECT GUTENBERG EBOOK";
    private const string EndMarker = "*** END OF THE PROJECT GUTENBERG EBOOK";

    private static readonly HttpClient httpClient = new HttpClient(new HttpClientHandler
    {
        AllowAutoRedirect = true
    });

    public static async Task<bool> DownloadBookAsync(int bookId, string baseOutputDir, string strategy)
    {
        // Resolve the output directory based on the selected strategy
        string outputDir = ResolveDatalakePath(baseOutputDir, bookId, strategy);

        // Define the file paths using the required nomenclature
        string bodyPath = Path.Combine(outputDir, $"{bookId}.body.txt");
        string headerPath = Path.Combine(outputDir, $"{bookId}.header.txt");

        // Check if files already exist to avoid duplicate network requests (Recovery Mode)
        if (File.Exists(bodyPath) && File.Exists(headerPath))
        {
            Console.WriteLine($"Skipping book {bookId}: Files already exist (Recovery Mode)");
            return true;
        }

        string url = $"https://www.gutenberg.org/cache/epub/{bookId}/pg{bookId}.txt";

        try
        {
            HttpResponseMessage response = await httpClient.GetAsync(url);

            if (!response.IsSuccessStatusCode)
            {
                Console.Error.WriteLine($"Error HTTP {(int)response.StatusCode} for the book ID: {bookId}");
                return false;
            }

            string text = await response.Content.ReadAsStringAsync();

            // Check if the Gutenberg markers exist in the text
            if (!text.Contains(StartMarker) || !text.Contains(EndMarker))
            {
                Console.Error.WriteLine($"Book not found or markers missing: {bookId}");
                return false;
            }

            // Split text to extract header and body
            int startIndex = text.IndexOf(StartMarker, StringComparison.Ordinal);
            string header = text.Substring(0, startIndex);

            int bodyStartIndex = startIndex + StartMarker.Length;
            int endIndex = text.IndexOf(EndMarker, bodyStartIndex, StringComparison.Ordinal);
            string body = text.Substring(bodyStartIndex, endIndex - bodyStartIndex);

            // Create directories if they do not exist
            Directory.CreateDirectory(outputDir);

            // Write the extracted text into the files
            await File.WriteAllTextAsync(bodyPath, body.Trim());
            await File.WriteAllTextAsync(headerPath, header.Trim());

            Console.WriteLine($"Book {bookId} saved using [{strategy}]");
            return true;
        }
        catch (Exception ex)
        {
            Console.Error.WriteLine($"Network or disk error while processing the book {bookId}: {ex.Message}");
            return false;
        }
    }

    private static string ResolveDatalakePath(string baseDir, int bookId, string strategy)
    {
        switch (strategy.ToLower())
        {
            case "by_book":
                return Path.Combine(baseDir, "by_book", bookId.ToString());

            case "by_batch":
                int lowerBound = (bookId / 1000) * 1000;
                int upperBound = lowerBound + 999;
                string batchName = $"{lowerBound}-{upperBound}";
                return Path.Combine(baseDir, "by_batch", batchName);

            case "by_time":
                string dateFolder = DateTime.Now.ToString("yyyyMMdd");
                string hourFolder = DateTime.Now.ToString("HH");
                return Path.Combine(baseDir, "by_time", dateFolder, hourFolder);

            default:
                throw new ArgumentException($"Unknown datalake strategy: {strategy}");
        }
    }

    public static async Task DownloadBatchAsync(int[] bookIds, string baseOutputDir, string strategy)
    {
        Console.WriteLine("=== STARTING BATCH DOWNLOAD (C#) ===");
        int successCount = 0;

        foreach (int bookId in bookIds)
        {
            bool success = await DownloadBookAsync(bookId, baseOutputDir, strategy);
            if (success)
            {
                successCount++;
            }

            // Sleep for 200ms between requests to respect rate limits
            await Task.Delay(200);
        }

        Console.WriteLine($"=== BATCH COMPLETED: {successCount}/{bookIds.Length} SUCCESSFUL ===");
    }

    public static async Task Main(string[] args)
    {
        int startId = 1;
        int endId = 1000;

        int[] sampleBooks = new int[endId - startId + 1];
        for (int i = 0; i < sampleBooks.Length; i++)
        {
            sampleBooks[i] = startId + i;
        }

        string[] strategies = { "by_book" };

        Console.WriteLine($"--- Generating Sample Dataset ({sampleBooks.Length} books) ---");

        foreach (string strategy in strategies)
        {
            Console.WriteLine($"\n-> Executing strategy: {strategy}");
            await DownloadBatchAsync(sampleBooks, "data/datalake", strategy);
        }
    }
}