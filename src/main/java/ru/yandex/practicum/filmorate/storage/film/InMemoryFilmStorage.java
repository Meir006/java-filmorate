package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class InMemoryFilmStorage implements FilmStorage {

    private static final LocalDate CINEMA_BIRTHDAY = LocalDate.of(1895, 12, 28);
    private final Map<Long, Film> films = new HashMap<>();
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

        log.info("Фильм «{}» успешно добавлен с ID {}", film.getName(), film.getId());
        return film;
    }

    @Override
    public Film updateFilm(Film newFilm) {
        log.info("Обновляем данные фильма с ID {}", newFilm.getId());

        if (newFilm.getId() == null) {
            log.warn("Не удалось обновить фильм: не указан ID");
            throw new ValidationException("Чтобы обновить фильм, нужно указать его ID");
        }

        if (!films.containsKey(newFilm.getId())) {
            log.warn("Фильм с ID {} не найден в базе", newFilm.getId());
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
            log.warn("Фильм с ID {} не найден в базе", id);
            throw new NotFoundException("Фильм с ID " + id + " не найден");
        }
        return film;
    }

    private void validateReleaseDate(Film film) {
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(CINEMA_BIRTHDAY)) {
            log.warn("Некорректная дата релиза: {}. Фильм не может быть старше самого кино (28 декабря 1895)", film.getReleaseDate());
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года — тогда кино ещё не было!");
        }
    }

    private Long getNextId() {
        return ++idCounter;
    }
}
