package org.ulpgc.bigdata.datamarts.storage;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.mongodb.client.model.Filters.eq;

/**
 * Arquitectura 2: MongoDB (Puntos 3, 10 y 32).
 * Guarda cada término como un documento independiente en una colección con upsert idempotente.
 * Mide el tiempo de inserción/actualización en la base de datos.
 */
public class MongoIndexStorage implements IndexStorage, AutoCloseable {

    private final MongoClient client;
    private final MongoCollection<Document> collection;

    public MongoIndexStorage(String connectionUri, String databaseName, String collectionName) {
        this.client = MongoClients.create(connectionUri);
        MongoDatabase database = client.getDatabase(databaseName);
        this.collection = database.getCollection(collectionName);
    }

    @Override
    public void save(Map<String, List<Integer>> index) {
        long startTime = System.nanoTime();
        ReplaceOptions upsert = new ReplaceOptions().upsert(true);
        int count = 0;

        for (Map.Entry<String, List<Integer>> entry : index.entrySet()) {
            Document doc = new Document("_id", entry.getKey())
                    .append("books", new ArrayList<>(entry.getValue()));

            collection.replaceOne(eq("_id", entry.getKey()), doc, upsert);
            count++;
        }

        long elapsedTime = System.nanoTime() - startTime;

        System.out.printf("[MongoIndexStorage] MongoDB persisted -> Collection: %s | Terms Upserted: %d | Time: %.2f ms%n",
                collection.getNamespace(), count, elapsedTime / 1_000_000.0);
    }

    @Override
    public void close() {
        client.close();
    }
}