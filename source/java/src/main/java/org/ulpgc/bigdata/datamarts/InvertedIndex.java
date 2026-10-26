package org.ulpgc.bigdata.datamarts;

import org.ulpgc.bigdata.datamarts.storage.IndexStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Clase que gestiona el Índice Invertido en memoria.
 * Actualizada para soportar construcción inicial vs. actualización incremental (Punto 30)
 * y estimación transparente de huella en memoria (Puntos 16 y 32).
 */
public class InvertedIndex {

    // Mapa que asocia un término (String) con una lista de IDs de libros (List<Integer>)
    private final Map<String, List<Integer>> index;

    public InvertedIndex() {
        this.index = new HashMap<>();
    }

    /**
     * Añade un documento al índice (permite tanto construcción inicial como actualización incremental - Punto 30).
     * Verifica que no se dupliquen IDs de libros en una misma posting list.
     */
    public void addDocument(int bookId, String bodyFilePath) {
        Set<String> words = Tokenizer.tokenize(bodyFilePath);

        for (String word : words) {
            index.putIfAbsent(word, new ArrayList<>());
            List<Integer> postings = index.get(word);

            // Evitar duplicados si se re-indexa el mismo libro incrementalmente
            if (!postings.contains(bookId)) {
                postings.add(bookId);
            }
        }
    }

    /**
     * Busca los IDs de los libros que contienen la palabra consultada.
     */
    public List<Integer> search(String word) {
        return index.getOrDefault(word.toLowerCase(), java.util.Collections.emptyList());
    }

    /**
     * Persiste el índice invertido en los almacenes (storages) indicados.
     */
    public void persistAll(List<IndexStorage> storages) {
        if (storages == null) return;
        for (IndexStorage storage : storages) {
            storage.save(this.getIndex());
        }
    }

    /**
     * Devuelve el mapa subyacente del índice invertido para los storages.
     */
    public Map<String, List<Integer>> getIndex() {
        return index;
    }

    /**
     * Calcula métricas reales de tamaño en memoria del índice (Puntos 16 y 32),
     * sustituyendo las afirmaciones de "cero asignaciones" por datos medibles de huella.
     */
    public MemoryFootprint getMemoryFootprint() {
        int totalTerms = index.size();
        long totalPostings = 0;
        for (List<Integer> postings : index.values()) {
            totalPostings += postings.size();
        }

        // Estimación aproximada de bytes en memoria (estructuras de mapas, strings y listas de enteros)
        long estimatedBytes = (totalTerms * 64L) + (totalPostings * 24L);

        return new MemoryFootprint(totalTerms, totalPostings, estimatedBytes);
    }

    /**
     * Registro auxiliar para la huella de memoria (Puntos 16 y 32).
     */
    public record MemoryFootprint(int totalTerms, long totalPostings, long estimatedBytes) {
        @Override
        public String toString() {
            return String.format("Terms: %d | Postings: %d | Est. Memory: %.2f KB",
                    totalTerms, totalPostings, estimatedBytes / 1024.0);
        }
    }

    // Método para ver el contenido en la consola
    public void printIndex() {
        for (Map.Entry<String, List<Integer>> entry : index.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
    }
}