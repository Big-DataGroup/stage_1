using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;

namespace BigDataPipeline.Control
{
    /// <summary>
    /// Capa de control del pipeline.
    /// Supervisa qué libros han sido descargados y cuáles ya han sido indexados.
    /// </summary>
    public class PipelineController
    {
        private readonly string _downloadedFile;
        private readonly string _indexedFile;

        private readonly HashSet<int> _downloadedBooks;
        private readonly HashSet<int> _indexedBooks;
        
        // C# utiliza un objeto de bloqueo explícito en lugar de la palabra 'synchronized'
        private readonly object _lockObject = new object();

        public PipelineController(string downloadedFilePath, string indexedFilePath)
        {
            _downloadedFile = downloadedFilePath;
            _indexedFile = indexedFilePath;

            _downloadedBooks = LoadIds(_downloadedFile);
            _indexedBooks = LoadIds(_indexedFile);
        }

        private HashSet<int> LoadIds(string filePath)
        {
            var ids = new HashSet<int>();
            
            if (!File.Exists(filePath))
            {
                return ids;
            }

            try
            {
                // Equivalente directo a Files.readAllLines(file)
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
            // Bloque lock equivale a un método 'synchronized' de Java
            lock (_lockObject)
            {
                if (_downloadedBooks.Add(bookId))
                {
                    AppendId(_downloadedFile, bookId);
                }
            }
        }

        public void MarkIndexed(int bookId)
        {
            lock (_lockObject)
            {
                if (_indexedBooks.Add(bookId))
                {
                    AppendId(_indexedFile, bookId);
                }
            }
        }

        public bool IsDownloaded(int bookId)
        {
            return _downloadedBooks.Contains(bookId);
        }

        public bool IsIndexed(int bookId)
        {
            return _indexedBooks.Contains(bookId);
        }

        public List<int> GetBooksPendingIndexing()
        {
            // Traducción de Stream.filter.sorted.collect a LINQ de C#
            return _downloadedBooks
                .Where(id => !_indexedBooks.Contains(id))
                .OrderBy(id => id)
                .ToList();
        }

        // IReadOnlySet sustituye a Set.copyOf() para asegurar la inmutabilidad[cite: 25]
        public IReadOnlySet<int> GetDownloadedBooks()
        {
            return _downloadedBooks;
        }

        public IReadOnlySet<int> GetIndexedBooks()
        {
            return _indexedBooks;
        }

        private void AppendId(string filePath, int bookId)
        {
            try
            {
                string? directory = Path.GetDirectoryName(filePath);
                if (!string.IsNullOrEmpty(directory))
                {
                    Directory.CreateDirectory(directory);
                }

                // AppendAllText abre, escribe y cierra el archivo automáticamente 
                // Sustituye a Files.writeString con StandardOpenOption.APPEND[cite: 25]
                File.AppendAllText(filePath, bookId + Environment.NewLine);
            }
            catch (Exception e)
            {
                Console.Error.WriteLine($"Error escribiendo en fichero de control {filePath}: {e.Message}");
            }
        }
    }
}