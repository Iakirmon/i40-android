# i40-android — prompty dla Cursora: rozszerzenie historii

Siedem etapów `H1`–`H7`, **jeden nowy czat na etap**. Wchodzą **po etapie O8** rozszerzenia
odniesienia.

---

## Jak to prowadzić

**Do każdego promptu dołącz pięć dokumentów:**

```
@docs/spec/2026-08-14-i40-android-design.md
@docs/spec/2026-08-14-i40-android-diagnostyka-design.md
@docs/spec/2026-08-14-i40-android-kontekst-design.md
@docs/spec/2026-08-14-i40-android-odniesienie-design.md
@docs/spec/2026-08-15-i40-android-historia-design.md
```

**Najważniejsze zdanie całego rozszerzenia — sekcja 2 specu:**

> **Przejazd z 14 marca o 8:15 zdarzył się raz i nigdy więcej się nie zdarzy.**

Dane w tej aplikacji mają cechę, której nie mają dane w większości programów: **nie da się ich
zebrać ponownie**. Dlatego każde kasowanie ma potwierdzenie, potwierdzenie wymienia, co ginie,
i nie ma cofania paskiem — bo pasek w jadącym aucie jest bezużyteczny.

**Po każdym etapie:**

```
./gradlew ktlintCheck ; if ($?) { ./gradlew lint } ; if ($?) { ./gradlew test }
```

**Pięć pytań kontrolnych:**

*„Czy dołożyłem jakiekolwiek zapytanie OBD?"* — **nie wolno.** To rozszerzenie liczy wyłącznie
z podsumowań już zapisanych w bazie. Sekcja 3.1, jest test.

*„Czy ta droga do skasowania ma potwierdzenie?"* — **wszystkie trzy** muszą prowadzić do tego
samego okna. Gest w bok też. Sekcja 4.1.

*„Czy sesja `w_toku` jest odrzucana?"* — trzy drogi, trzy osobne testy. Usługa właśnie do niej
pisze. Sekcja 4.5.

*„Czy ta liczba to próg, czy kryterium wyboru?"* — `5 min` w Porządkach niczego nie twierdzi
o samochodzie. Próg diagnostyczny wymaga źródła; kryterium wyboru nie. Sekcja 5.4.

*„Czy kolumna różnicy niczego nie ocenia?"* — sama liczba ze znakiem. Bez strzałek, bez kolorów,
bez słów. Sekcja 6.5.

---

## Etap H1 — migracja schematu i czyste funkcje wyboru

```
Etap H1 z sekcji 14 specu historii. Kontrakt: sekcje 11.1, 11.2, 5.4.

1. DRABINKA WERSJI SCHEMATU — sekcja 11.2. Wersja bazy 1 -> 3.

   wersja 1: przejazd                                (bazowy 8.6)
   wersja 2: punkt_odniesienia, przeglad             (odniesienie 8.1, 8.2)
   wersja 3: przejazd.chroniony                      (to rozszerzenie)

   UWAGA — TO NAPRAWIA ISTNIEJACA LUKE. Rozszerzenie odniesienia dokladalo dwie
   tabele NIE PODNOSZAC WERSJI. Jesli w kodzie jest DB_VERSION = 1 mimo tych
   tabel, to jest wlasnie ten blad — popraw go tutaj.

   onUpgrade(db, stare, nowe):
       jesli stare < 2:  CREATE TABLE punkt_odniesienia ...
                         CREATE TABLE przeglad ...
                         + indeksy z sekcji 8.1 i 8.2 odniesienia
       jesli stare < 3:  ALTER TABLE przejazd
                         ADD COLUMN chroniony INTEGER NOT NULL DEFAULT 0

   OSOBNE "jesli", NIGDY "gdy/inaczej". Aktualizacja z 1 na 3 musi wykonac OBA
   kroki. Wersja z "inaczej" wykona tylko pierwszy i baza zostanie bez kolumny.
   TO JEST NAJCZESTSZY BLAD W onUpgrade.

   onDowngrade RZUCA WYJATKIEM. Nie prubuj pracowac dalej na nowszej bazie.

2. TESTY MIGRACJI — DWA OSOBNE, oba na prawdziwym SQLite:
   - baza w wersji 1 -> po aktualizacji ma TRZY tabele I kolumne chroniony
   - baza w wersji 2 -> dostaje kolumne, tabel NIE tworzy powtornie
   Pierwszy oblewa sie, gdy uzyjesz "inaczej". O to w nim chodzi.

3. CZYSTE FUNKCJE WYBORU — bez Androida, bez SQLite:

     ktoreDoUsuniecia(przejazdy: List<Metadane>, kryterium: Kryterium)
        -> WynikWyboru(doUsuniecia: List<String>,
                       pominietoChronione: Int,
                       pominietoWToku: Int)

   Kryterium: KrotszeNiz(minuty) | Przerwane | StarszeNiz(miesiace)
   JEDNO NARAZ — bez laczenia. Sekcja 5.3.

   ZAWSZE odrzuca status w_toku ORAZ chroniony == 1, i LICZY oba pominiecia.
   Wiersze pominiec pojawiaja sie takze z zerami — sekcja 5.5.

4. Testy funkcji wyboru na JVM: chroniony spelniajacy kryterium nie trafia
   do listy i jest policzony; w_toku to samo; pusty zbior daje puste listy
   i zera, nie wyjatek.

NIE ROB w tym etapie zadnego interfejsu. To jest etap bazy i czystych funkcji.
```

**Ukończone, gdy:** obie migracje zielone na prawdziwym SQLite, `ktoreDoUsuniecia` przetestowane
na JVM bez Androida.

**Co zwykle idzie nie tak:** `gdy/inaczej` w `onUpgrade`. Wygląda naturalnie i przechodzi test
z wersji 2, a oblewa z wersji 1 — czyli dokładnie u tego użytkownika, który ma najstarsze dane.

---

## Etap H2 — kasowanie pojedyncze i okno potwierdzenia

```
Etap H2 z sekcji 14 specu historii. Kontrakt: sekcje 4.1, 4.2, 4.3, 4.5, 11.3.

1. OKNO POTWIERDZENIA — uklad doslownie z sekcji 4.2. Cztery bloki:
     a) CO GINIE: data, godzina, czas trwania, dystans, liczba probek, MB
     b) "Tego nagrania nie da sie odtworzyc."
     c) CO ZOSTAJE: liczba punktow odniesienia z tego przejazdu
     d) "Karta miesiaca dla <miesiac> przeliczy sie."

   Blok (a) NIE MOZE byc samym "Czy na pewno?". To pytanie, na ktore nie da sie
   odpowiedziec, bo nie wiadomo, o co chodzi.

   Gdy przejazd jest chroniony — DODATKOWY wiersz "Ten przejazd jest chroniony."
   Ochrona NIE BLOKUJE kasowania pojedynczego. Sekcja 4.4.

2. TRZY WEJSCIA, JEDNO OKNO:
     - przycisk "Usun" w szczegolach przejazdu
     - gest w bok na liscie dnia
     - (tryb zaznaczania powstaje w H3)

   GEST PRZESTAJE KASOWAC BEZPOSREDNIO. To zmiana wobec sekcji 12.5 bazowego,
   jedyna, ktora ta warstwa wprowadza w istniejacym zachowaniu. Jesli w kodzie
   z etapu 7 gest kasuje od razu — TO JEST TO, co tu naprawiasz.

3. PRZELACZNIK "chroniony" w szczegolach przejazdu. Kłodka przy pozycji na liscie.

4. KASOWANIE W BAZIE — sekcja 11.3:
     - odrzuc w_toku
     - DELETE w JEDNEJ TRANSAKCJI
     - VACUUM po skasowaniu wiecej niz 50 wierszy

   VACUUM jest konieczny, bo SQLite po DELETE NIE ODDAJE miejsca systemowi
   plikow. Bez niego panel Porzadki bedzie klamal o zwolnionym miejscu,
   a to klamstwo da sie sprawdzic w ustawieniach Androida.

5. PUNKTOW ODNIESIENIA NIE RUSZAMY. Kasowanie przejazdu NIE dotyka tabel
   punkt_odniesienia ani przeglad. Sekcja 4.3.

   TEST: po skasowaniu przejazdu liczba wierszy w punkt_odniesienia BEZ ZMIAN.
   Ten test istnieje po to, zeby ktos za pol roku nie "posprzatal osieroconych
   danych". Punkt przezywa CELOWO — pomiar naprawde sie odbyl.

6. TESTY:
   - gest NIE kasuje bez okna
   - w_toku odrzucone
   - punkty przezywaja
   - chroniony da sie skasowac pojedynczo, okno ma wiersz o ochronie
```

**Ukończone, gdy:** gest nie kasuje bez okna, `w_toku` odrzucone, punkty odniesienia przeżywają.

**Co zwykle idzie nie tak:** pokusa dorobienia paska z cofnięciem. Sekcja 10 odrzuca to
świadomie — w jadącym aucie pasek jest bezużyteczny, a jego obecność zdejmuje powagę z okna.

---

## Etap H3 — tryb zaznaczania i kasowanie wielokrotne

```
Etap H3 z sekcji 14 specu historii. Kontrakt: sekcja 4.6.

1. TRYB ZAZNACZANIA — wejscie przez przytrzymanie pozycji na liscie dnia.
   Uklad doslownie z sekcji 4.6.

   Pasek gorny: [X] "Zaznaczono N"  [Zaznacz dzien]
   Pasek dolny: [Usun zaznaczone (N)]

2. SESJA w_toku W TRYBIE ZAZNACZANIA:
     - NIE MA pola wyboru — ma KROPKE w jego miejscu
     - "Zaznacz dzien" JEJ NIE ZAZNACZA
     - podpis "nagrywanie trwa"

   Kropka zamiast pustego miejsca jest celowa: nieobecnosc pozycji
   w zaznaczeniu ma byc WIDOCZNA, a nie zagadkowa.

3. OKNO POTWIERDZENIA DLA WIELU — sekcja 4.6:
     - naglowek "Usunac N przejazdow?"
     - SUMA: laczny czas, laczny dystans, laczne MB
     - LISTA pozycji, po jednej w wierszu
     - powyzej 10 pozycji lista zwija sie do "... i N dalszych"
       ALE SUMA I LICZBA ZOSTAJA ZAWSZE — one sa tym, na co sie patrzy
     - "Tych nagran nie da sie odtworzyc."
     - "Zostaja N punktow odniesienia."
     - ktore miesiace przelicza karte

4. Kasowanie wielu w JEDNEJ TRANSAKCJI — przerwanie w polowie 137 wierszy
   zostawiloby zbior w stanie, ktorego uzytkownik nie zamawial.

5. TESTY:
   - "Zaznacz dzien" pomija w_toku
   - kasowanie wielokrotne odrzuca w_toku
   - okno pokazuje poprawna sume dla listy mieszanej
   - przerwana transakcja zostawia baze bez zmian
```

**Ukończone, gdy:** `Zaznacz dzień` pomija nagrywaną sesję, okno wymienia pozycje i sumy,
kasowanie jest transakcyjne.

---

## Etap H4 — panel Porządki

```
Etap H4 z sekcji 14 specu historii. Kontrakt: sekcja 5, uklad w 5.2.

1. WEJSCIE: przycisk "Porzadki" na karcie "od poczatku", obok zajetego miejsca.
   Naturalna kolejnosc — najpierw widzisz 184 MB, potem chcesz z tym cos zrobic.
   (Karta powstaje w H6; na razie wejscie tymczasowe, przepiete w H6.)

2. UKLAD doslownie z sekcji 5.2. Trzy bloki:
     - zajete miejsce ogolem
     - WYBIERZ JEDNO KRYTERIUM (trzy opcje, przelaczniki jednokrotne)
     - DO USUNIECIA (liczba + MB, pominiecia, lista, "pokaz wszystkie")

3. JEDNO KRYTERIUM NARAZ. Kryteriow NIE DA SIE laczyc — sekcja 5.3.
   Kazde dolozone kryterium to nowy sposob na skasowanie czegos,
   czego sie nie chcialo. Kto chce przeciecia, robi dwa przebiegi.

4. LICZBY W TYM PANELU NIE SA PROGAMI — sekcja 5.4. WAZNE:

   2/5/10/15 min i 3/6/12 miesiecy to KRYTERIA WYBORU. Nie wchodza do zadnej
   reguly, nie pojawiaja sie w zadnym werdykcie i NIE TWIERDZA NICZEGO
   O SAMOCHODZIE. Aplikacja nigdzie nie mowi, ze piecominutowy przejazd jest
   gorszy — mowi tylko "oto przejazdy krotsze niz piec minut".

   ZADNA nie jest oznaczona jako ZALECANA.
   DOMYSLNIE NIE JEST WYBRANE ZADNE KRYTERIUM -> lista pusta, przycisk nieaktywny.

5. TRZY WIERSZE PODSUMOWANIA POJAWIAJA SIE ZAWSZE, TAKZE Z ZERAMI:
     "N przejazdow - M MB"
     "N chronionych - pominiete"
     "N nagrywana teraz - pominieta"

   "0 chronionych - pominiete" niesie informacje, ze ochrona byla brana
   pod uwage. Ukrycie wiersza przy zerze te informacje kasuje.

6. Uzyj ktoreDoUsuniecia z H1. NIE PISZ TEJ LOGIKI DRUGI RAZ w warstwie
   interfejsu — dwie kopie rozjada sie przy pierwszej zmianie.

7. Zajete miejsce: SUM(LENGTH(przebieg) + LENGTH(podsumowanie)).

8. TESTY:
   - bez kryterium przycisk nieaktywny, lista pusta
   - chroniony spelniajacy kryterium pominiety I policzony
   - w_toku pominieta I policzona
   - po skasowaniu > 50 wierszy ROZMIAR PLIKU BAZY MALEJE (VACUUM)
   - SUM(LENGTH(...)) zgadza sie z suma po pojedynczych wierszach
```

**Ukończone, gdy:** bez kryterium nic się nie da usunąć, chronione są pomijane i policzone,
`VACUUM` naprawdę zwalnia miejsce.

**Co zwykle idzie nie tak:** dopisanie „(zalecane)" przy jednej z wartości minut. To zamienia
kryterium wyboru w twierdzenie o samochodzie, którego nie mamy z czego wyprowadzić.

---

## Etap H5 — porównanie dwóch przejazdów

```
Etap H5 z sekcji 14 specu historii. Kontrakt: sekcja 6, uklad w 6.2.

1. WEJSCIE: przycisk "Porownaj" w szczegolach przejazdu.
   DOMYSLNIE porownuje z POPRZEDNIM przejazdem tego samego auta —
   chronologicznie poprzednim po poczatek, z TYM SAMYM VIN-em.
   Drugi przejazd zmienialny przyciskiem [zmien] w naglowku kolumny.

2. ROZNY VIN -> POROWNANIE NIEDOSTEPNE. Sekcja 8.3 odniesienia zabrania
   mieszania historii dwoch aut, a porownanie jest dokladnie tym mieszaniem.

3. UKLAD doslownie z sekcji 6.2. Kolejnosc blokow JEST CZESCIA PROJEKTU:

     PRZEJAZD    dystans, czas, srednia predkosc     <- NA GORZE, sekcja 6.3
     SILNIK      maks/srednie obroty, maks predkosc
     TERMIKA     maks plyn, do 90 C, maks katalizator
     MIESZANKA   mediana korekty, poza pasmem
     WTRYSK      maks cisnienie szyny
     ZASILANIE   napiecie min
     KODY        na starcie, na koncu

   DYSTANS I CZAS SA PIERWSZE CELOWO. Porownanie 23 km z 4 km jest
   bezwartosciowe, ale aplikacja TEGO NIE OCENIA I NIE OSTRZEGA — "te
   przejazdy sa nieporownywalne" to WNIOSEK. Zamiast ostrzezenia: kontekst
   na gorze, zeby byl widoczny, zanim spojrzysz na reszte.

4. Srednia predkosc = dystansKm / czasTrwaniaS. JEDYNA wartosc wyliczana
   w tym rozszerzeniu. Bez nowego pola w podsumowaniu, bez nowej stalej.

5. WIERSZE NIEPOROWNYWALNE — KRESKA, NIGDY ZERO. Sekcja 6.4:
     null w jednym  -> "—" w wartosci I w roznicy
     null w obu     -> "—"
     wartosci rowne -> liczba w wartosciach, "—" w roznicy
     paliwoL        -> "—" ZAWSZE (to auto nie ma 5E)

6. KODY NIE MAJA KOLUMNY ROZNICY. P0171 minus P0300 nie jest niczym.
   Dwie listy obok siebie, porownanie zostawiamy oczom.

7. STATUS odzyskany: porownywalny, ALE z etykieta "przerwany" pod data.
   Jego podsumowanie policzono z przebiegu CZESCIOWEGO (bazowy 11.4)
   i to musi byc widoczne.

8. KOLUMNA ROZNICY ZAWIERA WYLACZNIE LICZBE ZE ZNAKIEM.
   BEZ strzalek. BEZ kolorow zaleznych od znaku. BEZ slow "lepiej"/"gorzej".
   Sekcja 6.5 — ta sama zasada co karta miesiaca (kontekst 10.3).
   Nie wiem, czy +28 s do 90 C to problem. Moze termostat, a moze bylo
   chlodniej. Liczby obok siebie to informacja, strzalka to wniosek.

9. NIE NAKLADAJ WYKRESOW dwoch przejazdow. Sekcja 12.5 bazowego odrzucila
   wykresy wieloseryjne jako nieczytelne. Porownanie jest NA LICZBACH.

10. TESTY:
    - null w ktorymkolwiek -> "—" w wartosci i roznicy, NIGDZIE 0
    - rozny VIN -> porownanie niedostepne
    - wiersze kodow nie maja kolumny roznicy
    - kolumna roznicy nie zawiera znakow strzalek ani slow oceniajacych
```

**Ukończone, gdy:** brak danych daje `—`, kody nie mają różnicy, różny VIN blokuje porównanie,
kolumna różnicy niczego nie ocenia.

---

## Etap H6 — karta „od początku" i filtry

```
Etap H6 z sekcji 14 specu historii. Kontrakt: sekcje 7 i 8.

1. KARTA "OD POCZATKU" TO DRUGI TRYB KARTY MIESIACA, NIE DRUGA KARTA.
   Druga karta zepchnelaby kalendarz poza ekran.
   Przelacznik [od poczatku] w naglowku karty, obok strzalek miesiaca.
   Strzalki miesiaca ZOSTAJA AKTYWNE — przelaczaja kalendarz pod spodem,
   bo karta calosci od miesiaca nie zalezy.

2. WIERSZE KARTY — sekcja 7.2:
     Pierwszy zapis      MIN(poczatek)
     Przejazdy           COUNT(*) bez w_toku
     Dystans             SUMA dystansKm
     Czas za kierownica  SUMA czasTrwaniaS
     Bez rozgrzania      ile podsumowan ma czasDo90CSekundy == null
     Zajete miejsce      SUM(LENGTH(przebieg) + LENGTH(podsumowanie))
                         + przycisk [Porzadki]

3. SUMY, NIE MEDIANY. Karta miesiaca uzywa median, bo szuka TYPOWEJ jazdy.
   Ta karta odpowiada na "ile tego jest lacznie" — mediana nie znaczy tam nic.
   NIE KOPIUJ logiki median z karty miesiaca.

4. "Bez rozgrzania" BEZ ZADNEGO PROGU — liczy wartosci null, dokladnie jak
   karta miesiaca (kontekst 10.2). To jedyny wiersz bez wymyslonej liczby
   i ma taki zostac.

5. PUSTY ZBIOR DAJE KRESKI, NIE ZERA. Swiezo zainstalowana aplikacja
   pokazuje "—" w kazdym wierszu.

6. TRZY FILTRY nad kalendarzem, wszystkie WYLACZONE domyslnie:
     [z kodami]    kodyNaStarcie lub kodyNaKoncu niepuste
     [przerwane]   status == odzyskany
     [chronione]   chroniony == 1

   WSZYSTKIE TRZY TO FAKTY LOGICZNE — zero progow, zero wymyslonych liczb.
   DLATEGO NIE MA filtru "dluzsze niz X": wymagalby wybrania X, a wybrane X
   wygladaloby jak twierdzenie, ktory przejazd jest wart uwagi.

7. WLACZONY FILTR ZAWEZA JEDNOCZESNIE KROPKI W KALENDARZU I LISTE DNIA.
   Rozjazd miedzy nimi to najbardziej mylacy mozliwy blad tego ekranu:
   dzien z kropka, ktory po dotknieciu jest pusty.
   TEST NA TO JEST OBOWIAZKOWY.

8. Filtry lacza sie przez I. Filtry NICZEGO NIE KASUJA, wiec ich laczenie
   nie niesie ryzyka z sekcji 5.3 — tam zakaz laczenia dotyczyl Porzadkow.

9. Pusty wynik filtra: komunikat "Brak przejazdow spelniajacych filtr"
   pod kalendarzem. NIE pusty ekran bez wyjasnienia.

10. Przepnij przycisk [Porzadki] z H4 na ta karte.

11. TESTY:
    - pusty zbior -> same "—", nigdzie 0
    - filtr zawezajacy kropki I liste JEDNOCZESNIE
    - pusty wynik -> komunikat
    - karta calosci liczy SUMY, nie mediany
```

**Ukończone, gdy:** pusty zbiór daje kreski, filtr zawęża kropki i listę jednocześnie.

---

## Etap H7 — weryfikacja w aucie

```
Etap H7 z sekcji 14 specu historii. Lista czynnosci: sekcja 15.

Tego etapu NIE DA SIE zrobic w Cursorze.

Przygotuj docs/weryfikacja-historia.md z miejscem na wynik kazdego
z dziesieciu punktow sekcji 15 — tak jak weryfikacja.md dla etapu 10
i weryfikacja-odniesienie.md dla O8. Przyjmij wyniki, dopisz testy
na nowych przypadkach.

DZIESIEC CZYNNOSCI z sekcji 15. Dwie najwazniejsze:

  #1 — przesun pozycje w bok PODCZAS JAZDY PO NIEROWNEJ DRODZE.
       Okno sie otwiera, NIC NIE GINIE. To jest caly powod tej warstwy.

  #9 — zainstaluj wersje z H1 na radiu z baza SPRZED rozszerzenia.
       NAJWAZNIEJSZY PUNKT CALEJ LISTY: jedyny, ktory sprawdza migracje
       na prawdziwych danych, a migracja jest jedyna czescia tego
       rozszerzenia, ktora potrafi NIEODWRACALNIE zepsuc to,
       co juz zebrales.

       ZROB KOPIE PLIKU BAZY PRZED TYM TESTEM.

NIE ZMIENIAJ zadnej stalej na podstawie jednego przejazdu.
```

**Ukończone, gdy:** lista z sekcji 15 przeszła w całości, a wyniki są dopisane do testów.

---

## Prompt kontrolny — po rozszerzeniu historii

Nowy czat, wszystkie pięć dokumentów w załączeniu:

```
Sprawdz rozszerzenie historii wobec spec
@docs/spec/2026-08-15-i40-android-historia-design.md

Odpowiedz TAK/NIE z odsylaczem do pliku i linii. Gdzie NIE — napisz,
co poprawic.

KASOWANIE
 1. Czy WSZYSTKIE trzy drogi (przycisk, gest, zaznaczanie) prowadza
    do okna potwierdzenia? Czy gest na pewno nie kasuje bezposrednio?
 2. Czy okno wymienia, CO GINIE — date, czas, dystans, probki, MB?
 3. Czy okno mowi, ze punkt odniesienia ZOSTAJE?
 4. Czy istnieje test, ze po skasowaniu przejazdu liczba wierszy
    w punkt_odniesienia sie NIE ZMIENIA?
 5. Czy sesja w_toku jest odrzucana we WSZYSTKICH trzech drogach,
    kazda z osobnym testem?
 6. Czy chroniony da sie skasowac pojedynczo, a Porzadki go pomijaja?
 7. Czy kasowanie jest w transakcji, a VACUUM woła sie po > 50 wierszach?
 8. Czy gdziekolwiek jest pasek z cofnieciem? NIE POWINNO GO BYC.

BAZA
 9. Czy onUpgrade uzywa OSOBNYCH "jesli", a nie "gdy/inaczej"?
10. Czy jest test migracji z wersji 1 ORAZ z wersji 2?
11. Czy onDowngrade rzuca wyjatkiem?

POROWNANIE
12. Czy kolumna roznicy zawiera GDZIEKOLWIEK strzalke, kolor zalezny
    od znaku albo slowo oceniajace? NIE POWINNA.
13. Czy dystans, czas i srednia predkosc sa NA GORZE?
14. Czy wiersze kodow NIE MAJA kolumny roznicy?
15. Czy rozny VIN blokuje porownanie?
16. Czy null daje "—" w wartosci I w roznicy, nigdzie 0?

ZESTAWIENIA
17. Czy karta "od poczatku" liczy SUMY, nie mediany?
18. Czy "bez rozgrzania" liczy null bez zadnego progu temperatury?
19. Czy pusty zbior daje "—", nie zera?
20. Czy filtr zaweza kropki w kalendarzu I liste dnia JEDNOCZESNIE?

ZRÓWNOWAZENIE
21. Czy dolozono JAKIEKOLWIEK zapytanie OBD? NIE POWINNO.
    Czy jest test porownujacy liste odpytywanych PID-ow przed i po?
22. Czy ruszono pętle gorąca, jej sklad, tempo albo ktorykolwiek prog
    diagnostyczny? NIE POWINNO.
23. Czy ktorakolwiek liczba z Porzadkow (2/5/10/15 min, 3/6/12 mies.)
    zostala opisana jako ZALECANA albo trafila do jakiejkolwiek reguly?
    NIE POWINNA — to kryteria wyboru, nie progi.
24. Czy zakladki nadal sa TRZY?
```

Punkty **4**, **9** i **21** są najważniejsze. Pierwszy pilnuje decyzji, którą łatwo „posprzątać"
jako niedoróbkę. Drugi pilnuje jedynej rzeczy w tym rozszerzeniu, która potrafi nieodwracalnie
zniszczyć zebrane dane. Trzeci pilnuje, żeby warstwa historii nie zaczęła po cichu dokładać
do magistrali.
