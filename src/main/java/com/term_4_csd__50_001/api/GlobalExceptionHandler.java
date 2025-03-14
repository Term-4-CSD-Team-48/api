package com.term_4_csd__50_001.api;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import com.mongodb.MongoWriteException;
import com.term_4_csd__50_001.api.exceptions.ConflictException;
import com.term_4_csd__50_001.api.exceptions.InternalServerErrorException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(
            ResponseStatusException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getReason());
        return new ResponseEntity<>(error, ex.getStatusCode());
    }

    @ExceptionHandler(MongoWriteException.class)
    public ResponseEntity<Map<String, String>> handleMongoWriteException(
            MongoWriteException exception) {
        Map<String, String> error = new HashMap<>();
        ResponseStatusException ex = translateMongoException(exception);
        error.put("error", ex.getReason());
        return new ResponseEntity<>(error, ex.getStatusCode());
    }

    public static ResponseStatusException translateMongoException(MongoWriteException e) {
        if (e.getCode() == 11000) {
            System.out.println("Translating MongoWriteException to ConflictException");
            return new ConflictException(
                    "Another document with same field and value already exists in the collection",
                    e);
        } else {
            return new InternalServerErrorException("Something went wrong");
        }
    }

}
