package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Хранилище фильмов в памяти. Оставлено для справки/тестов —
 * основным хранилищем приложения является {@link FilmDbStorage}.
 */
@Slf4j
@Component("inMemoryFilmStorage")
public class InMemoryFilmStorage implements FilmStorage {

    private static final LocalDate CINEMA_BIRTHDAY = LocalDate.of(1895, 12, 28);
    private final Map<Long, Film> films = new HashMap<>();
    private final Map<Long, Set<Long>> likes = new HashMap<>();
    private long idCounter = 0;

    @Override
    public Collection<Film> getFilms() {
        log.info("Запрошен список всех фильмов. Сейчас в базе: {} шт.", films.size());
        return films.values();
    }

    @Override
    public Film addFilm(Film film) {
        log.info("Добавляем новый фильм: {}", film.getName());
        validateReleaseDate(film);
        film.setId(getNextId());
        films.put(film.getId(), film);
        likes.put(film.getId(), new HashSet<>());
        log.info("Фильм «{}» успешно добавлен с ID {}", film.getName(), film.getId());
        return film;
    }

    @Override
    public Film updateFilm(Film newFilm) {
        log.info("Обновляем данные фильма с ID {}", newFilm.getId());
        if (newFilm.getId() == null) {
            log.error("Не удалось обновить фильм: не указан ID");
            throw new ValidationException("Чтобы обновить фильм, нужно указать его ID");
        }
        if (!films.containsKey(newFilm.getId())) {
            log.error("Фильм с ID {} не найден в базе", newFilm.getId());
            throw new NotFoundException("Фильм с ID " + newFilm.getId() + " не найден");
        }
        validateReleaseDate(newFilm);
        films.put(newFilm.getId(), newFilm);
        log.info("Данные фильма «{}» (ID {}) успешно обновлены", newFilm.getName(), newFilm.getId());
        return newFilm;
    }

    @Override
    public Film getFilmById(Long id) {
        Film film = films.get(id);
        if (film == null) {
            log.error("Фильм с ID {} не найден в базе", id);
            throw new NotFoundException("Фильм с ID " + id + " не найден");
        }
        return film;
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        getFilmById(filmId);
        likes.computeIfAbsent(filmId, k -> new HashSet<>()).add(userId);
        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        getFilmById(filmId);
        likes.getOrDefault(filmId, Collections.emptySet()).remove(userId);
        log.info("Пользователь {} удалил лайк с фильма {}", userId, filmId);
    }

    @Override
    public List<Film> getPopular(int count) {
        return films.values().stream()
                .sorted(Comparator.comparingInt((Film f) -> likes.getOrDefault(f.getId(), Collections.emptySet()).size()).reversed())
                .limit(count)
                .collect(Collectors.toList());
    }

    private void validateReleaseDate(Film film) {
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(CINEMA_BIRTHDAY)) {
            log.error("Некорректная дата релиза: {}. Фильм не может быть старше самого кино (28 декабря 1895)", film.getReleaseDate());
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года — тогда кино ещё не было!");
        }
    }

    private Long getNextId() {
        return ++idCounter;
    }
}
