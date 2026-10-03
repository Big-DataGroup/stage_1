using System;
using System.Collections.Generic;

namespace BigDataPipeline.Datamarts.Storage
{
    public class JsonFileIndexStorage : IIndexStorage
    {
        private readonly string _outputFile;

        public JsonFileIndexStorage(string outputFilePath)
        {
            _outputFile = outputFilePath;
        }

        public void Save(Dictionary<string, List<int>> index)
        {
            JsonIndexWriter.Write(index, _outputFile);
            // Count sustituye a size() de Java[cite: 35]
            Console.WriteLine($"Índice monolítico JSON escrito en {_outputFile} ({index.Count} términos).");
        }
    }
}