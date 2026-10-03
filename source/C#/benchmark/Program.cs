using BenchmarkDotNet.Attributes;
using BenchmarkDotNet.Running;
using System;
using System.IO;
using System.Linq;

namespace InvertedIndexProject
{
    [MemoryDiagnoser]
    public class InvertedIndexBenchmark
    {
        [Params(500, 1000)]
        public int DatasetSize;

        private string testFilePath;

        [GlobalSetup]
        public void Setup()
        {
            testFilePath = Path.GetTempFileName();
            string dummyContent = "Prueba de indexacion con palabras repetidas y signos! \n";
            File.WriteAllText(testFilePath, string.Concat(Enumerable.Repeat(dummyContent, DatasetSize)));
        }

        [Benchmark]
        public void UpdatePerformance()
        {
        }

        [Benchmark]
        public void QueryPerformance()
        {
        }

        [Benchmark]
        public void DiskIoPerformance()
        {
        }
    }

    public class Program
    {
        public static void Main(string[] args)
        {
            var artifactsPath = Path.GetFullPath(Path.Combine(AppContext.BaseDirectory, "../../../../data/benchmarks"));

            BenchmarkRunner.Run<InvertedIndexBenchmark>();
        }
    }
}