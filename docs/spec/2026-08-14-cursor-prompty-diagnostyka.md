# i40-android — prompty dla Cursora: warstwa diagnostyczna

**Wersja 2** — po sprawdzeniu źródeł. Numeracja zmieniła się względem wersji 1: doszedł
etap reguł jako `D3`, panele przesunęły się o jeden.

Dziesięć etapów `D1`–`D10`, **jeden nowy czat na etap**. Wchodzą **po etapie 8** projektu
bazowego.

---

## Jak to prowadzić

**Do każdego promptu dołącz oba dokumenty:**

```
@docs/spec/2026-08-14-i40-android-design.md
@docs/spec/2026-08-14-i40-android-diagnostyka-design.md
```

**Rozszerzenie zmienia dokument bazowy w jedenastu punktach** — lista w sekcji 12. Poza nią
bazowy obowiązuje bez zmian. Agent, który tego nie przeczyta, „przy okazji" ruszy pętlę gorącą
albo istniejące progi alarmów.

**Po każdym etapie:**

```
./gradlew ktlintCheck ; if ($?) { ./gradlew lint } ; if ($?) { ./gradlew test }
```

**Cztery pytania kontrolne:**

*„Czy pętla gorąca nadal ma sześć PID-ów, 4 Hz, i czy `0D`, `05` oraz `04` w niej są?"* —
`04` jest wejściem modelu oleju; bez niego model cicho zamiera, a kafel pokazuje wartość,
która przestała się odświeżać. Gniazd konfigurowalnych jest **trzy**.

*„Skąd ta liczba?"* — nowe progi są w sekcji 4 rozszerzenia, **z podanym źródłem**. Liczba
spoza sekcji 4 i spoza tabel 10.x bazowego jest wymyślona.

*„Jaką wagę ma ta reguła?"* — wszystkie trzy nowe mają **`uwaga`**, nigdy `usterka`.
Sekcja 4.3 tłumaczy, dlaczego, i jest wiążąca.

*„Czy ta oś się skaluje automatycznie?"* — nowe wykresy mają sztywne zakresy z sekcji 8.6.

*„Czy ta liczba ma obok pasmo?"* — **sekcja 8.8 jest wiążąca dla KAŻDEGO parametru w KAŻDYM
widoku.** Albo pasmo z reguły lub źródła, albo jawne `—` z uzasadnieniem. Pusta kolumna normy
jest błędem, nie brakiem funkcji. I odwrotnie: **pasmo narysowane „żeby ekran ładnie wyglądał"
jest liczbą wymyśloną pokazaną jako norma** — groźniejszą niż zwykła wymyślona stała, bo
użytkownik porównuje z nią swój samochód.

*„Czy przekroczenie pasma wydaje dźwięk?"* — **nie.** Alarmują wyłącznie cztery warunki
z tabeli 10.3 bazowego plus `KAT-2`. Reszta zmienia tylko znacznik `▲` / `▼`.

---

## Etap D1 — poziomy odpytywania

```
Etap D1 z sekcji 14 rozszerzenia. Kontrakt: sekcja 7.

1. Trzeci poziom w acquisition/SampleStream.kt: sześć PID-ów 23 3C 0B 11 43 44,
   jedno zapytanie, przy n % 4 == 0.
   Wszystkie sześć zweryfikowane w masce tego auta — sekcja 3.1.

2. NIE ZMIENIAJ FAZ ISTNIEJACYCH POZIOMOW. Rotacja zimna stoi na n % 10 == 5,
   odczyt 03 na n % 200 == 150 — obie fazy sa juz poprawione w dokumencie BAZOWYM
   (jego sekcja 10.1) i maja tam uzasadnienie. Jesli w kodzie sa na 0, to znaczy,
   ze etap 4 albo 5 zrobiono wedlug starszej wersji bazowego — zglos to i zapytaj,
   zanim cokolwiek dolozysz.

3. SPRAWDZ ROZLACZNOSC, to jest cale zadanie tej czesci:
     n % 4  == 0    =>  n parzyste
     n % 10 == 5    =>  n nieparzyste            -> rozlaczne z definicji
     n % 200 == 150 =>  n % 4 stale = 2          -> nigdy 0, rozlaczne
   Sprawdzone na 200 000 cykli: zero kolizji. Rozklad: 129 000 cykli z jednym
   zapytaniem, 71 000 z dwoma, ZERO z trzema.

4. Kolejność w cyklu: gorący → średni → zimny. Zbieg jest niemożliwy,
   ale kod NIE MA PRAWA na tym polegać — obsłuż go, dając średniemu pierwszeństwo.

ZAKAZ: nie dotykaj petli goracej. Sklad (0D, 05, 04 + trzy gniazda), tempo 4 Hz i wszystkie
bezpieczniki z tabeli 10.1 bazowego zostają. Rozszerzenie dokłada poziom, nie przestawia
istniejące.

5. Testy:
   - TEST ROZŁĄCZNOŚCI: wszystkich CZTERECH poziomow (goracy, sredni, zimny, odczyt 03):
     na co najmniej 20 000 CYKLACH zaden nie wykonuje trzech zapytan.
     Dwadziescia tysiecy, nie tysiac — przy tysiacu kolizja z odczytem 03 (co 200 cykli)
     wystapilaby zaledwie pare razy albo wcale, zaleznie od fazy.
   - pętla średnia wykonuje się dokładnie co czwarty cykl,
   - przy czterech poziomach suma zapytan nie przekracza sufitu 25/s,
   - skład i tempo pętli gorącej identyczne przed i po dodaniu poziomu średniego.
```

**Ukończone, gdy:** test rozłączności przechodzi na 20 000 cykli, a pętla gorąca jest nietknięta.

---

## Etap D2 — pola podsumowania

```
Etap D2 z sekcji 14 rozszerzenia. Kontrakt: sekcja 11.1.

Pięć nowych pól w storage/SummaryCalculator.kt — czyste funkcje z gotowego przebiegu:

  maxCisnienieSzynyBar          max serii 23
  obciazeniePrzyMaxCisnieniu    seria 43 w chwili NAJBLIŻSZEJ CZASOWO maksimum 23
  maxTempKatalizatoraC          max serii 3C
  czasDo90CSekundy              pierwsza próbka 05 >= 90 C, od startu sesji
  czasPozaPasmemKorektSekundy   SUMA ODSTĘPÓW gdy |06 + 07| > 10 %

Wszystkie null, gdy odpowiedniej serii brak. Niezmiennik 1 bazowego.

PUŁAPKA — obciazeniePrzyMaxCisnieniu. 23 i 43 są w tej samej pętli średniej, więc
KUSZĄCE jest dopasowanie po indeksie tablicy. Zadziała, dopóki któryś PID nie odpowie
i jedna seria nie zgubi próbki — a wtedy indeksy się rozjadą i liczba będzie fałszywa
BEZ ŻADNEGO OBJAWU. Dopasuj po CZASIE.

PUŁAPKA — czasPozaPasmemKorektSekundy to SUMA ODSTĘPÓW, nie liczba próbek.
Przy nierównym próbkowaniu to zupełnie różne wielkości.

Testy: każde pole osobno, w tym null gdy serii brak; dopasowanie po czasie sprawdzone
na seriach o CELOWO różnej liczbie próbek; czas poza pasmem na serii o nierównych odstępach.
```

**Ukończone, gdy:** test na seriach o różnej liczbie próbek przechodzi.

---

## Etap D3 — trzy reguły ze źródłami

```
Etap D3 z sekcji 14 rozszerzenia. Kontrakt: sekcja 10. Wartości: sekcja 4.

Reguły powstają PRZED panelami, celowo: są czystą logiką testowalną bez interfejsu,
a panele mają rysować linie odniesienia z TYCH SAMYCH stałych, nie z własnych kopii.

0. NAJPIERW `PasmaOdniesienia` — jeden obiekt z tabelą z sekcji 8.8 specu. Wszystkie progi
   projektu w JEDNYM miejscu: pasma normy, zakresy fizyczne i jawne wpisy "brak normy".

   Reguły z tego etapu, linie odniesienia na panelach (D5, D6), karty przeglądu (D8)
   i raport (D9) czytają Z NIEGO. Nie z własnych kopii.

   PUŁAPKA: duplikat progu w dwóch miejscach to gwarancja, że po pierwszej zmianie ekran
   i reguła zaczną mówić co innego — a użytkownik zobaczy wartość "w paśmie" obok wniosku
   "poza normą" i przestanie ufać obu.

   OSIEM PASM NORMY, ANI JEDNO NOWE. Pięć wyprowadzonych z progów istniejących reguł
   bazowego (płyn 70–105, olej >=90, napięcie 13,0–15,0 i >12,4, korekta długa ±10,
   suma korekt ±20), trzy ze sprawdzonych źródeł z sekcji 4 (ciśnienie szyny 34–55
   i 138–241, katalizator 650–870 z zapłonem 300). Lambda 1,000 z definicji stechiometrii.

   CZTERY PARAMETRY MAJĄ JAWNE "BRAK NORMY" z uzasadnieniem: wyprzedzenie zapłonu,
   ciśnienie w kolektorze, temperatura dolotu, temperatura otoczenia. NIE wymyślaj dla
   nich pasm — powód każdego jest w tabeli 8.8.

1. GDI-1 w rules/RuleEngine.kt — werdykt przeglądu.
   Warunek: silnik rozgrzany AND prędkość 0D == 0 AND obroty 0C > 500 AND 23 < 27 bar
   Waga: uwaga

   PRÓG 27 bar JEST WYPROWADZONY, nie wymyślony:
     34 bar (dolna granica ciśnienia zadanego na jałowym, sekcja 4.1)
     −  7 bar (dopuszczalne odchylenie rzeczywistego od zadanego, sekcja 4.1)
     = 27 bar
   Wpisz to wyprowadzenie w komentarzu przy stałej.

   "SILNIK ROZGRZANY" — sekcja 10.0 rozszerzenia, przeczytaj ja:
     silnikRozgrzany  ==  plyn 05 >= 70 C  AND  czas pracy 1F >= 600 s

   NIE UZYWAJ OilTempEstimator.silnikRozgrzany. Model oleju calkuje po czasie i istnieje
   wylacznie w trakcie nagrywania — przeglad jest jednorazowym skanem, wiec model nie ma
   tam stanu i warunek NIGDY BY SIE NIE SPELNIL. To byl blad wersji roboczej specu.

   70 C i 600 s to istniejace stale projektu (regula termostatu z tabeli 10.4 bazowego
   i prog "rozgrzany" z modelu oleju). Zero nowych stalych.

   Obroty > 500 to istniejacy prog "silnik pracuje". Predkosc == 0 odroznia jalowy od jazdy.

   ROZSZERZ STRUKTURY WEJSCIOWE — sekcja 10.4 rozszerzenia:
     RuleInput         + cisnienieSzynyBar, predkoscKmh, temperaturaKatalizatoraC
     Report.ruleInput    wypelnij je z readings (PID 23, 0D, 3C)
     AlertSnapshot     + temperaturaKatalizatoraC
   Wszystkie opcjonalne. Brakujaca wartosc POMIJA regule, nie zeruje jej.

2. KAT-1 w rules/RuleEngine.kt — werdykt przeglądu.
   Warunek: silnik rozgrzany AND 3C < 300 C
   Waga: uwaga.  Próg: 300 C — minimum skuteczne dla konwersji, sekcja 4.2.

3. KAT-2 w alerts/AlertEngine.kt — ALARM NA ŻYWO, nie reguła przeglądu.
   Warunek: 3C > 870 C.  Waga: uwaga.  Karencja 60 s jak pozostałe alarmy "uwaga".
   Próg: 870 C — górna granica normalnego zakresu pracy, sekcja 4.2.

   KAT-2 jest alarmem, bo przegrzanie zdarza się W JEŹDZIE i jest wtedy istotne —
   na postoju katalizator stygnie. Bez warunku "silnik rozgrzany": przegrzanie jest
   przegrzaniem niezależnie od tego, jak długo trwa jazda.

4. TREŚĆ WNIOSKÓW. Każdy MUSI zawierać zakres odniesienia i wzmiankę o pochodzeniu —
   wzory w sekcji 10. Liczba bez zakresu nic nie mówi komuś, kto nie wie, ile powinno być.
   Żaden wniosek nie orzeka, CO jest zepsute: GDI-1 wymienia dwie możliwe przyczyny
   i nie rozstrzyga między nimi, bo z jednego pomiaru się nie da.

ZAKAZ: żadna z tych trzech nie ma wagi "usterka". Sekcja 4.3: źródła są branżowe,
nie fabryczne dla G4NC, i na "jest zepsute" nie wystarczają. Podniesienie wagi "żeby
użytkownik na pewno zauważył" jest dokładnie tym błędem, przed którym ostrzega
tabela wag w bazowym.

Testy: każda reguła osobno; GDI-1 NIE odpala przy nierozgrzanym silniku ani w jeździe
przy tym samym ciśnieniu; karencja KAT-2; test sprawdzający, że żadna z trzech nie
ma wagi usterka; test, że treść każdego wniosku zawiera zakres odniesienia.

TEST KOMPLETNOŚCI PASM — obowiązkowy: przejdź listę wszystkich PID-ów wyświetlanych
gdziekolwiek w aplikacji i sprawdź, że KAŻDY ma wpis w PasmaOdniesienia — pasmo albo
jawne "brak". Bez tego testu parametr dodany za pół roku pojawi się z pustą kolumną normy.

TEST JEDNEGO ŹRÓDŁA: reguła GDI-1 i stała używana później przez panel GDI to ta sama
referencja, nie dwie równe liczby.
```

**Ukończone, gdy:** trzy reguły działają, żadna nie ma wagi `usterka`, a progi mają wyprowadzenie w komentarzu.

---

## Etap D4 — panel Mieszanka

```
Etap D4 z sekcji 14 rozszerzenia. Układ: sekcja 8.3. Osie: sekcja 8.6.

1. Panel 2 ekranu żywego. Listwa czterech kafli na górze ZOSTAJE — wspólna dla paneli.

2. Wykres pokazuje SUMĘ korekt (06 + 07), nie każdą osobno. Suma mówi, jak daleko
   sterownik odszedł od mapy bazowej. Składniki pod spodem jako liczby, bo czyta się
   je jako poziom, nie jako kształt.

3. Os Y sztywna −25…+25 %. Pasmo **±20 %** narysowane NA STALE jako dwie linie odniesienia.

   UWAGA, latwo pomylic: ±10 % to prog reguly dla korekty DLUGOTERMINOWEJ SAMEJ,
   ±20 % to prog reguly `trim_sum` dla SUMY. Wykres pokazuje sume, wiec pasmo to ±20 %.
   Wersja robocza specu rysowala tu ±10 % — czyli pasmo cudzej reguly.
   Obie liczby biore sie z PasmaOdniesienia z etapu D3, nie z literalow.

3a. LISTWA KAFLI dostaje TRZECI WIERSZ z pasmem — sekcja 8.8 specu:
      88 °C ~   |   92 °C    |  13,9 V    |  +3,9 %
      OLEJ mod. |   PLYN     |  NAPIECIE  | KOREKTA D
        >= 90   |  70–105    | 13,0–15,0  | -10 – +10
    Kolumna normy NIE MA PRAWA byc pusta.

    POPRAWKA P1: czwarty kafel to KOREKTA DLUGA 0107, nie poziom paliwa.
    Po tej zmianie WSZYSTKIE CZTERY kafle maja pasmo ze zrodla — wczesniej czwarty
    mial "—", bo poziom w baku zadnej normy nie ma.
    Pasmo -10 do +10 % to prog regul ltft_lean / ltft_rich, czytany z PasmaOdniesienia
    z etapu D3. NIE literal w kodzie UI.

    UWAGA, latwo pomylic z punktem 3: kafel pokazuje KOREKTE DLUGA SAMA (pasmo ±10 %),
    wykres pod nim pokazuje SUME korekt (pasmo ±20 %). Rozne wielkosci, rozne pasma,
    na jednym ekranie. To nie jest niespojnosc.

3b. Korekta krotkoterminowa ma w miejscu normy "—". Zadna regula nie dotyczy jej osobno.

4. Wiersz "Poza pasmem ±20 %: 0:00 z 4:12" — licznik czasu W TEJ SESJI, nie stan chwilowy.
   Odróżnia jednorazowy skok przy wyprzedzaniu od stanu trwającego pół przejazdu.
   Bierz z pola czasPozaPasmemKorektSekundy z etapu D2, nie licz ponownie.
   Prog tego licznika to ±20 %, ten sam co pasmo wykresu.

5. Pole ZA KAT. pokazuje "—" Z POWODEM, bo PID 15 nie ma formuły w katalogu (sekcja 3.2).
   NIE pomijaj tego wiersza i NIE wstawiaj zera — pominięcie wygląda jak brak funkcji
   w aucie, a zero jest nieodróżnialne od pomiaru.

Testy: formatowanie sumy; pole ZA KAT. renderuje "—" z powodem, nigdy "0".
```

**Ukończone, gdy:** pasmo ±10 % rysuje się na stałe, a `ZA KAT.` pokazuje `—` z powodem.

---

## Etap D5 — panel Wtrysk GDI

```
Etap D5 z sekcji 14 rozszerzenia. Układ: sekcja 8.4. Osie: sekcja 8.6.

1. Trzy przebiegi na WSPÓLNEJ osi czasu: ciśnienie szyny 23, obciążenie absolutne 43,
   pozycja przepustnicy 11.

   Ciśnienie samo w sobie nic nie mówi — 52 bary są świetne na luzie i fatalne przy
   pełnym gazie. Dopiero zestawienie z obciążeniem czyni z tego pomiar.

2. Oś Y ciśnienia sztywna 0–240 bar. Górna granica z pasma obciążeniowego (241 bar,
   sekcja 4.1), zaokrąglona w dół.
   NIE bierz jej z zakresu fizycznego PID-u w katalogu (655 350 kPa) — to granica typu,
   nie silnika, i cała krzywa zmieściłaby się w dolnym procencie wykresu.

3. DWA PASMA na jednym wykresie, bo cisnienie ma dwie normy zaleznie od stanu pracy:
   34–55 bar (jalowy) i 138–241 bar (obciazenie). Obie rysowane JEDNOCZESNIE — kierowca
   widzi, do ktorego pasma krzywa zmierza, bez przelaczania czegokolwiek.

   Wszystkie cztery liczby bierz z PasmaOdniesienia z etapu D3, TA SAMA REFERENCJA
   co regula GDI-1. Nie duplikuj ich jako literaly.

3a. Obciazenie i przepustnica NIE MAJA pasm — zakres fizyczny, bez normy (tabela 8.8).
    Sa tu jako odniesienie dla cisnienia, nie jako pomiary do oceny. Nie dorysowuj im linii.

4. Wiersz dolny: "Max w sesji: 148 bar przy 84 % obciążenia".
   Obciążenie przy maksimum jest OBOWIĄZKOWE. Bierz z pola podsumowania z etapu D2.

5. NIE pokazuj ciśnienia ZADANEGO. To PID producencki Hyundaia, poza standardem OBD-II
   (sekcja 5.1). Jeśli wspominasz o nim na ekranie — napisz, że auto go nie oddaje.

Testy: przycinanie do 240 bar i oznaczenie; linie odniesienia pochodzą z tych samych
stałych co GDI-1; wiersz maksimum pokazuje obciążenie.
```

**Ukończone, gdy:** cztery linie odniesienia z tych samych stałych co reguła, maksimum z obciążeniem.

---

## Etap D6 — panel Termika

```
Etap D6 z sekcji 14 rozszerzenia. Układ: sekcja 8.5. Osie: sekcja 8.6.

1. Trzy krzywe na wspólnej osi: katalizator 3C, płyn 05, olej z modelu.
   Pokazują to, czego żadna osobno nie pokaże: katalizator rozgrzewa się pierwszy
   i najszybciej, płyn drugi, olej najwolniej. Odstępstwo od tej kolejności samo
   w sobie jest sygnałem.

2. Os katalizatora sztywna 0–1000 C. Os do 900 przykleilaby prog KAT-2 (870 C)
   do krawedzi — czyli stan, dla ktorego sie na wykres patrzy, bylby najgorzej widoczny.

3. WSZYSTKIE TRZY KRZYWE MAJA PASMA, kazde z innego zrodla:
     katalizator  300 C (zaplon), 650–870 C (norma)   <- sekcja 4.2, reguly KAT-1/KAT-2
     plyn         70–105 C                            <- progi regul thermostat/overheat
     olej (model) >= 90 C                             <- prog reguly oil_cold
   Wszystkie z PasmaOdniesienia z etapu D3, ta sama referencja co reguly.

3a. Dolot 0F i otoczenie 46 maja w miejscu normy "—". Temperatura powietrza
    z definicji nie ma normy — nie wymyslaj jej.

4. Temperatura oleju NADAL ze skalą pewności — niezmiennik 14 bazowego. Kafel i krzywa
   podpisane jako model, nie pomiar.

5. Wiersz dolny: "Płyn 90 °C po 6:24 · olej po 12:10". Bierz z czasDo90CSekundy z D2.
   Gdy nigdy nie osiągnięto — pokaż "—", NIE "0:00". Zero znaczyłoby "natychmiast",
   czyli dokładnie odwrotnie niż prawda.

6. Dolot 0F i otoczenie 46 jako liczby, nie krzywe — zmieniają się przez minuty.

Testy: kolejność krzywych na sztucznym przebiegu rozgrzewania; "—" gdy 90 °C
nigdy nie osiągnięto; linie odniesienia z tych samych stałych co KAT-1 i KAT-2.
```

**Ukończone, gdy:** trzy linie odniesienia współdzielone z regułami, a nieosiągnięte 90 °C daje `—`.

---

## Etap D7 — przełączanie paneli

```
Etap D7 z sekcji 14 rozszerzenia. Kontrakt: sekcja 8.1 i 8.7.

1. Przełączanie: SZEROKIE przeciągnięcie w poziomie przez obszar wykresów. Bez małych
   celów, bez menu, bez przycisków. Listwa kafli i pasek panelu nieruchome.

2. Wskaźnik: ● ○ ○ ○ plus nazwa, zawsze widoczne.

3. ZMIANA REGUŁY BEZPIECZEŃSTWA z bazowego — sekcja 8.7 rozszerzenia.
   Dodaj do LiveInteraction wartość panelSwitch i pozwól na nią TAKŻE W RUCHU.

   Uzasadnienie ma trafić do komentarza w kodzie: to jedno szerokie przeciągnięcie
   bez celu do trafienia, nie zmienia niczego w nagrywaniu i jest mniej rozpraszające
   niż czytanie ośmiu liczb naraz — a po to panele powstały.

   NIC POZA TYM. parameterChange, settings, navigation i start zostają zablokowane
   w ruchu dokładnie jak dotąd.

4. Test: panelSwitch dozwolony przy prędkości > 0 i parameterChange przy tej samej
   prędkości NADAL zablokowany — OBA W JEDNYM TEŚCIE, żeby było widać, że to
   rozróżnienie, a nie poluzowanie.
```

**Ukończone, gdy:** jeden test pokazuje oba zachowania obok siebie.

---

## Etap D8 — karty przeglądu

```
Etap D8 z sekcji 14 rozszerzenia. Układy: sekcja 9.

1. Karta "Wtrysk GDI" — układ 9.1. Ciśnienie, obciążenie, obroty.
   ZAKRES ODNIESIENIA POD WARTOŚCIĄ ("34 – 55 bar") jest treścią tej karty, nie ozdobą.
   Liczba bez niego nie mówi nic komuś, kto nie wie, ile powinno być.

   Zdanie na dole zostaje: "Stan pompy widać dopiero pod obciążeniem — zobacz panel
   WTRYSK GDI podczas jazdy." Przegląd tego nie rozstrzygnie i ma to powiedzieć wprost.

2. Karta "Katalizator" — układ 9.2. Temperatura z dwoma zakresami odniesienia
   (zapłon 300 °C, normalna praca 650–870 °C), sonda za katalizatorem, monitory.

   Wiersz sondy pokazuje "—" z powodem: "PID 15 obsługiwany przez auto, brak formuły
   w katalogu". Kreska z powodem jest uczciwsza niż pominięcie wiersza, bo pominięcie
   wygląda jak brak funkcji w aucie.

   Monitory gotowości są już czytane z PID 01 — tylko je zestaw, nie odpytuj ponownie.

3. Karta "Odczyty", grupa "Powietrze i dolot": ciśnienie w kolektorze 0B,
   atmosferyczne 33, podciśnienie jako różnica. Podciśnienie jest JEDYNĄ wartością
   liczoną, nie mierzoną — oznacz to.

4. KONTRAKT PASM NA CALYM EKRANIE ODCZYTOW — sekcja 9.4 specu. Ekran wypisuje wszystkie
   obslugiwane PID-y z przeciecia katalogu i maski, kilkanascie pozycji na tym aucie.
   KAZDA dostaje kolumne "norma": pasmo albo "—".

   KOLUMNA NORMY NIE MA PRAWA BYC PUSTA W ZADNYM WIERSZU. Puste miejsce wyglada jak
   przeoczenie i uzytkownik nie wie, czy normy nie ma, czy zapomniano ja wpisac.

Testy: karty renderuja "—" tam, gdzie brak danych; podcisnienie liczone jako roznica;
zakresy odniesienia pochodza z PasmaOdniesienia, nie z literalow;
TEST KOMPLETNOSCI: zaden wiersz ekranu odczytow nie ma pustej kolumny normy.
```

**Ukończone, gdy:** każda liczba na kartach ma obok zakres odniesienia.

---

## Etap D9 — raport sesji

```
Etap D9 z sekcji 14 rozszerzenia. Układy: sekcja 11.2 i 11.3.

1. Blok DIAGNOSTYKA w naglowku raportu — uklad 11.2. Cztery wiersze z pol z etapu D2,
   kazdy Z PASMEM pod spodem. Puste pole (null) pokazuje "—", nie jest pomijane.

1a. CALY naglowek raportu podlega sekcji 8.8, nie tylko blok DIAGNOSTYKA. Max plyn,
    napiecie, obroty, predkosc — kazde z pasmem albo z "—". Wartosc poza pasmem
    dostaje znacznik ▲ / ▼, TAKZE W RAPORCIE, nie tylko na zywo.

   Zakresy odniesienia w raporcie ZASTĘPUJĄ regułę, której nie da się napisać
   (sekcja 5.1): użytkownik widzi 148 bar wobec spodziewanych 138–241 i ocenia sam.

2. Trzy nowe pasma w stosie: ciśnienie szyny, katalizator, suma korekt.
   Wszystkie na TYM SAMYM wspólnym suwaku co istniejące.

3. Decymacja min-max obowiązuje dla nowych pasm tak samo. Nie dopisuj im osobnej ścieżki.

PUŁAPKA: nowe pasma pochodzą z pętli ŚREDNIEJ, czyli mają około CZTERY RAZY MNIEJ
próbek niż pasma gorące. Wspólny suwak musi dla każdego pasma znaleźć wartość
NAJBLIŻSZĄ CZASOWO — nie zakładaj wspólnej siatki czasu ani wspólnej liczby próbek.

Testy: suwak zwraca poprawną wartość dla pasma o czterokrotnie rzadszym próbkowaniu;
blok DIAGNOSTYKA pokazuje "—" dla pól null.
```

**Ukończone, gdy:** wspólny suwak działa na pasmach o różnej gęstości próbek.

---

## Etap D10 — weryfikacja w aucie

```
Etap D10 z sekcji 14 rozszerzenia. Kontrakt: sekcja 15.

Ten etap wymaga człowieka i samochodu. Przygotuj docs/weryfikacja-diagnostyka.md
z miejscem na wynik każdego punktu:

1. Zimny start i jazda do pełnego rozgrzania — kolejność krzywych na panelu Termika,
   czas dojścia płynu do 90 °C, kiedy katalizator przekracza 300 °C.
2. JEDNO pełne otwarcie przepustnicy na bezpiecznym odcinku — czy ciśnienie szyny
   wchodzi w pasmo 138–241 bar.
3. Jazda miejska i trasa — czy korekty zachowują się różnie przy różnym obciążeniu,
   czy katalizator trzyma się pasma 650–870 °C.
4. WYKRESLONE — ROZSTRZYGNIETE POZA AUTEM (poprawka P1). Sprawdzenie 2F jest zbedne:
   wlasciciel potwierdzil 2026-08-16, ze poziomu paliwa nie ma. 2F wypadl
   z odpytywania i z wyswietlania. NIE powtarzaj tego kroku.
4a. Korekta dluga 0107 na czwartym kaflu — czy odswieza sie w jezdzie.
   Ma sie zmieniac przez minuty. Wartosc stojaca w miejscu znaczy, ze kafel czyta
   probke, ktora nie przychodzi.
4b. Kafel przy petli otwartej — czy pokazuje "— ○", a nie ostatnia liczbe.
   WYMAGA 0103, wiec sprawdzalne DOPIERO OD ETAPU K2. Na tym etapie tylko odnotuj.
5. Zapis prawdziwych odpowiedzi pętli średniej do MockI40Script.

PUNKT 2 JEST WERYFIKACJĄ ŹRÓDEŁ, NIE TYLKO KODU. Jeśli to auto pod pełnym obciążeniem
nie zbliża się do 138 bar, znaczy to ALBO że pompa słabnie, ALBO że zakres z sekcji 4.1
nie opisuje tego silnika. W tym drugim przypadku przepisujemy sekcję 4 — NIE naciągamy
do niej odczytu i NIE zmieniamy progu reguły, żeby przestała się odpalać.

Po powrocie przyjmiesz logi i dopiszesz je do atrapy. NIE zmieniaj żadnej stałej
na podstawie jednego przejazdu.
```

**Ukończone, gdy:** atrapa zawiera prawdziwe odpowiedzi pętli średniej. Sprawa `2F` jest
**rozstrzygnięta poza tym etapem** — poprawka P1, nic tu do sprawdzania.

---

## Prompt kontrolny — po rozszerzeniu

```
Przeczytaj oba specy i przejrzyj repozytorium. Nie pisz kodu. Odpowiedz z dowodem
z pliku i numeru linii:

1. Czy pętla gorąca ma nadal dokładnie sześć PID-ów, tempo 4 Hz i obowiązkowe 0D oraz 05?

2. Czy istnieje test dowodzący, że ŻADEN cykl nie wykonuje trzech zapytań? Pokaż go
   i pokaż, na ilu cyklach działa.

3. Czy rotacja zimna jest na n % 10 == 5, a odczyt 03 na n % 200 == 150?
   Jesli ktorykolwiek zostal na 0, kolizja z petla srednia wrocila. Sa to DWIE osobne
   zmiany fazy, obie konieczne — sekcje 7.2 i 7.4.

4. Wypisz wszystkie trzy nowe progi (27 bar, 300 °C, 870 °C) i pokaż przy każdym
   komentarz z wyprowadzeniem albo odsyłaczem do sekcji 4 specu. Próg bez tego jest
   wymyślony.

5. Czy któraś z trzech nowych reguł ma wagę "usterka"? Sekcja 4.3 tego zabrania.

6. Czy treść każdego nowego wniosku zawiera zakres odniesienia i wzmiankę o pochodzeniu?

7. Czy panele rysują linie odniesienia z TYCH SAMYCH stałych co reguły, czy z własnych
   kopii? Pokaż deklaracje.

8. Czy obciazeniePrzyMaxCisnieniu jest dopasowane po CZASIE, czy po indeksie tablicy?

9. Czy czasPozaPasmemKorektSekundy sumuje ODSTĘPY, czy liczy próbki?

10. Czy pole ZA KAT. i wiersz sondy w karcie katalizatora pokazują "—" z powodem,
    czy gdzieś wkradło się zero?

11. Czy wspólny suwak radzi sobie z pasmami o czterokrotnie różnej gęstości próbek?

12. Czy panelSwitch jest dozwolony w ruchu, a parameterChange nadal nie?

13. Wypisz KAZDY parametr wyswietlany gdziekolwiek w aplikacji i pokaz jego wpis
    w PasmaOdniesienia. Ktorykolwiek bez wpisu — albo pasma, albo jawnego "brak" —
    jest bledem. Pokaz tez test, ktory tego pilnuje.

14. Czy ktorekolwiek pasmo istnieje TYLKO w kodzie widoku, bez odpowiednika w regule
    albo w sekcji 4 specu? Takie pasmo jest liczba wymyslona pokazana jako norma.

15. Czy przekroczenie pasma gdziekolwiek wywoluje AlertPlayer? Powinno alarmowac
    wylacznie piec warunkow krytycznych — reszta zmienia tylko znacznik.

16. Czy wykres sumy korekt i licznik czasu poza pasmem uzywaja ±20 %, czy ±10 %?
```
