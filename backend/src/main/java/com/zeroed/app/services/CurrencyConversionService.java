package com.zeroed.app.services;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.zeroed.app.dto.ConversionQuote;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

@Service
public class CurrencyConversionService {
    private static final int MAX_PRECISION = 38;
    private static final int MAX_SCALE = 18;
    private final RestClient client;
    private final Map<RateKey, ProviderResponse> cache = new HashMap<>();
    private LocalDate cacheDate;

    public CurrencyConversionService(@Value("${zeroed.exchange-rates.base-url:https://api.frankfurter.dev}") String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(5000);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    public ConversionQuote quote(long amountMinor, Currency source, Currency target) {
        validateAmount(amountMinor);
        validateCurrency(source);
        validateCurrency(target);
        LocalDate requestDate = LocalDate.now(ZoneOffset.UTC);
        if (source.equals(target)) {
            return convert(amountMinor, source, target, BigDecimal.ONE, requestDate);
        }
        ProviderResponse rate = getRate(source, target, requestDate);
        return convert(amountMinor, source, target, rate.rate(), rate.date());
    }

    public ConversionQuote convert(long amountMinor, Currency source, Currency target, BigDecimal rate, LocalDate rateDate) {
        validateAmount(amountMinor);
        validateCurrency(source);
        validateCurrency(target);
        if (!validRate(rate)
                || rateDate == null || rateDate.isAfter(LocalDate.now(ZoneOffset.UTC))) {
            throw unavailable("Invalid exchange rate data");
        }
        int sourceExponent = source.getDefaultFractionDigits();
        int targetExponent = target.getDefaultFractionDigits();
        // Round the total once; split allocation happens afterward in target minor units.
        BigDecimal converted;
        try {
            converted = BigDecimal.valueOf(amountMinor, sourceExponent)
                    .multiply(rate)
                    .setScale(targetExponent, RoundingMode.HALF_UP);
            converted.movePointRight(targetExponent).longValueExact();
        } catch (ArithmeticException exception) {
            throw com.zeroed.app.exceptions.ExpenseRequestException.invalid(
                    "amount", "converted amount is outside the supported minor-unit range");
        }
        return new ConversionQuote(target.getCurrencyCode(), converted.toPlainString(), rate.toPlainString(), rateDate);
    }

    private synchronized ProviderResponse getRate(Currency source, Currency target, LocalDate requestDate) {
        if (!requestDate.equals(cacheDate)) {
            cache.clear();
            cacheDate = requestDate;
        }
        RateKey key = new RateKey(source.getCurrencyCode(), target.getCurrencyCode(), requestDate);
        ProviderResponse existing = cache.get(key);
        if (existing != null) return existing;
        ProviderResponse response;
        try {
            response = client.get()
                    .uri("/v2/rate/{source}/{target}", key.source(), key.target())
                    .retrieve()
                    .body(ProviderResponse.class);
        } catch (RuntimeException exception) {
            throw unavailable("Exchange rates are unavailable. Your expense was not saved. Please try again.");
        }
        if (response == null || !key.source().equals(response.base()) || !key.target().equals(response.quote())
                || response.date() == null || response.date().isAfter(requestDate)
                || !validRate(response.rate())) {
            throw unavailable("Exchange-rate provider returned invalid rate data");
        }
        cache.put(key, response);
        return response;
    }

    private static void validateAmount(long amountMinor) {
        if (amountMinor <= 0) throw new IllegalArgumentException("Amount must be positive");
    }

    private static void validateCurrency(Currency currency) {
        if (currency == null || currency.getDefaultFractionDigits() < 0) {
            throw new IllegalArgumentException("Unsupported ISO currency");
        }
    }

    private static ResponseStatusException unavailable(String reason) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, reason);
    }

    private static boolean validRate(BigDecimal rate) {
        return rate != null && rate.signum() > 0 && rate.precision() <= MAX_PRECISION
                && rate.scale() <= MAX_SCALE && rate.precision() - rate.scale() <= MAX_PRECISION - MAX_SCALE;
    }

    private record RateKey(String source, String target, LocalDate date) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ProviderResponse(LocalDate date, String base, String quote, BigDecimal rate) {}
}
