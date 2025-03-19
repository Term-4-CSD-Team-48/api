package com.term_4_csd__50_001.api.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class UnauthorizedRequestException extends ResponseStatusException {

    public UnauthorizedRequestException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }

    public UnauthorizedRequestException(String message, Throwable cause) {
        super(HttpStatus.UNAUTHORIZED, message, cause);
    }

}
