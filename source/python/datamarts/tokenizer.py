import re
import sys


class Tokenizer:

    @staticmethod
    def tokenize(filePath: str) -> set:
        uniqueWords = set()

        try:
            with open(filePath, 'r', encoding='utf-8') as br:
                for line in br:
                    line = re.sub(r'[^a-z0-9\s]', ' ', line.lower())

                    words = line.split()

                    for word in words:
                        if word.strip():
                            uniqueWords.add(word)

        except IOError as e:
            print(f"Error leyendo el cuerpo del libro: {e}", file=sys.stderr)

        return uniqueWords