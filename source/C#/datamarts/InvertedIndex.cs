using System;
using System.Collections.Generic;
using BigDataPipeline.Datamarts.Storage; 

namespace BigDataPipeline.Datamarts
{
    public class InvertedIndex
    {
        // Equivalente a Map<String, List<Integer>> en Java[cite: 28]
        private readonly Dictionary<string, List<int>> _index;

        public InvertedIndex()
        {
            _index = new Dictionary<string, List<int>>();
        }

        public void AddDocument(int bookId, string bodyFilePath)
        {
            HashSet<string> words = Tokenizer.Tokenize(bodyFilePath);

            foreach (string word in words)
            {
                // Equivalente a putIfAbsent de Java[cite: 28]
                if (!_index.ContainsKey(word))
                {
                    _index[word] = new List<int>();
                }
                
                _index[word].Add(bookId);
            }
            Console.WriteLine($"Libro {bookId} indexado en memoria correctamente.");
        }

        public void PrintIndex()
        {
            foreach (var entry in _index)
            {
                Console.WriteLine($"{entry.Key} -> [{string.Join(", ", entry.Value)}]");
            }
        }

        public void PersistAll(List<IIndexStorage> storages)
        {
            foreach (var storage in storages)
            {
                storage.Save(_index);
            }
        }
    }
}