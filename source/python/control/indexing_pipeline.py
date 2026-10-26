from control.pipeline_controller import PipelineController
from datamarts.inverted_index import InvertedIndex
from datamarts.storage.json_file_index_storage import JsonFileIndexStorage
from datamarts.storage.folder_hierarchy_index_storage import FolderHierarchyIndexStorage
from datamarts.storage.mongo_index_storage import MongoIndexStorage


class IndexingPipeline:
    """
    Ejemplo de cómo el Paso 3 (Control Layer + Índice Invertido) se conecta
    con el trabajo de tus compañeros de los Pasos 1 y 2:

    1. GutenbergIngestor descarga los libros en data/datalake/by_book/{id}/{id}.body.txt
       y debería llamar a controller.markDownloaded(id) al terminar cada descarga
       (basta con añadir esa línea en downloadBook() tras el "return True").
    2. Esta clase pregunta al PipelineController qué libros están descargados
       pero NO indexados todavía, y solo procesa esos (evita repetir trabajo).
    3. Tokeniza cada libro, construye el índice invertido en memoria y lo
       persiste en las tres arquitecturas a la vez.
    4. Marca cada libro como indexado para que no se repita en la siguiente ejecución.
    """

    @staticmethod
    def main():
        controller = PipelineController(
            "data/control/downloaded_books.txt",
            "data/control/indexed_books.txt"
        )

        pending = controller.getBooksPendingIndexing()

        # Equivalente a pending.isEmpty()
        if not pending:
            print("No hay libros pendientes de indexar.")
            return

        invertedIndex = InvertedIndex()

        for bookId in pending:
            bodyPath = f"data/datalake/by_book/{bookId}/{bookId}.body.txt"
            invertedIndex.addDocument(bookId, bodyPath)
            controller.markIndexed(bookId)

        storages = [
            JsonFileIndexStorage("data/index/monolithic/index.json"),
            FolderHierarchyIndexStorage("data/index/hierarchy"),
            MongoIndexStorage("mongodb://localhost:27017", "gutenberg", "inverted_index")
        ]

        invertedIndex.persistAll(storages)

        print(f"Indexación completada para {len(pending)} libro(s).")


# Esto permite ejecutar este archivo directamente igual que el main de Java
if __name__ == "__main__":
    IndexingPipeline.main()