from pathlib import Path
# Asumimos que IndexStorage y JsonIndexWriter estarán en esta misma carpeta
from datamarts.storage.index_storage import IndexStorage
from datamarts.storage.json_index_writer import JsonIndexWriter


class FolderHierarchyIndexStorage(IndexStorage):
    """
    Arquitectura 3: jerarquía de carpetas.

    En lugar de un único fichero gigante, se reparte el índice en varios
    ficheros más pequeños agrupando los términos por su letra inicial:

    index/
      a/index.json
      b/index.json
      ...
      0-9/index.json   (términos que empiezan por dígito)
      misc/index.json  (cualquier otro caso raro)

    Esto reduce el tamaño de cada fichero y evita reescribir todo el índice
    cuando solo cambian unos pocos términos de una letra concreta.
    """

    def __init__(self, baseDirPath: str):
        # Path.of() de Java es directamente Path() en Python
        self.baseDir = Path(baseDirPath)

    def save(self, index: dict):
        # buckets será el equivalente a Map<String, Map<String, List<Integer>>>
        buckets = {}

        # entrySet() de Java es items() en Python
        for key, value in index.items():
            bucket = self.bucketFor(key)
            # computeIfAbsent().put() se traduce mágicamente en Python combinando setdefault y asignación
            buckets.setdefault(bucket, {})[key] = value

        for bucketKey, bucketValue in buckets.items():
            # baseDir.resolve().resolve() se hace con el operador / gracias a pathlib
            bucketFile = self.baseDir / bucketKey / "index.json"
            JsonIndexWriter.write(bucketValue, bucketFile)

        print(f"Índice en jerarquía de carpetas escrito bajo {self.baseDir} "
              f"({len(buckets)} carpetas, {len(index)} términos).")

    def bucketFor(self, term: str) -> str:
        # En Python, 'if not term:' cubre tanto si es None como si es un String vacío ("")
        if not term:
            return "misc"

        c = term[0].lower()

        # En Python podemos encadenar comparaciones lógicas directamente
        if 'a' <= c <= 'z':
            return c

        # Character.isDigit(c) en Java equivale al método de string .isdigit()
        if c.isdigit():
            return "0-9"

        return "misc"