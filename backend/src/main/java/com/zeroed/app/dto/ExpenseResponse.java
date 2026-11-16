package com.zeroed.app.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import com.zeroed.app.models.Expense;
import com.zeroed.app.models.User;

public record ExpenseResponse(Long id, Long groupId, String description, String amount, String currency,
                              GroupMemberResponse paidBy, GroupMemberResponse createdBy, String splitType,
                              List<Split> splits, LocalDate date, Instant createdAt, Instant updatedAt) {
    public record Split(Long userId, String amount) {}

    public static ExpenseResponse from(Expense expense) {
        int exponent = expense.getCurrencyExponent();
        return new ExpenseResponse(expense.getId(), expense.getGroup().getId(), expense.getDescription(),
                format(expense.getAmountMinor(), exponent), expense.getCurrency(),
                member(expense.getPaidBy()), member(expense.getCreatedBy()),
                expense.getSplitType().toLowerCase(Locale.ROOT),
                expense.getSplits().stream().map(split -> new Split(split.getUser().getId(),
                        format(split.getAmountMinor(), exponent))).toList(),
                expense.getDate(), expense.getCreatedAt(), expense.getUpdatedAt());
    }

    private static GroupMemberResponse member(User user) {
        return new GroupMemberResponse(user.getId(), user.getEmail());
    }

    private static String format(long minor, int exponent) {
        return BigDecimal.valueOf(minor, exponent).toPlainString();
    }
}
