package com.zeroed.app.models;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ExpenseSplitId implements Serializable {
    @Column(name = "expense_id")
    private Long expenseId;

    @Column(name = "user_id")
    private Long userId;

    protected ExpenseSplitId() {}

    public ExpenseSplitId(Long expenseId, Long userId) {
        this.expenseId = expenseId;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ExpenseSplitId id)) return false;
        return Objects.equals(expenseId, id.expenseId) && Objects.equals(userId, id.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expenseId, userId);
    }
}
