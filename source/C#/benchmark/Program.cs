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
        // Equivalente a tus @Params de Java (1000 y 5000 elementos)
        [Params(1000, 5000)]
        public int DatasetSize;

        private string testFilePath;

        [GlobalSetup]
        public void Setup()
        {
            // Creamos un archivo temporal con texto de prueba
            testFilePath = Path.GetTempFileName();
            string dummyContent = "Prueba de indexacion con palabras repetidas y signos! \n";
            File.WriteAllText(testFilePath, string.Concat(Enumerable.Repeat(dummyContent, DatasetSize)));
        }

        [Benchmark]
        public void UpdatePerformance()
        {
            // TODO: Instancia aquí tu clase InvertedIndex de C# y llama a añadir documento
            // var index = new InvertedIndex();
            // index.AddDocument(999, testFilePath);
        }

        [Benchmark]
        public void QueryPerformance()
        {
            // Lógica de búsqueda en memoria
        }

        [Benchmark]
        public void DiskIoPerformance()
        {
            // Lógica de persistencia
        }
    }

    public class Program
    {
        public static void Main(string[] args)
        {
            // Esto ejecuta el benchmark y le decimos que guarde los CSV en la ruta global
            // Subimos 4 niveles desde benchmarks/ para llegar a la raíz del repo y entrar en data/benchmarks
            var artifactsPath = Path.GetFullPath(Path.Combine(AppContext.BaseDirectory, "../../../../data/benchmarks"));
            
            // Nota: BenchmarkDotNet maneja sus propias rutas de salida, 
            // así que lanzaremos el comando con un parámetro especial o copiaremos el resultado.
            BenchmarkRunner.Run<InvertedIndexBenchmark>();
        }
    }
}