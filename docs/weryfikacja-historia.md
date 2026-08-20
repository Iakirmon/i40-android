# Weryfikacja warstwy historii — etap H7

Lista z sekcji 15 rozszerzenia historii. Wypełnia człowiek na radiu, z prawdziwym zbiorem nagrań.
**Nie zmieniać żadnej stałej na podstawie jednego przejazdu.**

Punkt 1 jest powodem tej warstwy: gest na wyboju otwiera okno i **nic nie ginie**.
Punkt 9 jest najważniejszy technicznie: jedyny, który sprawdza migrację na prawdziwych danych.
**Zrób kopię pliku bazy przed punktem 9.**

Zero nowych zapytań OBD. `012F` nie jest na tej liście.

| # | Co zrobić | Czego szukamy | Wynik | Data |
|---|---|---|---|---|
| 1 | Przesuń pozycję w bok podczas jazdy po nierównej drodze | Okno się otwiera, **nic nie ginie** | | |
| 2 | Skasuj jeden przejazd, wróć do karty miesiąca | Liczby przeliczone; brak pustego wiersza po skasowanej pozycji | | |
| 3 | Skasuj przejazd, z którego powstał punkt odniesienia; otwórz przegląd | Kolumna „poprzednio" **niezmieniona** | | |
| 4 | Oznacz przejazd jako chroniony, uruchom Porządki z kryterium, które go łapie | Nie ma go na liście; wiersz „1 chroniony — pominięty" | | |
| 5 | Uruchom Porządki podczas nagrywania | Wiersz „1 nagrywana teraz — pominięta"; bieżąca sesja nietknięta | | |
| 6 | Skasuj > 50 przejazdów, sprawdź zajętość w ustawieniach Androida | Miejsce **naprawdę** zwolnione — `VACUUM` zadziałał | | |
| 7 | Porównaj dwa przejazdy o bardzo różnej długości | Dystans i czas na górze; **żadnego ostrzeżenia ani oceny** | | |
| 8 | Włącz filtr `z kodami` | Kropki w kalendarzu i lista dnia zawężone **zgodnie** | | |
| 9 | Zainstaluj wersję z H1 na radiu z bazą sprzed rozszerzenia. **Kopia pliku bazy najpierw.** | Migracja przechodzi, stare przejazdy widoczne, nic nie zginęło | | |
| 10 | Odczytaj kartę „od początku" po miesiącu jazdy | Sumy zgadzają się z odczuciem; „bez rozgrzania" wygląda sensownie | | |

## Logi

Nic nie dopisujemy do atrapy z tego rozszerzenia: **zero nowych zapytań OBD**.
Jeśli przy weryfikacji wyjdzie inna odpowiedź istniejącego PID-u niż w `MockI40Script`,
dopisać ją tam, nie zmieniać stałych.

## Uwagi z realizacji (Cursor)

- Schemat w kodzie to wersja **4**: O2/O3 zużyły wersję 3 na tabelę `przeglad`, więc `chroniony` stoi na 4. Drabinka: osobne `jeśli` (`< 2` punkt, `< 3` przegląd, `< 4` chroniony). `onDowngrade` rzuca.
- Testy migracji i `VACUUM` na JVM to szpiedzy kroków / flagi, nie żywe SQLite (brak Robolectric). Punkt 6 i 9 rozstrzygają to na radiu.
- `paliwoL` w porównaniu nie ma wiersza — to auto nie ma `5E`; kreska jest w odczytach, nie w bloku porównania.
