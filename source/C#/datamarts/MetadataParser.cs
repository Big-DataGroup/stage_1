using System;
using System.IO;
using System.Text.RegularExpressions;

namespace BigDataPipeline.Datamarts
{
    public class MetadataParser
    {
        public static BookMetadata ParseHeader(int bookId, string filePath)
        {
            string title = "Unknown";
            string author = "Unknown";
            string language = "Unknown";

            Regex titlePattern = new Regex("^Title:\\s+(.*)$");
            Regex authorPattern = new Regex("^Author:\\s+(.*)$");
            Regex langPattern = new Regex("^Language:\\s+(.*)$");

            try
            {
                using StreamReader sr = new StreamReader(filePath);
                string line;
                
                while ((line = sr.ReadLine()) != null)
                {
                    Match titleMatch = titlePattern.Match(line);
                    if (titleMatch.Success) title = titleMatch.Groups[1].Value.Trim();

                    Match authorMatch = authorPattern.Match(line);
                    if (authorMatch.Success) author = authorMatch.Groups[1].Value.Trim();

                    Match langMatch = langPattern.Match(line);
                    if (langMatch.Success) language = langMatch.Groups[1].Value.Trim();
                }
            }
            catch (IOException e)
            {
                Console.Error.WriteLine($"Error leyendo la cabecera: {e.Message}");
            }

            return new BookMetadata(bookId, title, author, language);
        }
    }
}