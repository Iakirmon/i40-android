# Weryfikacja warstwy odniesienia — etap O8

Lista z sekcji 15 rozszerzenia odniesienia. Wypełnia człowiek w aucie.
**Nie zmieniać definicji stanu ani żadnej stałej na podstawie jednego przejazdu.**

Punkt 6 jest sprawdzianem sensu całego rozszerzenia: jeśli po tygodniu zakres
obrotów na jałowym wynosi 690–760, jest zbyt szeroki, żeby cokolwiek znaczyć.
Wtedy trzeba albo zawęzić definicję stanu, albo uznać, że dla tego parametru
nie da się zrobić sensownego odniesienia. **Wynik negatywny też się zapisuje.**

`012F` **nie jest na tej liście.** Poprawka P1.

| # | Co zrobić | Czego szukamy | Wynik | Data |
|---|---|---|---|---|
| 1 | Przejazd 15 min zakończony postojem z pracującym silnikiem | Czy powstał punkt. Na ekranie diagnostycznym: liczba punktów i liczba próbek | | |
| 2 | Postój na światłach po 10 min jazdy | Panel Podstawowy przełącza się po ~5 s i wraca **natychmiast** po ruszeniu | | |
| 3 | Drugi przejazd tego samego dnia | Pojawia się `poprzednio`, a po nim zakres | | |
| 4 | Przegląd na rozgrzanym silniku | Nagłówek: `● jałowy rozgrzany`, porównanie liczbowe dostępne | | |
| 5 | Przegląd na zimnym silniku | Nagłówek: `○`; **liczby nieporównane, kody porównane** | | |
| 6 | Po tygodniu | Czy zakres jest wąski, czy szeroki — i czy ma to sens | | |

## Logi

Nic nie dopisujemy do atrapy z tego rozszerzenia: **zero nowych zapytań OBD**.
Jeśli przy weryfikacji wyjdzie inna odpowiedź istniejącego PID-u niż w `MockI40Script`,
dopisać ją tam, nie zmieniać stałych.
