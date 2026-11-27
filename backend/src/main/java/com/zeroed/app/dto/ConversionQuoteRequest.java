package com.zeroed.app.dto;

import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.Map;

import com.zeroed.app.exceptions.ExpenseRequestException;

import tools.jackson.databind.JsonNode;

public record ConversionQuoteRequest(long amountMinor, Currency currency) {
    public static ConversionQuoteRequest from(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw ExpenseRequestException.invalid("body", "must be a JSON object");
        }
        Map<String, String> errors = new LinkedHashMap<>();
        for (String field : body.propertyNames()) {
            if (!field.equals("amount") && !field.equals("currency")) {
                errors.put(field, "is not a supported field");
            }
        }
        if (!errors.isEmpty()) throw new ExpenseRequestException("unsupported_field", errors);
        Currency currency = ExpenseRequestValidation.currency(body.get("currency"), errors);
        long amount = ExpenseRequestValidation.amountMinor(
                ExpenseRequestValidation.amount(body.get("amount"), errors), currency, errors);
        ExpenseRequestValidation.throwIfInvalid(errors);
        return new ConversionQuoteRequest(amount, currency);
    }
}
