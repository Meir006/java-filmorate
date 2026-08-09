package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * DAO-хранилище фильмов поверх H2 (JdbcTemplate).
 */
@Slf4j
@Component("filmDbStorage")
@Qualifier("filmDbStorage")
public class FilmDbStorage implements FilmStorage {

    private static final LocalDate CINEMA_BIRTHDAY = LocalDate.of(1895, 12, 28);

    private static final String BASE_SELECT =
            "SELECT f.film_id, f.name, f.description, f.release_date, f.duration, " +
                    "m.mpa_id AS mpa_id, m.name AS mpa_name " +
                    "FROM films f LEFT JOIN mpa_ratings m ON f.mpa_id = m.mpa_id ";

    private final JdbcTemplate jdbcTemplate;

    public FilmDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Collection<Film> getFilms() {
        log.info("Запрошен список всех фильмов");
        List<Film> films = jdbcTemplate.query(BASE_SELECT, this::mapRowToFilm);
        films.forEach(this::attachGenres);
        return films;
    }

    @Override
    public Film addFilm(Film film) {
        validateReleaseDate(film);
        validateMpaExists(film.getMpa());
        validateGenresExist(film.getGenres());
        log.info("Добавляем новый фильм: {}", film.getName());
        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_id) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setDate(3, film.getReleaseDate() == null ? null : Date.valueOf(film.getReleaseDate()));
            ps.setInt(4, film.getDuration());
            if (film.getMpa() != null && film.getMpa().getId() != null) {
                ps.setInt(5, film.getMpa().getId());
            } else {
                ps.setNull(5, java.sql.Types.INTEGER);
            }
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        film.setId(key.longValue());
        saveGenres(film);
        log.info("Фильм «{}» успешно добавлен с ID {}", film.getName(), film.getId());
        return getFilmById(film.getId());
    }

    @Override
    public Film updateFilm(Film newFilm) {
        if (newFilm.getId() == null) {
            log.error("Не удалось обновить фильм: не указан ID");
            throw new ValidationException("Чтобы обновить фильм, нужно указать его ID");
        }
        getFilmById(newFilm.getId());
        validateReleaseDate(newFilm);
        validateMpaExists(newFilm.getMpa());
        validateGenresExist(newFilm.getGenres());
        log.info("Обновляем данные фильма с ID {}", newFilm.getId());
        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ? WHERE film_id = ?";
        jdbcTemplate.update(sql,
                newFilm.getName(),
                newFilm.getDescription(),
                newFilm.getReleaseDate() == null ? null : Date.valueOf(newFilm.getReleaseDate()),
                newFilm.getDuration(),
                newFilm.getMpa() == null ? null : newFilm.getMpa().getId(),
                newFilm.getId());
        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", newFilm.getId());
        saveGenres(newFilm);
        log.info("Данные фильма «{}» (ID {}) успешно обновлены", newFilm.getName(), newFilm.getId());
        return getFilmById(newFilm.getId());
    }

    @Override
    public Film getFilmById(Long id) {
        try {
            Film film = jdbcTemplate.queryForObject(BASE_SELECT + "WHERE f.film_id = ?", this::mapRowToFilm, id);
            attachGenres(film);
            return film;
        } catch (EmptyResultDataAccessException e) {
            log.error("Фильм с ID {} не найден в базе", id);
            throw new NotFoundException("Фильм с ID " + id + " не найден");
        }
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        getFilmById(filmId);
        try {
            jdbcTemplate.update("MERGE INTO likes (film_id, user_id) VALUES (?, ?)", filmId, userId);
        } catch (DataIntegrityViolationException e) {
            log.error("Не удалось поставить лайк: пользователь {} не найден", userId);
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }
        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        getFilmById(filmId);
        jdbcTemplate.update("DELETE FROM likes WHERE film_id = ? AND user_id = ?", filmId, userId);
        log.info("Пользователь {} удалил лайк с фильма {}", userId, filmId);
    }

    @Override
    public List<Film> getPopular(int count) {
        String sql = BASE_SELECT +
                "LEFT JOIN likes l ON f.film_id = l.film_id " +
                "GROUP BY f.film_id, m.mpa_id, m.name " +
                "ORDER BY COUNT(l.user_id) DESC " +
                "LIMIT ?";
        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm, count);
        films.forEach(this::attachGenres);
        return films;
    }

    private void saveGenres(Film film) {
        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            return;
        }
        Set<Integer> uniqueGenreIds = new LinkedHashSet<>();
        for (Genre genre : film.getGenres()) {
            uniqueGenreIds.add(genre.getId());
        }
        String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
        jdbcTemplate.batchUpdate(sql, uniqueGenreIds, uniqueGenreIds.size(), (ps, genreId) -> {
            ps.setLong(1, film.getId());
            ps.setInt(2, genreId);
        });
    }

    private void validateMpaExists(Mpa mpa) {
        if (mpa == null || mpa.getId() == null) {
            return;
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mpa_ratings WHERE mpa_id = ?", Integer.class, mpa.getId());
        if (count == null || count == 0) {
            log.error("Указан несуществующий рейтинг MPA с ID {}", mpa.getId());
            throw new NotFoundException("Рейтинг MPA с ID " + mpa.getId() + " не найден");
        }
    }

    private void validateGenresExist(Set<Genre> genres) {
        if (genres == null || genres.isEmpty()) {
            return;
        }
        for (Genre genre : genres) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM genres WHERE genre_id = ?", Integer.class, genre.getId());
            if (count == null || count == 0) {
                log.error("Указан несуществующий жанр с ID {}", genre.getId());
                throw new NotFoundException("Жанр с ID " + genre.getId() + " не найден");
            }
        }
    }

    private void attachGenres(Film film) {
        String sql = "SELECT g.genre_id, g.name FROM film_genres fg " +
                "JOIN genres g ON fg.genre_id = g.genre_id " +
                "WHERE fg.film_id = ? ORDER BY g.genre_id";
        List<Genre> genres = jdbcTemplate.query(sql,
                (rs, rowNum) -> new Genre(rs.getInt("genre_id"), rs.getString("name")),
                film.getId());
        film.setGenres(new LinkedHashSet<>(genres));
    }

    private Film mapRowToFilm(ResultSet rs, int rowNum) throws SQLException {
        Film film = new Film();
        film.setId(rs.getLong("film_id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));
        Date releaseDate = rs.getDate("release_date");
        film.setReleaseDate(releaseDate == null ? null : releaseDate.toLocalDate());
        film.setDuration(rs.getInt("duration"));
        int mpaId = rs.getInt("mpa_id");
        if (!rs.wasNull()) {
            film.setMpa(new Mpa(mpaId, rs.getString("mpa_name")));
        }
        return film;
    }

    private void validateReleaseDate(Film film) {
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(CINEMA_BIRTHDAY)) {
            log.error("Некорректная дата релиза: {}. Фильм не может быть старше самого кино (28 декабря 1895)", film.getReleaseDate());
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года — тогда кино ещё не было!");
        }
    }
}
