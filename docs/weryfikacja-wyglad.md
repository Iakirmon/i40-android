# Weryfikacja warstwy wyglądu — etap W5

Lista z sekcji 11 rozszerzenia wyglądu. Wypełnia człowiek na radiu.
**Nie zmieniać żadnej stałej na podstawie jednego przejazdu.**

Punkt 2 jest najważniejszy: motyw DZIEŃ istnieje wyłącznie po to, żeby wygrać ze słońcem.
Jeśli nie wygrywa — popraw go albo usuń, nie zostawiaj „bo już jest".

Zero nowych zapytań OBD. Zero nowych progów.

| # | Co zrobić | Czego szukamy | Wynik | Data |
|---|---|---|---|---|
| 1 | Odczytaj `smallestScreenWidthDp` i gęstość, wpisz do §7 specu wyglądu | Rozstrzygnięcie niewiadomej z §7 | | |
| 2 | Zaparkuj w pełnym słońcu, przełącz na DZIEŃ | **Wykres czytelny** — jeśli nie, jasne tło / ślad do poprawy | | |
| 3 | To samo miejsce, motyw NOC | Porównaj — potwierdza, po co są dwa motywy | | |
| 4 | Jazda nocą | Ekran nie oślepia w lusterku ani kątem oka | | |
| 5 | Przełącz motyw w trakcie nagrywania | **Nagrywanie nie przerywa się** | | |
| 6 | Zerknij na wartość zmieniającą się przy 4 Hz | **Liczba nie drga w poziomie** — cyfry tabelaryczne | | |
| 7 | Przełącz panel w ruchu, palcem, na nierównej drodze | Trafiasz za pierwszym razem (≥ 56 dp) | | |
| 8 | Odpal zimny silnik, patrz na puste pola | Linia skanująca widoczna, znika po pierwszym odczycie | | |
| 9 | Podkręć czcionkę systemową do maksimum | Układ trzyma się do 1,3×, wyżej degraduje się jawnie | | |

## Logi

Nic nie dopisujemy do atrapy z tego rozszerzenia: **zero nowych zapytań OBD**.

## Uwagi z realizacji (Cursor)

- Gęstość (§7) w Cursorze **nieodczytana** — obowiązuje założenie `sw600dp` do punktu 1 na radiu.
- Tokeny wyłącznie w `Theme.kt`; Inter + JetBrains Mono (SIL OFL) w `res/font/`.
- Jedyna animacja: `LiniaSkanujaca.kt`. Siatka z `SiatkaPasma` / `PasmaOdniesienia`.
