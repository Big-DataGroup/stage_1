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
            Console.WriteLine($"Índice monolítico JSON escrito en {_outputFile} ({index.Count} términos).");
        }
    }
}