package com.term_4_csd__50_001.api.collections;

import java.lang.reflect.Field;
import org.bson.Document;
import org.bson.conversions.Bson;

public class UpdateBuilder<M> {


    private M set;
    private M unset;

    public UpdateBuilder<M> set(M model) {
        set = model;
        return this;
    }

    public UpdateBuilder<M> unset(M model) {
        unset = model;
        return this;
    }

    private Document buildDocument(M model) {
        Document document = new Document();
        for (Field field : model.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(model);
                if (value != null) {
                    document.append(field.getName(), value);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return document;
    }

    public Bson build() {
        Document update = new Document();
        if (set != null) {
            update.append("$set", buildDocument(set));
        }
        if (unset != null) {
            update.append("$unset", buildDocument(unset));
        }
        return update;
    }

}


