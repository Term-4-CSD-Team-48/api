package com.term_4_csd__50_001.api.collections;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.term_4_csd__50_001.api.Database;
import com.term_4_csd__50_001.api.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserCollection extends Collection<User> {

    private MongoCollection<User> userCollection;

    @Autowired
    UserCollection(Database database) {
        userCollection = database.getCollection("users", User.class);
        IndexOptions unique = new IndexOptions().unique(true);
        userCollection.createIndex(Indexes.ascending("email"), unique);
        userCollection.createIndex(Indexes.ascending("username"), unique);
    }

    @Override
    protected MongoCollection<User> getCollection() {
        return userCollection;
    }

}
