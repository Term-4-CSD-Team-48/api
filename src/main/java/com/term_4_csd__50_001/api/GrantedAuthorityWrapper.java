package com.term_4_csd__50_001.api;

import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.springframework.security.core.GrantedAuthority;

public class GrantedAuthorityWrapper implements GrantedAuthority {

    private final String authority;

    @BsonCreator
    public GrantedAuthorityWrapper(@BsonProperty("authority") String authority) {
        this.authority = authority;
    }

    @Override
    public String getAuthority() {
        return authority;
    }

    @Override
    public String toString() {
        return authority;
    }

}
