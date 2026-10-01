# Suponiendo que el Tokenizer está en la misma carpeta o en datamarts
from datamarts.tokenizer import Tokenizer


class InvertedIndex:
    def __init__(self):
        # Mapa que asocia un término (String) con una lista de IDs de libros (List<Integer>)
        # En Python, un HashMap se traduce directamente a un diccionario (dict)
        self.index = {}

    def addDocument(self, bookId: int, bodyFilePath: str):
        words = Tokenizer.tokenize(bodyFilePath)

        for word in words:
            # setdefault actúa exactamente igual que putIfAbsent + get en Java:
            # Si la palabra no existe, crea la lista vacía. Luego, devuelve la lista (nueva o existente)
            # para que podamos hacer el .append() (el equivalente a .add() de Java).
            self.index.setdefault(word, []).append(bookId)

        print(f"Libro {bookId} indexado en memoria correctamente.")

    # Metodo temporal para ver el contenido en la consola
    def printIndex(self):
        # entrySet() de Java es equivalente a items() en diccionarios de Python
        for key, value in self.index.items():
            print(f"{key} -> {value}")

    def persistAll(self, storages: list):
        for storage in storages:
            storage.save(self.index)