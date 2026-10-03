using System;
using System.Collections.Generic;
using MongoDB.Bson;
using MongoDB.Driver;

namespace BigDataPipeline.Datamarts.Storage
{
    public class MongoIndexStorage : IIndexStorage
    {
        private readonly MongoClient _client;
        private readonly IMongoCollection<BsonDocument> _collection;

        public MongoIndexStorage(string connectionUri, string databaseName, string collectionName)
        {
            _client = new MongoClient(connectionUri);
            IMongoDatabase database = _client.GetDatabase(databaseName);
            _collection = database.GetCollection<BsonDocument>(collectionName);
        }

        public void Save(Dictionary<string, List<int>> index)
        {
            var options = new ReplaceOptions { IsUpsert = true };
            int count = 0;

            foreach (var entry in index)
            {
                var doc = new BsonDocument
                {
                    { "_id", entry.Key },
                    { "books", new BsonArray(entry.Value) }
                };

                var filter = Builders<BsonDocument>.Filter.Eq("_id", entry.Key);
                _collection.ReplaceOne(filter, doc, options);
                count++;
            }

            Console.WriteLine($"Índice persistido en MongoDB ({count} términos, colección {_collection.CollectionNamespace}).");
        }
    }
}