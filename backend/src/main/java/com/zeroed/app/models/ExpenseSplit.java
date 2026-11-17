package com.zeroed.app.models;

import org.hibernate.annotations.Check;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "expense_splits")
@Check(constraints = "amount_minor >= 0")
public class ExpenseSplit {
    @EmbeddedId
    private ExpenseSplitId id;

    @MapsId("expenseId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Expense expense;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    private User user;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    protected ExpenseSplit() {}

    public ExpenseSplit(Expense expense, User user, long amountMinor) {
        this.expense = expense;
        this.user = user;
        this.amountMinor = amountMinor;
        this.id = new ExpenseSplitId(expense.getId(), user.getId());
    }

    public User getUser() { return user; }
    public long getAmountMinor() { return amountMinor; }
    public void setAmountMinor(long amountMinor) { this.amountMinor = amountMinor; }
}
