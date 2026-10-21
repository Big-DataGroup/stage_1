package org.ulpgc.bigdata.datamarts;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Tokenizador y Normalizador de texto del Pipeline (Puntos 12 y 20).
 * <p>
 * Reglas de normalización estrictas aplicadas (idénticas para asegurar paridad en C#, Java y Python):
 * 1. <b>Codificación:</b> Lectura de ficheros en formato UTF-8 estricto.
 * 2. <b>Casing:</b> Conversión de todo el texto a minúsculas ({@code toLowerCase()}).
 * 3. <b>Limpieza de caracteres:</b> Sustitución de cualquier carácter que no sea una letra (a-z)
 *    o un dígito (0-9) por un espacio en blanco mediante la expresión regular: {@code [^a-z0-9\\s]}.
 * 4. <b>Tokenización:</b> División por espacios en blanco múltiples ({@code \\s+}).
 * 5. <b>Filtrado:</b> Descarte de tokens vacíos o en blanco ({@code isBlank()}).
 * 6. <b>Estructura de salida:</b> Conjunto de términos únicos por documento ({@code Set<String>}).
 */
public class Tokenizer {

    public static Set<String> tokenize(String filePath) {
        Set<String> uniqueWords = new HashSet<>();
        Path path = Path.of(filePath);

        // Uso de UTF-8 explícito para garantizar paridad multiplataforma (Punto 21)
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                // Aplicar reglas de normalización documentadas
                line = line.toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
                String[] words = line.split("\\s+");

                for (String word : words) {
                    if (!word.isBlank()) {
                        uniqueWords.add(word);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error leyendo el cuerpo del libro en " + filePath + ": " + e.getMessage());
        }

        return uniqueWords;
    }
}