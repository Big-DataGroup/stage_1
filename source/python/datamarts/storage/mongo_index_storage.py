from pymongo import MongoClient
from datamarts.storage.index_storage import IndexStorage


class MongoIndexStorage(IndexStorage):
    def __init__(self, connectionUri: str, databaseName: str, collectionName: str):
        self.client = MongoClient(connectionUri)
        database = self.client[databaseName]
        self.collection = database[collectionName]

    def save(self, index: dict):
        count = 0

        for key, value in index.items():
            doc = {
                "_id": key,
                "books": list(value)
            }

            self.collection.replace_one({"_id": key}, doc, upsert=True)
            count += 1

        print(f"Índice persistido en MongoDB ({count} términos, colección {self.collection.full_name}).")

    def close(self):
        self.client.close()

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.close()