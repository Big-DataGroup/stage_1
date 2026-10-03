from pathlib import Path
from datamarts.storage.index_storage import IndexStorage
from datamarts.storage.json_index_writer import JsonIndexWriter


class JsonFileIndexStorage(IndexStorage):
    def __init__(self, outputFilePath: str):
        self.outputFile = Path(outputFilePath)

    def save(self, index: dict):
        JsonIndexWriter.write(index, self.outputFile)
        print(f"Índice monolítico JSON escrito en {self.outputFile} "
              f"({len(index)} términos).")