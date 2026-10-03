package org.ulpgc.bigdata.control;

import org.ulpgc.bigdata.datamarts.InvertedIndex;
import org.ulpgc.bigdata.datamarts.storage.FolderHierarchyIndexStorage;
import org.ulpgc.bigdata.datamarts.storage.IndexStorage;
import org.ulpgc.bigdata.datamarts.storage.JsonFileIndexStorage;
import org.ulpgc.bigdata.datamarts.storage.MongoIndexStorage;

import java.util.List;

public class IndexingPipeline {

    public static void main(String[] args) {
        PipelineController controller = new PipelineController(
                "data/control/downloaded_books.txt",
                "data/control/indexed_books.txt"
        );

        List<Integer> pending = controller.getBooksPendingIndexing();
        if (pending.isEmpty()) {
            System.out.println("No hay libros pendientes de indexar.");
            return;
        }

        InvertedIndex invertedIndex = new InvertedIndex();

        for (int bookId : pending) {
            String bodyPath = "data/datalake/by_book/" + bookId + "/" + bookId + ".body.txt";
            invertedIndex.addDocument(bookId, bodyPath);
            controller.markIndexed(bookId);
        }

        List<IndexStorage> storages = List.of(
                new JsonFileIndexStorage("data/index/monolithic/index.json"),
                new FolderHierarchyIndexStorage("data/index/hierarchy"),
                new MongoIndexStorage("mongodb://localhost:27017", "gutenberg", "inverted_index")
        );

        invertedIndex.persistAll(storages);

        System.out.println("Indexación completada para " + pending.size() + " libro(s).");
    }
}
