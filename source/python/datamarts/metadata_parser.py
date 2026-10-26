import re
import sys
from datamarts.book_metadata import BookMetadata


class MetadataParser:

    @staticmethod
    def parseHeader(bookId: int, filePath: str) -> BookMetadata:
        title = "Unknown"
        author = "Unknown"
        language = "Unknown"

        # Expresiones regulares para extraer los campos solicitados
        titlePattern = re.compile(r"^Title:\s+(.*)$")
        authorPattern = re.compile(r"^Author:\s+(.*)$")
        langPattern = re.compile(r"^Language:\s+(.*)$")

        try:
            # En Python usamos 'with' como equivalente al try-with-resources de Java
            with open(filePath, 'r', encoding='utf-8') as br:
                for line in br:
                    titleMatcher = titlePattern.search(line)
                    if titleMatcher:
                        title = titleMatcher.group(1).strip()

                    authorMatcher = authorPattern.search(line)
                    if authorMatcher:
                        author = authorMatcher.group(1).strip()

                    langMatcher = langPattern.search(line)
                    if langMatcher:
                        language = langMatcher.group(1).strip()

        except IOError as e:
            # Equivalente a System.err.println
            print(f"Error leyendo la cabecera: {e}", file=sys.stderr)

        return BookMetadata(bookId, title, author, language)