package com.dzfashion.catalog.exception;

public class UnauthorizedPriceMutationException extends RuntimeException {
    public UnauthorizedPriceMutationException(String message) {
        super(message);
    }
}
