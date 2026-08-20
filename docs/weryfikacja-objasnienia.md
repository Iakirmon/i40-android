# Weryfikacja warstwy objaśnień — etap S5

Lista z sekcji 14 rozszerzenia objaśnień. Wypełnia człowiek na radiu, z zimnym silnikiem
i prawdziwym przejazdem. **Nie zmieniać żadnej stałej na podstawie jednego przejazdu.**

Punkt 1 jest najważniejszy: uspokajający komunikat przed pierwszym pomiarem byłby
szkodliwy. Punkt 3 mówi, czy panel da się czytać w jednym spojrzeniu — jeśli nie,
trzeba go przerzedzić, nie „przyzwyczaić się".

Zero nowych zapytań OBD. Zero nowych progów. `012F` nie jest na tej liście.

| # | Co zrobić | Czego szukamy | Wynik | Data |
|---|---|---|---|---|
| 1 | Odpal zimny silnik, patrz na panel Stan przez pierwsze pół minuty | **„Jeszcze nie wiem", potem „silnik się rozgrzewa"** — ani razu „wszystko w normie" | | |
| 2 | Poczekaj do rozgrzania na postoju | Przejście na „wszystko w normie" z drugą linijką o rozgrzaniu | | |
| 3 | Zerknij na panel w ruchu, licz czas | **Da się odczytać w jednym spojrzeniu** — jeśli nie, panel jest za gęsty | | |
| 4 | Doprowadź do odchylenia (np. zimny silnik, wysokie obroty) | Zdanie **identyczne** z tym, które daje Przegląd | | |
| 5 | Spróbuj otworzyć słownik w ruchu | Nie otwiera się | | |
| 6 | Otwórz słownik na postoju, przejdź trzy odsyłacze w głąb | Pojawia się `wróć do początku` | | |
| 7 | Porównaj `Norma` w słowniku z kolumną normy w Odczytach | **Identyczne** — jedno źródło działa | | |
| 8 | Przejdź wszystkie 33 wiersze Odczytów, dotykając każdego | Każdy otwiera hasło; żadnej pustej rubryki | | |

## Logi

Nic nie dopisujemy do atrapy z tego rozszerzenia: **zero nowych zapytań OBD**.
Jeśli przy weryfikacji wyjdzie inna odpowiedź istniejącego PID-u niż w `MockI40Script`,
dopisać ją tam, nie zmieniać stałych.

## Uwagi z realizacji (Cursor)

- Treść haseł wyłącznie z `docs/slownik.md` / `assets/slownik.md` — test porównuje co do zdania.
- Kontrakt haseł: 32 PID-y z §8.2 + kafle + wykresy + 33 wiersze Odczytów; `012F` bez mapowania.
- Blokada słownika: `WejscieSlownika.moznaOtworzyc` przy `wRuchu` — przełączanie paneli bez zmian.
- Schemat bazy w kodzie to wersja **4** (`chroniony` po O2/O3); warstwa objaśnień **nie** zmienia schematu.
