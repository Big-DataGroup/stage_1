from datamarts.tokenizer import Tokenizer


class InvertedIndex:
    def __init__(self):
        # Mapa que asocia un término (String) con una lista de IDs de libros (List<Integer>)
        self.index = {}

    def addDocument(self, bookId: int, bodyFilePath: str):
        words = Tokenizer.tokenize(bodyFilePath)

        for word in words:
            postings = self.index.setdefault(word, [])
            # Evitar duplicados si se re-indexa el mismo libro (literal a Java)[cite: 6]
            if bookId not in postings:
                postings.append(bookId)

    def search(self, word: str) -> list:
        """Busca los IDs de los libros que contienen la palabra consultada (literal a Java/C#)."""
        return self.index.get(word.lower(), [])

    def printIndex(self):
        for key, value in self.index.items():
            print(f"{key} -> {value}")

    def persistAll(self, storages: list):
        for storage in storages:
            storage.save(self.index)