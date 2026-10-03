using System;
using System.Collections.Generic;
using System.IO;

namespace BigDataPipeline.Datamarts.Storage
{
    public class FolderHierarchyIndexStorage : IIndexStorage
    {
        private readonly string _baseDir;

        public FolderHierarchyIndexStorage(string baseDirPath)
        {
            _baseDir = baseDirPath;
        }

        public void Save(Dictionary<string, List<int>> index)
        {
            var buckets = new Dictionary<string, Dictionary<string, List<int>>>();

            foreach (var entry in index)
            {
                string bucket = BucketFor(entry.Key);

                if (!buckets.ContainsKey(bucket))
                {
                    buckets[bucket] = new Dictionary<string, List<int>>();
                }
                buckets[bucket][entry.Key] = entry.Value;
            }

            foreach (var bucketEntry in buckets)
            {
                string bucketFile = Path.Combine(_baseDir, bucketEntry.Key, "index.json");
                JsonIndexWriter.Write(bucketEntry.Value, bucketFile);
            }

            Console.WriteLine($"Índice en jerarquía de carpetas escrito bajo {_baseDir} ({buckets.Count} carpetas, {index.Count} términos).");
        }

        private string BucketFor(string term)
        {
            if (string.IsNullOrEmpty(term))
            {
                return "misc";
            }
            
            char c = char.ToLower(term[0]);
            if (char.IsLetter(c) && c <= 'z' && c >= 'a')
            {
                return c.ToString();
            }
            if (char.IsDigit(c))
            {
                return "0-9";
            }
            return "misc";
        }
    }
}