package org.ulpgc.bigdata.datamarts;

import org.ulpgc.bigdata.datamarts.storage.IndexStorage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InvertedIndex {
    private final Map<String, List<Integer>> index;

    public InvertedIndex() {
        this.index = new HashMap<>();
    }

    public void addDocument(int bookId, String bodyFilePath) {
        Set<String> words = Tokenizer.tokenize(bodyFilePath);

        for (String word : words) {
            index.putIfAbsent(word, new ArrayList<>());
            index.get(word).add(bookId);
        }
    }

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

    public List<Integer> search(String word) {
        return index.getOrDefault(word, new ArrayList<>());
    }
}