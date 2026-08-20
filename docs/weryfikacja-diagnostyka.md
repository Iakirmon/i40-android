# Weryfikacja warstwy diagnostycznej — etap D10

Lista z sekcji 15 rozszerzenia diagnostycznego. Wypełnia człowiek w aucie.
Po powrocie: dopisać prawdziwe odpowiedzi pętli średniej do `MockI40Script`.
**Nie zmieniać żadnej stałej na podstawie jednego przejazdu.**

Punkt 2 jest weryfikacją **źródeł**, nie tylko kodu: jeśli szyna pod pełnym
obciążeniem nie zbliża się do 138 bar, albo pompa słabnie, albo zakres z §4.1
nie opisuje tego silnika — wtedy przepisuje się sekcję 4, a nie naciąga odczyt.

`012F` **nie jest na tej liście.** Poprawka P1: auto zwraca zero niezależnie
od stanu baku; PID wypadł z odpytywania i z wyświetlania. Nie powtarzać.

| # | Co zrobić | Czego szukamy | Wynik | Data |
|---|---|---|---|---|
| 1 | Zimny start, jazda do pełnego rozgrzania | Kolejność krzywych na panelu Termika; czas dojścia płynu do 90 °C; kiedy katalizator przekracza 300 °C | | |
| 2 | Jedno pełne otwarcie przepustnicy na bezpiecznym odcinku | Czy ciśnienie szyny wchodzi w pasmo 138–241 bar | | |
| 3 | Jazda miejska i trasa | Czy korekty zachowują się różnie przy różnym obciążeniu; czy katalizator trzyma się pasma 650–870 °C | | |
| 4 | ~~Sprawdzenie `012F`~~ | **Wykreślone — P1, 2026-08-16.** Nie powtarzać | — | — |
| 4a | Korekta długa `0107` na czwartym kaflu w jeździe | Ma się zmieniać przez minuty. Wartość stojąca w miejscu znaczy, że kafel czyta próbkę, która nie przychodzi | | |
| 4b | Kafel przy pętli otwartej | Czy pokazuje `— ○`, a nie ostatnią liczbę. **Wymaga `0103` — sprawdzalne od K2.** Tu tylko odnotować | odłożone do K2 | |
| 5 | Zapis prawdziwych odpowiedzi pętli średniej | Wpisy do `MockI40Script` (polecenie + ramka z `>`) | | |

## Logi do atrapy

Miejsce na surowe odpowiedzi ELM z pętli średniej (`0123`, `013C`, `010B`, `0111`, `0143`, `0144` — skład D1; po K1 skład A/B się zmienia, zapisujemy to, co auto oddało):

```
polecenie:
odpowiedź:

polecenie:
odpowiedź:
```
