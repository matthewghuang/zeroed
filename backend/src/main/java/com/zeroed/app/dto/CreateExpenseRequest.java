package com.zeroed.app.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.zeroed.app.exceptions.ExpenseRequestException;

import tools.jackson.databind.JsonNode;

public record CreateExpenseRequest(String description, long amountMinor, Currency currency,
                                   LocalDate date, Long paidByUserId, List<Long> participantIds) {
    private static final Set<String> FIELDS = Set.of(
            "description", "amount", "currency", "date", "paidByUserId", "participantIds");
    private static final Pattern CURRENCY_CODE = Pattern.compile("[A-Z]{3}");
    private static final Pattern DATE = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}");
    private static final Pattern[] AMOUNTS = {
        Pattern.compile("[0-9]{1,9}"), Pattern.compile("[0-9]{1,9}\\.[0-9]"),
        Pattern.compile("[0-9]{1,9}\\.[0-9]{2}"), Pattern.compile("[0-9]{1,9}\\.[0-9]{3}"),
        Pattern.compile("[0-9]{1,9}\\.[0-9]{4}")
    };

    // Parse explicitly: JSON numbers/booleans must never be coerced into money strings or IDs.
    public static CreateExpenseRequest from(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw ExpenseRequestException.invalid("body", "must be a JSON object");
        }
        Map<String, String> errors = new LinkedHashMap<>();
        for (String field : body.propertyNames()) {
            if (!FIELDS.contains(field)) errors.put(field, "is not a supported field");
        }
        if (!errors.isEmpty()) throw new ExpenseRequestException("unsupported_field", errors);

        String description = text(body.get("description"));
        if (description != null) description = description.strip();
        if (description == null || description.isBlank() || description.length() > 200) {
            errors.put("description", "must contain 1–200 characters after trimming");
        }

        String code = text(body.get("currency"));
        Currency currency = null;
        if (code != null && CURRENCY_CODE.matcher(code).matches()) {
            try {
                Currency candidate = Currency.getInstance(code);
                if (candidate.getDefaultFractionDigits() >= 0 && candidate.getDefaultFractionDigits() <= 4) {
                    currency = candidate;
                }
            } catch (IllegalArgumentException ignored) {
                // Report unsupported codes as field errors, not server errors.
            }
        }
        if (currency == null) errors.put("currency", "must be a supported uppercase ISO 4217 code");

        String amount = text(body.get("amount"));
        long amountMinor = 0;
        if (amount == null) {
            errors.put("amount", "must be an exact decimal string");
        } else if (currency != null) {
            int exponent = currency.getDefaultFractionDigits();
            if (!AMOUNTS[exponent].matcher(amount).matches()) {
                errors.put("amount", "must have 1–9 integer digits and exactly " + exponent + " fractional digits");
            } else {
                amountMinor = new BigDecimal(amount).movePointRight(exponent).longValueExact();
                if (amountMinor == 0) errors.put("amount", "must be greater than zero");
            }
        }

        LocalDate date = null;
        if (body.has("date")) {
            String value = text(body.get("date"));
            if (value == null || !DATE.matcher(value).matches()) {
                errors.put("date", "must be a valid YYYY-MM-DD date");
            } else {
                try {
                    date = LocalDate.parse(value);
                } catch (DateTimeParseException exception) {
                    errors.put("date", "must be a valid YYYY-MM-DD date");
                }
            }
        }

        Long payer = null;
        if (body.has("paidByUserId")) {
            payer = userId(body.get("paidByUserId"));
            if (payer == null) errors.put("paidByUserId", "must be an integer user ID");
        }

        // Null represents omission; an explicitly supplied JSON null remains invalid.
        List<Long> ids = null;
        if (body.has("participantIds")) {
            JsonNode participants = body.get("participantIds");
            ids = new ArrayList<>();
            Set<Long> seen = new HashSet<>();
            if (!participants.isArray() || participants.isEmpty()) {
                errors.put("participantIds", "must be a non-empty array of unique integer user IDs");
            } else {
                for (JsonNode participant : participants) {
                    Long id = userId(participant);
                    if (id == null || !seen.add(id)) {
                        errors.put("participantIds", "must be a non-empty array of unique integer user IDs");
                    } else {
                        ids.add(id);
                    }
                }
            }
        }
        if (!errors.isEmpty()) throw new ExpenseRequestException("validation_failed", errors);
        if (ids != null) ids.sort(Long::compareTo);
        return new CreateExpenseRequest(description, amountMinor, currency, date, payer, ids);
    }

    private static String text(JsonNode node) {
        return node != null && node.isString() ? node.asString() : null;
    }

    private static Long userId(JsonNode node) {
        return node != null && node.isIntegralNumber() && node.canConvertToLong() ? node.longValue() : null;
    }
}
