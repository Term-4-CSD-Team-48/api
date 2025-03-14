package com.term_4_csd__50_001.api;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

@Service
public class Database {

    private final MongoDatabase mongoDatabase;
    private List<MongoCollection<?>> mongoCollections = new ArrayList<>();

    @Autowired
    public Database(MongoClientConnection mongoClientConnection) {
        this.mongoDatabase = mongoClientConnection.getDatabase("term-4-csd-team-48");
    }

    public <T> MongoCollection<T> getCollection(String name, Class<T> clazz) {
        MongoCollection<T> mongoCollection = mongoDatabase.getCollection(name, clazz);
        mongoCollections.add(mongoCollection);
        return mongoCollection;
    }

    public void cleanDatabase() {
        for (MongoCollection<?> mongoCollection : mongoCollections) {
            mongoCollection.deleteMany(Filters.empty());
        }
    }

}
