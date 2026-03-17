package ru.practicum.moviehub.http;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Year;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Тесты API MovieHub")
public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;

    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer();
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @AfterAll
    static void afterAll() {
        if (server != null) {
            server.stop();
        }
    }

    @BeforeEach
    void beforeEach() {
        server.getStore().clear();
    }

    @Test
    @DisplayName("GET /movies возвращает пустой массив, если фильмов нет")
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");
        assertJsonContentType(resp);

        String body = resp.body().trim();
        assertEquals("[]", body, "Пустое хранилище должно возвращать []");
    }

    @Test
    @DisplayName("POST /movies добавляет фильм при корректных данных")
    void postMovie_whenValid_returnsCreatedMovie() throws Exception {
        String json = "{"
                + "\"title\":\"Аватар\","
                + "\"year\":2010"
                + "}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(201, resp.statusCode(), "POST /movies должен вернуть 201");
        assertJsonContentType(resp);

        String body = resp.body();
        assertTrue(body.contains("\"id\":1"), "Должен быть присвоен id");
        assertTrue(body.contains("\"title\":\"Аватар\""), "Должно вернуться название фильма");
        assertTrue(body.contains("\"year\":2010"), "Должен вернуться год фильма");
    }

    @Test
    @DisplayName("GET /movies возвращает список с ранее добавленным фильмом")
    void getMovies_whenMovieAdded_returnsListWithMovie() throws Exception {
        createMovie("Inception", 2010);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");
        assertJsonContentType(resp);

        String body = resp.body();
        assertTrue(body.startsWith("["), "Ожидается JSON-массив");
        assertTrue(body.contains("\"title\":\"Inception\""), "Список должен содержать фильм");
        assertTrue(body.contains("\"year\":2010"), "Список должен содержать год фильма");
    }

    @Test
    @DisplayName("POST /movies возвращает 422 при пустом названии")
    void postMovie_whenTitleEmpty_returns422() throws Exception {
        String json = "{"
                + "\"title\":\"   \","
                + "\"year\":2010"
                + "}";

        HttpResponse<String> resp = sendPostMovie(json, "application/json");

        assertEquals(422, resp.statusCode(), "При пустом title должен быть 422");
        assertJsonContentType(resp);

        String body = resp.body();
        assertTrue(body.contains("\"error\":\"Ошибка валидации\""), "Должна быть ошибка валидации");
        assertTrue(body.contains("название не должно быть пустым"), "Должна быть деталь про пустое название");
    }

    @Test
    @DisplayName("POST /movies возвращает 422 при слишком длинном названии")
    void postMovie_whenTitleTooLong_returns422() throws Exception {
        String longTitle = "A".repeat(101);
        String json = "{"
                + "\"title\":\"" + longTitle + "\","
                + "\"year\":2010"
                + "}";

        HttpResponse<String> resp = sendPostMovie(json, "application/json");

        assertEquals(422, resp.statusCode(), "При слишком длинном title должен быть 422");
        assertJsonContentType(resp);

        String body = resp.body();
        assertTrue(body.contains("\"error\":\"Ошибка валидации\""), "Должна быть ошибка валидации");
        assertTrue(body.contains("название не должно быть длиннее 100 символов"),
                "Должна быть деталь про длину названия");
    }

    @Test
    @DisplayName("POST /movies возвращает 422, если год меньше 1888")
    void postMovie_whenYearTooSmall_returns422() throws Exception {
        String json = "{"
                + "\"title\":\"Old movie\","
                + "\"year\":1800"
                + "}";

        HttpResponse<String> resp = sendPostMovie(json, "application/json");

        assertEquals(422, resp.statusCode(), "При некорректном year должен быть 422");
        assertJsonContentType(resp);

        String body = resp.body();
        assertTrue(body.contains("\"error\":\"Ошибка валидации\""), "Должна быть ошибка валидации");
        assertTrue(body.contains("год должен быть между 1888 и"), "Должна быть деталь про диапазон года");
    }

    @Test
    @DisplayName("POST /movies возвращает 422, если год больше текущего года плюс один")
    void postMovie_whenYearTooLarge_returns422() throws Exception {
        int invalidYear = Year.now().getValue() + 2;
        String json = "{"
                + "\"title\":\"Future movie\","
                + "\"year\":" + invalidYear
                + "}";

        HttpResponse<String> resp = sendPostMovie(json, "application/json");

        assertEquals(422, resp.statusCode(), "При year больше текущего + 1 должен быть 422");
        assertJsonContentType(resp);

        String body = resp.body();
        assertTrue(body.contains("\"error\":\"Ошибка валидации\""), "Должна быть ошибка валидации");
        assertTrue(body.contains("год должен быть между 1888 и"), "Должна быть деталь про диапазон года");
    }

    @Test
    @DisplayName("POST /movies возвращает 415 при неверном Content-Type")
    void postMovie_whenContentTypeInvalid_returns415() throws Exception {
        String json = "{"
                + "\"title\":\"Аватар\","
                + "\"year\":2010"
                + "}";

        HttpResponse<String> resp = sendPostMovie(json, "text/plain");

        assertEquals(415, resp.statusCode(), "При неверном Content-Type должен быть 415");
        assertJsonContentType(resp);

        String body = resp.body();
        assertTrue(body.contains("\"error\":\"Unsupported Media Type\""),
                "Должна быть ошибка Unsupported Media Type");
    }

    @Test
    @DisplayName("POST /movies возвращает 400 при некорректном JSON")
    void postMovie_whenJsonInvalid_returns400() throws Exception {
        String json = "{"
                + "\"title\":\"Аватар\","
                + "\"year\":"
                + "}";

        HttpResponse<String> resp = sendPostMovie(json, "application/json");

        assertEquals(400, resp.statusCode(), "При битом JSON должен быть 400");
        assertJsonContentType(resp);

        String body = resp.body();
        assertTrue(body.contains("\"error\":\"Некорректный JSON\""),
                "Должна быть ошибка про некорректный JSON");
    }

    @Test
    @DisplayName("GET /movies/{id} возвращает фильм по существующему id")
    void getMovieById_whenExists_returnsMovie() throws Exception {
        createMovie("Inception", 2010);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(200, resp.statusCode(), "GET /movies/{id} должен вернуть 200");
        assertJsonContentType(resp);

        String body = resp.body();
        assertTrue(body.contains("\"id\":1"), "Должен вернуться правильный id");
        assertTrue(body.contains("\"title\":\"Inception\""), "Должен вернуться фильм");
        assertTrue(body.contains("\"year\":2010"), "Должен вернуться год");
    }

    @Test
    @DisplayName("GET /movies/{id} возвращает 404, если фильм не найден")
    void getMovieById_whenNotFound_returns404() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/999"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(404, resp.statusCode(), "Если фильм не найден, должен быть 404");
        assertJsonContentType(resp);

        String body = resp.body();
        assertTrue(body.contains("\"error\":\"Фильм не найден\""),
                "Должно быть сообщение о том, что фильм не найден");
    }

    @Test
    @DisplayName("GET /movies/{id} возвращает 400, если id не число")
    void getMovieById_whenIdIsNotNumber_returns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/abc"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(400, resp.statusCode(), "Если id не число, должен быть 400");
        assertJsonContentType(resp);

        String body = resp.body();
        assertTrue(body.contains("\"error\":\"Некорректный ID\""),
                "Должно быть сообщение о некорректном ID");
    }

    @Test
    @DisplayName("DELETE /movies/{id} возвращает 204 при успешном удалении")
    void deleteMovie_whenExists_returns204() throws Exception {
        createMovie("Inception", 2010);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(204, resp.statusCode(), "DELETE /movies/{id} должен вернуть 204");
    }

    @Test
    @DisplayName("DELETE /movies/{id} возвращает 404, если фильм не найден")
    void deleteMovie_whenNotFound_returns404() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/999"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(404, resp.statusCode(), "Если фильма нет, должен быть 404");
        assertJsonContentType(resp);

        String body = resp.body();
        assertTrue(body.contains("\"error\":\"Фильм не найден\""),
                "Должно быть сообщение о том, что фильм не найден");
    }

    @Test
    @DisplayName("DELETE /movies/{id} возвращает 400, если id не число")
    void deleteMovie_whenIdIsNotNumber_returns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/abc"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(400, resp.statusCode(), "Если id не число, должен быть 400");
        assertJsonContentType(resp);

        String body = resp.body();
        assertTrue(body.contains("\"error\":\"Некорректный ID\""),
                "Должно быть сообщение о некорректном ID");
    }

    @Test
    @DisplayName("GET /movies?year=YYYY возвращает фильмы указанного года")
    void getMoviesByYear_whenMatchesFound_returnsMovies() throws Exception {
        createMovie("Inception", 2010);
        createMovie("Interstellar", 2014);
        createMovie("Another 2010", 2010);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2010"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(200, resp.statusCode(), "GET /movies?year=YYYY должен вернуть 200");
        assertJsonContentType(resp);

        String body = resp.body();
        assertTrue(body.contains("\"title\":\"Inception\""), "Должен быть фильм 2010 года");
        assertTrue(body.contains("\"title\":\"Another 2010\""), "Должен быть второй фильм 2010 года");
        assertTrue(!body.contains("\"title\":\"Interstellar\""), "Фильм другого года не должен попасть в выборку");
    }

    @Test
    @DisplayName("GET /movies?year=YYYY возвращает пустой массив, если совпадений нет")
    void getMoviesByYear_whenNoMatches_returnsEmptyArray() throws Exception {
        createMovie("Inception", 2010);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2022"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(200, resp.statusCode(), "GET /movies?year=YYYY должен вернуть 200");
        assertJsonContentType(resp);

        String body = resp.body().trim();
        assertEquals("[]", body, "Если фильмов по году нет, должен вернуться пустой массив");
    }

    @Test
    @DisplayName("GET /movies?year=YYYY возвращает 400, если year не число")
    void getMoviesByYear_whenYearIsNotNumber_returns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=abc"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(400, resp.statusCode(), "Если year не число, должен быть 400");
        assertJsonContentType(resp);

        String body = resp.body();
        assertTrue(body.contains("Некорректный параметр запроса"),
                "Должна быть ошибка по query-параметру year");
    }

    @Test
    @DisplayName("Неподдерживаемый HTTP-метод возвращает 405")
    void methodNotAllowed_returns405() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .PUT(HttpRequest.BodyPublishers.ofString("", StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(405, resp.statusCode(), "Неподдерживаемый метод должен вернуть 405");
        assertJsonContentType(resp);

        String body = resp.body();
        assertTrue(body.contains("\"error\":\"Method Not Allowed\""),
                "Должна быть ошибка Method Not Allowed");
    }

    private static HttpResponse<String> sendPostMovie(String json, String contentType) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        return client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static void createMovie(String title, int year) throws Exception {
        String json = "{"
                + "\"title\":\"" + title + "\","
                + "\"year\":" + year
                + "}";

        HttpResponse<String> resp = sendPostMovie(json, "application/json");
        assertEquals(201, resp.statusCode(), "Подготовительный POST должен вернуть 201");
    }

    private static void assertJsonContentType(HttpResponse<String> resp) {
        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен быть application/json; charset=UTF-8");
    }
}