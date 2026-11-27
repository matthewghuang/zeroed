package com.zeroed.app.controllers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.sun.net.httpserver.HttpServer;

final class FrankfurterStub implements AutoCloseable {
    private final HttpServer server;
    volatile boolean unavailable;

    FrankfurterStub() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
        server.createContext("/v2/rate/", exchange -> {
            String[] pair = exchange.getRequestURI().getPath().substring("/v2/rate/".length()).split("/");
            String rate = Map.of("USD", "1.4", "EUR", "1.6", "JPY", "0.01", "KWD", "4.5", "CLF", "50", "BWP", "0.1").get(pair[0]);
            int status = unavailable || rate == null || !pair[1].equals("CAD") ? 503 : 200;
            byte[] body = (status == 200
                    ? "{\"date\":\"2020-01-02\",\"base\":\"" + pair[0] + "\",\"quote\":\"CAD\",\"rate\":" + rate + "}"
                    : "{\"message\":\"unavailable\"}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    String baseUrl() { return "http://127.0.0.1:" + server.getAddress().getPort(); }

    @Override
    public void close() { server.stop(0); }
}
