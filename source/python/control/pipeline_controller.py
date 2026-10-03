import sys
import threading
from pathlib import Path


class PipelineController:
    def __init__(self, downloadedFilePath: str, indexedFilePath: str):
        self.downloadedFile = Path(downloadedFilePath)
        self.indexedFile = Path(indexedFilePath)

        self.downloadedBooks = self._loadIds(self.downloadedFile)
        self.indexedBooks = self._loadIds(self.indexedFile)

        self._lock = threading.Lock()

    def _loadIds(self, file: Path) -> set:
        ids = set()
        if not file.exists():
            return ids

        try:
            with open(file, 'r', encoding='utf-8') as f:
                for line in f:
                    line = line.strip()
                    if line:
                        ids.add(int(line))
        except OSError as e:
            print(f"Error leyendo fichero de control {file}: {e}", file=sys.stderr)
        except ValueError as e:
            # Captura el equivalente a NumberFormatException
            print(f"Entrada inválida en fichero de control {file}: {e}", file=sys.stderr)

        return ids

    def markDownloaded(self, bookId: int):
        with self._lock:
            if bookId not in self.downloadedBooks:
                self.downloadedBooks.add(bookId)
                self._appendId(self.downloadedFile, bookId)

    def markIndexed(self, bookId: int):
        with self._lock:
            if bookId not in self.indexedBooks:
                self.indexedBooks.add(bookId)
                self._appendId(self.indexedFile, bookId)

    def isDownloaded(self, bookId: int) -> bool:
        return bookId in self.downloadedBooks

    def isIndexed(self, bookId: int) -> bool:
        return bookId in self.indexedBooks

    def getBooksPendingIndexing(self) -> list:
        pending = [book_id for book_id in self.downloadedBooks if book_id not in self.indexedBooks]
        return sorted(pending)

    def getDownloadedBooks(self) -> set:
        return set(self.downloadedBooks)

    def getIndexedBooks(self) -> set:
        return set(self.indexedBooks)

    def _appendId(self, file: Path, bookId: int):
        try:
            if file.parent:
                file.parent.mkdir(parents=True, exist_ok=True)

            with open(file, 'a', encoding='utf-8') as f:
                f.write(f"{bookId}\n")

        except OSError as e:
            print(f"Error escribiendo en fichero de control {file}: {e}", file=sys.stderr)