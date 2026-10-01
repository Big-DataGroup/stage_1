import urllib.request
from urllib.error import URLError
from pathlib import Path
from datetime import datetime
import time
import sys


class GutenbergIngestor:
    START_MARKER = "*** START OF THE PROJECT GUTENBERG EBOOK"
    END_MARKER = "*** END OF THE PROJECT GUTENBERG EBOOK"

    @staticmethod
    def downloadBook(bookId: int, baseOutputDir: str, strategy: str) -> bool:
        # Resolve the output directory based on the selected strategy
        outputDir = GutenbergIngestor.resolveDatalakePath(Path(baseOutputDir), bookId, strategy)

        # Define the file paths using the required nomenclature
        # En Python, pathlib permite unir rutas directamente con el operador '/'
        bodyPath = outputDir / f"{bookId}.body.txt"
        headerPath = outputDir / f"{bookId}.header.txt"

        # Check if files already exist to avoid duplicate network requests
        if bodyPath.exists() and headerPath.exists():
            print(f"Skipping book {bookId}: Files already exist (Recovery Mode)")
            return True

        url = f"https://www.gutenberg.org/cache/epub/{bookId}/pg{bookId}.txt"

        try:
            # Configurar y lanzar la petición HTTP (sigue redirecciones por defecto)
            req = urllib.request.Request(url, method="GET")
            with urllib.request.urlopen(req) as response:
                if response.status != 200:
                    print(f"Error HTTP {response.status} for the book ID: {bookId}", file=sys.stderr)
                    return False

                text = response.read().decode('utf-8')

            # Check if the Gutenberg markers exist in the text
            if GutenbergIngestor.START_MARKER not in text or GutenbergIngestor.END_MARKER not in text:
                print(f"Book not found: {bookId}", file=sys.stderr)
                return False

            # Split text to extract header and body
            # En Java .split(regex, 2) corta el string en 2 trozos.
            # En Python .split(sep, 1) significa "haz 1 corte" (generando también 2 trozos).
            # Tampoco hace falta regex.quote() porque el split de Python usa cadenas literales por defecto.
            parts1 = text.split(GutenbergIngestor.START_MARKER, 1)
            header = parts1[0]

            parts2 = parts1[1].split(GutenbergIngestor.END_MARKER, 1)
            body = parts2[0]

            # Create directories if they do not exist (equivalente a Files.createDirectories)
            outputDir.mkdir(parents=True, exist_ok=True)

            # Write the extracted text into the files
            bodyPath.write_text(body.strip(), encoding='utf-8')
            headerPath.write_text(header.strip(), encoding='utf-8')

            print(f"Book {bookId} saved using [{strategy}]")
            return True

        except (URLError, OSError) as e:
            # URLError captura fallos de red y OSError captura fallos de disco (IOException)
            print(f"Network or disk error while processing the book {bookId}: {e}", file=sys.stderr)
            return False

    @staticmethod
    def resolveDatalakePath(baseDir: Path, bookId: int, strategy: str) -> Path:
        strat = strategy.lower()

        if strat == "by_book":
            return baseDir / "by_book" / str(bookId)

        elif strat == "by_batch":
            # En Python, el operador // hace la división entera (truncada) igual que en Java con ints
            lowerBound = (bookId // 1000) * 1000
            upperBound = lowerBound + 999
            batchName = f"{lowerBound}-{upperBound}"
            return baseDir / "by_batch" / batchName

        elif strat == "by_time":
            now = datetime.now()
            # En Python no necesitamos un objeto DateTimeFormatter separado,
            # el formateo se aplica directamente sobre el objeto fecha
            dateFolder = now.strftime("%Y%m%d")
            hourFolder = now.strftime("%H")
            return baseDir / "by_time" / dateFolder / hourFolder

        else:
            raise ValueError(f"Unknown datalake strategy: {strategy}")

    # --- BATCH DOWNLOAD FEATURE ---
    @staticmethod
    def downloadBatch(bookIds: list, baseOutputDir: str, strategy: str):
        print("=== STARTING BATCH DOWNLOAD ===")
        successCount = 0

        for bookId in bookIds:
            success = GutenbergIngestor.downloadBook(bookId, baseOutputDir, strategy)
            if success:
                successCount += 1

            # sleep for 200ms between requests
            try:
                # Java recibe milisegundos (200), Python recibe segundos (0.2)
                time.sleep(0.2)
            except KeyboardInterrupt:
                # Equivalente a capturar InterruptedException en Java
                print("Batch interrupted.", file=sys.stderr)
                break

        print(f"=== BATCH COMPLETED: {successCount}/{len(bookIds)} SUCCESSFUL ===")


if __name__ == "__main__":
    sampleBooks = [1342, 84, 11, 2701, 1661]
    strategies = ["by_book", "by_time", "by_batch"]

    print("--- Generating Sample Dataset ---")

    for strategy in strategies:
        print(f"\n-> Executing strategy: {strategy}")
        GutenbergIngestor.downloadBatch(sampleBooks, "data/datalake", strategy)