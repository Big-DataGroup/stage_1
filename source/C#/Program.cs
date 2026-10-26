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
                
                AddExporter(BenchmarkDotNet.Exporters.Csv.CsvExporter.Default);
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
            var summary = BenchmarkRunner.Run<InvertedIndexBenchmark>();

            // --- GENERADOR DIRECTO DE METRICAS CSV ---
            try
            {
                // Buscar la raíz del proyecto localizando la carpeta 'data'
                string? currentDir = AppContext.BaseDirectory;
                while (currentDir != null && !Directory.Exists(Path.Combine(currentDir, "data")))
                {
                    var parent = Directory.GetParent(currentDir);
                    currentDir = parent?.FullName;
                }

                if (currentDir != null)
                {
                    string targetDir = Path.Combine(currentDir, "data", "benchmarks");
                    Directory.CreateDirectory(targetDir);
                    string targetPath = Path.Combine(targetDir, "metricas_csharp.csv");

                    using (var writer = new StreamWriter(targetPath))
                    {
                        // Cabecera clara en milisegundos y bytes
                        writer.WriteLine("Method;DatasetSize;Mean_ms;Error_ms;StdDev_ms;Allocated_Bytes");

                        foreach (var report in summary.Reports)
                        {
                            var method = report.BenchmarkCase.Descriptor.WorkloadMethod.Name;
                            var datasetSize = report.BenchmarkCase.Parameters["DatasetSize"];
                            
                            // Convertir nanosegundos nativos de BenchmarkDotNet a milisegundos (/ 1.000.000)
                            double meanMs = report.ResultStatistics != null ? report.ResultStatistics.Mean / 1_000_000.0 : 0;
                            double errorMs = report.ResultStatistics != null ? report.ResultStatistics.StandardError / 1_000_000.0 : 0;
                            double stdDevMs = report.ResultStatistics != null ? report.ResultStatistics.StandardDeviation / 1_000_000.0 : 0;
                            
                            var allocated = report.GcStats.GetBytesAllocatedPerOperation(report.BenchmarkCase);

                            writer.WriteLine($"{method};{datasetSize};{meanMs:F4};{errorMs:F4};{stdDevMs:F4};{allocated}");
                        }
                    }

                    Console.WriteLine($"\n[ÉXITO] File CSV generado correctamente en: {targetPath}");
                }
                else
                {
                    Console.WriteLine("\n[AVISO] No se pudo localizar la carpeta raíz 'data' del proyecto.");
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"\n[ERROR] Fallo al escribir el CSV: {ex.Message}");
            }
        }
    }
}