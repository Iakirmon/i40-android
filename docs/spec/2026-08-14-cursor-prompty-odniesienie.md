# i40-android — prompty dla Cursora: rozszerzenie odniesienia

Osiem etapów `O1`–`O8`, **jeden nowy czat na etap**. Wchodzą **po etapie K6** rozszerzenia
kontekstowego.

---

## Jak to prowadzić

**Do każdego promptu dołącz cztery dokumenty:**

```
@docs/spec/2026-08-14-i40-android-design.md
@docs/spec/2026-08-14-i40-android-diagnostyka-design.md
@docs/spec/2026-08-14-i40-android-kontekst-design.md
@docs/spec/2026-08-14-i40-android-odniesienie-design.md
```

**Najważniejsze zdanie całego rozszerzenia — sekcja 2 specu:**

> **Norma mówi, ile powinno być. „Poprzednio" mówi, ile było u Ciebie.**

Wartość z historii **nigdy nie może zostać opisana jako `norma`**. Kolumna normy zostaje kreską
tam, gdzie nią była; obok stoi osobna kolumna. Zatarcie tej różnicy to ciche kłamstwo:
użytkownik, który zobaczy `norma 705–714`, uzna, że tak ma być w tym modelu.

**Po każdym etapie:**

```
./gradlew ktlintCheck ; if ($?) { ./gradlew lint } ; if ($?) { ./gradlew test }
```

**Cztery pytania kontrolne:**

*„Czy dołożyłem jakiekolwiek zapytanie OBD?"* — **nie wolno.** To rozszerzenie liczy z próbek,
które już przechodzą przez pętlę. Sekcja 3.1.

*„Skąd ta liczba?"* — rozszerzenie **nie wprowadza ani jednej nowej stałej**. Cztery progi stanu
pochodzą z dokumentów wcześniejszych.

*„Czy to jest norma, czy poprzedni pomiar?"* — dwie różne rzeczy, dwie różne etykiety.

*„Czy porównuję to samo z tym samym?"* — pomiar spoza stanu nie tworzy punktu i nie jest
z niczym zestawiany.

---

## Etap O1 — definicja stanu

```
Etap O1 z sekcji 14 specu odniesienia. Kontrakt: sekcja 4.

1. Czysta funkcja rozpoznajaca stan JALOWY ROZGRZANY:

     obroty 010C  > 500          silnik pracuje
   ∧ predkosc 010D = 0           postoj
   ∧ plyn 0105 >= 70 C           rozgrzany
   ∧ czas pracy 011F >= 600 s    rozgrzany na dobre

   ZERO NOWYCH STALYCH. Wszystkie cztery progi juz istnieja:
     > 500 i = 0   z dokumentu bazowego (tabela 10.4 i sekcja 12.4)
     >= 70 C i >= 600 s  z sekcji 10.0 rozszerzenia diagnostycznego

   UZYJ TYCH SAMYCH STALYCH, ktorych uzywaja reguly GDI-1 i KAT-1 — TEJ SAMEJ
   REFERENCJI, nie kopii o tej samej wartosci. Trzy kopie rozjada sie przy pierwszej
   zmianie i wtedy regula bedzie mowic co innego niz ekran.

2. Funkcja jest CZYSTA: bez Androida, bez zegara, bez I/O. Dostaje odczyty,
   zwraca true albo false.

3. Testy — CZTERY OSOBNE PRZYPADKI, kazdy warunek lamany pojedynczo:
   - obroty 400, reszta spelniona -> false
   - predkosc 3 km/h, reszta spelniona -> false
   - plyn 65 C, reszta spelniona -> false
   - czas pracy 400 s, reszta spelniona -> false
   - wszystko spelnione -> true
   Plus test, ze stan uzywa tej samej stalej co GDI-1 (pokaz referencje, nie wartosc).
```

**Ukończone, gdy:** cztery warunki łamane pojedynczo, a stała jest wspólna z regułami.

---

## Etap O2 — okno, mediana, zapis

```
Etap O2 z sekcji 14 specu odniesienia. Kontrakt: sekcja 7 i 8.

1. Zbieranie — sekcja 7.1:
     stan zaczyna zachodzic   -> otworz okno, zbieraj probki wszystkich parametrow
     stan przestaje zachodzic -> jesli okno objelo co najmniej JEDEN PELNY OBIEG
                                 POZIOMU WOLNEGO: policz MEDIANE kazdego parametru
                                 i zapisz punkt. W przeciwnym razie odrzuc okno.

   MINIMALNE OKNO TO WYMOG MECHANICZNY, NIE PROG. Poziom wolny odpytuje 011F, 0133,
   0146 i 010F raz na dwadziescia cykli goracych. Przed uplywem tego czasu
   TYCH PARAMETROW PO PROSTU NIE MA W OKNIE, wiec punkt bylby niekompletny.
   Licz to w cyklach (20 cykli goracych), nie w sekundach — przy innej nastawie
   tempa sekundy sie zmieniaja, a liczba cykli nie.

2. MEDIANA, NIE SREDNIA — sekcja 7.2. Odczyty na jalowym drgaja: obroty oscyluja,
   korekty pracuja, cisnienie szyny pulsuje z praca pompy. Srednia da sie przesunac
   jednym wyskokiem przy wlaczeniu klimatyzacji albo wentylatora.

3. Tabela punkt_odniesienia — schemat w sekcji 8.1.
   Pole stan istnieje mimo jednej mozliwej wartosci, zeby dolozenie drugiego stanu
   nie wymagalo migracji schematu.
   Pole probek jest widoczne na ekranie diagnostycznym.

4. ROZDZIAL PO VIN — sekcja 8.3:
     VIN zgodny        -> normalna praca
     VIN inny          -> zapytaj uzytkownika, czy prowadzic osobna historie
     VIN nieodczytany  -> PUNKT NIE POWSTAJE

   Ostatni przypadek jest wazny: punkt bez VIN-u trafilby do wspolnego worka
   i zatrul zakresy obu aut. Lepiej go nie tworzyc.

5. ZAKAZ: nie dokladaj ZADNEGO zapytania OBD. Wszystkie potrzebne parametry juz
   plyna przez petle — tabela w sekcji 3.1 wymienia, ktory z ktorego poziomu.
   Napisz test, ktory to pilnuje (patrz punkt 6).

6. Testy:
   - okno krotsze niz 20 cykli goracych NIE tworzy punktu,
   - mediana na oknie z jednym skrajnym wyskokiem — wynik rozni sie od sredniej.
     Napisz ten test tak, zeby implementacja na sredniej go OBLALA,
   - punkty z dwoch roznych VIN-ow nie mieszaja sie,
   - brak VIN-u nie tworzy punktu,
   - TEST ZERA ZAPYTAN: liczba zapytan na cykl i na sekunde IDENTYCZNA przed
     i po wdrozeniu tego etapu.
```

**Ukończone, gdy:** test zera zapytań przechodzi, a mediana oblewa implementację na średniej.

---

## Etap O3 — zapisane przeglądy i porównanie kodów

```
Etap O3 z sekcji 14 specu odniesienia. Kontrakt: sekcja 8.2 i 9.3.

Ten etap jest NIEZALEZNY od O1 i O2 — porownanie kodow nie potrzebuje punktow
odniesienia. Mozna go zrobic rownolegle.

1. Tabela przeglad — schemat w sekcji 8.2.
   Przeglad zapisuje sie ZAWSZE, takze gdy warunki nie byly spelnione. Pole stan
   mowi, czy ODCZYTY LICZBOWE nadaja sie do porownania — kody i monitory nadaja sie
   zawsze, bo nie zaleza od temperatury silnika.

2. Porownanie z poprzednim przegladem tego samego VIN-u. Cztery rodzaje zmian:
     kod POJAWIL sie
     kod ZNIKNAL
     monitor STRACIL gotowosc  (albo ja odzyskal)
     kontrolka MIL zmienila stan

3. Blok "ZMIANY OD <data>" — uklad w sekcji 9.3.
   Gdy nic sie nie zmienilo, napisz to WPROST: "Bez zmian: te same kody, te same
   monitory, ta sama kontrolka." Puste miejsce wyglada jak brak funkcji.

4. TEN BLOK NIE WYMAGA ZADNEJ LICZBY. Pojawienie sie kodu to fakt, nie ocena.
   Nie dopisuj do niego wagi, wniosku ani reguly werdyktu.

Testy: cztery rodzaje zmian, kazdy osobno; przypadek "bez zmian"; porownanie dziala
takze wtedy, gdy jeden z przegladow byl poza stanem.
```

**Ukończone, gdy:** cztery rodzaje zmian wykrywane, a porównanie kodów działa też spoza stanu.

---

## Etap O4 — ekran Odczyty: kolumna „poprzednio"

```
Etap O4 z sekcji 14 specu odniesienia. Uklad: sekcja 9.2.

1. Trzecia kolumna miedzy wartoscia a norma: "poprzednio", z data pod naglowkiem.

2. NAJWAZNIEJSZA ZASADA TEGO ETAPU — sekcja 2 specu:
   wartosc z historii NIGDY nie moze zostac opisana jako "norma".
   Kolumna normy zostaje kreska tam, gdzie nia byla.

   Cala wartosc tej funkcji jest w wierszach, gdzie NORMA TO KRESKA, a POPRZEDNIO
   MA LICZBE — bo nie wiadomo, ile powinno byc wyprzedzenia zaplonu, ale wiadomo,
   ile dawalo to auto miesiac temu.

3. Wiersze, ktorych nie da sie porownac, maja w kolumnie kreske: czas pracy jest
   za kazdym razem inny, a status petli nie jest liczba — porownuje sie go slowem.

3a. POZIOMU PALIWA NA TEJ KARCIE NIE MA (poprawka P1). Nie dodawaj wiersza
   "Poziom paliwa  0 % ⌀  |  0 ⌀  |  —" — to bylyby trzy kolumny niczego.
   Parametr bez danych USUWAMY Z WIDOKU, a nie pokazujemy jako pusty. Inaczej karta
   uczy, ze kreski sa normalne, i przestaje sie je zauwazac tam, gdzie cos znacza.
   Naglowek sekcji to "POWIETRZE I WTRYSK", nie "POWIETRZE I PALIWO".

4. Wizualnie odroznij "poprzednio" od pasma normy — inna waga czcionki albo inne
   polozenie. Uzytkownik ma widziec, ze to dwa rozne rodzaje informacji,
   nie dwie kolumny tego samego.

Testy: wartosc z historii renderuje sie jako "poprzednio", NIGDY jako "norma"
(test na tekscie, nie na kolorze); brak punktu daje kreske, nie zero.
```

**Ukończone, gdy:** test tekstowy potwierdza, że słowo `norma` nie pojawia się przy wartości z historii.

---

## Etap O5 — nagłówek przeglądu

```
Etap O5 z sekcji 14 specu odniesienia. Uklad: sekcja 9.1.

1. Wiersz warunkow w naglowku: stan, wartosci ktore o nim decyduja, i czy porownanie
   jest dostepne.

2. DWA ROZNE KOMUNIKATY, nie jeden:

   W stanie:
     Warunki  ● jalowy rozgrzany
              plyn 92 C · postoj · 712 obr/min · 14 min
     Porownanie z 12 lipca — ten sam stan   ✓

   Poza stanem:
     Warunki  ○ silnik nierozgrzany, plyn 52 C
     Porownanie liczbowe niedostepne — poprzedni przeglad byl na rozgrzanym
     silniku, wartosci nieporownywalne
     Kody bledow i monitory porownane mimo to   ✓

3. OSTATNI WIERSZ DRUGIEGO KOMUNIKATU JEST OBOWIAZKOWY. Aplikacja ma powiedziec
   WPROST, co porownala, a czego nie — zamiast po cichu pominac jedno albo pokazac
   drugie jako pelne porownanie.

Testy: przeglad w stanie pokazuje porownanie liczbowe; przeglad poza stanem NIE
pokazuje liczb, ale POKAZUJE kody i monitory. Oba w jednym tescie, zeby bylo widac,
ze to rozroznienie, a nie wylaczenie funkcji.
```

**Ukończone, gdy:** jeden test pokazuje oba zachowania obok siebie.

---

## Etap O6 — panel Podstawowy: dwa tryby

```
Etap O6 z sekcji 14 specu odniesienia. Uklad: sekcja 10.

1. Dwa tryby panelu Podstawowy — uklady w sekcji 10.1.
   W ruchu: wykresy, dokladnie jak dzis.
   Na postoju w stanie: trzy wartosci z porownaniem, bez wykresow.

   Przy stojacym aucie wykresy sa plaskimi kreskami — ta sama przestrzen uzyta
   na cos, co niesie tresc.

2. Wiersz stanu stoi NAD trescia, tak jak wiersz statusu petli na panelu Mieszanka.
   Ten sam jezyk w calej aplikacji: stan, ktory rozstrzyga o sensie tego, co ponizej,
   stoi wyzej niz to.

3. PRZEJSCIA SA ASYMETRYCZNE — sekcja 10.2, i latwo to zgubic:
     wykresy -> porownanie   po PELNYM OBIEGU POZIOMU WOLNEGO (20 cykli goracych)
     porownanie -> wykresy   NATYCHMIAST, gdy stan przestaje zachodzic

   Wolno wchodzic w tryb bez wykresow. NIE WOLNO w nim zostac, gdy auto rusza.
   Nie implementuj obu przejsc tym samym opoznieniem.

4. Progresja wyswietlania — sekcja 10.3:
     0 punktow  -> "pierwszy pomiar — brak porownania"
     1 punkt    -> "poprzednio 708"
     2 i wiecej -> "poprzednio 708 · N pomiarow 705–714"

   ZAKRES OD DRUGIEGO PUNKTU, nie od trzeciego. "Trzy" byloby liczba wzieta znikad;
   zakres z dwoch punktow to po prostu te dwie wartosci.

Testy: wejscie po 20 cyklach, wyjscie natychmiast — dwa osobne przypadki;
trzy stopnie progresji wyswietlania.
```

**Ukończone, gdy:** test potwierdza asymetrię przejść, a zakres pojawia się od drugiego punktu.

---

## Etap O7 — panel Wtrysk GDI: wiersz odniesienia

```
Etap O7 z sekcji 14 specu odniesienia. Uklad: sekcja 11.

1. Dodatkowy wiersz pod wierszem "Max w sesji":

     Na jalowym rozgrzanym:        38,4 bar
       poprzednio 38,1  ·  47 pomiarow 37,9–38,6

2. Panel NIE przelacza trybow — to jest dodatkowy wiersz, nie drugi uklad.

3. Wiersz widoczny WYLACZNIE w stanie. Poza nim ZNIKA, a nie pokazuje starej wartosci.
   Stara wartosc bez informacji, ze jest stara, bylaby wartoscia nieaktualna udajaca
   biezaca — czyli tym samym bledem co zero zamiast kreski.

Testy: wiersz znika poza stanem; wartosc pochodzi z punktow tego VIN-u.
```

**Ukończone, gdy:** wiersz znika poza stanem zamiast pokazywać starą wartość.

---

## Etap O8 — weryfikacja w aucie

```
Etap O8 z sekcji 14 specu odniesienia. Kontrakt: sekcja 15.

Przygotuj docs/weryfikacja-odniesienie.md z miejscem na wynik kazdego punktu:

1. Przejazd 15 min zakonczony postojem z pracujacym silnikiem — czy powstal punkt.
   Sprawdz na ekranie diagnostycznym liczbe punktow i liczbe probek.
2. Postoj na swiatlach po 10 min jazdy — czy panel przelacza sie po ~5 s
   i wraca NATYCHMIAST po ruszeniu.
3. Drugi przejazd tego samego dnia — czy pojawilo sie "poprzednio", a po nim zakres.
4. Przeglad na rozgrzanym silniku — naglowek pokazuje stan, porownanie dostepne.
5. Przeglad na zimnym silniku — liczby nieporownane, kody porownane.
6. Po tygodniu — czy zakres jest waski, czy szeroki.

PUNKT 6 JEST SPRAWDZIANEM SENSU CALEGO ROZSZERZENIA. Jesli po tygodniu zakres
obrotow na jalowym wynosi 690–760, to jest ZBYT SZEROKI, zeby cokolwiek znaczyc.
Wtedy trzeba ALBO zawezic definicje stanu, ALBO uznac, ze dla tego parametru
nie da sie zrobic sensownego odniesienia.

WYNIK NEGATYWNY TEZ SIE ZAPISUJE. Nie naciagaj do niego danych i nie poszerzaj
milczaco definicji stanu, zeby zakres wygladal lepiej.
```

**Ukończone, gdy:** punkt 6 ma zapisany wynik — także gdy jest negatywny.

---

## Prompt kontrolny — po rozszerzeniu odniesienia

```
Przeczytaj cztery specy i przejrzyj repozytorium. Nie pisz kodu. Odpowiedz z dowodem
z pliku i numeru linii:

1. Czy liczba zapytan OBD na cykl i na sekunde jest identyczna jak przed tym
   rozszerzeniem? Pokaz test, ktory tego pilnuje.

2. Czy gdziekolwiek wartosc pochodzaca z historii pomiarow jest opisana slowem
   "norma"? Nie powinna. Pokaz test na tekscie.

3. Czy definicja stanu JALOWY ROZGRZANY uzywa tej samej referencji co reguly
   GDI-1 i KAT-1, czy wlasnej kopii o tej samej wartosci?

4. Czy okno krotsze niz pelny obieg poziomu wolnego tworzy punkt? Nie powinno.

5. Czy mediana jest mediana, czy srednia? Pokaz kod i test, ktory je rozroznia.

6. Czy punkt bez odczytanego VIN-u moze powstac? Nie powinien.

7. Czy przejscia panelu Podstawowy sa asymetryczne — wejscie po 20 cyklach,
   wyjscie natychmiast? Pokaz oba.

8. Czy wiersz odniesienia na panelu GDI znika poza stanem, czy pokazuje stara
   wartosc?

9. Czy przeglad wykonany poza stanem pokazuje porownanie kodow bledow? Powinien.

10. Czy to rozszerzenie wprowadzilo JAKAKOLWIEK nowa stala liczbowa? Nie powinno.
    Wypisz wszystkie stale uzyte w definicji stanu i wskaz, z ktorego wczesniejszego
    dokumentu kazda pochodzi.
```
