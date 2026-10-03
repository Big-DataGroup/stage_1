using System;
using System.Collections.Generic;
using System.IO;
using System.Text.RegularExpressions;

namespace BigDataPipeline.Datamarts
{
    public class Tokenizer
    {
        public static HashSet<string> Tokenize(string filePath)
        {
            var uniqueWords = new HashSet<string>();

            try
            {
                using StreamReader sr = new StreamReader(filePath);
                string line;
                
                while ((line = sr.ReadLine()) != null)
                {

                    line = line.ToLower();
                    line = Regex.Replace(line, "[^a-z0-9\\s]", " ");
                    
                    string[] words = Regex.Split(line, "\\s+");

                    foreach (string word in words)
                    {
                        if (!string.IsNullOrWhiteSpace(word))
                        {
                            uniqueWords.Add(word);
                        }
                    }
                }
            }
            catch (IOException e)
            {
                Console.Error.WriteLine($"Error leyendo el cuerpo del libro: {e.Message}");
            }

            return uniqueWords;
        }
    }
}