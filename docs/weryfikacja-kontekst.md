# Weryfikacja warstwy kontekstowej — etap K7

Lista z sekcji 14 rozszerzenia kontekstowego. Wypełnia człowiek w aucie.
Po powrocie: dopisać prawdziwe odpowiedzi poziomów A, B i C do `MockI40Script`.
**Nie zmieniać żadnej stałej na podstawie jednego przejazdu.**

Punkt 2 jest sprawdzianem sensu całego rozszerzenia: jeśli przedmuchiwanie
nigdy nie przypada na skoki korekt, to albo próbkowanie 0,4 Hz jest za wolne
(§7.4), albo hipoteza była błędna. **Zapisuje się wynik, nie naciąga danych.**

`012F` **nie jest na tej liście.** Poprawka P1.

| # | Co zrobić | Czego szukamy | Wynik | Data |
|---|---|---|---|---|
| 1 | Zimny start, panel Mieszanka | Status przechodzi z `1` (za zimny) na `2` (zamknięta). Kiedy? Powinno po kilkudziesięciu sekundach | | |
| 2 | Jazda ustabilizowana 10–15 min | **Czy widać cieniowanie przedmuchiwania** i czy skoki korekt na nie przypadają | | |
| 3 | Mocne przyspieszenie | Status ma przejść na `4` — pełne obciążenie. Korekty w tym czasie zamierają | | |
| 4 | Hamowanie silnikiem, noga z gazu | Też `4` — odcięcie paliwa. Czy dekoder rozróżnia stany | | |
| 5 | Panel Powietrze na jałowym | Podciśnienie ustabilizowane; zadana i rzeczywista przepustnica pokrywają się | | |
| 6 | Panel Powietrze przy zmianach gazu | Czy krzywe zadanej i rzeczywistej nadążają, czy jedna zostaje w tyle | | |
| 7 | Po miesiącu jazdy | Karta miesiąca ma sensowne liczby; wiersz „bez rozgrzania” zgadza się z odczuciem | | |

Punkt 1 przy okazji: od kiedy korekty w ogóle mają sens (dojście do pętli zamkniętej).

## Logi do atrapy

Prawdziwe odpowiedzi poziomów A, B i C (po K1 skład jest w STAN AKTUALNY).
Zapisujemy to, co auto oddało — polecenie + ramka z `>`:

```
polecenie:
odpowiedź:

polecenie:
odpowiedź:

polecenie:
odpowiedź:
```
