from datamarts.gutenberg_ingestor import GutenbergIngestor
from control.pipeline_controller import PipelineController
from datamarts.metadata_parser import MetadataParser
from datamarts.metadata_store import MetadataStore
from datamarts.inverted_index import InvertedIndex
from datamarts.storage.json_file_index_storage import JsonFileIndexStorage
from datamarts.storage.folder_hierarchy_index_storage import FolderHierarchyIndexStorage
from datamarts.storage.mongo_index_storage import MongoIndexStorage


def main():
    datalakeDir = "../../data/datalake"
    controlDir = "../../data/control"
    sampleBooks = [1342, 84, 11, 2701, 1661]

    controller = PipelineController(
        f"{controlDir}/downloaded_books.txt",
        f"{controlDir}/indexed_books.txt"
    )
    MetadataStore.initializeDatabase()

    print("=== FASE 1: DESCARGA ===")
    ingestor = GutenbergIngestor(base_output_dir=datalakeDir)
    for bookId in sampleBooks:
        success = ingestor.download_book(bookId, "by_book")
        if success:
            controller.markDownloaded(bookId)

    print("\n=== FASE 2: EXTRACCIÓN E INDEXACIÓN ===")
    pending = controller.getBooksPendingIndexing()

    if not pending:
        print("No hay libros pendientes de procesar.")
        return

    invertedIndex = InvertedIndex()

    for bookId in pending:
        basePath = f"{datalakeDir}/by_book/{bookId}/{bookId}"
        headerPath = f"{basePath}.header.txt"
        bodyPath = f"{basePath}.body.txt"

        metadata = MetadataParser.parseHeader(bookId, headerPath)
        MetadataStore.insertMetadata(metadata)

        invertedIndex.addDocument(bookId, bodyPath)

        controller.markIndexed(bookId)

    print("\n=== FASE 3: PERSISTENCIA DEL ÍNDICE ===")

    storages = [
        JsonFileIndexStorage("../../data/index/python/monolithic/index.json"),
        FolderHierarchyIndexStorage("../../data/index/python/hierarchy"),
        MongoIndexStorage("mongodb://localhost:27017", "gutenberg", "inverted_index")
    ]

    invertedIndex.persistAll(storages)
    print("\nPipeline Big Data completado con éxito.")


if __name__ == "__main__":
    main()