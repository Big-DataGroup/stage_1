package org.ulpgc.bigdata.datamarts.storage;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FolderHierarchyIndexStorage implements IndexStorage {

    private final Path baseDir;

    public FolderHierarchyIndexStorage(String baseDirPath) {
        this.baseDir = Path.of(baseDirPath);
    }

    @Override
    public void save(Map<String, List<Integer>> index) {
        Map<String, Map<String, List<Integer>>> buckets = new HashMap<>();

        for (Map.Entry<String, List<Integer>> entry : index.entrySet()) {
            String bucket = bucketFor(entry.getKey());
            buckets.computeIfAbsent(bucket, b -> new HashMap<>()).put(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, Map<String, List<Integer>>> bucketEntry : buckets.entrySet()) {
            Path bucketFile = baseDir.resolve(bucketEntry.getKey()).resolve("index.json");
            JsonIndexWriter.write(bucketEntry.getValue(), bucketFile);
        }

        System.out.println("Índice en jerarquía de carpetas escrito bajo " + baseDir
                + " (" + buckets.size() + " carpetas, " + index.size() + " términos).");
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
