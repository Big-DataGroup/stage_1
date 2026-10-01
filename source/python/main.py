from datamarts.gutenberg_ingestor import GutenbergIngestor
from control.pipeline_controller import PipelineController
from datamarts.metadata_parser import MetadataParser
from datamarts.metadata_store import MetadataStore
from datamarts.inverted_index import InvertedIndex
from datamarts.storage.json_file_index_storage import JsonFileIndexStorage
from datamarts.storage.folder_hierarchy_index_storage import FolderHierarchyIndexStorage
from datamarts.storage.mongo_index_storage import MongoIndexStorage


def main():
    # 1. Configuración
    # Nota: Si este main.py está en la raíz de 'source/python/', la ruta real sería "../data/..."
    # Lo dejo como "../../data/..." para mantener tu código exacto, pero tenlo en cuenta si da error de ruta.
    datalakeDir = "../../data/datalake"
    controlDir = "../../data/control"
    sampleBooks = [1342, 84, 11, 2701, 1661]

    # 2. Inicializar Control y Metadatos
    controller = PipelineController(
        f"{controlDir}/downloaded_books.txt",
        f"{controlDir}/indexed_books.txt"
    )
    MetadataStore.initializeDatabase()  # Tu base de datos SQLite

    # 3. FASE DE DESCARGA (Persona 1)
    print("=== FASE 1: DESCARGA ===")
    for bookId in sampleBooks:
        success = GutenbergIngestor.downloadBook(bookId, datalakeDir, "by_book")
        if success:
            controller.markDownloaded(bookId)

    # 4. FASE DE PROCESAMIENTO (Persona 2 y Persona 3)
    print("\n=== FASE 2: EXTRACCIÓN E INDEXACIÓN ===")
    pending = controller.getBooksPendingIndexing()

    # Equivalente a pending.isEmpty() en Java
    if not pending:
        print("No hay libros pendientes de procesar.")
        return

    invertedIndex = InvertedIndex()

    for bookId in pending:
        # Rutas exactas que genera la Persona 1
        basePath = f"{datalakeDir}/by_book/{bookId}/{bookId}"
        headerPath = f"{basePath}.header.txt"
        bodyPath = f"{basePath}.body.txt"

        # --- TU PARTE: Metadatos en SQLite ---
        metadata = MetadataParser.parseHeader(bookId, headerPath)
        MetadataStore.insertMetadata(metadata)

        # --- SU PARTE: Índice en memoria ---
        invertedIndex.addDocument(bookId, bodyPath)

        # Anotamos en los ficheros de control
        controller.markIndexed(bookId)

    # 5. FASE DE EXPORTACIÓN (Persona 3)
    print("\n=== FASE 3: PERSISTENCIA DEL ÍNDICE ===")

    # En Python, List.of() es simplemente crear una lista con corchetes []
    # IMPORTANTE: He cambiado '/java/' por '/python/' en las rutas de salida para que
    # cuando ejecutéis la comparativa de Big Data, los resultados no se pisen entre sí.
    storages = [
        JsonFileIndexStorage("../../data/index/python/monolithic/index.json"),
        FolderHierarchyIndexStorage("../../data/index/python/hierarchy"),
        MongoIndexStorage("mongodb://localhost:27017", "gutenberg", "inverted_index")
    ]

    invertedIndex.persistAll(storages)
    print("\nPipeline Big Data completado con éxito.")


# Este es el equivalente al public static void main(String[] args)
if __name__ == "__main__":
    main()