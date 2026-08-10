package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;

/**
 * DAO-хранилище пользователей поверх H2 (JdbcTemplate).
 */
@Slf4j
@Component
public class UserDbStorage implements UserStorage {

    private static final String SELECT_ALL_USERS = "SELECT user_id, email, login, name, birthday FROM users";
    private static final String SELECT_USER_BY_ID = SELECT_ALL_USERS + " WHERE user_id = ?";
    private static final String INSERT_USER = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
    private static final String UPDATE_USER =
            "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE user_id = ?";
    // Наличие строки (user_id, friend_id) само по себе означает, что дружба существует —
    // отдельный статус не нужен: подтверждение дружбы в проекте не предусмотрено.
    private static final String MERGE_FRIENDSHIP = "MERGE INTO friendships (user_id, friend_id) VALUES (?, ?)";
    private static final String DELETE_FRIENDSHIP = "DELETE FROM friendships WHERE user_id = ? AND friend_id = ?";
    private static final String SELECT_FRIENDS =
            "SELECT u.user_id, u.email, u.login, u.name, u.birthday " +
                    "FROM users u JOIN friendships f ON u.user_id = f.friend_id " +
                    "WHERE f.user_id = ?";
    private static final String SELECT_COMMON_FRIENDS =
            "SELECT u.user_id, u.email, u.login, u.name, u.birthday " +
                    "FROM users u " +
                    "JOIN friendships f1 ON u.user_id = f1.friend_id AND f1.user_id = ? " +
                    "JOIN friendships f2 ON u.user_id = f2.friend_id AND f2.user_id = ?";

    private final JdbcTemplate jdbcTemplate;

    public UserDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Collection<User> getUsers() {
        log.info("Запрошен список всех пользователей");
        return jdbcTemplate.query(SELECT_ALL_USERS, this::mapRowToUser);
    }

    @Override
    public User createUser(User user) {
        validateUser(user);
        log.info("Регистрируем нового пользователя с логином: {}", user.getLogin());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(INSERT_USER, Statement.RETURN_GENERATED_KEYS);
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
        jdbcTemplate.update(UPDATE_USER,
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
        try {
            return jdbcTemplate.queryForObject(SELECT_USER_BY_ID, this::mapRowToUser, id);
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
        jdbcTemplate.update(MERGE_FRIENDSHIP, userId, friendId);
        log.info("Пользователь {} добавил пользователя {} в друзья", userId, friendId);
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        getUserById(userId);
        getUserById(friendId);
        jdbcTemplate.update(DELETE_FRIENDSHIP, userId, friendId);
        log.info("Пользователь {} удалил пользователя {} из друзей", userId, friendId);
    }

    @Override
    public List<User> getFriends(Long userId) {
        getUserById(userId);
        return jdbcTemplate.query(SELECT_FRIENDS, this::mapRowToUser, userId);
    }

    @Override
    public List<User> getCommonFriends(Long userId, Long otherId) {
        getUserById(userId);
        getUserById(otherId);
        return jdbcTemplate.query(SELECT_COMMON_FRIENDS, this::mapRowToUser, userId, otherId);
    }

    private User mapRowToUser(ResultSet rs, int rowNum) throws SQLException {
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
