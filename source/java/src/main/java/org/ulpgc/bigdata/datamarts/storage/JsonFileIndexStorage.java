package org.ulpgc.bigdata.datamarts.storage;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Arquitectura 1: archivo monolítico.
 * Vuelca TODO el índice invertido en un único fichero JSON, con la forma:
 * <pre>
 * {
 *   "ballena": [1, 84],
 *   "capitan": [1, 1661]
 * }
 * </pre>
 * Sencillo de implementar y de leer, pero no escala bien si el vocabulario
 * es muy grande (el fichero crece indefinidamente y hay que reescribirlo entero).
 */
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
