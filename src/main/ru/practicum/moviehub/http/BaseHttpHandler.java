package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public abstract class BaseHttpHandler implements HttpHandler {
    protected static final String CONTENT_TYPE = "application/json; charset=UTF-8";
    protected final Gson gson = new Gson();

    protected void sendJson(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] responseBytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    protected void sendJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        sendJson(exchange, statusCode, gson.toJson(body));
    }

    protected void sendNoContent(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE);
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }

    protected void sendError(HttpExchange exchange, int statusCode, String errorMessage) throws IOException {
        sendJson(exchange, statusCode, new ErrorResponse(errorMessage));
    }

    protected String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}