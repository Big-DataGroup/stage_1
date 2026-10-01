package org.ulpgc.bigdata.datamarts.storage;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utilidad interna para volcar un mapa término -> [IDs] a un fichero JSON,
 * sin depender de librerías externas (Gson/Jackson). Si el proyecto ya
 * incluye una de esas librerías, se puede sustituir por ella sin tocar
 * el resto del código: solo la usan {@link JsonFileIndexStorage} y
 * {@link FolderHierarchyIndexStorage}.
 */
final class JsonIndexWriter {

    private JsonIndexWriter() {
    }

    static void write(Map<String, List<Integer>> index, Path outputFile) {
        try {
            if (outputFile.getParent() != null) {
                Files.createDirectories(outputFile.getParent());
            }

            // TreeMap para que la salida sea determinista (términos ordenados alfabéticamente)
            Map<String, List<Integer>> sorted = new TreeMap<>(index);

            try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
                writer.write("{\n");
                int i = 0;
                int total = sorted.size();
                for (Map.Entry<String, List<Integer>> entry : sorted.entrySet()) {
                    writer.write("  \"" + escape(entry.getKey()) + "\": [");
                    List<Integer> ids = entry.getValue();
                    for (int j = 0; j < ids.size(); j++) {
                        writer.write(String.valueOf(ids.get(j)));
                        if (j < ids.size() - 1) writer.write(", ");
                    }
                    writer.write("]");
                    i++;
                    if (i < total) writer.write(",");
                    writer.write("\n");
                }
                writer.write("}\n");
            }
        } catch (IOException e) {
            System.err.println("Error escribiendo índice JSON en " + outputFile + ": " + e.getMessage());
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
