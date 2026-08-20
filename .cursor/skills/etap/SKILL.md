---
name: etap
description: Prowadzi jeden etap realizacji i40-android z sekcji 16 specu. Użyj, gdy prośba brzmi "zrób etap N", "etap 4", "kolejny etap" albo gdy zaczynasz pracę nad nowym fragmentem projektu opisanym w kolejności realizacji.
---

# Etap realizacji

Prowadzisz **jeden** etap. Nie dwa. Nie „etap 5, a przy okazji trochę 6".

**Dwa ciągi etapów:**

- **`0`–`10`** — sekcja 16 dokumentu `docs/spec/2026-08-14-i40-android-design.md`
- **`D1`–`D10`** — sekcja 14 rozszerzenia `docs/spec/2026-08-14-i40-android-diagnostyka-design.md`,
  wchodzą **po etapie 8**; prompty w `2026-08-14-cursor-prompty-diagnostyka.md`
- **`K1`–`K7`** — sekcja 13 rozszerzenia `docs/spec/2026-08-14-i40-android-kontekst-design.md`,
  wchodzą **po D9**; prompty w `2026-08-14-cursor-prompty-kontekst.md`
- **`O1`–`O8`** — sekcja 14 rozszerzenia `docs/spec/2026-08-14-i40-android-odniesienie-design.md`,
  wchodzą **po K6**; prompty w `2026-08-14-cursor-prompty-odniesienie.md`
- **`H1`–`H7`** — sekcja 14 rozszerzenia `docs/spec/2026-08-15-i40-android-historia-design.md`,
  wchodzą **po O8**; prompty w `2026-08-15-cursor-prompty-historia.md`
- **`S1`–`S5`** — sekcja 13 rozszerzenia `docs/spec/2026-08-15-i40-android-objasnienia-design.md`,
  wchodzą **po H7**; prompty w `2026-08-15-cursor-prompty-objasnienia.md`
- **`W1`–`W5`** — sekcja 14 rozszerzenia `docs/spec/2026-08-15-i40-android-wyglad-design.md`,
  wchodzą **po S4**; prompty w `2026-08-15-cursor-prompty-wyglad.md`

Przy etapach `D` dołączaj **oba** specy. Rozszerzenie zmienia bazowy tylko w ośmiu punktach —
lista w jego sekcji 10. Poza nią bazowy obowiązuje bez zmian, a najczęstszy błąd polega na
„przy okazji" ruszeniu pętli gorącej albo progów alarmów.

## Zanim zaczniesz

1. Przeczytaj `docs/spec/2026-08-14-i40-android-design.md` — całość, nie tylko wiersz tabeli
   z sekcji 16. **Sekcje 2, 3 i 10 są obowiązkowe.**

   Z sekcji 2 wynika kolejność pracy: prawdziwy Bluetooth powstaje w etapie 9, po wszystkim
   innym. Sekcja 3 to **siedem rozbieżności między specyfikacją iOS a kodem iOS** plus
   zdekodowane maski tego konkretnego auta — bez niej napiszesz kod odpytujący PID-y, których
   ten samochód nie ma, i timeout 5 s, który wywali się na pierwszym połączeniu. Sekcja 10 to
   wszystkie stałe projektu w jednym miejscu; wpisywanie ich z pamięci jest głównym sposobem,
   w jaki ten projekt może cicho przestać mówić prawdę.

2. Przeczytaj opis tego etapu w `docs/spec/2026-08-14-cursor-prompty.md`. Zawiera zakres,
   kryterium ukończenia i — dla większości etapów — ostrzeżenie o tym, co zwykle idzie nie tak.

3. Sprawdź, czy poprzedni etap jest **faktycznie** ukończony:
   `./gradlew ktlintCheck ; if ($?) { ./gradlew lint } ; if ($?) { ./gradlew test }`
   Jeśli nie jest zielony, zgłoś to i zapytaj, czy najpierw domykamy poprzedni.

## Jak prowadzić

**TDD jest obowiązkowe.** Najpierw test, uruchom go, **pokaż użytkownikowi czerwony wynik**,
dopiero potem implementacja. Test, którego nikt nie widział czerwonego, nie jest testem.

**Zatrzymuj się na kryteriach cząstkowych.** Etapy 2 i 6 idą modułami: jeden moduł, test
czerwony, implementacja, test zielony, raport, następny. Nie rób ośmiu plików i nie pokazuj
wyniku dopiero na końcu.

**Zadawaj sobie pytanie kwalifikujące.** Przy każdej stałej, która trafia do kodu: *skąd ona
jest?* Jeśli odpowiedź nie wskazuje na tabelę w sekcji 9 albo 10, stała jest wymyślona.

**Sprawdzaj maskę, zanim odpytasz PID.** Standard mówi, co **może** istnieć; maska tego
egzemplarza mówi, co **istnieje**. `5C`, `5E` i `10` nie istnieją.

**Nie wybiegaj do przodu.** Jeśli zauważysz, że coś z późniejszego etapu byłoby teraz wygodne —
powiedz o tym i zapytaj. Nie dopisuj tego samodzielnie.

**Nie refaktoryzuj poprzednich etapów** bez wyraźnej prośby.

## Po zakończeniu

Zamknij raportem, który zawiera:

1. wynik `ktlintCheck`, `lint` i `test` — **wklejony, nie streszczony**,
2. listę plików, które powstały albo się zmieniły,
3. wprost: czy kryterium ukończenia z sekcji 16 jest spełnione i skąd to wiesz,
4. liczbę testów po tym etapie (cel końcowy: **nie mniej niż 153**),
5. wszystko, co zostało niedokończone albo pominięte, i dlaczego.

Punkt 5 jest obowiązkowy także wtedy, gdy nic nie zostało pominięte — wtedy napisz, że nic.

Nie zaczynaj kolejnego etapu z własnej inicjatywy. Czekaj na „dalej".

## Etapy, które mają dodatkowe warunki

**Etap 0** — nie piszemy jeszcze logiki OBD. Dwie rzeczy, które łatwo zepsuć: w manifeście
**nie ma** `BLUETOOTH_SCAN` ani uprawnień do lokalizacji (adapter paruje się w ustawieniach
systemu, aplikacja nie skanuje), a `MockI40Script` przenosi się **dosłownie** — z odpowiedzią
na `0100` pociętą na trzy kawałki, z echem przed `ATE0` i z dwoma wpisami dla `0101`.
Nie „poprawiaj" `NO DATA` przy `015C` i `0A` — to prawda o tym samochodzie.

**Etap 1** — najtrudniejszy etap projektu i jedyny, w którym błąd objawia się jako „czasem nie
działa". Trzy pułapki w `.cursor/rules/10-transport-elm.mdc`, wszystkie realne. Test
szeregowania napisz **przed** implementacją i sprawdź, czy naprawdę testuje współbieżność —
implementacja na `Mutex` przechodzi jego część i oblewa kolejność.

**Etap 2** — kończy się **testem regresji masek**. Trzy maski z zapisu z auta muszą dać
dokładnie zbiór z sekcji 3.2: bez `5C`, `5E` i `10`, z `2F`, z odjętymi bitami kontynuacji
`20 40 60 80`. Ten jeden test pilnuje zgodności całego dekodera z prawdziwym samochodem.
**`2F` zostaje w oczekiwaniach mimo poprawki P1** — maska go zgłasza, więc dekoder ma go zwrócić;
to, że auto nie daje danych, rozstrzyga się poziom wyżej.

**Etap 3** — rotacja zimna to **pięć** PID-ów: `46 1F 42 0F 07`. Limit jednego zapytania to
sześć, więc jedno miejsce zostaje wolne — **`2F` wypadł, poprawka P1**. Wersja iOS miała tam
osiem, z czego dwa nieobsługiwane — to rozbieżność 7.
Model oleju przepisz co do stałej; to nie jest miejsce na ulepszenia.

**Etap 4** — napisz test potwierdzający, że `paliwoL` jest `null`, gdy serii `5E` nie ma.
Wygląda na test trywialny, a pilnuje uczciwości pola, którego ten egzemplarz nigdy nie wypełni.

**Etap 5** — tu domykamy **rozbieżność 2**: alarm „nowy kod błędu" w wersji iOS nigdy się nie
odpalał, bo dostawał puste zbiory mimo dwunastu testów reguły. Odczyt `03` co 200 cykli
gorących jest tym, czego brakowało. `TripStateMachine` jest czystą funkcją — testuj ją bez
Androida i pokryj wszystkie przejścia, bo błąd tutaj widać dopiero, gdy zabraknie nagrania.

**Etap 6** — **timeout 25 sekund**, nie 5. To rozbieżność 5: specyfikacja iOS mówi 5 s,
działający kod używa 25, bo `SEARCHING...` przy pierwszej negocjacji potrafi ciągnąć się
kilkanaście sekund. Sekwencja inicjalizacji zawiera `ATI`, `AT@1` i **dwa** wywołania `ATDPN`.
`permanentDTCs` przy `NO DATA` ma być `null`, nie pustą listą.

**Etap 7** — **ostatni moment na cofnięcie decyzji o Compose** (sekcja 6.1). Powiedz o tym
przed napisaniem pierwszego ekranu. Osie Y sztywne, wartości z tabeli 10.7 — to decyzja
projektowa, nie brak funkcji.

**Etap 8** — test decymacji jest **testem sensu całego algorytmu**: seria z pojedynczym ostrym
skokiem po decymacji musi ten skok zachować, a po uśrednieniu by go straciła. Napisz go tak,
żeby wersja ze średnią go oblała.

**Etap 8½ — STYK 1.** Nie jest etapem kodowania. Po etapie 8 aplikacja jest kompletna
i chodzi na zapisie z auta: wgraj APK na radio z transportem `Atrapa` i przejdź listę 15.1 B.
Sprawdzasz instalację, usługę pierwszoplanową, układ na ekranie radia i restart — **wszystko bez
udziału Bluetootha**. To rozdziela „czy aplikacja działa na tym radiu" od „czy Bluetooth
działa na tym radiu", a te dwa problemy mają różne rozwiązania.

**Etap 9** — dopiero teraz sprzęt. `GattPairFinder` jest czystą funkcją i ma testy bez
Androida. Przed `connect()` w SPP zawsze `cancelDiscovery()`.

**Etapy `D1`–`D9`** — warstwa diagnostyczna, po etapie 8. Trzy zakazy obowiązują w każdym
z nich: **nie dotykaj pętli gorącej** (skład, tempo, obowiązkowe `0D`, `05` i `04`), **nie dopisuj
żadnego nowego progu werdyktu** (sekcja 4.1 rozszerzenia — nie mamy na nie źródła), **nie
skaluj automatycznie nowych osi** (zakresy w sekcji 7.6). Dwie pułapki, obie z dopasowaniem
danych: `obciazeniePrzyMaxCisnieniu` dopasowuje się **po czasie, nie po indeksie tablicy**,
a wspólny suwak w raporcie musi radzić sobie z pasmami o **czterokrotnie różnej gęstości
próbek**, bo pętla średnia chodzi rzadziej niż gorąca.

**Etapy `O1`–`O8`** — warstwa odniesienia, po K6. Dwa zakazy w każdym etapie: **nie dokładaj
żadnego zapytania OBD** (rozszerzenie liczy z próbek, które już płyną — jest osobny test, który
tego pilnuje) i **nie nazywaj wartości z historii normą**. Trzy pułapki: minimalne okno to
**dwadzieścia cykli gorących**, nie sekundy; **mediana, nie średnia**; przejścia panelu
Podstawowy są **asymetryczne** — wejście po pełnym obiegu, wyjście natychmiast.

**Etapy `H1`–`H7`** — warstwa historii, po O8. Trzy zakazy: **nie dokładaj żadnego zapytania
OBD** (rozszerzenie liczy wyłącznie z podsumowań już zapisanych w bazie — jest test
porównujący listę odpytywanych PID-ów przed i po), **nie rób paska z cofnięciem** (sekcja 10
odrzuca go świadomie: w jadącym aucie jest bezużyteczny, a zdejmuje powagę z okna
potwierdzenia) i **nie oznaczaj żadnej wartości w panelu Porządki jako zalecanej** — to
kryteria wyboru, nie progi diagnostyczne. Dwie pułapki: `onUpgrade` musi używać **osobnych
`jeśli`**, nigdy `gdy/inaczej` (aktualizacja z wersji 1 na 3 wykona wtedy tylko pierwszy krok —
to najczęstszy błąd w `onUpgrade` i psuje dane u użytkownika z najstarszą bazą), a **filtr musi
zawężać kropki w kalendarzu i listę dnia jednocześnie** — dzień z kropką, który po dotknięciu
jest pusty, to najbardziej mylący możliwy błąd tego ekranu.

**Etapy `S1`–`S5`** — warstwa objaśnień, po H7. Trzy zakazy: **nie dokładaj zapytania OBD ani
progu** (rozszerzenie liczy z pasm i reguł, które już są), **nie dokładaj dźwięku** (alarmów
jest pięć) i — najważniejsze — **nie pisz ani jednego zdania treści słownika**. Siedemdziesiąt
haseł jest gotowych w `docs/slownik.md`; zadaniem etapu S3 jest przenieść je **co do zdania**.
Rubryki „gdy wyjdzie poza pasmo” i „czego to nie mówi” to dokładnie te miejsca,
w których zmyślone wyjaśnienie wygląda identycznie jak prawdziwe. Jedna pułapka, za to poważna:
w `stanParametru` **`NIE_ZMIERZONY` musi być sprawdzany pierwszy** — inaczej parametr, którego
nie odczytano, wyjdzie jako „w normie”, a to jedyna rzecz, którą ta warstwa może naprawdę
zepsuć.

**Etapy `W1`–`W5`** — warstwa wyglądu, po S4. Cztery zakazy: **żaden literał koloru poza
`Theme.kt`**, **żadne wygładzanie krzywej** (spline rysuje wartości, których czujnik nie podał —
ta sama nieprawda co zero zamiast kreski), **żadna igła, tarcza ani obrotomierz** (tak wygląda
każda aplikacja OBD na rynku) i **dokładnie jedna animacja** — linia skanująca. Jedna pułapka:
linie siatki pod wykresem to **granice pasm z `PasmaOdniesienia`**, nie okrągłe liczby co dwadzieścia
jednostek; biblioteki wykresów robią to drugie domyślnie i wtedy cały pomysł znika. `W1` zaczyna
się od **odczytania gęstości ekranu na prawdziwym radiu** — karta produktu mówi 8 cali, instrukcja
z pudełka wymienia 9 i 10, więc się tego nie zgaduje.

**Etap 10** — nie da się zrobić w Cursorze. Przygotuj listę kontrolną, przyjmij logi z auta,
dopisz je do atrapy, uruchom testy na nowych danych. **Nie zmieniaj żadnej innej stałej na
podstawie jednego przejazdu.**
