package org.ulpgc.bigdata.datamarts.storage;

import org.ulpgc.bigdata.datamarts.InvertedIndex;
import java.util.List;
import java.util.Map;

public interface IndexStorage {

    /**
     * Persiste el mapa término -> lista de IDs de libros.
     */
    void save(Map<String, List<Integer>> index);

    /**
     * Método por defecto para guardar directamente a partir de un InvertedIndex.
     */
    default void save(InvertedIndex invertedIndex) {
        if (invertedIndex != null) {
            save(invertedIndex.getIndex());
        }
    }
}