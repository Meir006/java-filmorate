-- Используем MERGE, а не INSERT: файловая база H2 сохраняется между перезапусками
-- приложения, и обычный INSERT привёл бы к нарушению первичного ключа при повторном старте.

MERGE INTO mpa_ratings (mpa_id, name) VALUES (1, 'G');
MERGE INTO mpa_ratings (mpa_id, name) VALUES (2, 'PG');
MERGE INTO mpa_ratings (mpa_id, name) VALUES (3, 'PG-13');
MERGE INTO mpa_ratings (mpa_id, name) VALUES (4, 'R');
MERGE INTO mpa_ratings (mpa_id, name) VALUES (5, 'NC-17');

MERGE INTO genres (genre_id, name) VALUES (1, 'Комедия');
MERGE INTO genres (genre_id, name) VALUES (2, 'Драма');
MERGE INTO genres (genre_id, name) VALUES (3, 'Мультфильм');
MERGE INTO genres (genre_id, name) VALUES (4, 'Триллер');
MERGE INTO genres (genre_id, name) VALUES (5, 'Документальный');
MERGE INTO genres (genre_id, name) VALUES (6, 'Боевик');
