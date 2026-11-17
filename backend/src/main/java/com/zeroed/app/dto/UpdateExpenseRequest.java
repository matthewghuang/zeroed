package com.zeroed.app.dto;

import java.time.LocalDate;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.zeroed.app.exceptions.ExpenseRequestException;

import tools.jackson.databind.JsonNode;

public record UpdateExpenseRequest(String description, String amount, Currency currency,
                                   LocalDate date, Long paidByUserId, List<Long> participantIds) {
    private static final long[] POWERS_OF_TEN = {1, 10, 100, 1000, 10000};

    public static UpdateExpenseRequest from(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw ExpenseRequestException.invalid("body", "must be a JSON object");
        }
        if (body.isEmpty()) {
            throw ExpenseRequestException.invalid("body", "must contain at least one expense field");
        }
        Map<String, String> errors = ExpenseRequestValidation.supportedFields(body);
        String description = body.has("description")
                ? ExpenseRequestValidation.description(body.get("description"), errors) : null;
        Currency currency = body.has("currency")
                ? ExpenseRequestValidation.currency(body.get("currency"), errors) : null;
        String amount = body.has("amount")
                ? ExpenseRequestValidation.amount(body.get("amount"), errors) : null;
        LocalDate date = body.has("date")
                ? ExpenseRequestValidation.date(body.get("date"), errors) : null;
        Long payer = body.has("paidByUserId")
                ? ExpenseRequestValidation.paidByUserId(body.get("paidByUserId"), errors) : null;
        List<Long> ids = body.has("participantIds")
                ? ExpenseRequestValidation.participantIds(body.get("participantIds"), errors) : null;
        ExpenseRequestValidation.throwIfInvalid(errors);
        return new UpdateExpenseRequest(description, amount, currency, date, payer, ids);
    }

    public long amountMinor(long currentMinor, int currentExponent, Currency effectiveCurrency) {
        if (amount != null) {
            Map<String, String> errors = new LinkedHashMap<>();
            long resolved = ExpenseRequestValidation.amountMinor(amount, effectiveCurrency, errors);
            ExpenseRequestValidation.throwIfInvalid(errors);
            return resolved;
        }
        int exponent = effectiveCurrency.getDefaultFractionDigits();
        if (exponent == currentExponent) return currentMinor;
        if (exponent > currentExponent) {
            try {
                return Math.multiplyExact(currentMinor, POWERS_OF_TEN[exponent - currentExponent]);
            } catch (ArithmeticException exception) {
                throw ExpenseRequestException.invalid("amount", "retained amount exceeds the supported range");
            }
        }
        long divisor = POWERS_OF_TEN[currentExponent - exponent];
        if (currentMinor % divisor != 0) {
            throw ExpenseRequestException.invalid("amount", "retained amount cannot be represented exactly in the selected currency");
        }
        return currentMinor / divisor;
    }
}
