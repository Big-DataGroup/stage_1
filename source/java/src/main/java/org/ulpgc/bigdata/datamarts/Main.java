package org.ulpgc.bigdata.datamarts;

import DATALAKE.GutenbergIngestor;
import org.ulpgc.bigdata.control.PipelineController;
import org.ulpgc.bigdata.datamarts.storage.*;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        // 1. Configuración
        String datalakeDir = "../../data/datalake";
        String controlDir = "../../data/control";
        int[] sampleBooks = {1342, 84, 11, 2701, 1661};

        // 2. Inicializar Control y Metadatos
        PipelineController controller = new PipelineController(
                controlDir + "/downloaded_books.txt",
                controlDir + "/indexed_books.txt"
        );
        MetadataStore.initializeDatabase(); // Tu base de datos SQLite

        // 3. FASE DE DESCARGA (Persona 1)
        System.out.println("=== FASE 1: DESCARGA ===");
        for (int bookId : sampleBooks) {
            boolean success = GutenbergIngestor.downloadBook(bookId, datalakeDir, "by_book");
            if (success) {
                controller.markDownloaded(bookId);
            }
        }

        // 4. FASE DE PROCESAMIENTO (Persona 2 y Persona 3)
        System.out.println("\n=== FASE 2: EXTRACCIÓN E INDEXACIÓN ===");
        List<Integer> pending = controller.getBooksPendingIndexing();

        if (pending.isEmpty()) {
            System.out.println("No hay libros pendientes de procesar.");
            return;
        }

        InvertedIndex invertedIndex = new InvertedIndex();

        for (int bookId : pending) {
            // Rutas exactas que genera la Persona 1
            String basePath = datalakeDir + "/by_book/" + bookId + "/" + bookId;
            String headerPath = basePath + ".header.txt";
            String bodyPath = basePath + ".body.txt";

            // --- TU PARTE: Metadatos en SQLite ---
            BookMetadata metadata = MetadataParser.parseHeader(bookId, headerPath);
            MetadataStore.insertMetadata(metadata);

            // --- SU PARTE: Índice en memoria ---
            invertedIndex.addDocument(bookId, bodyPath);

            // Anotamos en los ficheros de control
            controller.markIndexed(bookId);
        }

        // 5. FASE DE EXPORTACIÓN (Persona 3)
        System.out.println("\n=== FASE 3: PERSISTENCIA DEL ÍNDICE ===");
        List<IndexStorage> storages = List.of(
                new JsonFileIndexStorage("../../data/index/monolithic/index.json"),
                new FolderHierarchyIndexStorage("../../data/index/hierarchy"),
                new MongoIndexStorage("mongodb://localhost:27017", "gutenberg", "inverted_index")
        );

        invertedIndex.persistAll(storages);
        System.out.println("\nPipeline Big Data completado con éxito.");
    }
}