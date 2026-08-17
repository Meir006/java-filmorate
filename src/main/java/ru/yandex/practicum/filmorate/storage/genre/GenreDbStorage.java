package ru.yandex.practicum.filmorate.storage.genre;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;

@Slf4j
@Component
public class GenreDbStorage implements GenreStorage {

    private static final String SELECT_ALL_GENRES = "SELECT genre_id, name FROM genres ORDER BY genre_id";
    private static final String SELECT_GENRE_BY_ID = "SELECT genre_id, name FROM genres WHERE genre_id = ?";

    private final JdbcTemplate jdbcTemplate;

    public GenreDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Genre> getAllGenres() {
        return jdbcTemplate.query(SELECT_ALL_GENRES,
                (rs, rowNum) -> new Genre(rs.getInt("genre_id"), rs.getString("name")));
    }

    @Override
    public Genre getGenreById(Integer id) {
        try {
            return jdbcTemplate.queryForObject(SELECT_GENRE_BY_ID,
                    (rs, rowNum) -> new Genre(rs.getInt("genre_id"), rs.getString("name")), id);
        } catch (EmptyResultDataAccessException e) {
            log.error("Жанр с ID {} не найден", id);
            throw new NotFoundException("Жанр с ID " + id + " не найден");
        }
    }
}
