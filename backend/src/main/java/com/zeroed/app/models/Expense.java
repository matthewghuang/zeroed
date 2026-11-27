package com.zeroed.app.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.Check;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;


import com.zeroed.app.dto.ConversionQuote;
@Entity
@Table(name = "expenses")
@Check(constraints = "amount_minor > 0 and currency_exponent between 0 and 4 and split_type = 'EQUAL' and converted_amount_minor >= 0 and exchange_rate > 0")
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "currency_exponent", nullable = false)
    private int currencyExponent;

    @Column(name = "converted_amount_minor", nullable = false)
    private long convertedAmountMinor;

    @Column(name = "exchange_rate", nullable = false, precision = 38, scale = 18)
    private BigDecimal exchangeRate;

    @Column(name = "rate_date", nullable = false)
    private LocalDate rateDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paid_by_user_id", nullable = false)
    private User paidBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @Column(name = "split_type", nullable = false, length = 16)
    private String splitType = "EQUAL";

    @Column(name = "expense_date", nullable = false)
    private LocalDate date;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<ExpenseSplit> splits = new ArrayList<>();

    protected Expense() {}

    public Expense(Group group, String description, long amountMinor, Currency currency,
                   User paidBy, User createdBy, LocalDate date, ConversionQuote conversion) {
        this.group = group;
        this.description = description;
        this.amountMinor = amountMinor;
        this.currency = currency.getCurrencyCode();
        this.currencyExponent = currency.getDefaultFractionDigits();
        this.paidBy = paidBy;
        this.createdBy = createdBy;
        this.date = date;
        applyConversion(conversion);
    }

    @PrePersist
    protected void onCreate() {
        if (currencyExponent < 0 || currencyExponent > 4
                || currencyExponent != Currency.getInstance(currency).getDefaultFractionDigits()) {
            throw new IllegalStateException("Currency exponent does not match ISO 4217");
        }
        createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        updatedAt = createdAt;
    }

    public void applyConversion(ConversionQuote conversion) {
        if (!group.getBaseCurrency().equals(conversion.baseCurrency())) {
            throw new IllegalArgumentException("Conversion must use the group's base currency");
        }
        convertedAmountMinor = conversion.convertedAmountMinor();
        exchangeRate = new BigDecimal(conversion.exchangeRate());
        rateDate = conversion.rateDate();
    }

    public void addSplit(User user, long amountMinor) {
        splits.add(new ExpenseSplit(this, user, amountMinor));
    }

    public void update(String description, long amountMinor, Currency currency, User paidBy, LocalDate date) {
        this.description = description;
        this.amountMinor = amountMinor;
        this.currency = currency.getCurrencyCode();
        this.currencyExponent = currency.getDefaultFractionDigits();
        this.paidBy = paidBy;
        this.date = date;
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        Instant nextTimestamp = updatedAt.plus(1, ChronoUnit.MICROS);
        this.updatedAt = now.isAfter(nextTimestamp) ? now : nextTimestamp;
    }

    public void recalculateEqualSplits(List<User> participants) {
        Map<Long, ExpenseSplit> remaining = new HashMap<>();
        for (ExpenseSplit split : splits) remaining.put(split.getUser().getId(), split);
        long share = convertedAmountMinor / participants.size();
        long remainder = convertedAmountMinor % participants.size();
        // Participants are ordered by ID so the lowest IDs receive the remainder.
        for (int index = 0; index < participants.size(); index++) {
            User participant = participants.get(index);
            long amount = share + (index < remainder ? 1 : 0);
            ExpenseSplit split = remaining.remove(participant.getId());
            if (split == null) {
                addSplit(participant, amount);
            } else {
                split.setAmountMinor(amount);
            }
        }
        // Keep overlapping composite-key rows managed; only departed participants are orphans.
        splits.removeIf(split -> remaining.containsKey(split.getUser().getId()));
    }

    public Long getId() { return id; }
    public Group getGroup() { return group; }
    public String getDescription() { return description; }
    public long getAmountMinor() { return amountMinor; }
    public String getCurrency() { return currency; }
    public int getCurrencyExponent() { return currencyExponent; }
    public long getConvertedAmountMinor() { return convertedAmountMinor; }
    public BigDecimal getExchangeRate() { return exchangeRate; }
    public LocalDate getRateDate() { return rateDate; }
    public int getBaseCurrencyExponent() {
        return Currency.getInstance(group.getBaseCurrency()).getDefaultFractionDigits();
    }
    public User getPaidBy() { return paidBy; }
    public User getCreatedBy() { return createdBy; }
    public String getSplitType() { return splitType; }
    public LocalDate getDate() { return date; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<ExpenseSplit> getSplits() { return splits; }
}
