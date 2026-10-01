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
 * Arquitectura 2: MongoDB.
 * <p>
 * Cada término se guarda como un documento independiente en una colección:
 * <pre>
 * { "_id": "ballena", "books": [1, 84] }
 * </pre>
 * Se usa {@code replaceOne(..., upsert=true)} por término, de forma que si
 * el término ya existía se sobrescribe con la lista de libros actualizada
 * (idempotente: relanzar el proceso no duplica datos).
 * <p>
 * Requiere la dependencia (Maven):
 * <pre>{@code
 * <dependency>
 *   <groupId>org.mongodb</groupId>
 *   <artifactId>mongodb-driver-sync</artifactId>
 *   <version>5.1.0</version>
 * </dependency>
 * }</pre>
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
        ReplaceOptions upsert = new ReplaceOptions().upsert(true);
        int count = 0;

        for (Map.Entry<String, List<Integer>> entry : index.entrySet()) {
            Document doc = new Document("_id", entry.getKey())
                    .append("books", new ArrayList<>(entry.getValue()));

            collection.replaceOne(eq("_id", entry.getKey()), doc, upsert);
            count++;
        }

        System.out.println("Índice persistido en MongoDB (" + count + " términos, colección "
                + collection.getNamespace() + ").");
    }

    @Override
    public void close() {
        client.close();
    }
}
