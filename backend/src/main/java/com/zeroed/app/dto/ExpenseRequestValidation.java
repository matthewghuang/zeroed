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

final class ExpenseRequestValidation {
    private static final Set<String> FIELDS = Set.of(
            "description", "amount", "currency", "date", "paidByUserId", "participantIds");
    private static final Pattern CURRENCY_CODE = Pattern.compile("[A-Z]{3}");
    private static final Pattern DATE = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}");
    private static final Pattern[] AMOUNTS = {
        Pattern.compile("[0-9]{1,9}"), Pattern.compile("[0-9]{1,9}\\.[0-9]"),
        Pattern.compile("[0-9]{1,9}\\.[0-9]{2}"), Pattern.compile("[0-9]{1,9}\\.[0-9]{3}"),
        Pattern.compile("[0-9]{1,9}\\.[0-9]{4}")
    };

    private ExpenseRequestValidation() {}

    static Map<String, String> supportedFields(JsonNode body) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (String field : body.propertyNames()) {
            if (!FIELDS.contains(field)) errors.put(field, "is not a supported field");
        }
        if (!errors.isEmpty()) throw new ExpenseRequestException("unsupported_field", errors);
        return errors;
    }

    static String description(JsonNode node, Map<String, String> errors) {
        String description = text(node);
        if (description != null) description = description.strip();
        if (description == null || description.isBlank() || description.length() > 200) {
            errors.put("description", "must contain 1–200 characters after trimming");
        }
        return description;
    }

    static Currency currency(JsonNode node, Map<String, String> errors) {
        String code = text(node);
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
        return currency;
    }

    static String amount(JsonNode node, Map<String, String> errors) {
        String amount = text(node);
        if (amount == null) errors.put("amount", "must be an exact decimal string");
        return amount;
    }

    static long amountMinor(String amount, Currency currency, Map<String, String> errors) {
        if (amount == null || currency == null) return 0;
        int exponent = currency.getDefaultFractionDigits();
        if (!AMOUNTS[exponent].matcher(amount).matches()) {
            errors.put("amount", "must have 1–9 integer digits and exactly " + exponent + " fractional digits");
            return 0;
        }
        long amountMinor = new BigDecimal(amount).movePointRight(exponent).longValueExact();
        if (amountMinor == 0) errors.put("amount", "must be greater than zero");
        return amountMinor;
    }

    static LocalDate date(JsonNode node, Map<String, String> errors) {
        String value = text(node);
        if (value != null && DATE.matcher(value).matches()) {
            try {
                return LocalDate.parse(value);
            } catch (DateTimeParseException ignored) {
                // Report invalid dates as field errors.
            }
        }
        errors.put("date", "must be a valid YYYY-MM-DD date");
        return null;
    }

    static Long paidByUserId(JsonNode node, Map<String, String> errors) {
        Long payer = userId(node);
        if (payer == null) errors.put("paidByUserId", "must be an integer user ID");
        return payer;
    }

    static List<Long> participantIds(JsonNode node, Map<String, String> errors) {
        List<Long> ids = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        if (node == null || !node.isArray() || node.isEmpty()) {
            errors.put("participantIds", "must be a non-empty array of unique integer user IDs");
        } else {
            for (JsonNode participant : node) {
                Long id = userId(participant);
                if (id == null || !seen.add(id)) {
                    errors.put("participantIds", "must be a non-empty array of unique integer user IDs");
                } else {
                    ids.add(id);
                }
            }
        }
        ids.sort(Long::compareTo);
        return ids;
    }

    static void throwIfInvalid(Map<String, String> errors) {
        if (!errors.isEmpty()) throw new ExpenseRequestException("validation_failed", errors);
    }

    private static String text(JsonNode node) {
        return node != null && node.isString() ? node.asString() : null;
    }

    private static Long userId(JsonNode node) {
        return node != null && node.isIntegralNumber() && node.canConvertToLong() ? node.longValue() : null;
    }
}
