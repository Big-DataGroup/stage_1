package org.ulpgc.bigdata.datamarts;

import org.ulpgc.bigdata.datamarts.storage.IndexStorage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InvertedIndex {
    // Mapa que asocia un término (String) con una lista de IDs de libros (List<Integer>)
    private final Map<String, List<Integer>> index;

    public InvertedIndex() {
        this.index = new HashMap<>();
    }

    public void addDocument(int bookId, String bodyFilePath) {
        Set<String> words = Tokenizer.tokenize(bodyFilePath);

        for (String word : words) {
            // Si la palabra no existe en el índice, crea una nueva lista vacía
            index.putIfAbsent(word, new ArrayList<>());
            // Añade el ID del libro a la lista de esa palabra
            index.get(word).add(bookId);
        }
        System.out.println("Libro " + bookId + " indexado en memoria correctamente.");
    }

    // Metodo temporal para ver el contenido en la consola
    public void printIndex() {
        for (Map.Entry<String, List<Integer>> entry : index.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
    }

    public void persistAll(List<IndexStorage> storages) {
        for (IndexStorage storage : storages) {
            storage.save(this.index);
        }
    }
}