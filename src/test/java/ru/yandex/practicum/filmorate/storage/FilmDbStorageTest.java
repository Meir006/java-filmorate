package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JdbcTest
@AutoConfigureTestDatabase
@Import({FilmDbStorage.class, UserDbStorage.class})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class FilmDbStorageTest {

    private final FilmDbStorage filmStorage;
    private final UserDbStorage userStorage;

    private Film newFilm(String name) {
        Film film = new Film();
        film.setName(name);
        film.setDescription("Описание фильма " + name);
        film.setReleaseDate(LocalDate.of(2010, 6, 15));
        film.setDuration(120);
        film.setMpa(new Mpa(1, null));
        return film;
    }

    private User newUser(String login) {
        User user = new User();
        user.setEmail(login + "@mail.ru");
        user.setLogin(login);
        user.setName(login);
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return user;
    }

    @Test
    void addFilmShouldAssignGeneratedIdAndMpaName() {
        Film created = filmStorage.addFilm(newFilm("Матрица"));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getMpa().getId()).isEqualTo(1);
        assertThat(created.getMpa().getName()).isEqualTo("G");
    }

    @Test
    void addFilmWithGenresShouldPersistGenres() {
        Film film = newFilm("Фильм с жанрами");
        Set<Genre> genres = new LinkedHashSet<>();
        genres.add(new Genre(1, null));
        genres.add(new Genre(2, null));
        film.setGenres(genres);

        Film created = filmStorage.addFilm(film);

        assertThat(created.getGenres()).extracting(Genre::getId).containsExactly(1, 2);
    }

    @Test
    void addFilmWithUnknownMpaShouldThrowNotFoundException() {
        Film film = newFilm("Фильм с неизвестным рейтингом");
        film.setMpa(new Mpa(999, null));

        assertThrows(NotFoundException.class, () -> filmStorage.addFilm(film));
    }

    @Test
    void addFilmWithUnknownGenreShouldThrowNotFoundException() {
        Film film = newFilm("Фильм с неизвестным жанром");
        film.setGenres(new LinkedHashSet<>(Set.of(new Genre(999, null))));

        assertThrows(NotFoundException.class, () -> filmStorage.addFilm(film));
    }

    @Test
    void getFilmByIdShouldReturnSavedFilm() {
        Film created = filmStorage.addFilm(newFilm("Начало"));

        Film found = filmStorage.getFilmById(created.getId());

        assertThat(found.getName()).isEqualTo("Начало");
    }

    @Test
    void getFilmByIdWithUnknownIdShouldThrowNotFoundException() {
        assertThrows(NotFoundException.class, () -> filmStorage.getFilmById(999L));
    }

    @Test
    void getFilmsShouldReturnAllSavedFilms() {
        filmStorage.addFilm(newFilm("Фильм 1"));
        filmStorage.addFilm(newFilm("Фильм 2"));

        assertThat(filmStorage.getFilms().size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void updateFilmShouldReplaceFieldsAndGenres() {
        Film created = filmStorage.addFilm(newFilm("Старое имя"));
        created.setName("Новое имя");
        created.setGenres(new LinkedHashSet<>(Set.of(new Genre(3, null))));

        Film updated = filmStorage.updateFilm(created);

        assertThat(updated.getName()).isEqualTo("Новое имя");
        assertThat(updated.getGenres()).extracting(Genre::getId).containsExactly(3);
    }

    @Test
    void updateFilmWithUnknownIdShouldThrowNotFoundException() {
        Film film = newFilm("Фантом");
        film.setId(999L);

        assertThrows(NotFoundException.class, () -> filmStorage.updateFilm(film));
    }

    @Test
    void addLikeAndGetPopularShouldOrderByLikeCount() {
        Film popular = filmStorage.addFilm(newFilm("Популярный"));
        Film unpopular = filmStorage.addFilm(newFilm("Непопулярный"));
        User user1 = userStorage.createUser(newUser("liker1"));
        User user2 = userStorage.createUser(newUser("liker2"));

        filmStorage.addLike(popular.getId(), user1.getId());
        filmStorage.addLike(popular.getId(), user2.getId());

        List<Film> topOne = filmStorage.getPopular(1);

        assertThat(topOne).hasSize(1);
        assertThat(topOne.get(0).getId()).isEqualTo(popular.getId());
    }

    @Test
    void removeLikeShouldDecreasePopularity() {
        Film film = filmStorage.addFilm(newFilm("С лайком"));
        User user = userStorage.createUser(newUser("liker3"));
        filmStorage.addLike(film.getId(), user.getId());

        filmStorage.removeLike(film.getId(), user.getId());

        List<Film> popular = filmStorage.getPopular(10);
        assertThat(popular).extracting(Film::getId).contains(film.getId());
    }
}
