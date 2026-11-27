package com.zeroed.app.services;

import com.zeroed.app.dto.ConversionQuote;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Currency;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CurrencyConversionServiceTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void cachesProviderRateAndPreservesActualRateDate() throws IOException {
        AtomicInteger calls = new AtomicInteger();
        startServer(200, "{\"date\":\"2020-01-02\",\"base\":\"USD\",\"quote\":\"CAD\",\"rate\":1.4}", calls);
        CurrencyConversionService service = service();
        ConversionQuote one = service.quote(100, currency("USD"), currency("CAD"));
        ConversionQuote two = service.quote(200, currency("USD"), currency("CAD"));
        assertEquals("1.40", one.convertedAmount());
        assertEquals("2020-01-02", one.rateDate().toString());
        assertEquals(280L, two.convertedAmountMinor());
        assertEquals(1, calls.get());
    }

    @Test
    void sameCurrencyNeedsNoProviderAndConvertsByExponent() {
        ConversionQuote quote = service().quote(123, currency("JPY"), currency("JPY"));
        assertEquals("123", quote.convertedAmount());
        assertEquals("1", quote.exchangeRate());
        assertEquals(123L, quote.convertedAmountMinor());
    }

    @Test
    void conversionRoundsOnceAndRejectsOverflow() {
        var date = java.time.LocalDate.of(2020, 1, 2);
        ConversionQuote quote = service().convert(100, currency("USD"), currency("JPY"), new BigDecimal("0.5"), date);
        assertEquals("1", quote.convertedAmount());
        assertEquals("0", service().convert(1, currency("USD"), currency("JPY"), new BigDecimal("0.5"), date).convertedAmount());
        assertEquals("0.005", service().convert(1, currency("CAD"), currency("KWD"), new BigDecimal("0.45"), date).convertedAmount());
        assertThrows(com.zeroed.app.exceptions.ExpenseRequestException.class,
                () -> service().convert(Long.MAX_VALUE, currency("USD"), currency("CAD"), new BigDecimal("2"), date));
    }

    @Test
    void providerMalformedAndUnavailableResponsesBecome503() throws IOException {
        AtomicInteger calls = new AtomicInteger();
        startServer(200, "{\"date\":\"2099-01-01\",\"base\":\"USD\",\"quote\":\"CAD\",\"rate\":1.2}", calls);
        assertThrows(ResponseStatusException.class, () -> service().quote(100, currency("USD"), currency("CAD")));
        stopServer();
        calls.set(0);
        startServer(503, "unavailable", calls);
        assertThrows(ResponseStatusException.class, () -> service().quote(100, currency("USD"), currency("CAD")));
    }

    private CurrencyConversionService service() {
        return new CurrencyConversionService(server == null ? "http://localhost:1" : "http://localhost:" + server.getAddress().getPort());
    }

    private void startServer(int status, String body, AtomicInteger calls) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/v2/rate/USD/CAD", exchange -> {
            calls.incrementAndGet();
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    private static Currency currency(String code) {
        return Currency.getInstance(code);
    }
}
