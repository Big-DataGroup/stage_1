using System;
using System.Collections.Generic;
using BigDataPipeline.Datamarts.Storage; 

namespace BigDataPipeline.Datamarts
{
    public class InvertedIndex
    {
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
                if (!_index.ContainsKey(word))
                {
                    _index[word] = new List<int>();
                }

                // Evitar duplicados si se re-indexa el mismo libro incrementalmente
                if (!_index[word].Contains(bookId))
                {
                    _index[word].Add(bookId);
                }
            }
            //Console.WriteLine($"Libro {bookId} indexado en memoria correctamente.");
        }

        public List<int> Search(string word)
        {
            string key = word.ToLower();
            return _index.TryGetValue(key, out var list) ? list : new List<int>();
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