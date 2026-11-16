package com.zeroed.app.exceptions;

import java.util.Map;

public class ExpenseRequestException extends RuntimeException {
    private final String error;
    private final Map<String, String> fields;

    public ExpenseRequestException(String error, Map<String, String> fields) {
        super("Request body is invalid.");
        this.error = error;
        this.fields = fields;
    }

    public static ExpenseRequestException invalid(String field, String message) {
        return new ExpenseRequestException("validation_failed", Map.of(field, message));
    }

    public String getError() { return error; }
    public Map<String, String> getFields() { return fields; }
}
