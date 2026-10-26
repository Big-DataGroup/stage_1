using BenchmarkDotNet.Attributes;
using BenchmarkDotNet.Running;
using BenchmarkDotNet.Configs; // <-- NUEVO
using Perfolizer.Horology;     // <-- NUEVO (Controla las unidades de tiempo)
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using BigDataPipeline.Datamarts;
using BigDataPipeline.Datamarts.Storage;

namespace InvertedIndexProject
{
    [MemoryDiagnoser]
    [Config(typeof(Config))] // <-- NUEVO: Vincula la configuración de abajo
    public class InvertedIndexBenchmark
    {
        // <-- NUEVO: Forzamos la tabla para que se imprima siempre en milisegundos
        private class Config : ManualConfig
        {
            public Config()
            {
                SummaryStyle = BenchmarkDotNet.Reports.SummaryStyle.Default
                    .WithTimeUnit(TimeUnit.Millisecond);
            }
        }

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
        public object UpdatePerformance()
        {
            var index = new InvertedIndex();
            index.AddDocument(999, testFilePath);
            return index; // <-- Evita que el compilador borre el código
        }

        [Benchmark]
        public object QueryPerformance()
        {
            // Devolvemos el resultado de la búsqueda
            return preFilledIndex.Search("indexacion"); 
        }

        [Benchmark]
        public void DiskIoPerformance()
        {
            // Este puede ser void porque interactuar con el disco duro 
            // ya obliga al procesador a ejecutar el código.
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