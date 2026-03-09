package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MoviesStore {
    private final Map<Long, Movie> movies = new HashMap<>();
    private long nextId = 1;

    public List<Movie> findAll() {
        List<Movie> result = new ArrayList<>(movies.values());
        result.sort(Comparator.comparingLong(Movie::getId));
        return result;
    }

    public List<Movie> findByYear(int year) {
        List<Movie> result = new ArrayList<>();
        for (Movie movie : movies.values()) {
            if (movie.getYear() == year) {
                result.add(movie);
            }
        }
        result.sort(Comparator.comparingLong(Movie::getId));
        return result;
    }

    public Movie add(String title, int year) {
        Movie movie = new Movie(nextId++, title, year);
        movies.put(movie.getId(), movie);
        return movie;
    }

    public Optional<Movie> findById(long id) {
        return Optional.ofNullable(movies.get(id));
    }

    public boolean delete(long id) {
        return movies.remove(id) != null;
    }

    public void clear() {
        movies.clear();
        nextId = 1;
    }
}