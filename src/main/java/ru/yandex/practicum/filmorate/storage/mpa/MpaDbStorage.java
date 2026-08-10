package ru.yandex.practicum.filmorate.storage.mpa;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.List;

@Slf4j
@Component
public class MpaDbStorage implements MpaStorage {

    private static final String SELECT_ALL_MPA = "SELECT mpa_id, name FROM mpa_ratings ORDER BY mpa_id";
    private static final String SELECT_MPA_BY_ID = "SELECT mpa_id, name FROM mpa_ratings WHERE mpa_id = ?";

    private final JdbcTemplate jdbcTemplate;

    public MpaDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Mpa> getAllMpa() {
        return jdbcTemplate.query(SELECT_ALL_MPA, (rs, rowNum) -> new Mpa(rs.getInt("mpa_id"), rs.getString("name")));
    }

    @Override
    public Mpa getMpaById(Integer id) {
        try {
            return jdbcTemplate.queryForObject(SELECT_MPA_BY_ID,
                    (rs, rowNum) -> new Mpa(rs.getInt("mpa_id"), rs.getString("name")), id);
        } catch (EmptyResultDataAccessException e) {
            log.error("Рейтинг MPA с ID {} не найден", id);
            throw new NotFoundException("Рейтинг MPA с ID " + id + " не найден");
        }
    }
}
