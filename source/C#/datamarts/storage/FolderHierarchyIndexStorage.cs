using System;
using System.Collections.Generic;
using System.IO;

namespace BigDataPipeline.Datamarts.Storage
{
    public class FolderHierarchyIndexStorage : IIndexStorage
    {
        private readonly string _baseDir;

        public FolderHierarchyIndexStorage(string baseDir)
        {
            _baseDir = baseDir;
        }

        public void Save(Dictionary<string, List<int>> index)
        {
            int folderCount = 0;
            Directory.CreateDirectory(_baseDir);

            foreach (var entry in index)
            {
                string term = entry.Key;
                List<int> bookIds = entry.Value;

                // Carpeta según la primera letra (o '_other')
                char firstChar = char.ToLower(term[0]);
                string folderName = (firstChar >= 'a' && firstChar <= 'z') ? firstChar.ToString() : "_other";
                string targetDir = Path.Combine(_baseDir, folderName);

                if (!Directory.Exists(targetDir))
                {
                    Directory.CreateDirectory(targetDir);
                    folderCount++;
                }

                string filePath = Path.Combine(targetDir, $"{term}.json");

                // Formato JSON para cada término
                string jsonContent = $"{{\"{term}\": [{string.Join(", ", bookIds)}]}}";
                File.WriteAllText(filePath, jsonContent);
            }

            Console.WriteLine($"Índice en jerarquía escrito bajo {_baseDir} ({folderCount} carpetas, {index.Count} términos).");
        }
    }
}