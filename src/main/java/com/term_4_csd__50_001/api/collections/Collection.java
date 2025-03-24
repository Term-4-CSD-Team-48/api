package com.term_4_csd__50_001.api.collections;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.bson.conversions.Bson;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.term_4_csd__50_001.api.exceptions.ConflictException;

public abstract class Collection<M> {

    protected abstract MongoCollection<M> getCollection();

    private Bson getCombinedFilter(M model) {
        List<Bson> filters = new ArrayList<>();
        for (Field field : model.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(model);
                if (value != null) {
                    filters.add(eq(field.getName(), value));
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        Bson combinedFilter = and(filters);
        return combinedFilter;
    }

    public DeleteResult deleteOne(M model) {
        DeleteResult deleteResult = getCollection().deleteOne(getCombinedFilter(model));
        return deleteResult;
    }

    public M findOne(M model) {
        M fetchedModel = getCollection().find(getCombinedFilter(model)).first();
        return fetchedModel;
    }

    /**
     * @param model
     * @return
     * @throws MongoWriteException
     */
    public String insertOne(M model) {
        MongoCollection<M> collection = getCollection();
        InsertOneResult result = collection.insertOne(model);
        return result.getInsertedId().toString();
    }

    public void updateOne(M oldModel, UpdateBuilder<M> update) {
        MongoCollection<M> collection = getCollection();
        collection.updateOne(getCombinedFilter(oldModel), update.build());
    }

}
