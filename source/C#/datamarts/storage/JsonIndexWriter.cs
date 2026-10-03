using System;
using System.Collections.Generic;
using System.IO;

namespace BigDataPipeline.Datamarts.Storage
{
    internal static class JsonIndexWriter
    {
        public static void Write(Dictionary<string, List<int>> index, string outputFile)
        {
            try
            {
                string directory = Path.GetDirectoryName(outputFile);
                if (!string.IsNullOrEmpty(directory))
                {
                    Directory.CreateDirectory(directory);
                }

                var sorted = new SortedDictionary<string, List<int>>(index);

                using StreamWriter writer = new StreamWriter(outputFile);
                writer.Write("{\n");
                
                int i = 0;
                int total = sorted.Count;
                
                foreach (var entry in sorted)
                {
                    writer.Write($"  \"{Escape(entry.Key)}\": [");
                    
                    List<int> ids = entry.Value;
                    for (int j = 0; j < ids.Count; j++)
                    {
                        writer.Write(ids[j]);
                        if (j < ids.Count - 1) writer.Write(", ");
                    }
                    
                    writer.Write("]");
                    i++;
                    if (i < total) writer.Write(",");
                    writer.Write("\n");
                }
                writer.Write("}\n");
            }
            catch (IOException e)
            {
                Console.Error.WriteLine($"Error escribiendo índice JSON en {outputFile}: {e.Message}");
            }
        }

        private static string Escape(string s)
        {
            return s.Replace("\\", "\\\\").Replace("\"", "\\\"");
        }
    }
}