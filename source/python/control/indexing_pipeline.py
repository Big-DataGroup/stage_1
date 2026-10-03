from control.pipeline_controller import PipelineController
from datamarts.inverted_index import InvertedIndex
from datamarts.storage.json_file_index_storage import JsonFileIndexStorage
from datamarts.storage.folder_hierarchy_index_storage import FolderHierarchyIndexStorage
from datamarts.storage.mongo_index_storage import MongoIndexStorage


class IndexingPipeline:
    @staticmethod
    def main():
        controller = PipelineController(
            "data/control/downloaded_books.txt",
            "data/control/indexed_books.txt"
        )

        pending = controller.getBooksPendingIndexing()

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


if __name__ == "__main__":
    IndexingPipeline.main()