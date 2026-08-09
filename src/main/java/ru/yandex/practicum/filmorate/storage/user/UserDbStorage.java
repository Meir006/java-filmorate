package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;

/**
 * DAO-хранилище пользователей поверх H2 (JdbcTemplate).
 */
@Slf4j
@Component("userDbStorage")
@Qualifier("userDbStorage")
public class UserDbStorage implements UserStorage {

    // Дружба применяется сразу, без подтверждения — единственный используемый статус.
    private static final int CONFIRMED_STATUS_ID = 1;

    private final JdbcTemplate jdbcTemplate;

    public UserDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Collection<User> getUsers() {
        String sql = "SELECT user_id, email, login, name, birthday FROM users";
        log.info("Запрошен список всех пользователей");
        return jdbcTemplate.query(sql, this::mapRowToUser);
    }

    @Override
    public User createUser(User user) {
        validateUser(user);
        log.info("Регистрируем нового пользователя с логином: {}", user.getLogin());
        String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getLogin());
            ps.setString(3, user.getName());
            ps.setDate(4, user.getBirthday() == null ? null : Date.valueOf(user.getBirthday()));
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        user.setId(key.longValue());
        log.info("Пользователь {} (ID {}) успешно создан", user.getLogin(), user.getId());
        return user;
    }

    @Override
    public User updateUser(User newUser) {
        if (newUser.getId() == null) {
            log.error("Не удалось обновить пользователя: не передан ID");
            throw new ValidationException("Для обновления профиля необходимо указать ID пользователя");
        }
        getUserById(newUser.getId());
        validateUser(newUser);
        log.info("Обновляем профиль пользователя с ID {}", newUser.getId());
        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE user_id = ?";
        jdbcTemplate.update(sql,
                newUser.getEmail(),
                newUser.getLogin(),
                newUser.getName(),
                newUser.getBirthday() == null ? null : Date.valueOf(newUser.getBirthday()),
                newUser.getId());
        log.info("Профиль пользователя с ID {} успешно обновлён", newUser.getId());
        return newUser;
    }

    @Override
    public User getUserById(Long id) {
        String sql = "SELECT user_id, email, login, name, birthday FROM users WHERE user_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, this::mapRowToUser, id);
        } catch (EmptyResultDataAccessException e) {
            log.error("Пользователь с ID {} не зарегистрирован", id);
            throw new NotFoundException("Пользователь с ID " + id + " не найден");
        }
    }

    @Override
    public void addFriend(Long userId, Long friendId) {
        getUserById(userId);
        getUserById(friendId);
        // MERGE вместо INSERT — повторная заявка в друзья не должна падать с ошибкой дублирования PK.
        String sql = "MERGE INTO friendships (user_id, friend_id, status_id) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, userId, friendId, CONFIRMED_STATUS_ID);
        log.info("Пользователь {} добавил пользователя {} в друзья", userId, friendId);
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        getUserById(userId);
        getUserById(friendId);
        String sql = "DELETE FROM friendships WHERE user_id = ? AND friend_id = ?";
        jdbcTemplate.update(sql, userId, friendId);
        log.info("Пользователь {} удалил пользователя {} из друзей", userId, friendId);
    }

    @Override
    public List<User> getFriends(Long userId) {
        getUserById(userId);
        String sql = "SELECT u.user_id, u.email, u.login, u.name, u.birthday " +
                "FROM users u JOIN friendships f ON u.user_id = f.friend_id " +
                "WHERE f.user_id = ?";
        return jdbcTemplate.query(sql, this::mapRowToUser, userId);
    }

    @Override
    public List<User> getCommonFriends(Long userId, Long otherId) {
        getUserById(userId);
        getUserById(otherId);
        String sql = "SELECT u.user_id, u.email, u.login, u.name, u.birthday " +
                "FROM users u " +
                "JOIN friendships f1 ON u.user_id = f1.friend_id AND f1.user_id = ? " +
                "JOIN friendships f2 ON u.user_id = f2.friend_id AND f2.user_id = ?";
        return jdbcTemplate.query(sql, this::mapRowToUser, userId, otherId);
    }

    private User mapRowToUser(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        User user = new User();
        user.setId(rs.getLong("user_id"));
        user.setEmail(rs.getString("email"));
        user.setLogin(rs.getString("login"));
        user.setName(rs.getString("name"));
        Date birthday = rs.getDate("birthday");
        user.setBirthday(birthday == null ? null : birthday.toLocalDate());
        return user;
    }

    private void validateUser(User user) {
        if (user.getLogin() != null && user.getLogin().contains(" ")) {
            log.error("В логине «{}» обнаружены пробелы", user.getLogin());
            throw new ValidationException("Логин должен быть слитным, без пробелов");
        }
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.info("Имя не указано — подставили логин «{}» в качестве имени", user.getLogin());
        }
    }
}
