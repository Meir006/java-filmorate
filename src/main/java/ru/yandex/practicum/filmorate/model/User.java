package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.time.LocalDate;

@Data
public class User {
    private Long id;

    @NotBlank(message = "Электронная почта обязательна для заполнения")
    @Email(message = "Проверьте формат почты — кажется, тут ошибка (нужен символ @)")
    private String email;

    @NotBlank(message = "Придумайте логин — он не может быть пустым")
    private String login;

    private String name;

    @PastOrPresent(message = "Дата рождения не может быть в будущем")
    private LocalDate birthday;
}