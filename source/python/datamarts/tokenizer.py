import re
import sys


class Tokenizer:

    @staticmethod
    def tokenize(filePath: str) -> set:
        uniqueWords = set()

        try:
            with open(filePath, 'r', encoding='utf-8') as br:
                for line in br:
                    # 1. Convertir a minúsculas
                    # 2. Reemplazar todo lo que no sea letra o número por espacios
                    # En Python usamos re.sub() para hacer el equivalente a replaceAll()
                    line = re.sub(r'[^a-z0-9\s]', ' ', line.lower())

                    # El split() sin argumentos en Python separa automáticamente por
                    # cualquier cantidad de espacios (equivalente a split("\\s+"))
                    words = line.split()

                    for word in words:
                        # En Python, si la palabra tiene texto, word.strip() será True (no está en blanco)
                        if word.strip():
                            # El método de inserción en un set de Python se llama add(), igual que en HashSet
                            uniqueWords.add(word)

        except IOError as e:
            print(f"Error leyendo el cuerpo del libro: {e}", file=sys.stderr)

        return uniqueWords