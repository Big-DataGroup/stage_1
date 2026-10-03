package org.ulpgc.bigdata.datamarts.storage;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;


public class JsonFileIndexStorage implements IndexStorage {

    private final Path outputFile;

    public JsonFileIndexStorage(String outputFilePath) {
        this.outputFile = Path.of(outputFilePath);
    }

    @Override
    public void save(Map<String, List<Integer>> index) {
        JsonIndexWriter.write(index, outputFile);
        System.out.println("Índice monolítico JSON escrito en " + outputFile
                + " (" + index.size() + " términos).");
    }
}
