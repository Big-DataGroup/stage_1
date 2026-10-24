package org.ulpgc.bigdata.datamarts.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Arquitectura 1: Archivo Monolítico JSON (Puntos 3, 10, 27 y 32).
 * Vuelca todo el índice en un único fichero, midiendo el tamaño real en bytes y el tiempo de escritura.
 */
public class JsonFileIndexStorage implements IndexStorage {

    private final Path outputFile;

    public JsonFileIndexStorage(String outputFilePath) {
        this.outputFile = Path.of(outputFilePath);
    }

    @Override
    public void save(Map<String, List<Integer>> index) {
        long startTime = System.nanoTime();

        JsonIndexWriter.write(index, outputFile);

        long elapsedTime = System.nanoTime() - startTime;
        long fileSizeInBytes = 0;
        try {
            if (Files.exists(outputFile)) {
                fileSizeInBytes = Files.size(outputFile);
            }
        } catch (IOException e) {
            System.err.println("No se pudo calcular el tamaño del fichero monolítico: " + e.getMessage());
        }

        //System.out.println("[JsonFileIndexStorage] Monolithic JSON saved -> Path: " + outputFile.toString()
             //   + " | Terms: " + index.size()
             //   + " | Size: " + String.format("%.2f", fileSizeInBytes / 1024.0) + " KB"
             //   + " | Time: " + String.format("%.2f", elapsedTime / 1_000_000.0) + " ms");
    }
}
