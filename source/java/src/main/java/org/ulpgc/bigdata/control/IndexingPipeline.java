package org.ulpgc.bigdata.control;

import org.ulpgc.bigdata.datamarts.InvertedIndex;
import org.ulpgc.bigdata.datamarts.storage.FolderHierarchyIndexStorage;
import org.ulpgc.bigdata.datamarts.storage.IndexStorage;
import org.ulpgc.bigdata.datamarts.storage.JsonFileIndexStorage;
import org.ulpgc.bigdata.datamarts.storage.MongoIndexStorage;

import java.util.List;

/**
 * Pipeline de Indexación corregido para resolver el Punto 44 (Consistencia y orden de persistencia)
 * y el control de tiempos robustos (Punto 4 y 42).
 */
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

        System.out.println("=== INICIANDO PIPELINE DE INDEXACIÓN PARA " + pending.size() + " LIBRO(S) ===");
        long startTime = System.nanoTime();

        InvertedIndex invertedIndex = new InvertedIndex();

        // 1. Procesar y tokenizar cada libro pendiente en memoria
        for (int bookId : pending) {
            String bodyPath = "data/datalake/by_book/" + bookId + "/" + bookId + ".body.txt";
            invertedIndex.addDocument(bookId, bodyPath);
        }

        // 2. Definir las tres arquitecturas de almacenamiento evaluadas
        List<IndexStorage> storages = List.of(
                new JsonFileIndexStorage("data/index/monolithic/index.json"),
                new FolderHierarchyIndexStorage("data/index/hierarchy")
                //new MongoIndexStorage("mongodb://localhost:27017", "gutenberg", "inverted_index")
        );

        // 3. PERSISTIR PRIMERO: Garantiza que si falla la escritura, el control no da por bueno el índice (Punto 44)
        try {
            for (IndexStorage storage : storages) {
                storage.save(invertedIndex.getIndex());
            }

            // 4. SOLO DESPUÉS DE LA PERSISTENCIA EXITOSA: Marcar como indexados en el control
            for (int bookId : pending) {
                controller.markIndexed(bookId);
            }

            long elapsedTime = System.nanoTime() - startTime;
            System.out.printf("=== INDEXACIÓN Y PERSISTENCIA COMPLETADAS EXITOSAMENTE en %.2f ms ===%n",
                    elapsedTime / 1_000_000.0);

        } catch (Exception e) {
            System.err.println("ERROR CRÍTICO: Falló la persistencia del índice. Los libros NO se marcan como indexados para permitir recuperación (Recovery Mode): " + e.getMessage());
        } finally {
            // Cierre seguro de conexiones (ej. MongoDB)
            for (IndexStorage storage : storages) {
                if (storage instanceof AutoCloseable c) {
                    try {
                        c.close();
                    } catch (Exception ignored) {}
                }
            }
        }
    }
}