from pymongo import MongoClient
from datamarts.storage.index_storage import IndexStorage


class MongoIndexStorage(IndexStorage):
    """
    Arquitectura 2: MongoDB.

    Cada término se guarda como un documento independiente en una colección:
    { "_id": "ballena", "books": [1, 84] }

    Se usa replace_one(..., upsert=True) por término, de forma que si
    el término ya existía se sobrescribe con la lista de libros actualizada
    (idempotente: relanzar el proceso no duplica datos).

    Requiere la dependencia (pip):
    pip install pymongo
    """

    def __init__(self, connectionUri: str, databaseName: str, collectionName: str):
        # MongoClient maneja automáticamente el pool de conexiones, igual que en Java
        self.client = MongoClient(connectionUri)
        database = self.client[databaseName]
        self.collection = database[collectionName]

    def save(self, index: dict):
        count = 0

        # entrySet() de Java es items() en Python
        for key, value in index.items():
            # En Python, el equivalente a org.bson.Document es un diccionario nativo (dict)
            doc = {
                "_id": key,
                "books": list(value)  # list() emula el new ArrayList<>() de Java
            }

            # El filtro eq("_id", key) de Java se escribe como un dict {"_id": key} en Python
            self.collection.replace_one({"_id": key}, doc, upsert=True)
            count += 1

        # getNamespace() en el driver de Java equivale a full_name en pymongo
        print(f"Índice persistido en MongoDB ({count} términos, colección {self.collection.full_name}).")

    def close(self):
        self.client.close()

    # --- Equivalente a AutoCloseable (Try-with-resources) ---
    # En Python, para que la clase se pueda usar dentro de un bloque 'with'
    # (que es el equivalente al try-with-resources de Java), definimos estos dos métodos:
    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.close()