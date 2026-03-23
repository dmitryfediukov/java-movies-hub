package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {
    private final MoviesStore store;
    private final HttpServer server;

    public MoviesServer() {
        this(new MoviesStore(), 8080);
    }

    public MoviesServer(MoviesStore store, int port) {
        this.store = store;
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
            this.server.createContext("/movies", new MoviesHandler(store));
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать HTTP-сервер", e);
        }
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    public MoviesStore getStore() {
        return store;
    }
}