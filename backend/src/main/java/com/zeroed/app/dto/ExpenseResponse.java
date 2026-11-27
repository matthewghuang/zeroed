package com.zeroed.app.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.zeroed.app.models.Expense;
import com.zeroed.app.models.User;

public record ExpenseResponse(Long id, Long groupId, String description, String amount, String currency,
                              GroupMemberResponse paidBy, GroupMemberResponse createdBy, String splitType,
                              List<Split> splits, LocalDate date, Instant createdAt, Instant updatedAt,
                              String baseCurrency, String convertedAmount, String exchangeRate, LocalDate rateDate) {
    public record Split(Long userId, String amount) {}

    public static ExpenseResponse from(Expense expense) {
        int exponent = expense.getCurrencyExponent();
        return new ExpenseResponse(expense.getId(), expense.getGroup().getId(), expense.getDescription(),
                format(expense.getAmountMinor(), exponent), expense.getCurrency(),
                member(expense.getPaidBy()), member(expense.getCreatedBy()),
                expense.getSplitType().toLowerCase(Locale.ROOT),
                expense.getSplits().stream()
                        .sorted(Comparator.comparing(split -> split.getUser().getId()))
                        .map(split -> new Split(split.getUser().getId(),
                                format(split.getAmountMinor(), expense.getBaseCurrencyExponent()))).toList(),
                expense.getDate(), expense.getCreatedAt(), expense.getUpdatedAt(),
                expense.getGroup().getBaseCurrency(),
                format(expense.getConvertedAmountMinor(), expense.getBaseCurrencyExponent()),
                expense.getExchangeRate().stripTrailingZeros().toPlainString(), expense.getRateDate());
    }

    private static GroupMemberResponse member(User user) {
        return new GroupMemberResponse(user.getId(), user.getEmail());
    }

    private static String format(long minor, int exponent) {
        return BigDecimal.valueOf(minor, exponent).toPlainString();
    }
}
