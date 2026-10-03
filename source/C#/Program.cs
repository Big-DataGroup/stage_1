using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using BigDataPipeline.Control;
using BigDataPipeline.Datamarts;
using BigDataPipeline.Datamarts.Storage;

namespace BigDataPipeline
{
    class Program
    {
        static async Task Main(string[] args)
        {
            string datalakeDir = "../../data/datalake";
            string controlDir = "../../data/control";
            int[] sampleBooks = { 1332, 322, 31, 2731, 1361 };

            PipelineController controller = new PipelineController(
                    $"{controlDir}/downloaded_books.txt",
                    $"{controlDir}/indexed_books.txt"
            );
            
            MetadataStore.InitializeDatabase(); 

            Console.WriteLine("=== FASE 1: DESCARGA ===");
            foreach (int bookId in sampleBooks)
            {
                bool success = await GutenbergIngestor.DownloadBookAsync(bookId, datalakeDir, "by_book");
                if (success)
                {
                    controller.MarkDownloaded(bookId);
                }
            }

            Console.WriteLine("\n=== FASE 2: EXTRACCIÓN E INDEXACIÓN ===");
            List<int> pending = controller.GetBooksPendingIndexing();

            if (pending.Count == 0)
            {
                Console.WriteLine("No hay libros pendientes de procesar.");
                return;
            }

            InvertedIndex invertedIndex = new InvertedIndex();

            foreach (int bookId in pending)
            {
                string basePath = $"{datalakeDir}/by_book/{bookId}/{bookId}";
                string headerPath = basePath + ".header.txt";
                string bodyPath = basePath + ".body.txt";

                BookMetadata metadata = MetadataParser.ParseHeader(bookId, headerPath);
                MetadataStore.InsertMetadata(metadata);

                invertedIndex.AddDocument(bookId, bodyPath);
                controller.MarkIndexed(bookId);
            }

            Console.WriteLine("\n=== FASE 3: PERSISTENCIA DEL ÍNDICE ===");
            
            
            List<IIndexStorage> storages = new List<IIndexStorage>
            {
                new JsonFileIndexStorage("../../data/index/csharp/monolithic/index.json"),
                new FolderHierarchyIndexStorage("../../data/index/csharp/hierarchy"),
                new MongoIndexStorage("mongodb://localhost:27017", "gutenberg", "inverted_index")
            };

            invertedIndex.PersistAll(storages);
            
            
            Console.WriteLine("\nPipeline Big Data completado con éxito.");
        }
    }
}