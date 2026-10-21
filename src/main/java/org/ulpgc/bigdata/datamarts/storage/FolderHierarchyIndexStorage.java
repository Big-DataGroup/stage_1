package org.ulpgc.bigdata.datamarts.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Arquitectura 3: Jerarquía de Carpetas (Puntos 3, 10, 27 y 32).
 * Reparte el índice en subdirectorios por letra inicial (a-z, 0-9, misc).
 * Mide el tamaño total en bytes del directorio y el número de ficheros generados.
 */
public class FolderHierarchyIndexStorage implements IndexStorage {

    private final Path baseDir;

    public FolderHierarchyIndexStorage(String baseDirPath) {
        this.baseDir = Path.of(baseDirPath);
    }

    @Override
    public void save(Map<String, List<Integer>> index) {
        long startTime = System.nanoTime();

        Map<String, Map<String, List<Integer>>> buckets = new HashMap<>();

        for (Map.Entry<String, List<Integer>> entry : index.entrySet()) {
            String bucket = bucketFor(entry.getKey());
            buckets.computeIfAbsent(bucket, b -> new HashMap<>()).put(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, Map<String, List<Integer>>> bucketEntry : buckets.entrySet()) {
            Path bucketFile = baseDir.resolve(bucketEntry.getKey()).resolve("index.json");
            JsonIndexWriter.write(bucketEntry.getValue(), bucketFile);
        }

        long elapsedTime = System.nanoTime() - startTime;

        // Calcular tamaño total y número de ficheros creados en la jerarquía (Punto 27)
        long totalBytes = 0;
        int totalFiles = 0;
        try {
            if (Files.exists(baseDir)) {
                try (Stream<Path> walk = Files.walk(baseDir)) {
                    for (Path p : (Iterable<Path>) walk::iterator) {
                        if (!Files.isDirectory(p)) {
                            totalFiles++;
                            totalBytes += Files.size(p);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error calculando tamaño de la jerarquía de carpetas: " + e.getMessage());
        }

        System.out.printf("[FolderHierarchyIndexStorage] Hierarchy saved -> Path: %s | Buckets/Files: %d | Total Size: %.2f KB | Time: %.2f ms%n",
                baseDir, totalFiles, totalBytes / 1024.0, elapsedTime / 1_000_000.0);
    }

    private String bucketFor(String term) {
        if (term == null || term.isEmpty()) {
            return "misc";
        }
        char c = Character.toLowerCase(term.charAt(0));
        if (Character.isLetter(c) && c <= 'z' && c >= 'a') {
            return String.valueOf(c);
        }
        if (Character.isDigit(c)) {
            return "0-9";
        }
        return "misc";
    }
}