from pathlib import Path
from datamarts.storage.index_storage import IndexStorage
from datamarts.storage.json_index_writer import JsonIndexWriter


class JsonFileIndexStorage(IndexStorage):
    """
    Arquitectura 1: archivo monolítico.
    Vuelca TODO el índice invertido en un único fichero JSON, con la forma:

    {
       "ballena": [1, 84],
       "capitan": [1, 1661]
    }

    Sencillo de implementar y de leer, pero no escala bien si el vocabulario
    es muy grande (el fichero crece indefinidamente y hay que reescribirlo entero).
    """

    def __init__(self, outputFilePath: str):
        self.outputFile = Path(outputFilePath)

    def save(self, index: dict):
        JsonIndexWriter.write(index, self.outputFile)
        print(f"Índice monolítico JSON escrito en {self.outputFile} "
              f"({len(index)} términos).")