package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JdbcTest
@AutoConfigureTestDatabase
@Import(UserDbStorage.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class UserDbStorageTest {

    private final UserDbStorage userStorage;

    private User newUser(String email, String login) {
        User user = new User();
        user.setEmail(email);
        user.setLogin(login);
        user.setName(login + " name");
        user.setBirthday(LocalDate.of(1995, 5, 20));
        return user;
    }

    @Test
    void createUserShouldAssignGeneratedId() {
        User created = userStorage.createUser(newUser("a@mail.ru", "alogin"));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getEmail()).isEqualTo("a@mail.ru");
    }

    @Test
    void createUserWithBlankNameShouldUseLoginAsName() {
        User user = newUser("b@mail.ru", "blogin");
        user.setName("");

        User created = userStorage.createUser(user);

        assertThat(created.getName()).isEqualTo("blogin");
    }

    @Test
    void createUserWithSpacesInLoginShouldThrowValidationException() {
        User user = newUser("c@mail.ru", "bad login");

        assertThrows(ValidationException.class, () -> userStorage.createUser(user));
    }

    @Test
    void getUserByIdShouldReturnSavedUser() {
        User created = userStorage.createUser(newUser("d@mail.ru", "dlogin"));

        User found = userStorage.getUserById(created.getId());

        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getLogin()).isEqualTo("dlogin");
    }

    @Test
    void getUserByIdWithUnknownIdShouldThrowNotFoundException() {
        assertThrows(NotFoundException.class, () -> userStorage.getUserById(999L));
    }

    @Test
    void getUsersShouldReturnAllSavedUsers() {
        userStorage.createUser(newUser("e1@mail.ru", "e1"));
        userStorage.createUser(newUser("e2@mail.ru", "e2"));

        Collection<User> users = userStorage.getUsers();

        assertThat(users.size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void updateUserShouldChangeStoredFields() {
        User created = userStorage.createUser(newUser("f@mail.ru", "flogin"));
        created.setName("Updated Name");
        created.setEmail("new-f@mail.ru");

        User updated = userStorage.updateUser(created);

        assertThat(updated.getName()).isEqualTo("Updated Name");
        User reloaded = userStorage.getUserById(created.getId());
        assertThat(reloaded.getEmail()).isEqualTo("new-f@mail.ru");
    }

    @Test
    void updateUserWithUnknownIdShouldThrowNotFoundException() {
        User user = newUser("g@mail.ru", "glogin");
        user.setId(999L);

        assertThrows(NotFoundException.class, () -> userStorage.updateUser(user));
    }

    @Test
    void addFriendShouldBeOneDirectional() {
        User user1 = userStorage.createUser(newUser("h1@mail.ru", "h1"));
        User user2 = userStorage.createUser(newUser("h2@mail.ru", "h2"));

        userStorage.addFriend(user1.getId(), user2.getId());

        List<User> user1Friends = userStorage.getFriends(user1.getId());
        List<User> user2Friends = userStorage.getFriends(user2.getId());

        assertThat(user1Friends).extracting(User::getId).containsExactly(user2.getId());
        assertThat(user2Friends).isEmpty();
    }

    @Test
    void removeFriendShouldDeleteRelation() {
        User user1 = userStorage.createUser(newUser("i1@mail.ru", "i1"));
        User user2 = userStorage.createUser(newUser("i2@mail.ru", "i2"));
        userStorage.addFriend(user1.getId(), user2.getId());

        userStorage.removeFriend(user1.getId(), user2.getId());

        assertThat(userStorage.getFriends(user1.getId())).isEmpty();
    }

    @Test
    void getCommonFriendsShouldReturnIntersection() {
        User user1 = userStorage.createUser(newUser("j1@mail.ru", "j1"));
        User user2 = userStorage.createUser(newUser("j2@mail.ru", "j2"));
        User common = userStorage.createUser(newUser("j3@mail.ru", "j3"));

        userStorage.addFriend(user1.getId(), common.getId());
        userStorage.addFriend(user2.getId(), common.getId());

        List<User> commonFriends = userStorage.getCommonFriends(user1.getId(), user2.getId());

        assertThat(commonFriends).extracting(User::getId).containsExactly(common.getId());
    }
}
