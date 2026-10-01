package org.ulpgc.bigdata.control;

import org.ulpgc.bigdata.datamarts.InvertedIndex;
import org.ulpgc.bigdata.datamarts.storage.FolderHierarchyIndexStorage;
import org.ulpgc.bigdata.datamarts.storage.IndexStorage;
import org.ulpgc.bigdata.datamarts.storage.JsonFileIndexStorage;
import org.ulpgc.bigdata.datamarts.storage.MongoIndexStorage;

import java.util.List;

/**
 * Ejemplo de cómo el Paso 3 (Control Layer + Índice Invertido) se conecta
 * con el trabajo de tus compañeros de los Pasos 1 y 2:
 * <p>
 * 1. GutenbergIngestor descarga los libros en data/datalake/by_book/{id}/{id}.body.txt
 *    y debería llamar a controller.markDownloaded(id) al terminar cada descarga
 *    (basta con añadir esa línea en downloadBook() tras el "return true").
 * 2. Esta clase pregunta al PipelineController qué libros están descargados
 *    pero NO indexados todavía, y solo procesa esos (evita repetir trabajo).
 * 3. Tokeniza cada libro, construye el índice invertido en memoria y lo
 *    persiste en las tres arquitecturas a la vez.
 * 4. Marca cada libro como indexado para que no se repita en la siguiente ejecución.
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
