package org.ulpgc.bigdata.datamarts;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetadataParser {

    public static BookMetadata parseHeader(int bookId, String filePath) {
        String title = "Unknown";
        String author = "Unknown";
        String language = "Unknown";

        // Expresiones regulares para extraer los campos solicitados[cite: 1]
        Pattern titlePattern = Pattern.compile("^Title:\\s+(.*)$", Pattern.CASE_INSENSITIVE);
        Pattern authorPattern = Pattern.compile("^Author:\\s+(.*)$", Pattern.CASE_INSENSITIVE);
        Pattern langPattern = Pattern.compile("^Language:\\s+(.*)$", Pattern.CASE_INSENSITIVE);

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
            System.err.println("Error leyendo la cabecera para el libro " + bookId + ": " + e.getMessage());
        }

        return new BookMetadata(bookId, title, author, language);
    }

    /**
     * Método main para probar y verificar el parseo de metadatos e inserción en SQLite (Punto 12).
     */
    public static void main(String[] args) {
        System.out.println("--- INICIANDO PRUEBA DE METADATOS Y SQLITE ---");

        // 1. Inicializar la base de datos SQLite
        MetadataStore.initializeDatabase();

        // 2. Probar con los libros que ya tenemos descargados en el datalake
        int[] sampleBooks = {1342, 84, 11, 2701, 1661};

        for (int bookId : sampleBooks) {
            Path headerPath = Path.of("data", "datalake", "by_book", String.valueOf(bookId), bookId + ".header.txt");

            if (Files.exists(headerPath)) {
                BookMetadata metadata = parseHeader(bookId, headerPath.toString());
                System.out.printf("Parsed -> ID: %d | Title: %s | Author: %s | Lang: %s%n",
                        metadata.bookId(), metadata.title(), metadata.author(), metadata.language());

                // Insertar en SQLite
                MetadataStore.insertMetadata(metadata);
            } else {
                System.out.println("No se encontró la cabecera para el libro " + bookId + " en " + headerPath);
            }
        }
        System.out.println("=== PRUEBA DE METADATOS FINALIZADA ===");
    }
}