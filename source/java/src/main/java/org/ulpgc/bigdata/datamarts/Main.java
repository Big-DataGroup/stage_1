package org.ulpgc.bigdata.datamarts;

import org.ulpgc.bigdata.control.PipelineController;
import org.ulpgc.bigdata.datamarts.storage.*;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        String datalakeDir = "../../data/datalake";
        String controlDir = "../../data/control";
        int[] sampleBooks = {1342, 84, 11, 2701, 1661};

        PipelineController controller = new PipelineController(
                controlDir + "/downloaded_books.txt",
                controlDir + "/indexed_books.txt"
        );
        MetadataStore.initializeDatabase();

        System.out.println("=== FASE 1: DESCARGA ===");
        for (int bookId : sampleBooks) {
            boolean success = GutenbergIngestor.downloadBook(bookId, datalakeDir, "by_book");
            if (success) {
                controller.markDownloaded(bookId);
            }
        }

        System.out.println("\n=== FASE 2: EXTRACCIÓN E INDEXACIÓN ===");
        List<Integer> pending = controller.getBooksPendingIndexing();

        if (pending.isEmpty()) {
            System.out.println("No hay libros pendientes de procesar.");
            return;
        }

        InvertedIndex invertedIndex = new InvertedIndex();

        for (int bookId : pending) {
            String basePath = datalakeDir + "/by_book/" + bookId + "/" + bookId;
            String headerPath = basePath + ".header.txt";
            String bodyPath = basePath + ".body.txt";

            BookMetadata metadata = MetadataParser.parseHeader(bookId, headerPath);
            MetadataStore.insertMetadata(metadata);

            invertedIndex.addDocument(bookId, bodyPath);

            controller.markIndexed(bookId);
        }

        System.out.println("\n=== FASE 3: PERSISTENCIA DEL ÍNDICE ===");
        List<IndexStorage> storages = List.of(
                new JsonFileIndexStorage("../../data/index/java/monolithic/index.json"),
                new FolderHierarchyIndexStorage("../../data/index/java/hierarchy"),
                new MongoIndexStorage("mongodb://localhost:27017", "gutenberg", "inverted_index")
        );

        invertedIndex.persistAll(storages);
        System.out.println("\nPipeline Big Data completado con éxito.");
    }
}