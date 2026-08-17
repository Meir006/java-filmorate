package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.genre.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaDbStorage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JdbcTest
@AutoConfigureTestDatabase
@Import({GenreDbStorage.class, MpaDbStorage.class})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class GenreAndMpaDbStorageTest {

    private final GenreDbStorage genreStorage;
    private final MpaDbStorage mpaStorage;

    @Test
    void getAllGenresShouldReturnSixSeededGenres() {
        List<Genre> genres = genreStorage.getAllGenres();

        assertThat(genres).hasSize(6);
        assertThat(genres.get(0).getId()).isEqualTo(1);
    }

    @Test
    void getGenreByIdShouldReturnGenre() {
        Genre genre = genreStorage.getGenreById(1);

        assertThat(genre.getName()).isEqualTo("Комедия");
    }

    @Test
    void getGenreByIdWithUnknownIdShouldThrowNotFoundException() {
        assertThrows(NotFoundException.class, () -> genreStorage.getGenreById(999));
    }

    @Test
    void getAllMpaShouldReturnExactlyFiveRatingsInOrder() {
        List<Mpa> ratings = mpaStorage.getAllMpa();

        assertThat(ratings).hasSize(5);
        assertThat(ratings).extracting(Mpa::getId).containsExactly(1, 2, 3, 4, 5);
        assertThat(ratings).extracting(Mpa::getName).containsExactly("G", "PG", "PG-13", "R", "NC-17");
    }

    @Test
    void getMpaByIdShouldReturnRating() {
        Mpa mpa = mpaStorage.getMpaById(1);

        assertThat(mpa.getName()).isEqualTo("G");
    }

    @Test
    void getMpaByIdWithUnknownIdShouldThrowNotFoundException() {
        assertThrows(NotFoundException.class, () -> mpaStorage.getMpaById(999));
    }
}
