from pathlib import Path
from datamarts.storage.index_storage import IndexStorage
from datamarts.storage.json_index_writer import JsonIndexWriter


class FolderHierarchyIndexStorage(IndexStorage):
    def __init__(self, baseDirPath: str):
        self.baseDir = Path(baseDirPath)

    def save(self, index: dict):
        buckets = {}

        for key, value in index.items():
            bucket = self.bucketFor(key)
            buckets.setdefault(bucket, {})[key] = value

        for bucketKey, bucketValue in buckets.items():
            bucketFile = self.baseDir / bucketKey / "index.json"
            JsonIndexWriter.write(bucketValue, bucketFile)

        print(f"Índice en jerarquía de carpetas escrito bajo {self.baseDir} "
              f"({len(buckets)} carpetas, {len(index)} términos).")

    def bucketFor(self, term: str) -> str:
        if not term:
            return "misc"

        c = term[0].lower()

        if 'a' <= c <= 'z':
            return c

        if c.isdigit():
            return "0-9"

        return "misc"