using System;
using System.Collections.Generic;
using BigDataPipeline.Datamarts;
using BigDataPipeline.Datamarts.Storage;

namespace BigDataPipeline.Control
{
    public class IndexingPipeline
    {
        public static void Run(PipelineController controller)
        {
            List<int> pending = controller.GetBooksPendingIndexing();
            
            if (pending.Count == 0)
            {
                Console.WriteLine("No hay libros pendientes de indexar.");
                return;
            }

            InvertedIndex invertedIndex = new InvertedIndex();

            foreach (int bookId in pending)
            {
                string bodyPath = $"data/datalake/by_book/{bookId}/{bookId}.body.txt";
                invertedIndex.AddDocument(bookId, bodyPath);
                controller.MarkIndexed(bookId);
            }

            List<IIndexStorage> storages = new List<IIndexStorage>
            {
                new JsonFileIndexStorage("data/index/monolithic/index.json"),
                new FolderHierarchyIndexStorage("data/index/hierarchy")
            };

            invertedIndex.PersistAll(storages);

            Console.WriteLine($"Indexación completada para {pending.Count} libro(s).");
        }
    }
}
