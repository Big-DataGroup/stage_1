package org.ulpgc.bigdata.datamarts;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetadataParser {

    public static BookMetadata parseHeader(int bookId, String filePath) {
        String title = "Unknown";
        String author = "Unknown";
        String language = "Unknown";

        // Expresiones regulares para extraer los campos solicitados[cite: 1]
        Pattern titlePattern = Pattern.compile("^Title:\\s+(.*)$");
        Pattern authorPattern = Pattern.compile("^Author:\\s+(.*)$");
        Pattern langPattern = Pattern.compile("^Language:\\s+(.*)$");

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                Matcher titleMatcher = titlePattern.matcher(line);
                if (titleMatcher.find()) title = titleMatcher.group(1).trim();

                Matcher authorMatcher = authorPattern.matcher(line);
                if (authorMatcher.find()) author = authorMatcher.group(1).trim();

                Matcher langMatcher = langPattern.matcher(line);
                if (langMatcher.find()) language = langMatcher.group(1).trim();
            }
        } catch (IOException e) {
            System.err.println("Error leyendo la cabecera: " + e.getMessage());
        }

        return new BookMetadata(bookId, title, author, language);
    }
}