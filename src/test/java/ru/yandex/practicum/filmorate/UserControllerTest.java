package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerTest {

    private UserController userController;

    @BeforeEach
    void setUp() {
        userController = new UserController();
    }

    @Test
    void createUserWithValidDataShouldSucceed() {
        User user = new User();
        user.setEmail("user@yandex.ru");
        user.setLogin("user_login");
        user.setName("User Name");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        User createdUser = userController.createUser(user);

        assertNotNull(createdUser);
        assertEquals(1, createdUser.getId());
        assertEquals("user_login", createdUser.getLogin());
        
        Collection<User> users = userController.getUsers();
        assertEquals(1, users.size());
    }

    @Test
    void createUserWithEmptyNameShouldUseLoginAsName() {
        User user = new User();
        user.setEmail("user@yandex.ru");
        user.setLogin("user_login");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        User createdUser = userController.createUser(user);

        assertNotNull(createdUser);
        assertEquals("user_login", createdUser.getName());
    }

    @Test
    void createUserWithLoginContainingSpacesShouldThrowException() {
        User user = new User();
        user.setEmail("user@yandex.ru");
        user.setLogin("invalid login");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userController.createUser(user));

        assertTrue(exception.getMessage().contains("пробел"));
    }
}
