package com.zeroed.app.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;

public record ConversionQuote(
        String baseCurrency,
        String convertedAmount,
        String exchangeRate,
        LocalDate rateDate) {

    public long convertedAmountMinor() {
        int exponent = Currency.getInstance(baseCurrency).getDefaultFractionDigits();
        try {
            return new BigDecimal(convertedAmount).movePointRight(exponent).longValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new IllegalArgumentException("Converted amount is outside the supported range", exception);
        }
    }
}
