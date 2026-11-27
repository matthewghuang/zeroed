package com.zeroed.app.dto;

import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Map;

import com.zeroed.app.exceptions.ExpenseRequestException;

import tools.jackson.databind.JsonNode;

public record CreateExpenseRequest(String description, long amountMinor, Currency currency,
                                   LocalDate date, Long paidByUserId, List<Long> participantIds) {
    // Parse explicitly: JSON numbers/booleans must never be coerced into money strings or IDs.
    public static CreateExpenseRequest from(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw ExpenseRequestException.invalid("body", "must be a JSON object");
        }
        Map<String, String> errors = ExpenseRequestValidation.supportedFields(body);
        String description = ExpenseRequestValidation.description(body.get("description"), errors);
        Currency currency = ExpenseRequestValidation.currency(body.get("currency"), errors);
        String amount = ExpenseRequestValidation.amount(body.get("amount"), errors);
        long amountMinor = ExpenseRequestValidation.amountMinor(amount, currency, errors);
        LocalDate date = body.has("date") ? ExpenseRequestValidation.date(body.get("date"), errors) : null;
        Long payer = body.has("paidByUserId")
                ? ExpenseRequestValidation.paidByUserId(body.get("paidByUserId"), errors) : null;
        List<Long> ids = body.has("participantIds")
                ? ExpenseRequestValidation.participantIds(body.get("participantIds"), errors) : null;
        ExpenseRequestValidation.throwIfInvalid(errors);
        return new CreateExpenseRequest(description, amountMinor, currency, date, payer, ids);
    }
}
