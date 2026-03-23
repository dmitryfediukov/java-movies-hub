package ru.practicum.moviehub.http;

import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore store;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();

        if ("/movies".equalsIgnoreCase(path)) {
            handleMoviesRoot(exchange, method, query);
            return;
        }

        if (path.startsWith("/movies/")) {
            handleMovieById(exchange, method, path);
            return;
        }

        sendError(exchange, 404, "Ресурс не найден");
    }

    private void handleMoviesRoot(HttpExchange exchange, String method, String query) throws IOException {
        switch (method) {
            case "GET":
                handleGetMovies(exchange, query);
                break;
            case "POST":
                handlePostMovie(exchange);
                break;
            default:
                sendError(exchange, 405, "Method Not Allowed");
        }
    }

    private void handleGetMovies(HttpExchange exchange, String query) throws IOException {
        if (query == null || query.isBlank()) {
            sendJson(exchange, 200, store.findAll());
            return;
        }

        Integer year = parseYearQuery(query);
        if (year == null) {
            sendError(exchange, 400, "Некорректный параметр запроса - 'year'");
            return;
        }

        sendJson(exchange, 200, store.findByYear(year));
    }

    private Integer parseYearQuery(String query) {
        String[] parts = query.split("&");
        String yearValue = null;

        for (String part : parts) {
            String[] kv = part.split("=", 2);
            String key = kv[0];
            String value = kv.length > 1 ? kv[1] : "";

            if ("year".equalsIgnoreCase(key)) {
                yearValue = value;
            } else {
                return null;
            }
        }

        if (yearValue == null || yearValue.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(yearValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void handlePostMovie(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.toLowerCase().startsWith("application/json")) {
            sendError(exchange, 415, "Unsupported Media Type");
            return;
        }

        String body = readRequestBody(exchange);
        CreateMovieRequest request;

        try {
            request = gson.fromJson(body, CreateMovieRequest.class);
        } catch (JsonSyntaxException e) {
            sendError(exchange, 400, "Некорректный JSON");
            return;
        }

        if (request == null) {
            sendError(exchange, 400, "Некорректный JSON");
            return;
        }

        List<String> details = validateMovieRequest(request);
        if (!details.isEmpty()) {
            sendJson(exchange, 422, new ErrorResponse("Ошибка валидации", details));
            return;
        }

        Movie created = store.add(request.title.trim(), request.year);
        sendJson(exchange, 201, created);
    }

    private List<String> validateMovieRequest(CreateMovieRequest request) {
        List<String> errors = new ArrayList<>();

        if (request.title == null || request.title.trim().isEmpty()) {
            errors.add("название не должно быть пустым");
        } else if (request.title.trim().length() > 100) {
            errors.add("название не должно быть длиннее 100 символов");
        }

        int maxYear = Year.now().getValue() + 1;
        if (request.year < 1888 || request.year > maxYear) {
            errors.add("год должен быть между 1888 и " + maxYear);
        }

        return errors;
    }

    private void handleMovieById(HttpExchange exchange, String method, String path) throws IOException {
        String idPart = path.substring("/movies/".length());

        if (idPart.isBlank() || idPart.contains("/")) {
            sendError(exchange, 404, "Ресурс не найден");
            return;
        }

        long id;
        try {
            id = Long.parseLong(idPart);
        } catch (NumberFormatException e) {
            sendError(exchange, 400, "Некорректный ID");
            return;
        }

        switch (method) {
            case "GET":
                handleGetMovieById(exchange, id);
                break;
            case "DELETE":
                handleDeleteMovie(exchange, id);
                break;
            default:
                sendError(exchange, 405, "Method Not Allowed");
        }
    }

    private void handleGetMovieById(HttpExchange exchange, long id) throws IOException {
        Optional<Movie> movie = store.findById(id);
        if (movie.isPresent()) {
            sendJson(exchange, 200, movie.get());
        } else {
            sendError(exchange, 404, "Фильм не найден");
        }
    }

    private void handleDeleteMovie(HttpExchange exchange, long id) throws IOException {
        boolean deleted = store.delete(id);
        if (deleted) {
            sendNoContent(exchange);
        } else {
            sendError(exchange, 404, "Фильм не найден");
        }
    }

    private static class CreateMovieRequest {
        String title;
        int year;
    }
}