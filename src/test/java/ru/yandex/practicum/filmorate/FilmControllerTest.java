package ru.yandex.practicum.filmorate;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FilmControllerTest {

    private FilmController filmController;

    @BeforeEach
    void setUp() {
        filmController = new FilmController();
    }

    @Test
    void addFilmWithValidDataShouldSuccess() {
        Film film = new Film();
        film.setName("Matrix");
        film.setDescription("Sci-Fi movie");
        film.setReleaseDate(LocalDate.of(1999, 3, 31));
        film.setDuration(136);

        Film created = filmController.addFilm(film);

        assertNotNull(created.getId());
        assertEquals("Matrix", created.getName());
        assertEquals(1, filmController.getFilms().size());
    }

    @Test
    void addFilmWithReleaseDateBeforeCinemaBirthdayShouldThrowException() {
        Film film = new Film();
        film.setName("Old Film");
        film.setDescription("Very old");
        film.setReleaseDate(LocalDate.of(1895, 12, 27)); // на 1 день раньше 28.12.1895
        film.setDuration(100);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> filmController.addFilm(film));
        assertTrue(exception.getMessage().contains("28 декабря 1895 года"));
    }

    @Test
    void addFilmWithReleaseDateOnCinemaBirthdayShouldSuccess() {
        Film film = new Film();
        film.setName("First Film");
        film.setDescription("Arrival of a Train");
        film.setReleaseDate(LocalDate.of(1895, 12, 28));
        film.setDuration(1);

        Film created = filmController.addFilm(film);

        assertNotNull(created.getId());
    }
}