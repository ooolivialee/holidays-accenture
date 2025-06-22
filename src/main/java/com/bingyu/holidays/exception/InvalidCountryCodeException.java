package com.bingyu.holidays.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a provided country code is not recognized.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidCountryCodeException extends RuntimeException{
    public InvalidCountryCodeException(String message) {
        super(message);
    }
}
