package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Хранилище пользователей в памяти. Оставлено для справки/тестов —
 * основным хранилищем приложения является {@link UserDbStorage}.
 */
@Slf4j
@Component("inMemoryUserStorage")
public class InMemoryUserStorage implements UserStorage {

    private final Map<Long, User> users = new HashMap<>();
    private final Map<Long, Set<Long>> friends = new HashMap<>();
    private long idCounter = 0;

    @Override
    public Collection<User> getUsers() {
        log.info("Запрошен список всех пользователей. Всего зарегистрировано: {}", users.size());
        return users.values();
    }

    @Override
    public User createUser(User user) {
        log.info("Регистрируем нового пользователя с логином: {}", user.getLogin());
        validateUser(user);
        user.setId(getNextId());
        users.put(user.getId(), user);
        friends.put(user.getId(), new HashSet<>());
        log.info("Пользователь {} (ID {}) успешно создан", user.getLogin(), user.getId());
        return user;
    }

    @Override
    public User updateUser(User newUser) {
        log.info("Обновляем профиль пользователя с ID {}", newUser.getId());
        if (newUser.getId() == null) {
            log.error("Не удалось обновить пользователя: не передан ID");
            throw new ValidationException("Для обновления профиля необходимо указать ID пользователя");
        }
        if (!users.containsKey(newUser.getId())) {
            log.error("Пользователь с ID {} не зарегистрирован", newUser.getId());
            throw new NotFoundException("Пользователь с ID " + newUser.getId() + " не найден");
        }
        validateUser(newUser);
        users.put(newUser.getId(), newUser);
        log.info("Профиль пользователя с ID {} успешно обновлён", newUser.getId());
        return newUser;
    }

    @Override
    public User getUserById(Long id) {
        User user = users.get(id);
        if (user == null) {
            log.error("Пользователь с ID {} не зарегистрирован", id);
            throw new NotFoundException("Пользователь с ID " + id + " не найден");
        }
        return user;
    }

    @Override
    public void addFriend(Long userId, Long friendId) {
        getUserById(userId);
        getUserById(friendId);
        friends.computeIfAbsent(userId, k -> new HashSet<>()).add(friendId);
        log.info("Пользователь {} добавил пользователя {} в друзья", userId, friendId);
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        getUserById(userId);
        getUserById(friendId);
        friends.getOrDefault(userId, Collections.emptySet()).remove(friendId);
        log.info("Пользователь {} удалил пользователя {} из друзей", userId, friendId);
    }

    @Override
    public List<User> getFriends(Long userId) {
        getUserById(userId);
        return friends.getOrDefault(userId, Collections.emptySet()).stream()
                .map(this::getUserById)
                .collect(Collectors.toList());
    }

    @Override
    public List<User> getCommonFriends(Long userId, Long otherId) {
        getUserById(userId);
        getUserById(otherId);
        Set<Long> userFriends = friends.getOrDefault(userId, Collections.emptySet());
        Set<Long> otherFriends = friends.getOrDefault(otherId, Collections.emptySet());
        return userFriends.stream()
                .filter(otherFriends::contains)
                .map(this::getUserById)
                .collect(Collectors.toList());
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

    private Long getNextId() {
        return ++idCounter;
    }
}
