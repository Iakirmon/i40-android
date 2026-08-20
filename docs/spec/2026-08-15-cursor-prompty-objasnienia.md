# i40-android — prompty dla Cursora: rozszerzenie objaśnień

Pięć etapów `S1`–`S5`, **jeden nowy czat na etap**. Wchodzą **po etapie H7** rozszerzenia
historii.

---

## Jak to prowadzić

**Do każdego promptu dołącz sześć dokumentów:**

```
@docs/spec/2026-08-14-i40-android-design.md
@docs/spec/2026-08-14-i40-android-diagnostyka-design.md
@docs/spec/2026-08-14-i40-android-kontekst-design.md
@docs/spec/2026-08-14-i40-android-odniesienie-design.md
@docs/spec/2026-08-15-i40-android-historia-design.md
@docs/spec/2026-08-15-i40-android-objasnienia-design.md
```

**Przy etapie S3 dołącz dodatkowo `@docs/slownik.md`.**

**Najważniejsze zdanie całego rozszerzenia — sekcja 4.2 specu:**

> **Parametr, którego nie zmierzono, nie jest „w normie".**

Trzy sekundy po odpaleniu silnika połowa parametrów nie została ani razu odczytana. Napis
„wszystko w normie" byłby wtedy nieprawdą — i to nieprawdą **uspokajającą**, czyli najgorszego
rodzaju. To jest jedyna rzecz, którą to rozszerzenie może naprawdę zepsuć.

**Po każdym etapie:**

```
./gradlew ktlintCheck ; if ($?) { ./gradlew lint } ; if ($?) { ./gradlew test }
```

**Pięć pytań kontrolnych:**

*„Czy dołożyłem jakiekolwiek zapytanie OBD?"* — **nie wolno.** Sekcja 3.1, jest test.

*„Czy parametr bez odczytu może gdziekolwiek wyjść jako w normie?"* — **nie.** `NIE_ZMIERZONY`
sprawdzany jako pierwszy. Sekcja 5.

*„Czy parametr bez pasma liczy się do »wszystko w normie«?"* — **nie.** `BEZ_PASMA` to osobny
stan. Liczenie niewiedzy jako dobrej wiadomości to cichy fałsz.

*„Czy dołożyłem dźwięk?"* — **nie wolno.** Alarmów jest pięć. Sekcja 4.8.

*„Skąd wziąłem to zdanie słownika?"* — **wyłącznie z `docs/slownik.md`.** Ani jedno zdanie nie
powstaje w Cursorze. Sekcja 8.6.

---

## Etap S1 — stan parametru, czyste funkcje

```
Etap S1 z sekcji 13 specu objasnien. Kontrakt: sekcje 5 i 4.7.

1. CZYSTA FUNKCJA stanParametru:

     stanParametru(pid, wartosc, odczytanoWTejSesji,
                   warunkiWaznosciSpelnione) -> StanParametru

     NIE_ZMIERZONY   odczytanoWTejSesji == false
     NIEWAZNY_TERAZ  warunkiWaznosciSpelnione == false
     BEZ_PASMA       pasmo dla pid to jawne "brak"
     W_NORMIE        wartosc wewnatrz pasma
     PONIZEJ         ponizej dolnej granicy
     POWYZEJ         powyzej gornej granicy

   KOLEJNOSC SPRAWDZEN JEST OBOWIAZKOWA. NIE_ZMIERZONY MUSI BYC PIERWSZY.
   Parametr bez odczytu ma wartosc null. Kazde inne uporzadkowanie skonczy sie
   porownaniem z pustka albo — gorzej — z podstawionym zerem, a wtedy
   nieodczytany parametr wyjdzie jako "w normie". TO JEST TEN BLAD,
   ktoremu cale rozszerzenie ma zapobiec.

   BEZ_PASMA TO OSOBNY STAN, NIE ODMIANA W_NORMIE. Parametr bez normy
   NIE JEST w normie — o nim po prostu nic nie wiadomo.

1a. NIEWAZNY_TERAZ — CZWARTY STAN, POPRAWKA P1, sekcja 5.1 specu.

   warunkiWaznosciSpelnione:
      dla 0106 i 0107  ->  status0103 nalezy do {2, 16}
      dla reszty       ->  zawsze true

   To DOKLADNIE TA SAMA regula, co warunek czwartego kafla (sekcja 8.5
   warstwy kontekstowej). JEDNO ZRODLO, nie dwie kopie — wyciagnij ja
   do wspolnej funkcji i wolaj z obu miejsc.

   POWOD, dla ktorego to nie jest ozdoba: bez tego stanu KAFEL na gorze
   pokazuje "— ○", a PANEL STAN pod nim w TEJ SAMEJ SEKUNDZIE orzeka
   "Mieszanka uboga, korekta +14 %" — z wartosci zamrozonej przed przejsciem
   w petle otwarta. Aplikacja przeczy sama sobie w jednym spojrzeniu.

   NIEWAZNY_TERAZ liczy sie do JESZCZE_NIE_WIEM, NIE do W_NORMIE
   i NIE do ODCHYLEN. To niewiedza, nie dobra wiadomosc — tak samo jak BEZ_PASMA.

   BRAK ODCZYTU 0103 -> warunkiWaznosciSpelnione = false. ZACHOWAWCZO.

   WYMAGA 0103, czyli etapu K2. Jesli robisz S1 przed K2, zostaw parametr
   z wartoscia domyslna true i ZOSTAW KOMENTARZ, ze to stan przejsciowy.

2. Pasma czytaj z PasmaOdniesienia (etap D3). NIE TWORZ DRUGIEJ KOPII
   ZADNEGO PROGU. Zero nowych stalych w tym etapie i w calym rozszerzeniu.

3. SKLADANIE STANU PANELU — czysta funkcja:

     jest choc jeden PONIZEJ/POWYZEJ                     -> ODCHYLENIA
     inaczej jest choc jeden NIE_ZMIERZONY
             albo NIEWAZNY_TERAZ                         -> JESZCZE_NIE_WIEM
     inaczej                                             -> W_NORMIE

4. MAPA parametr -> panel z sekcji 4.7. OSTATNI WIERSZ OBOWIAZKOWY:
   parametr bez wlasnego wykresu prowadzi do Przegladu, nie donikad.
   Skrot prowadzacy w prozne miejsce jest gorszy niz jego brak.

5. TESTY NA JVM, bez Androida:
   - wartosc null -> NIE_ZMIERZONY, NIGDY PONIZEJ
   - pasmo "brak" -> BEZ_PASMA, i NIE liczy sie do "wszystko w normie"
   - jeden odczyt w sesji -> JESZCZE_NIE_WIEM, NIGDY W_NORMIE
   - odchylenie + nieodczytane -> ODCHYLENIA (odchylenie ma pierwszenstwo)
   - kazdy parametr z mapy prowadzi do istniejacego panelu
   - 0107 przy status0103 = 1, 4, 8, 0 -> NIEWAZNY_TERAZ, NIGDY POWYZEJ
   - 0107 przy BRAKU odczytu 0103      -> NIEWAZNY_TERAZ, NIGDY POWYZEJ
   - 0107 przy status0103 = 2 albo 16  -> normalna ocena wzgledem pasma
   - TEN SAM MOMENT na kaflu i na panelu Stan: albo oba milcza, albo oba mowia

NIE ROB w tym etapie zadnego interfejsu.
```

**Ukończone, gdy:** `NIE_ZMIERZONY` pierwszy, `BEZ_PASMA` osobno, **`NIEWAZNY_TERAZ` dla korekt
poza pętlą zamkniętą i przy braku `0103`**, wszystko na JVM.

**Co zwykle idzie nie tak:** sprawdzanie pasma przed sprawdzeniem, czy w ogóle jest odczyt.
Przechodzi wszystkie testy z prawdziwymi danymi i wywraca się dokładnie w pierwszych sekundach
po uruchomieniu silnika — czyli wtedy, kiedy człowiek na ten panel patrzy najczęściej.

---

## Etap S2 — panel Stan

```
Etap S2 z sekcji 13 specu objasnien. Kontrakt: sekcja 4, uklady w 4.3-4.6.

1. PANEL STAN JEST PIERWSZY W KOLEJNOSCI:

     ● ○ ○ ○ ○ ○  STAN > Podstawowy > Mieszanka > Wtrysk GDI > Termika > Powietrze

   To zmiana wobec sekcji 8.1 rozszerzenia diagnostycznego — paneli jest SZESC.
   Kolejnosc pozostalych pieciu BEZ ZMIAN.

2. TRZY STANY, uklady doslownie z sekcji 4.3, 4.4 i 4.5.

3. STAN "W NORMIE" — JEDNO ZDANIE, duze, na srodku, plus linijka kontekstu
   o rozgrzaniu.

   NIE DOPISUJ WYLICZENIA SPRAWDZONYCH PARAMETROW. Lista, ktora zawsze
   wyglada tak samo, przestaje byc czytana — a wtedy nie widac zmiany.
   To jest cala tresc sekcji 2 specu i najczestsza pokusa w tym etapie.

4. STAN "ODCHYLENIA" — trzy wiersze na odchylenie:
     a) zdanie po polsku + znacznik ▲ albo ▼
     b) wartosc i pasmo
     c) skrot do panelu

   ZDANIE POCHODZI Z ISTNIEJACEJ REGULY, gdy ktoras odpala — DOSLOWNIE,
   nie parafraza. Siedemnascie wnioskow jest juz napisanych i uzasadnionych;
   drugi komplet rozjechalby sie z pierwszym przy pierwszej zmianie.
   Gdy zadna regula nie pasuje: nazwa parametru + "poniżej/powyżej normy".

   KOLEJNOSC: waga malejaco (usterka, uwaga, informacja), potem kolejnosc
   z PasmaOdniesienia. Odchylenie bez reguly dostaje wage uwaga.

   MAKSIMUM CZTERY WIERSZE + "... i N dalszych -> Przeglad".
   Panel, ktorego nie da sie przeczytac w dwie sekundy, przestaje byc
   panelem podsumowujacym.

5. STAN "JESZCZE NIE WIEM" WSPOLISTNIEJE Z ODCHYLENIAMI — gdy cos odstaje,
   a reszta jeszcze sie nie odczytala, odchylenia sa u gory, a wiersz
   "jeszcze nie zmierzone" pod spodem.

6. WIERSZ KODU BLEDU na samej gorze, przed odchyleniami (sekcja 4.6).
   SUROWY KOD ZAWSZE, opis ze slownika DTC tylko gdy istnieje.

7. ZERO DZWIEKOW. Panel Stan NIE WYWOLUJE odtwarzacza ani razu.
   Alarmow jest piec i tyle ma zostac. JEST NA TO TEST.

8. TESTY:
   - sesja z jednym odczytem -> "jeszcze nie wiem", NIGDY "wszystko w normie"
   - szesc odchylen -> cztery wiersze + "i 2 dalsze"
   - wniosek dla korekty poza pasmem DOSLOWNIE rowny wnioskowi reguly 10.4
   - kolejnosc: usterka przed uwaga przed informacja
   - panel nie wywoluje dzwieku
```

**Ukończone, gdy:** sesja z jednym odczytem daje „jeszcze nie wiem", zero dźwięków, limit
czterech działa.

---

## Etap S3 — słownik

```
Etap S3 z sekcji 13 specu objasnien. Kontrakt: sekcje 7 i 8.
DOLACZ @docs/slownik.md — to jest zrodlo tresci.

╔═══════════════════════════════════════════════════════════════════╗
║  ANI JEDNO ZDANIE TRESCI HASEL NIE POWSTAJE W CURSORZE.           ║
║                                                                   ║
║  docs/slownik.md zawiera 70 gotowych hasel. Zadaniem tego etapu   ║
║  jest PRZENIESC JE DO ZASOBOW CO DO ZDANIA — nie napisac na nowo, ║
║  nie skrocic, nie "poprawic stylu", nie uzupelnic z pamieci.      ║
║                                                                   ║
║  Rubryki "GDY WYJDZIE POZA PASMO" i "CZEGO TO NIE MOWI" to        ║
║  dokladnie te miejsca, w ktorych zmyslone wyjasnienie wyglada     ║
║  IDENTYCZNIE jak prawdziwe — i w ktorych pomylka wysyla czlowieka ║
║  naprawiac nie to, co trzeba.                                     ║
║                                                                   ║
║  Brakujace haslo ZGLOS. Nie dopisuj.                              ║
╚═══════════════════════════════════════════════════════════════════╝

1. STRUKTURA HASLA — sekcja 7.2:
     - blok gorny: Teraz / Norma / Poprzednio
     - rubryki: parametry CZTERY, pojecia TRZY
     - stopka: PID + poziom + czestotliwosc, albo WZOR dla wyliczanych

2. LICZBY W BLOKU GORNYM SA CZYTANE, NIGDY WPISANE — sekcja 7.3:
     Teraz      biezaca probka; "—" gdy nieodczytana
     Norma      z PasmaOdniesienia — TO SAMO ZRODLO co kolumna normy
     Poprzednio z punkt_odniesienia — TO SAMO ZRODLO co kolumna poprzednio

   Gdyby norma byla wklepana w tekst hasla, przy pierwszej zmianie progu
   SLOWNIK ZACZALBY MOWIC CO INNEGO NIZ PANEL — i nikt by tego nie zauwazyl,
   bo nikt nie sprawdza tekstow pomocy.

3. TRESC HASEL JAKO DANE, NIE KOD. Plik zasobow, nie stale w Kotlinie.
   Test ma dac sie uruchomic na samych danych.

4. ARKUSZ WYSUWANY Z DOLU, nie okno modalne. Da sie zsunac palcem.
   Sekcja 12.4 bazowego zabrania okien modalnych w ruchu.

5. ODSYLACZE miedzy haslami — sekcja 7.5. Stos, przycisk wstecz,
   a PO TRZECH PRZESKOKACH przycisk "wroc do poczatku".

6. TESTY:
   - 70 hasel obecnych
   - ZADNEJ pustej rubryki; parametr ma cztery, pojecie trzy
   - ZADEN tekst hasla nie zawiera liczby wystepujacej w PasmaOdniesienia,
     poza LISTA ZAMKNIETA dozwolonych z naglowka docs/slownik.md

     UWAGA: test na golych podciagach ZGLOSI FALSZYWE TRAFIENIA — sprawdzono
     to na gotowej tresci. Musi odsiac: numery PID-ow (0105 zawiera 105),
     nazwe adaptera (ELM327 zawiera 27) i stopki z czestotliwoscia.
     Odrzuc stopki, tekst w apostrofach odwrotnych i nazwy wlasne,
     potem porownuj liczby w prozie rubryk.
   - kazdy odsylacz prowadzi do istniejacego hasla
   - ZGODNOSC Z ZRODLEM: test czyta docs/slownik.md i zasoby aplikacji
     i porownuje CO DO ZDANIA
```

**Ukończone, gdy:** 70 haseł, żadnej pustej rubryki, żadnej liczby z pasm w tekście, zgodność
z `docs/slownik.md` co do zdania.

**Co zwykle idzie nie tak:** „skrócę te opisy, są za długie". Nie są — są takiej długości,
żeby dało się z nich czegoś nauczyć. Skracanie zaczyna się od rubryki „czego to nie mówi",
czyli od jedynej, która pilnuje granicy wnioskowania.

---

## Etap S4 — wpięcie słownika

```
Etap S4 z sekcji 13 specu objasnien. Kontrakt: sekcje 7.1 i 8.1.

1. KONTRAKT HASEL, blizniak kontraktu pasm z sekcji 9.4 diagnostycznego:

     KAZDA WARTOSC WYSWIETLANA GDZIEKOLWIEK MA HASLO W SLOWNIKU.

   Test przechodzi liste wyswietlanych parametrow i sprawdza obecnosc hasla —
   dokladnie tak, jak istniejacy test przechodzi je pod katem pasm.
   Bez tego parametr dodany za pol roku pojawi sie bez wyjasnienia
   i nikt tego nie zauwazy.

2. WEJSCIA — dotkniecie otwiera haslo:
     - cztery kafle gornego paska
     - kazdy wiersz ekranu Odczyty (33 pozycje)
     - podpis kazdego wykresu na panelach
     - wiersze raportu sesji i przegladu
     - wiersze panelu Stan

3. BLOKADA PREDKOSCIOWA: przy 010D > 0 SLOWNIK SIE NIE OTWIERA.
   To tresc do czytania, nie do zerkania. Sekcja 12.4 bazowego.
   Przelaczanie paneli pozostaje dozwolone w ruchu — to sie NIE zmienia.

4. TESTY:
   - kazdy z 33 wierszy Odczytow otwiera haslo
   - kazdy kafel otwiera haslo
   - przy predkosci > 0 slownik nie otwiera sie z zadnego wejscia
   - kontrakt hasel: brak parametru bez hasla
```

**Ukończone, gdy:** każda wyświetlana wartość otwiera hasło, a w ruchu nie otwiera się żadne.

---

## Etap S5 — weryfikacja w aucie

```
Etap S5 z sekcji 13 specu objasnien. Lista czynnosci: sekcja 14.

Tego etapu NIE DA SIE zrobic w Cursorze.

Przygotuj docs/weryfikacja-objasnienia.md z miejscem na wynik kazdego
z osmiu punktow sekcji 14 — tak jak weryfikacja-historia.md dla H7.
Przyjmij wyniki, dopisz testy na nowych przypadkach.

DWA PUNKTY NAJWAZNIEJSZE:

  #1 — odpal ZIMNY silnik i patrz na panel Stan przez pierwsze pol minuty.
       Ma pokazac "jeszcze nie wiem", potem "silnik sie rozgrzewa".
       ANI RAZU "wszystko w normie".
       To jedyna rzecz, ktora w tym rozszerzeniu moze byc SZKODLIWA:
       uspokajajacy komunikat wystawiony, zanim cokolwiek zmierzono.

  #3 — zerknij na panel w ruchu i policz czas.
       Jesli nie da sie odczytac w JEDNYM spojrzeniu, panel jest za gesty
       i trzeba go przerzedzic, a nie tlumaczyc, ze "da sie przyzwyczaic".

NIE ZMIENIAJ zadnej stalej na podstawie jednego przejazdu.
```

**Ukończone, gdy:** lista z sekcji 14 przeszła w całości.

---

## Prompt kontrolny — po rozszerzeniu objaśnień

Nowy czat, wszystkie dokumenty plus `@docs/slownik.md`:

```
Sprawdz rozszerzenie objasnien wobec spec
@docs/spec/2026-08-15-i40-android-objasnienia-design.md
@docs/slownik.md

Odpowiedz TAK/NIE z odsylaczem do pliku i linii. Gdzie NIE — napisz,
co poprawic.

UCZCIWOSC PANELU
 1. Czy parametr bez odczytu moze GDZIEKOLWIEK wyjsc jako "w normie"?
    NIE POWINIEN. Czy NIE_ZMIERZONY jest sprawdzany PIERWSZY?
 2. Czy jest test: sesja z jednym odczytem daje "jeszcze nie wiem"?
 3. Czy BEZ_PASMA liczy sie do "wszystko w normie"? NIE POWINNO.
 4. Czy stan "w normie" wylicza sprawdzone parametry? NIE POWINIEN —
    sekcja 2.
 5. Czy wiersz kodu bledu pokazuje surowy kod ZAWSZE?

PANEL
 6. Czy panel Stan jest PIERWSZY, a kolejnosc pozostalych piaciu
    niezmieniona?
 7. Czy zdania odchylen sa DOSLOWNIE wnioskami regul z tabeli 10.4,
    a nie parafrazami?
 8. Czy limit czterech odchylen dziala i czy jest wiersz "i N dalszych"?
 9. Czy panel wywoluje gdziekolwiek dzwiek? NIE POWINIEN.
10. Czy kazdy skrot z mapy 4.7 prowadzi do ISTNIEJACEGO panelu?

SLOWNIK
11. Czy jest 70 hasel?
12. Czy ktorekolwiek haslo ma pusta rubryke? NIE POWINNO.
    Czy parametry maja cztery rubryki, a pojecia trzy?
13. Czy tekst ktoregokolwiek hasla zawiera liczbe z PasmaOdniesienia
    spoza listy dozwolonych? NIE POWINIEN.
14. Czy tresc w zasobach zgadza sie z docs/slownik.md CO DO ZDANIA?
15. Czy kazdy odsylacz prowadzi do istniejacego hasla?
16. Czy po trzech przeskokach pojawia sie "wroc do poczatku"?
17. Czy slownik otwiera sie w ruchu? NIE POWINIEN.
18. Czy KAZDA wyswietlana wartosc ma haslo — 33 wiersze Odczytow,
    cztery kafle, podpisy wykresow, wiersze raportu?

ZROWNOWAZENIE
19. Czy dolozono JAKIEKOLWIEK zapytanie OBD? NIE POWINNO.
    Czy jest test porownujacy liste odpytywanych PID-ow przed i po?
20. Czy dolozono JAKIKOLWIEK nowy prog? NIE POWINNO.
21. Czy alarmow jest nadal PIEC?
22. Czy zakladki sa nadal TRZY?
23. Czy wersja schematu bazy zostala na 3?
```

Punkty **1**, **13** i **19** są najważniejsze. Pierwszy pilnuje jedynej rzeczy, która w tym
rozszerzeniu może być szkodliwa. Drugi pilnuje, żeby słownik nie zaczął po cichu mówić czegoś
innego niż panel. Trzeci — żeby warstwa objaśnień nie zaczęła dokładać do magistrali.
