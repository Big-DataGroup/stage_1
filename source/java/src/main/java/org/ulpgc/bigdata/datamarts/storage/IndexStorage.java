package org.ulpgc.bigdata.datamarts.storage;

import java.util.List;
import java.util.Map;

/**
 * Estrategia de persistencia para el índice invertido (patrón Strategy).
 * Cada implementación decide CÓMO se guarda físicamente el índice
 * (JSON monolítico, MongoDB, jerarquía de carpetas...), sin que
 * {@code InvertedIndex} tenga que conocer los detalles.
 */
public interface IndexStorage {

    /**
     * Persiste el índice completo.
     *
     * @param index mapa término -> lista de IDs de libros donde aparece
     */
    void save(Map<String, List<Integer>> index);
}