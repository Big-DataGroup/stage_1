from datamarts.tokenizer import Tokenizer


class InvertedIndex:
    def __init__(self):
        self.index = {}

    def addDocument(self, bookId: int, bodyFilePath: str):
        words = Tokenizer.tokenize(bodyFilePath)

        for word in words:
            self.index.setdefault(word, []).append(bookId)


    def printIndex(self):
        for key, value in self.index.items():
            print(f"{key} -> {value}")

    def persistAll(self, storages: list):
        for storage in storages:
            storage.save(self.index)