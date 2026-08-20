# i40-android — prompty dla Cursora: rozszerzenie wyglądu

Pięć etapów `W1`–`W5`, **jeden nowy czat na etap**. Wchodzą **po etapie S4** rozszerzenia
objaśnień.

---

## Jak to prowadzić

**Do każdego promptu dołącz siedem dokumentów:**

```
@docs/spec/2026-08-14-i40-android-design.md
@docs/spec/2026-08-14-i40-android-diagnostyka-design.md
@docs/spec/2026-08-14-i40-android-kontekst-design.md
@docs/spec/2026-08-14-i40-android-odniesienie-design.md
@docs/spec/2026-08-15-i40-android-historia-design.md
@docs/spec/2026-08-15-i40-android-objasnienia-design.md
@docs/spec/2026-08-15-i40-android-wyglad-design.md
```

**Najważniejsze zdanie całego rozszerzenia — sekcja 2.2 specu:**

> **Ta aplikacja nie pokazuje chwilowych wskazań. Ona rejestruje przebieg.**

Dlatego metafora jest z rejestratora taśmowego, a nie z tarczy zegara — i dlatego **nie wolno
narysować ani jednej igły, tarczy ani obrotomierza**. Tak wygląda każda aplikacja OBD na rynku
i tak wygląda każdy domyślny pomysł generatora.

**Po każdym etapie:**

```
./gradlew ktlintCheck ; if ($?) { ./gradlew lint } ; if ($?) { ./gradlew test }
```

**Sześć pytań kontrolnych:**

*„Skąd ten kolor?"* — **wyłącznie z tabel §3.2 i §3.3.** Literał koloru poza plikiem motywu to
błąd, jest na to test.

*„Czy dołożyłem animację?"* — **wolno dokładnie jedną**: linię skanującą. Sekcja 6.4.

*„Czy ta krzywa jest wygładzana?"* — **nie wolno.** Spline rysuje wartości, których czujnik
nie podał. To ta sama nieprawda co zero zamiast kreski.

*„Czy ta liczba ma cyfry tabelaryczne?"* — przy 4 Hz każda inna drga w poziomie.

*„Czy oś się skaluje sama?"* — **nie.** Osie są sztywne od warstwy diagnostycznej.

*„Czy zmieniłem układ panelu?"* — **nie wolno.** Ta warstwa nadaje język, nie przestawia paneli.

---

## Etap W1 — gęstość ekranu i dwa motywy

```
Etap W1 z sekcji 14 specu wygladu. Kontrakt: sekcje 3 i 7.

╔══════════════════════════════════════════════════════════════════╗
║  ZACZNIJ OD ODCZYTU NA PRAWDZIWYM RADIU, NIE OD KODU.            ║
║                                                                  ║
║  Karta produktu mowi 8", instrukcja z pudelka wymienia 9" i 10". ║
║  Rozdzielczosc jest pewna (1280x720), gestosc NIE.               ║
║                                                                  ║
║  Odczytaj i WPISZ DO SEKCJI 7 SPECU:                             ║
║    - densityDpi                                                  ║
║    - smallestScreenWidthDp                                       ║
║    - screenWidthDp x screenHeightDp                              ║
║    - wersje Androida (13 czy 14 — karta produktu i instrukcja    ║
║      mowia co innego)                                            ║
║                                                                  ║
║  Do czasu odczytu obowiazuje zalozenie sw600dp i ZADEN uklad     ║
║  nie ma prawa zakladac wiecej.                                   ║
╚══════════════════════════════════════════════════════════════════╝

1. ui/Theme.kt — DWA motywy, tokeny doslownie z sekcji 3.2 i 3.3.

   NOC jest domyslny. DZIEN dochodzi obok, nie zamiast.

   OSIEM ROL W OBU MOTYWACH, te same nazwy:
     tlo-glebia  pole  siatka  odczyt  przygasle  model  uwaga  usterka

   ZADEN literal koloru poza tym plikiem. Test tego pilnuje.

2. CZEROWN JEST ZAREZERWOWANA. Token `usterka` wolno uzyc TYLKO dla:
     - wagi `usterka` we wnioskach
     - alarmu powtarzanego (plyn powyzej progu)
   Nic innego w calej aplikacji nie ma prawa byc czerwone.
   Jesli czerwony znaczy wiele rzeczy, przestaje znaczyc "zjedz".

3. Token `model` WYLACZNIE przy wartosciach z tylda. Praktycznie tylko
   temperatura oleju. Zobaczenie go gdziekolwiek indziej to BLAD.

4. KROJE — wbuduj w APK, obydwa SIL OFL 1.1:
     JetBrains Mono   dane, cyfry TABELARYCZNE
     Inter            tekst i etykiety

   NIE ROBOTO. Jest domyslny w Androidzie, czyli niewidoczny —
   aplikacja w nim wyglada jak kazda inna.

5. TEST KONTRASTU — liczony z wartosci tokenow, NIE oceniany na oko:
   kazda para tekst/tlo w OBU motywach >= 4,5 : 1 (WCAG 2.1, 1.4.3).

6. TESTY:
   - zaden literal koloru poza Theme.kt
   - oba motywy przechodza 4,5:1
   - token usterka uzyty tylko w dwoch dozwolonych miejscach
   - token model uzyty tylko przy wartosciach z tylda

NIE ROB w tym etapie zadnego wykresu ani ukladu.
```

**Ukończone, gdy:** gęstość odczytana i wpisana do §7, oba motywy przechodzą test kontrastu,
zero literałów koloru poza `Theme.kt`.

**Co zwykle idzie nie tak:** dobranie kolorów „na oko, żeby ładnie wyglądało" zamiast przepisania
tabel. Tokeny są dobrane pod ekran w desce rozdzielczej, nie pod monitor.

---

## Etap W2 — skala, siatka, pola dotykowe, przełącznik motywu

```
Etap W2 z sekcji 14 specu wygladu. Kontrakt: sekcje 4, 5 i 3.6.

1. SKALA TYPOGRAFICZNA — doslownie z sekcji 4.2:
     kafel-wartosc 44sp | slad-wartosc 28sp | stan-zdanie 34sp
     tekst 17sp | etykieta 13sp | os 12sp

   17sp TO PODLOGA, NIE PROPOZYCJA. Androidowe domyslne 14-15sp
   projektowano na telefon w dloni. Na wyciagniecie reki, przy
   drganiach, to za malo — i kontrastem sie tego nie nadrobi.

2. OGRANICZENIE SKALI SYSTEMOWEJ do 1,3x. Radio bywa dostarczane
   z podkrecona czcionka; powyzej tego panele przestaja sie miescic.
   Ograniczenie ma byc JAWNE w kodzie i udokumentowane, nie ciche.

3. SIATKA — jednostka 8dp, wszystkie odstepy jej wielokrotnoscia.
   Promien rogu 4dp. LEDWIE zaokraglone — to przyrzad, nie widzet.

4. POLA DOTYKOWE:
     56 x 56dp   elementy uzywane W RUCHU — dokladnie trzy:
                 przelaczanie paneli, zatrzymanie nagrywania,
                 zamkniecie alarmu
     48 x 48dp   reszta (postoj)
     8dp         odstep miedzy celami

5. PRZELACZNIK MOTYWU w ustawieniach: NOC / DZIEN / AUTOMATYCZNIE.

   AUTOMATYCZNIE bez sygnalu z radia wybiera NOC. NIE ZGADUJ Z ZEGARA —
   zegar w tym radiu bywa nieustawiony, a falszywe przelaczenie na jasny
   motyw w tunelu jest gorsze niz ciemny motyw w dzien.

6. PRZELACZENIE MOTYWU NIE PRZERYWA NAGRYWANIA. Motyw to warstwa
   rysowania; nagrywanie zyje w usludze pierwszoplanowej. JEST NA TO TEST.

7. TESTY:
   - przelaczenie motywu w trakcie nagrywania -> sesja nietknieta
   - skala systemowa 2,0x -> aplikacja ogranicza do 1,3x
   - trzy elementy "w ruchu" maja >= 56dp
   - automat bez sygnalu wybiera NOC
```

**Ukończone, gdy:** przełączenie motywu w trakcie nagrywania nie przerywa sesji, ograniczenie
1,3× działa, trzy cele w ruchu mają 56dp.

---

## Etap W3 — pole kalibrowane

```
Etap W3 z sekcji 14 specu wygladu. Kontrakt: sekcje 2.3, 2.4 i 6.1.

1. TO JEST ELEMENT SYGNATUROWY CALEJ APLIKACJI. Przeczytaj sekcje 2.3.

   LINIE SIATKI POD WYKRESEM TO GRANICE PASM — nie okragle liczby
   co dwadziescia jednostek. Norma jest WYDRUKOWANA na papierze,
   po ktorym idzie slad. Uzytkownik nie sprawdza, ile powinno byc:
   widzi to pod krzywa.

   Zrodlo granic: PasmaOdniesienia (etap D3). TO SAMO ZRODLO co kolumna
   normy. NIE TWORZ DRUGIEJ KOPII ZADNEGO PROGU.

2. BRAK PASMA = BRAK SIATKI. Parametr bez normy dostaje puste pole,
   bez ani jednej linii. Brak normy staje sie WIDOCZNY jako brak nadruku
   — ta sama zasada, co kreska zamiast zera, tylko narysowana.

   TEST OBOWIAZKOWY: parametr z pasmem "brak" nie ma linii siatki.

3. KOLEJNOSC WARSTW JEST OBOWIAZKOWA, od spodu:
     1. pole
     2. cieniowanie  (przedmuchiwanie / petla otwarta — kontekst 8.3)
     3. siatka       (linie granic + podpisy wartosci)
     4. slad
     5. wartosc      (liczba przy prawej krawedzi)

   CIENIOWANIE IDZIE POD SIATKE, NIE NAD SLAD. Nad sladem zaslanialoby
   dane, ktorych dotyczy.

4. SIATKA BEZ ANTYALIASINGU. Linie poziome na calych pikselach sa ostre.
   Rozmyta linia granicy pasma wyglada jak niepewnosc, ktora nie jest.

5. TESTY:
   - linie siatki odpowiadaja granicom z PasmaOdniesienia, nie okraglym liczbom
   - pasmo "brak" -> zero linii
   - kolejnosc warstw: cieniowanie pod siatka
```

**Ukończone, gdy:** siatka pochodzi z pasm, parametr bez pasma nie ma linii, warstwy w kolejności.

**Co zwykle idzie nie tak:** narysowanie „normalnej" siatki co okrągłą wartość, bo tak robi
każda biblioteka wykresów. Wtedy cały pomysł znika i zostaje zwykły wykres.

---

## Etap W4 — ślad, zakazy, linia skanująca

```
Etap W4 z sekcji 14 specu wygladu. Kontrakt: sekcje 6.2, 6.3 i 6.4.

1. SLAD: grubosc 2dp, laczenia OSTRE, kolor `odczyt`,
   antyaliasing WLACZONY na krzywej, WYLACZONY na siatce.

   Ostre laczenia sa celowe. Decymacja minimum-maksimum istnieje po to,
   zeby ZACHOWAC skoki; zaokraglanie rogow zjadaloby je z powrotem
   na poziomie rysowania.

2. ZAKAZY — kazdy z powodem, zaden nie jest kwestia gustu:

   BEZ WYGLADZANIA KRZYWEJ (spline).
     Rysuje punkty, ktorych nie zmierzono. TO JEST TA SAMA NIEPRAWDA
     CO ZERO ZAMIAST KRESKI, tylko na poziomie pikseli.

   BEZ WYPELNIENIA POD KRZYWA.
     Sugeruje calke, czyli sume. Zadna z tych wielkosci nie sumuje sie
     sensownie.

   BEZ GRADIENTOW, CIENI, WYPUKLOSCI.
     Nic nie znacza, a maskuja odczyt wartosci z pozycji.

   BEZ AUTOMATYCZNEGO SKALOWANIA OSI.
     Osie sa sztywne od warstwy diagnostycznej. Ruchoma os klamie
     o skali zmiany.

   BEZ IGIEL, TARCZ I OBROTOMIERZY.
     Sekcja 2.1. Tak wyglada kazda aplikacja OBD na rynku.

3. LINIA SKANUJACA — JEDYNA ANIMACJA W CALEJ APLIKACJI:
     gdzie    puste pole kalibrowane (parametr jeszcze niezmierzony)
     wyglad   pionowa linia 1dp w kolorze `siatka`, przesuw w prawo
     okres    2,4 s na przebieg pola
     znika    po pierwszym odczycie tego parametru

   NIESIE INFORMACJE, KTOREJ NIC INNEGO NIE NIESIE: aplikacja zyje,
   po prostu jeszcze nie dostala danych — w odroznieniu od: zawiesila sie.
   Bez niej puste pole i zamrozona aplikacja wygladaja IDENTYCZNIE.

   Przy wlaczonym ograniczeniu ruchu w systemie: statyczna kreskowana
   krawedz. Informacja zostaje, ruch znika.

4. NIC INNEGO SIE NIE ANIMUJE. Wartosci zmieniaja sie skokowo, przejscia
   paneli sa natychmiastowe, wykresy nie wjezdzaja. Ekran, ktory sie rusza
   podczas jazdy, odciaga wzrok od drogi.

   TEST: wyszukanie `animate` w ui/ daje trafienia TYLKO w jednym
   komponencie.

5. TESTY:
   - seria z ostrym skokiem po narysowaniu ZACHOWUJE skok (bez spline)
   - zero wypelnien, gradientow i cieni w kodzie wykresow
   - `animate` tylko w komponencie linii skanujacej
   - ograniczenie ruchu -> linia statyczna
```

**Ukończone, gdy:** ostry skok przeżywa rysowanie, jedna animacja w całym `ui/`.

---

## Etap W5 — weryfikacja w aucie

```
Etap W5 z sekcji 14 specu wygladu. Lista czynnosci: sekcja 11.

Tego etapu NIE DA SIE zrobic w Cursorze.

Przygotuj docs/weryfikacja-wyglad.md z miejscem na wynik kazdego
z dziewieciu punktow sekcji 11.

PUNKT NAJWAZNIEJSZY — #2: zaparkuj w PELNYM SLONCU i przelacz na DZIEN.

  To jedyny powod, dla ktorego motyw DZIEN w ogole istnieje.
  Jesli nie wygrywa z motywem NOC w sloncu — POPRAW GO ALBO USUN.
  Nie zostawiaj "bo juz jest".

Punkt #6 sprawdza cyfry tabelaryczne na zywo: patrz na wartosc
zmieniajaca sie 4 razy na sekunde. Jesli liczba DRGA W POZIOMIE,
krój nie ma cyfr tabelarycznych albo nie wlaczono odpowiedniej cechy.

NIE ZMIENIAJ zadnego tokenu na podstawie jednego spojrzenia.
```

**Ukończone, gdy:** lista z sekcji 11 przeszła w całości.

---

## Prompt kontrolny — po rozszerzeniu wyglądu

```
Sprawdz rozszerzenie wygladu wobec spec
@docs/spec/2026-08-15-i40-android-wyglad-design.md

Odpowiedz TAK/NIE z odsylaczem do pliku i linii.

KOLOR
 1. Czy istnieje JAKIKOLWIEK literal koloru poza Theme.kt? NIE POWINIEN.
 2. Czy oba motywy przechodza 4,5:1 dla tekstu — liczone, nie na oko?
 3. Czy token `usterka` (czerwien) jest uzyty gdziekolwiek poza waga
    `usterka` i alarmem powtarzanym? NIE POWINIEN.
 4. Czy token `model` jest uzyty gdziekolwiek poza wartoscia z tylda?
 5. Czy znaczniki ▲ ▼ ~ ⌀ ○ nadal dzialaja BEZ koloru?

TYPOGRAFIA
 6. Czy wszystkie wartosci liczbowe maja cyfry TABELARYCZNE?
 7. Czy uzyto Roboto? NIE POWINNO.
 8. Czy tekst nigdzie nie schodzi ponizej 17sp?
 9. Czy skala systemowa jest ograniczona do 1,3x?

WYKRESY
10. Czy linie siatki pochodza z PasmaOdniesienia, czy z okraglych liczb?
    MAJA POCHODZIC Z PASM.
11. Czy parametr bez pasma ma linie siatki? NIE POWINIEN.
12. Czy krzywa jest gdziekolwiek wygladzana? NIE POWINNA.
13. Czy jest jakiekolwiek wypelnienie pod krzywa, gradient albo cien?
14. Czy ktorakolwiek os skaluje sie automatycznie? NIE POWINNA.
15. Czy jest gdziekolwiek igla, tarcza albo obrotomierz? NIE POWINNO.

RUCH
16. Czy `animate` wystepuje poza komponentem linii skanujacej?
17. Czy ograniczenie ruchu zamienia linie na statyczna?

ZROWNOWAZENIE
18. Czy dolozono JAKIEKOLWIEK zapytanie OBD? NIE POWINNO.
19. Czy dolozono JAKIKOLWIEK prog diagnostyczny? NIE POWINNO.
20. Czy zmieniono uklad ktoregokolwiek z szesciu paneli? NIE POWINNO.
21. Czy zakladki sa nadal TRZY, a panele SZESC?
22. Czy gestosc ekranu zostala odczytana i wpisana do sekcji 7?
```

Punkty **10**, **12** i **16** są najważniejsze. Pierwszy pilnuje elementu, na którym stoi cały
kierunek. Drugi pilnuje, żeby rysowanie nie zaczęło kłamać tam, gdzie dane są uczciwe. Trzeci —
żeby ekran nie zaczął odciągać wzroku od drogi.
