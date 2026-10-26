import sys
import threading
from pathlib import Path


class PipelineController:
    """
    Capa de control del pipeline.

    Supervisa qué libros han sido descargados (Paso 1) y cuáles ya han sido
    indexados (Paso 3), persistiendo el estado en dos ficheros de texto plano
    (uno ID de libro por línea). Esto evita repetir descargas o indexaciones
    si el proceso se relanza (idempotencia / recovery mode).
    """

    def __init__(self, downloadedFilePath: str, indexedFilePath: str):
        self.downloadedFile = Path(downloadedFilePath)
        self.indexedFile = Path(indexedFilePath)

        self.downloadedBooks = self._loadIds(self.downloadedFile)
        self.indexedBooks = self._loadIds(self.indexedFile)

        # Equivalente a la palabra reservada 'synchronized' de Java
        self._lock = threading.Lock()

    def _loadIds(self, file: Path) -> set:
        """Carga los IDs existentes en el fichero de control, o un set vacío si no existe aún."""
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
        """Marca un libro como descargado. No hace nada si ya estaba marcado (evita duplicados)."""
        with self._lock:
            # En Java, .add() devuelve true si el elemento no existía.
            # En Python no devuelve nada, así que comprobamos antes:
            if bookId not in self.downloadedBooks:
                self.downloadedBooks.add(bookId)
                self._appendId(self.downloadedFile, bookId)

    def markIndexed(self, bookId: int):
        """Marca un libro como indexado. No hace nada si ya estaba marcado."""
        with self._lock:
            if bookId not in self.indexedBooks:
                self.indexedBooks.add(bookId)
                self._appendId(self.indexedFile, bookId)

    def isDownloaded(self, bookId: int) -> bool:
        return bookId in self.downloadedBooks

    def isIndexed(self, bookId: int) -> bool:
        return bookId in self.indexedBooks

    def getBooksPendingIndexing(self) -> list:
        """Libros que están descargados pero todavía no indexados: el trabajo pendiente para el Paso 3."""
        # En Python, el equivalente a Java Streams (.stream().filter(...).sorted().collect(...))
        # es una "list comprehension" combinada con sorted(). Es más rápido y conciso.
        pending = [book_id for book_id in self.downloadedBooks if book_id not in self.indexedBooks]
        return sorted(pending)

    def getDownloadedBooks(self) -> set:
        # Set.copyOf(set) de Java genera una copia. En Python usamos set() sobre el original.
        return set(self.downloadedBooks)

    def getIndexedBooks(self) -> set:
        return set(self.indexedBooks)

    def _appendId(self, file: Path, bookId: int):
        try:
            if file.parent:
                file.parent.mkdir(parents=True, exist_ok=True)

            # El modo 'a' (append) hace exactamente lo mismo que
            # StandardOpenOption.CREATE + StandardOpenOption.APPEND
            with open(file, 'a', encoding='utf-8') as f:
                f.write(f"{bookId}\n")

        except OSError as e:
            print(f"Error escribiendo en fichero de control {file}: {e}", file=sys.stderr)