package ru.yandex.practicum.filmorate.model;

/**
 * Film.
 */

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class Film {
    private Long id;

    @NotBlank(message = "Укажите название фильма — оно не может быть пустым")
    private String name;

    @Size(max = 200, message = "Описание слишком длинное — максимум 200 символов")
    private String description;

    @NotNull(message = "Укажите дату выхода фильма")
    private LocalDate releaseDate;

    @NotNull(message = "Укажите продолжительность фильма")
    @Positive(message = "Продолжительность фильма должна быть больше нуля")
    private Integer duration;
}