using System;
using System.IO;
using System.Net.Http;
using System.Threading.Tasks;

namespace BigDataPipeline.Datalake
{
    public class GutenbergIngestor
    {
        private const string StartMarker = "*** START OF THE PROJECT GUTENBERG EBOOK";
        private const string EndMarker = "*** END OF THE PROJECT GUTENBERG EBOOK";

        // HttpClient en C# está diseñado para ser estático y reutilizarse
        private static readonly HttpClient HttpClient = new HttpClient();

        public static async Task<bool> DownloadBookAsync(int bookId, string baseOutputDir, string strategy)
        {
            string outputDir = ResolveDatalakePath(baseOutputDir, bookId, strategy);
            
            // System.IO.Path.Combine sustituye a resolve() de Java[cite: 27]
            string bodyPath = Path.Combine(outputDir, $"{bookId}.body.txt");
            string headerPath = Path.Combine(outputDir, $"{bookId}.header.txt");

            if (File.Exists(bodyPath) && File.Exists(headerPath))
            {
                Console.WriteLine($"Skipping book {bookId}: Files already exist (Recovery Mode)");
                return true;
            }

            string url = $"https://www.gutenberg.org/cache/epub/{bookId}/pg{bookId}.txt";

            try
            {
                // Llamada asíncrona sustituyendo al HttpRequest bloqueante de Java[cite: 27]
                HttpResponseMessage response = await HttpClient.GetAsync(url);

                if (!response.IsSuccessStatusCode)
                {
                    Console.Error.WriteLine($"Error HTTP {(int)response.StatusCode} for the book ID: {bookId}");
                    return false;
                }

                string text = await response.Content.ReadAsStringAsync();

                if (!text.Contains(StartMarker) || !text.Contains(EndMarker))
                {
                    Console.Error.WriteLine($"Book not found: {bookId}");
                    return false;
                }

                int startIndex = text.IndexOf(StartMarker, StringComparison.Ordinal);
                int endIndex = text.IndexOf(EndMarker, StringComparison.Ordinal);

                string header = text.Substring(0, startIndex).Trim();
                string body = text.Substring(startIndex + StartMarker.Length, endIndex - (startIndex + StartMarker.Length)).Trim();

                Directory.CreateDirectory(outputDir);
                File.WriteAllText(bodyPath, body.Trim());
                File.WriteAllText(headerPath, header.Trim());

                Console.WriteLine($"Book {bookId} saved using [{strategy}]");
                return true;
            }
            catch (Exception e)
            {
                Console.Error.WriteLine($"Network or disk error while processing the book {bookId}: {e.Message}");
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
                    return Path.Combine(baseDir, "by_batch", $"{lowerBound}-{upperBound}");

                case "by_time":
                    DateTime now = DateTime.Now;
                    return Path.Combine(baseDir, "by_time", now.ToString("yyyyMMdd"), now.ToString("HH"));

                default:
                    throw new ArgumentException($"Unknown datalake strategy: {strategy}");
            }
        }

        public static async Task DownloadBatchAsync(int[] bookIds, string baseOutputDir, string strategy)
        {
            Console.WriteLine("=== STARTING BATCH DOWNLOAD ===");
            int successCount = 0;

            foreach (int bookId in bookIds)
            {
                bool success = await DownloadBookAsync(bookId, baseOutputDir, strategy);
                if (success)
                {
                    successCount++;
                }

                // Task.Delay asíncrono sustituye a Thread.sleep de Java[cite: 27]
                await Task.Delay(200);
            }
            Console.WriteLine($"=== BATCH COMPLETED: {successCount}/{bookIds.Length} SUCCESSFUL ===");
        }
    }
}