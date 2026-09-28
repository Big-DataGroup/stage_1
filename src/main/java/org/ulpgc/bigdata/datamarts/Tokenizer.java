package org.ulpgc.bigdata.datamarts;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class Tokenizer {

    public static Set<String> tokenize(String filePath) {
        Set<String> uniqueWords = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Convertir a minusculas y reemplazar_todo lo que no sea letra o numero por espacios
                line = line.toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
                String[] words = line.split("\\s+");

                for (String word : words) {
                    if (!word.isBlank()) {
                        uniqueWords.add(word);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error leyendo el cuerpo del libro: " + e.getMessage());
        }

        return uniqueWords;
    }
}