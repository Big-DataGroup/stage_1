using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;

namespace BigDataPipeline.Control
{
    public class PipelineController
    {
        private readonly string downloadedFile;
        private readonly string indexedFile;

        private readonly HashSet<int> downloadedBooks;
        private readonly HashSet<int> indexedBooks;

        private readonly object _lock = new object();

        public PipelineController(string downloadedFilePath, string indexedFilePath)
        {
            this.downloadedFile = downloadedFilePath;
            this.indexedFile = indexedFilePath;

            this.downloadedBooks = loadIds(this.downloadedFile);
            this.indexedBooks = loadIds(this.indexedFile);
        }

        private HashSet<int> loadIds(string filePath)
        {
            var ids = new HashSet<int>();
            if (!File.Exists(filePath)) return ids;

            try
            {
                string[] lines = File.ReadAllLines(filePath);
                foreach (string line in lines)
                {
                    string trimmedLine = line.Trim();
                    if (!string.IsNullOrEmpty(trimmedLine) && int.TryParse(trimmedLine, out int id))
                    {
                        ids.Add(id);
                    }
                }
            }
            catch (Exception e)
            {
                Console.Error.WriteLine($"Error leyendo fichero de control {filePath}: {e.Message}");
            }

            return ids;
        }

        public void MarkDownloaded(int bookId)
        {
            lock (_lock)
            {
                if (downloadedBooks.Add(bookId))
                {
                    appendId(downloadedFile, bookId);
                }
            }
        }

        public void MarkIndexed(int bookId)
        {
            lock (_lock)
            {
                if (indexedBooks.Add(bookId))
                {
                    appendId(indexedFile, bookId);
                }
            }
        }

        public bool IsDownloaded(int bookId) => downloadedBooks.Contains(bookId);

        public bool IsIndexed(int bookId) => indexedBooks.Contains(bookId);

        public List<int> GetBooksPendingIndexing()
        {
            return downloadedBooks
                .Where(id => !indexedBooks.Contains(id))
                .OrderBy(id => id)
                .ToList();
        }

        public IReadOnlySet<int> GetDownloadedBooks() => downloadedBooks;

        public IReadOnlySet<int> GetIndexedBooks() => indexedBooks;

        private void appendId(string filePath, int bookId)
        {
            try
            {
                string? directory = Path.GetDirectoryName(filePath);
                if (!string.IsNullOrEmpty(directory))
                {
                    Directory.CreateDirectory(directory);
                }
                File.AppendAllText(filePath, bookId + Environment.NewLine);
            }
            catch (Exception e)
            {
                Console.Error.WriteLine($"Error escribiendo en fichero de control {filePath}: {e.Message}");
            }
        }
    }
}