package ru.yandex.practicum.filmorate.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mpa (возрастной рейтинг фильма — американская система MPAA).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Mpa {
    private Integer id;
    private String name;
}
