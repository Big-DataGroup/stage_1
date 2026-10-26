using BenchmarkDotNet.Attributes;
using BenchmarkDotNet.Running;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using BigDataPipeline.Datamarts;
using BigDataPipeline.Datamarts.Storage;

namespace InvertedIndexProject
{
    [MemoryDiagnoser]
    public class InvertedIndexBenchmark
    {
        [Params(500, 1000)]
        public int DatasetSize;

        private string testFilePath;
        private InvertedIndex preFilledIndex;
        private List<IIndexStorage> storages;

        [GlobalSetup]
        public void Setup()
        {
            // 1. Crear dataset temporal de prueba
            testFilePath = Path.GetTempFileName();
            string dummyContent = "Prueba de indexacion con palabras repetidas y signos! \n";
            File.WriteAllText(testFilePath, string.Concat(Enumerable.Repeat(dummyContent, DatasetSize)));

            // 2. Pre-llenar índice para Query y Disk IO (igual que en Java/Python)
            preFilledIndex = new InvertedIndex();
            preFilledIndex.AddDocument(1, testFilePath);

            // 3. Configurar persistencia real para la prueba de disco
            string tempStorageFile = Path.Combine(Path.GetTempPath(), "benchmark_index.json");
            storages = new List<IIndexStorage>
            {
                new JsonFileIndexStorage(tempStorageFile)
            };
        }

        [Benchmark]
        public void UpdatePerformance()
        {
            var index = new InvertedIndex();
            index.AddDocument(999, testFilePath);
        }

        [Benchmark]
        public void QueryPerformance()
        {
            // Búsqueda en memoria sobre el índice pre-llenado
            preFilledIndex.Search("indexacion");
        }

        [Benchmark]
        public void DiskIoPerformance()
        {
            // Persistencia real en disco
            preFilledIndex.PersistAll(storages);
        }
    }

    public class Program
    {
        public static void Main(string[] args)
        {
            BenchmarkRunner.Run<InvertedIndexBenchmark>();
        }
    }
}