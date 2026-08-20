# i40-android — rozszerzenie historii

**Data:** 2026-08-15
**Status:** zaakceptowany, gotowy do realizacji
**Rozszerza:** `2026-08-14-i40-android-design.md` (bazowy),
`2026-08-14-i40-android-diagnostyka-design.md` (diagnostyczne),
`2026-08-14-i40-android-kontekst-design.md` (kontekstowe),
`2026-08-14-i40-android-odniesienie-design.md` (odniesienia).
Wszystkie obowiązują bez zmian poza tym, co ta specyfikacja jawnie zmienia (sekcja 12)

---

## 1. Cel

Dwie rzeczy, obie w zakładce Historia.

**Pierwsza: kasowanie przejazdów zrobione bezpiecznie.** Kasowanie w bazowym specu istnieje —
sekcja 12.5 poświęca mu jedno zdanie: *„Przesunięcie w bok kasuje sesję."* Nie ma potwierdzenia,
nie ma cofnięcia, nie ma testu, nie ma wzmianki w promptach. To jest gorsze niż brak funkcji.
Gest jest niewidoczny, a na ekranie zamontowanym w desce rozdzielczej przesunięcie w bok zdarza
się przypadkiem — na wyboju, przy sięganiu po coś obok. Kasuje wtedy nagranie, którego **nie da
się odtworzyć**, bo ten przejazd zdarzył się raz.

**Druga: zestawienia, których jeszcze nie ma.** Karta miesiąca z rozszerzenia kontekstowego
porównuje **miesiąc do miesiąca**. Nie da się porównać dwóch konkretnych przejazdów ani zobaczyć
całości zbioru. Po roku jazdy to kilkaset nagrań, w których nie sposób nic znaleźć.

Rozszerzenie **nie dokłada ani jednego zapytania OBD**. Wszystko liczy z pól, które już są
w podsumowaniach.

---

## 2. Zasada: kasowanie jest nieodwracalne i ma tak wyglądać

W tej aplikacji dane mają jedną cechę, której nie mają dane w większości programów: **nie da się
ich zebrać ponownie**. Plik można odtworzyć z kopii, zdjęcie zrobić jeszcze raz, dokument
napisać od nowa. Przejazd z 14 marca o 8:15 zdarzył się raz i nigdy więcej się nie zdarzy.

Z tego wynikają trzy decyzje, które w innym programie byłyby przesadą:

1. **Każde kasowanie ma potwierdzenie** — także pojedyncze, także gestem.
2. **Potwierdzenie pokazuje, co konkretnie ginie** — data, dystans, czas, rozmiar. Nie „czy na
   pewno?", tylko „to ginie".
3. **Nie ma cofania paskiem na dole.** Pięciosekundowy pasek w jadącym aucie jest bezużyteczny:
   albo patrzysz na drogę, albo zdążył zniknąć. Zabezpieczenie musi działać **przed** akcją,
   nie po niej.

---

## 3. Zrównoważenie — co to rozszerzenie obciąża

### 3.1 Magistrala: nic

**Zero nowych zapytań OBD.** Skład i tempo pętli bez zmian — sekcja STAN AKTUALNY
w `.cursor/rules/00-projekt.mdc` obowiązuje bez poprawki. Wszystkie liczby pochodzą
z podsumowań już zapisanych w bazie.

To jest sprawdzane testem, nie deklaracją — sekcja 13.

### 3.2 Nastawa: bez zmian

`Zrównoważona` 4 Hz zostaje domyślna i wiążąca.

### 3.3 Baza: jedna kolumna, zero nowych tabel

Dokładamy `chroniony` do tabeli `przejazd`. Nic poza tym — porównania i zestawienia liczą się
z lotu, bo dotyczą kilkuset wierszy, a nie milionów.

**Kasowanie zmniejsza bazę** — jedyne rozszerzenie w tym projekcie, które zajmowane miejsce
odbiera, zamiast dokładać.

### 3.4 Ekran: bez nowej zakładki

Zakładki są trzy i zostają trzy. Wszystko mieści się w Historii:

| Co | Gdzie |
|---|---|
| Kasowanie pojedyncze | Przycisk w szczegółach przejazdu + gest na liście |
| Kasowanie wielokrotne | Tryb zaznaczania po przytrzymaniu pozycji |
| Panel Porządki | Osobny ekran, wejście z karty „Od początku" |
| Porównanie dwóch przejazdów | Osobny ekran, wejście ze szczegółów przejazdu |
| Karta „Od początku" | Drugi tryb karty miesiąca — ta sama karta, przełącznik |
| Filtry | Trzy znaczniki nad kalendarzem |

Jedyne dwa nowe ekrany to Porządki i Porównanie, oba wchodzą z miejsca, w którym pytanie
naturalnie powstaje.

---

## 4. Kasowanie

### 4.1 Trzy drogi, jedno okno potwierdzenia

| Droga | Gdzie | Kiedy jest wygodna |
|---|---|---|
| **Przycisk `Usuń`** | Szczegóły przejazdu | Obejrzałeś nagranie i wiesz, że jest bezwartościowe |
| **Gest w bok** | Lista dnia | Widzisz pozycję na liście i chcesz ją usunąć od razu |
| **Tryb zaznaczania** | Lista dnia, po przytrzymaniu | Kilka krótkich skoków do sklepu tego samego dnia |

**Wszystkie trzy prowadzą do tego samego okna potwierdzenia.** Gest przestaje kasować
bezpośrednio — to jest zmiana wobec sekcji 12.5 bazowego i jedyny powód, dla którego ta warstwa
w ogóle rusza istniejące zachowanie.

### 4.2 Okno potwierdzenia — pojedynczy przejazd

```
┌─────────────────────────────────────────────────────────┐
│  Usunąć ten przejazd?                                   │
│                                                         │
│  15 sierpnia 2026, 14:03                                │
│  12 min · 6,1 km · 2 880 próbek · 0,3 MB                │
│                                                         │
│  Tego nagrania nie da się odtworzyć.                    │
│                                                         │
│  Zostaje 1 punkt odniesienia zebrany podczas tego       │
│  przejazdu — kolumna „poprzednio" się nie zmieni.       │
│                                                         │
│  Karta miesiąca dla sierpnia przeliczy się.             │
│                                                         │
│                    [ Anuluj ]        [ Usuń ]           │
└─────────────────────────────────────────────────────────┘
```

Cztery bloki i każdy ma powód:

**Co ginie** — data, czas trwania, dystans, liczba próbek, rozmiar. Bez tego okno pyta
„czy na pewno?", a to pytanie, na które nie da się odpowiedzieć, bo nie wiadomo, o co chodzi.

**Nieodwracalność, wprost.** Nie „ta operacja jest nieodwracalna" tylko *„tego nagrania nie da
się odtworzyć"* — bo o to naprawdę chodzi.

**Co zostaje** — sekcja 4.3.

**Co się przeliczy** — karta miesiąca liczy z przejazdów, więc skasowanie przejazdu z lipca
zmienia lipcowe liczby. Zmiana danych historycznych bez uprzedzenia wygląda jak błąd
aplikacji, dlatego jest uprzedzenie.

### 4.3 Co ginie, a co zostaje

To jest jedyne miejsce w tym rozszerzeniu, gdzie zapadła decyzja, a nie tylko projekt.

**Punkt odniesienia przeżywa skasowanie przejazdu, z którego powstał.**

Wynika to ze schematu, który już istnieje: `punkt_odniesienia` trzyma **własne mediany**
w kolumnie `odczyty` i **nie ma odsyłacza do przejazdu** (sekcja 8.1 rozszerzenia odniesienia).
Skasowanie przejazdu niczego nie psuje i niczego nie osieroca. Rozszerzenie odniesienia mówi
przy tym wprost: *„Starych punktów nie usuwamy"* (sekcja 7.4).

| | Ginie | Zostaje |
|---|---|---|
| Przebieg (`przebieg` BLOB) | ✓ | |
| Podsumowanie (`podsumowanie` BLOB) | ✓ | |
| Notatka | ✓ | |
| Punkty odniesienia z tego przejazdu | | ✓ |
| Zapisane przeglądy | | ✓ — nie są związane z przejazdami |

**Dlaczego punkt zostaje, a nie ginie:** ten pomiar naprawdę się odbył. Silnik naprawdę miał
wtedy takie ciśnienie na szynie. Skasowanie wykresu nie cofa faktu, że pomiar był, a kolumna
„poprzednio" opisuje **stan auta**, nie zbiór plików. Wymazanie punktu byłoby zafałszowaniem
historii samochodu przy okazji sprzątania dysku.

**Dlaczego mimo to jest o tym komunikat:** zachowanie, którego użytkownik się nie spodziewa,
musi być powiedziane wprost. Ciche przetrwanie danych po „usuń" to dokładnie ten rodzaj
niespodzianki, który każe zwątpić w cały program.

### 4.4 Znacznik `chroniony`

Przejazd, przy którym coś się działo — zapaliła się kontrolka, silnik dziwnie chodził, jechałeś
do mechanika — ma nie wypaść przy porządkach.

```sql
ALTER TABLE przejazd ADD COLUMN chroniony INTEGER NOT NULL DEFAULT 0;
```

Przełącznik w szczegółach przejazdu, kłódka `🔒` przy pozycji na liście.

**Ochrona działa przeciwko kasowaniu hurtem, nie przeciwko decyzji.** Chroniony przejazd da się
skasować pojedynczo — okno potwierdzenia dopisuje wtedy wiersz *„Ten przejazd jest chroniony."*
Blokowanie na amen zmusiłoby do zdejmowania ochrony przed kasowaniem, czyli do dwóch kliknięć
zamiast jednego ostrzeżenia, i niczego by nie chroniło.

**Panel Porządki chronionych nie rusza nigdy** — i mówi, ile pominął.

### 4.5 Sesja `w_toku` jest nietykalna

Przejazdu o statusie `w_toku` **nie da się skasować żadną z trzech dróg**. Usługa
pierwszoplanowa właśnie do niego pisze; skasowanie wiersza pod pracującym zapisem to błąd
w połowie nagrania.

Na liście taka pozycja nie ma gestu, nie ma pola zaznaczenia i nie wchodzi do wyników Porządków.
Panel Porządki liczy ją osobno: *„1 nagrywana teraz — pominięta"*.

### 4.6 Tryb zaznaczania

Przytrzymanie pozycji na liście dnia wchodzi w tryb zaznaczania.

```
┌─────────────────────────────────────────────────────────┐
│  ✕   Zaznaczono 2                      [ Zaznacz dzień ]│
├─────────────────────────────────────────────────────────┤
│  PIĄTEK 15 SIERPNIA                                     │
│                                                         │
│   ☐  07:12   41 min   23,4 km                      🔒   │
│   ☑  14:03   12 min    6,1 km                           │
│   ☑  17:48    8 min    3,2 km       przerwany           │
│   ·   19:30    — — —   nagrywanie trwa                  │
├─────────────────────────────────────────────────────────┤
│                        [  Usuń zaznaczone (2)  ]        │
└─────────────────────────────────────────────────────────┘
```

`Zaznacz dzień` zaznacza wszystkie pozycje widoczne w tym dniu — **z pominięciem nagrywanej**.
Pozycja `w_toku` ma kropkę zamiast pola wyboru, żeby jej nieobecność w zaznaczeniu była widoczna,
a nie zagadkowa.

Okno potwierdzenia dla wielu wymienia je z osobna:

```
┌─────────────────────────────────────────────────────────┐
│  Usunąć 2 przejazdy?                                    │
│                                                         │
│  Łącznie 20 min · 9,3 km · 0,5 MB                       │
│                                                         │
│   15 sierpnia 14:03   12 min   6,1 km                   │
│   15 sierpnia 17:48    8 min   3,2 km    przerwany      │
│                                                         │
│  Tych nagrań nie da się odtworzyć.                      │
│                                                         │
│  Zostają 2 punkty odniesienia.                          │
│  Karta miesiąca dla sierpnia przeliczy się.             │
│                                                         │
│                    [ Anuluj ]      [ Usuń 2 ]           │
└─────────────────────────────────────────────────────────┘
```

Powyżej dziesięciu pozycji lista się zwija do `… i 7 dalszych`, ale **suma i liczba zostają
zawsze** — one są tym, na co się patrzy przed naciśnięciem.

---

## 5. Panel Porządki

### 5.1 Po co osobny ekran

Kasowanie po jednym ma sens przy trzech przejazdach. Przy czterystu — nie. Po roku jazdy
większość zbioru to pięciominutowe skoki do sklepu, których nikt nigdy nie otworzy, a które
zajmują większość miejsca.

Wejście: przycisk `Porządki` na karcie „Od początku", obok liczby zajętego miejsca. To jest
naturalna kolejność — najpierw widzisz 184 MB, potem chcesz z tym coś zrobić.

### 5.2 Wygląd

```
┌─────────────────────────────────────────────────────────┐
│  ←  Porządki                                            │
├─────────────────────────────────────────────────────────┤
│  Zajęte miejsce        184 MB w 412 przejazdach         │
├─────────────────────────────────────────────────────────┤
│  WYBIERZ JEDNO KRYTERIUM                                │
│                                                         │
│   ○  Krótsze niż        [ 2 ] [ 5 ] [ 10 ] [ 15 ] min   │
│   ○  Sesje przerwane                                    │
│   ○  Starsze niż        [ 3 ] [ 6 ] [ 12 ] miesięcy     │
├─────────────────────────────────────────────────────────┤
│  DO USUNIĘCIA                                           │
│                                                         │
│   137 przejazdów · 10 MB                                │
│   4 chronione — pominięte                               │
│   1 nagrywana teraz — pominięta                         │
│                                                         │
│   12 mar 08:15    3 min   0,9 km                        │
│   14 mar 17:02    4 min   1,8 km                        │
│   18 mar 07:44    2 min   0,6 km                        │
│   … pokaż wszystkie                                     │
├─────────────────────────────────────────────────────────┤
│                  [  Usuń 137 przejazdów  ]              │
└─────────────────────────────────────────────────────────┘
```

### 5.3 Jedno kryterium naraz — świadomie

Kryteriów **nie da się łączyć**. „Krótsze niż 5 minut **i** starsze niż 3 miesiące" byłoby
wygodniejsze i jednocześnie znacznie łatwiejsze do pomylenia: każde dołożone kryterium to nowy
sposób na skasowanie czegoś, czego się nie chciało.

Jedno kryterium, widoczna lista, potwierdzenie. Kto chce przecięcia dwóch warunków, robi dwa
przebiegi — i za każdym razem widzi, co ginie.

### 5.4 Liczby w tym panelu nie są progami

`2 / 5 / 10 / 15` minut i `3 / 6 / 12` miesięcy to **kryteria wyboru, nie progi diagnostyczne**.

Ta różnica jest w tym projekcie istotna, bo obowiązuje zasada „nie ma źródła — nie ma stałej".
Te liczby nie wchodzą do żadnej reguły, nie pojawiają się w żadnym werdykcie i **nie twierdzą
niczego o samochodzie**. Aplikacja nigdzie nie mówi, że pięciominutowy przejazd jest gorszy ani
mniej wartościowy — mówi tylko: *oto przejazdy krótsze niż pięć minut, chcesz je usunąć?*

Żadna z nich nie jest oznaczona jako zalecana. Domyślnie **nie jest wybrane żadne kryterium**,
więc lista do usunięcia jest pusta, a przycisk nieaktywny.

### 5.5 Podgląd jest obowiązkowy

Przycisk usuwania jest nieaktywny, dopóki nie wybrano kryterium, i **zawsze pokazuje liczbę**.
Lista pozycji jest przewijalna i pełna — `… pokaż wszystkie` rozwija wszystko, bez skracania.

Trzy wiersze podsumowania (do usunięcia / chronione / nagrywana) pojawiają się **zawsze**, także
z zerami, bo `0 chronionych — pominięte` niesie informację, że ochrona w ogóle była brana
pod uwagę.

---

## 6. Porównanie dwóch przejazdów

### 6.1 Wybór: ten i poprzedni, z możliwością zmiany

Przycisk `Porównaj` w szczegółach przejazdu otwiera porównanie z **poprzednim przejazdem tego
samego auta** — chronologicznie poprzednim po `poczatek`, z tym samym VIN-em.

Drugi przejazd da się zmienić przyciskiem `[ zmień ]` w nagłówku kolumny. To pokrywa oba
przypadki: „jak dziś w porównaniu z ostatnim razem" jednym dotknięciem, a „jak dziś w porównaniu
z majem" — dwoma.

**Przy niezgodnym VIN-ie porównanie się nie otwiera.** Sekcja 8.3 rozszerzenia odniesienia
zabrania mieszania historii dwóch aut; porównanie jest dokładnie tym mieszaniem.

### 6.2 Wygląd

```
┌───────────────────────────────────────────────────────────┐
│  ←  Porównanie                                            │
├───────────────────────────────────────────────────────────┤
│                 15 sie 07:12     8 sie 07:20     różnica  │
│                        (ten)       [ zmień ]              │
├───────────────────────────────────────────────────────────┤
│  PRZEJAZD                                                 │
│  Dystans              23,4 km       21,8 km      +1,6 km  │
│  Czas                  41 min        39 min       +2 min  │
│  Średnia prędkość     34 km/h       34 km/h            —  │
├───────────────────────────────────────────────────────────┤
│  SILNIK                                                   │
│  Maks. obroty           3 240         3 410        −170   │
│  Średnie obroty         1 780         1 820         −40   │
│  Maks. prędkość        92 km/h       88 km/h     +4 km/h  │
├───────────────────────────────────────────────────────────┤
│  TERMIKA                                                  │
│  Maks. płyn              97 °C        96 °C        +1 °C  │
│  Do 90 °C                 6:40         6:12        +28 s  │
│  Maks. katalizator      712 °C       698 °C       +14 °C  │
├───────────────────────────────────────────────────────────┤
│  MIESZANKA                                                │
│  Mediana korekty        +4,1 %       +3,8 %      +0,3 pp  │
│  Poza pasmem         2 min 10 s   1 min 40 s       +30 s  │
├───────────────────────────────────────────────────────────┤
│  WTRYSK                                                   │
│  Maks. ciśnienie       186 bar      191 bar      −5 bar   │
├───────────────────────────────────────────────────────────┤
│  ZASILANIE                                                │
│  Napięcie min           13,6 V       13,7 V      −0,1 V   │
├───────────────────────────────────────────────────────────┤
│  KODY                                                     │
│  Na starcie              brak         brak                │
│  Na końcu                brak         brak                │
└───────────────────────────────────────────────────────────┘
```

### 6.3 Dystans i czas są pierwsze — to nie jest przypadek

Porównanie dwudziestotrzykilometrowej trasy z czterokilometrowym skokiem do sklepu jest
bezwartościowe: oczywiście maksymalne obroty będą inne. Ale **aplikacja tego nie ocenia i nie
ostrzega** — bo „te przejazdy są nieporównywalne" to wniosek, a wniosków nie stawiamy
(sekcja 10.3 rozszerzenia kontekstowego).

Zamiast ostrzeżenia: **dystans, czas i średnia prędkość stoją na samej górze**, żeby kontekst
był widoczny, zanim spojrzysz na resztę. Informacja zamiast oceny — ta sama zasada, inny środek.

`Średnia prędkość` to `dystansKm / czasTrwaniaS`, jedyna wartość wyliczana w tym rozszerzeniu.
Bez nowego pola w podsumowaniu i bez nowej stałej.

### 6.4 Wiersze, których nie da się porównać

Kreska, nigdy zero — jak w całym projekcie.

| Sytuacja | Kolumna wartości | Kolumna różnicy |
|---|---|---|
| Pole `null` w jednym przejeździe | `—` | `—` |
| Pole `null` w obu | `—` | `—` |
| Wartości równe | liczba | `—` |
| `paliwoL` | `—` zawsze | `—` — to auto nie ma `5E` |
| Kody błędów | lista albo `brak` | **brak kolumny różnicy** |

**Kody nie mają różnicy liczbowej.** `P0171` minus `P0300` nie jest niczym. Pokazujemy dwie
listy obok siebie i zostawiamy porównanie oczom.

Sesja o statusie `odzyskany` daje się porównywać, ale ma pod datą etykietę `przerwany` — jej
podsumowanie policzono z przebiegu częściowego (sekcja 11.4 bazowego) i to musi być widoczne.

### 6.5 Bez ocen — konsekwentnie

Kolumna różnicy zawiera **wyłącznie liczbę ze znakiem**. Bez strzałek, bez kolorów, bez słów
`lepiej` i `gorzej`.

Powód jest ten sam, dla którego karta miesiąca nie ocenia: **nie wiem, czy +28 s do 90 °C to
problem**. Może termostat zaczyna siadać, a może po prostu było chłodniej. Liczby obok siebie
to informacja, strzałka to wniosek — pierwsze mogę dać uczciwie, drugiego nie.

---

## 7. Karta „Od początku"

### 7.1 Ta sama karta, drugi tryb

Karta miesiąca stoi już w nagłówku Historii. Dołożenie drugiej karty zepchnęłoby kalendarz poza
ekran, więc zamiast tego **karta ma przełącznik**:

```
┌───────────────────────────────────────────────────────────┐
│  ◀   CZERWIEC 2027   ▶                  [ od początku ]   │
├───────────────────────────────────────────────────────────┤
│  ... karta miesiąca bez zmian ...                         │
└───────────────────────────────────────────────────────────┘
```

Po dotknięciu:

```
┌───────────────────────────────────────────────────────────┐
│  ◀   CZERWIEC 2027   ▶                  [ od początku ✓ ] │
├───────────────────────────────────────────────────────────┤
│  OD POCZĄTKU                                              │
│  Pierwszy zapis          14 sierpnia 2026                 │
│  Przejazdy               412                              │
│  Dystans               3 680 km                           │
│  Czas za kierownicą      122 h 40                         │
│  Bez rozgrzania         94 z 412                          │
├───────────────────────────────────────────────────────────┤
│  Zajęte miejsce          184 MB           [ Porządki ]    │
└───────────────────────────────────────────────────────────┘
```

Strzałki miesiąca zostają aktywne — przełączają kalendarz pod spodem, bo karta całości od
miesiąca nie zależy. Wyjście: ponowne dotknięcie przełącznika.

### 7.2 Skąd te liczby

| Wiersz | Skąd |
|---|---|
| Pierwszy zapis | `MIN(poczatek)` |
| Przejazdy | `COUNT(*)` bez `w_toku` |
| Dystans | suma `dystansKm` |
| Czas za kierownicą | suma `czasTrwaniaS` |
| Bez rozgrzania | ile podsumowań ma `czasDo90CSekundy == null` |
| Zajęte miejsce | `SUM(LENGTH(przebieg) + LENGTH(podsumowanie))` |

**Sumy, nie mediany.** Karta miesiąca używa median, bo szuka *typowej* jazdy i pojedynczy wybryk
by jej trend zepsuł. Ta karta odpowiada na pytanie *ile tego jest łącznie* — a tam mediana nie
znaczy nic.

**Wiersz „bez rozgrzania" zostaje bez progu**, dokładnie jak na karcie miesiąca: liczy wartości
`null`, nie stosuje żadnej temperatury granicznej.

**Pusty zbiór daje kreski, nie zera** — świeżo zainstalowana aplikacja pokazuje `—` w każdym
wierszu.

---

## 8. Filtry listy

Trzy znaczniki nad kalendarzem, wszystkie wyłączone domyślnie:

```
[ z kodami ]   [ przerwane ]   [ chronione ]
```

Włączony znacznik zawęża **jednocześnie kropki w kalendarzu i listę dnia**. Dzięki temu kalendarz
sam staje się wyszukiwarką: włączasz `z kodami` i widzisz, w które dni w ogóle coś się działo.

| Znacznik | Warunek | Po co |
|---|---|---|
| `z kodami` | `kodyNaStarcie` lub `kodyNaKoncu` niepuste | Odnalezienie przejazdu, w którym zapaliła się kontrolka |
| `przerwane` | `status == odzyskany` | Sprawdzenie, czy usługa nie ginie regularnie |
| `chronione` | `chroniony == 1` | Odnalezienie własnych zaznaczeń |

**Wszystkie trzy to fakty logiczne — zero progów, zero wymyślonych liczb.** Dlatego nie ma tu
filtru „dłuższe niż X", choć byłby przydatny: wymagałby wybrania X, a wybrane X wyglądałoby jak
twierdzenie o tym, jaki przejazd jest wart uwagi.

Znaczniki łączą się przez **i** — `przerwane` + `chronione` daje przerwane, które oznaczyłeś.
Filtry niczego nie kasują, więc łączenie ich nie niesie ryzyka z sekcji 5.3.

Gdy filtr nie daje żadnego wyniku w danym miesiącu: `Brak przejazdów spełniających filtr` pod
kalendarzem — nie pusty ekran bez wyjaśnienia.

---

## 9. Nie-cele

**Eksport i udostępnianie.** Wysyłanie nagrań gdziekolwiek to osobna funkcja z własnymi
pytaniami o prywatność i format. Ta warstwa dotyczy porządkowania tego, co jest na urządzeniu.

**Kosz i przywracanie.** Kosz zamienia „nie da się odtworzyć" na „da się, przez trzydzieści
dni", czyli zdejmuje powagę z okna potwierdzenia, a jednocześnie **nie zwalnia miejsca** — czyli
nie robi tego, po co się kasuje. Zabezpieczeniem jest potwierdzenie i znacznik `chroniony`.

**Kopia zapasowa.** Warta zrobienia, ale to inne zadanie: wymaga decyzji o miejscu, formacie
i szyfrowaniu. Wyraźnie poza zakresem.

**Automatyczne usuwanie starych przejazdów.** Program nie kasuje niczego sam, nigdy. Miejsce na
radiu z 128 GB nie jest problemem, który uzasadniałby ryzyko cichej utraty nagrania.

**Ocena przejazdu.** Ani noty, ani gwiazdek, ani „gorszy niż zwykle". Sekcja 6.5.

**Nakładanie wykresów dwóch przejazdów.** Sekcja 12.5 bazowego odrzuciła wykresy wieloseryjne
jako nieczytelne i to nadal obowiązuje. Porównanie jest na liczbach.

**Wykres trendu przez wiele miesięcy.** Kusi i jest tani, ale wymaga danych z kilkunastu
miesięcy, żeby cokolwiek pokazać. Do rozważenia, gdy te dane będą — nie wcześniej.

---

## 10. Decyzje projektowe

| Decyzja | Dlaczego |
|---|---|
| **Potwierdzenie zamiast cofania** | Pasek z cofnięciem trwa kilka sekund. W jadącym aucie albo patrzysz na drogę, albo już zniknął. Zabezpieczenie musi być przed akcją |
| **Gest przestaje kasować bezpośrednio** | Na ekranie w desce rozdzielczej przesunięcie w bok zdarza się przypadkiem. Gest zostaje jako skrót, ale prowadzi do tego samego okna |
| **Potwierdzenie wymienia, co ginie** | „Czy na pewno?" to pytanie, na które nie da się odpowiedzieć. „12 min · 6,1 km · 0,4 MB" to pytanie, na które da się |
| **Punkt odniesienia przeżywa** | Pomiar naprawdę się odbył. Kolumna „poprzednio" opisuje stan auta, nie zbiór plików |
| **…ale okno o tym mówi** | Ciche przetrwanie danych po „usuń" to niespodzianka, która każe zwątpić w cały program |
| **`chroniony` nie blokuje kasowania pojedynczego** | Blokada wymuszałaby zdejmowanie ochrony przed skasowaniem — dwa kliknięcia zamiast ostrzeżenia, zero zysku |
| **Jedno kryterium Porządków naraz** | Każde dołożone kryterium to nowy sposób na skasowanie czegoś, czego się nie chciało |
| **Liczby w Porządkach nie są progami** | Nie wchodzą do żadnej reguły i niczego nie twierdzą o aucie. Żadna nie jest oznaczona jako zalecana |
| **Karta „Od początku" to tryb, nie druga karta** | Druga karta zepchnęłaby kalendarz poza ekran. Przełącznik nie kosztuje nawigacji |
| **Sumy, nie mediany, na karcie całości** | Karta miesiąca szuka typowej jazdy — tu chodzi o to, ile tego jest łącznie |
| **Porównanie domyślnie z poprzednim** | Najczęstsze pytanie brzmi „jak dziś wobec ostatniego razu" i ma kosztować jedno dotknięcie |
| **Dystans i czas na górze porównania** | Kontekst przed liczbami zamiast ostrzeżenia o nieporównywalności — informacja, nie ocena |
| **Filtry tylko na faktach logicznych** | Filtr „dłuższe niż X" wymagałby X, a X wyglądałoby jak twierdzenie, który przejazd jest wart uwagi |
| **Sesja `w_toku` nietykalna** | Usługa właśnie do niej pisze |
| **Brak kosza** | Zdejmuje powagę z potwierdzenia i nie zwalnia miejsca, czyli nie robi tego, po co się kasuje |

---

## 11. Zapis

### 11.1 Nowa kolumna

```sql
ALTER TABLE przejazd ADD COLUMN chroniony INTEGER NOT NULL DEFAULT 0;
```

Jedyna zmiana schematu w tym rozszerzeniu. `0` = zwykły, `1` = chroniony.

### 11.2 Drabinka wersji schematu — do naprawy przy okazji

**W projekcie jest luka:** bazowy deklaruje *„wersja schematu 1"* (sekcja 8.6), a rozszerzenie
odniesienia dokłada dwie tabele — `punkt_odniesienia` i `przeglad` — **nie podnosząc wersji
i nie opisując `onUpgrade`**. Aplikacja zainstalowana przed rozszerzeniem O nie dostałaby tych
tabel i wywróciłaby się na pierwszym zapytaniu.

To rozszerzenie porządkuje drabinkę w całości:

| Wersja | Co dokłada | Skąd |
|---|---|---|
| **1** | `przejazd` | bazowy §8.6 |
| **2** | `punkt_odniesienia`, `przeglad` | rozszerzenie odniesienia §8.1, §8.2 — **wersja nie była zadeklarowana** |
| **3** | `przejazd.chroniony` | to rozszerzenie |

```
onUpgrade(db, stare, nowe):
    jeśli stare < 2:  CREATE TABLE punkt_odniesienia ...; CREATE TABLE przeglad ...
                      (wraz z indeksami z §8.1 i §8.2 rozszerzenia odniesienia)
    jeśli stare < 3:  ALTER TABLE przejazd ADD COLUMN chroniony INTEGER NOT NULL DEFAULT 0
```

**Kroki wykonują się po kolei, każdy jako osobne `jeśli`** — nie `gdy/inaczej`. Aktualizacja
z wersji 1 na 3 musi wykonać oba, a konstrukcja z `inaczej` wykonałaby tylko pierwszy. To jest
najczęstszy błąd w `onUpgrade` i test go pilnuje.

`onDowngrade` **rzuca wyjątkiem** — cofnięcie wersji aplikacji przy nowszej bazie oznacza, że
coś poszło bardzo nie tak, i cicha próba dalszej pracy skończy się utratą danych.

### 11.3 Kasowanie w bazie

```
usun(ids):
    1. odrzuć wszystkie o statusie w_toku
    2. w jednej transakcji: DELETE FROM przejazd WHERE id IN (...)
    3. VACUUM po skasowaniu więcej niż 50 wierszy
```

**Transakcja jest obowiązkowa** — przerwanie w połowie kasowania 137 wierszy zostawiłoby zbiór
w stanie, którego użytkownik nie zamawiał.

`VACUUM` dlatego, że SQLite po `DELETE` **nie oddaje miejsca systemowi plików**. Panel Porządki,
który pokazuje „zwolniono 48 MB", a po którym zajętość się nie zmienia, byłby kłamstwem —
i to takim, które da się sprawdzić w ustawieniach Androida.

Progu `50` nie stosujemy do decyzji o niczym poza wywołaniem `VACUUM`; to koszt operacji, nie
twierdzenie o danych.

### 11.4 Punktów odniesienia nie ruszamy

Kasowanie przejazdów **nie dotyka tabel `punkt_odniesienia` ani `przeglad`**. Sekcja 4.3.
Osobny test tego pilnuje.

---

## 12. Zmiany w kontraktach

| Miejsce | Zmiana |
|---|---|
| **§12.5 bazowego** | *„Przesunięcie w bok kasuje sesję"* → **gest otwiera okno potwierdzenia, nie kasuje bezpośrednio**. Dochodzą: przycisk w szczegółach, tryb zaznaczania, trzy filtry nad kalendarzem |
| **§8.6 bazowego — schemat** | Kolumna `chroniony`. **Wersja schematu 1 → 3**, z jawnym `onUpgrade` — patrz §11.2 |
| **§8.6 bazowego — wersja** | Deklaracja *„wersja schematu 1"* zastąpiona drabinką z §11.2. Naprawa luki: rozszerzenie odniesienia dokładało tabele bez podniesienia wersji |
| **§10.1 rozszerzenia kontekstowego** | Karta miesiąca dostaje **drugi tryb „od początku"** i przełącznik w nagłówku. Sama karta miesiąca **bez zmian** |
| **§7.4 rozszerzenia odniesienia** | Bez zmian, potwierdzone: punkty przeżywają skasowanie przejazdu. Ta warstwa **dopisuje o tym komunikat**, nie zmienia zachowania |
| **§10.3 rozszerzenia kontekstowego** | Bez zmian, rozszerzone: zakaz oceniania obowiązuje także kolumnę różnicy w porównaniu przejazdów |
| **Pętla, PID-y, nastawy, progi, reguły, alarmy** | **Bez żadnych zmian.** To rozszerzenie nie dotyka akwizycji |

**Czego nie zmieniamy:** pętli gorącej, jej częstotliwości, składu obowiązkowego `0D`, `05`
i `04`, żadnego progu, żadnej reguły werdyktu, żadnej osi wykresu, składu żadnego poziomu
odpytywania.

---

## 13. Testy

| Obszar | Test | Dlaczego akurat ten |
|---|---|---|
| **Zero nowych zapytań** | Lista PID-ów odpytywanych przed i po rozszerzeniu jest **identyczna** | Jedyny test pilnujący, że warstwa historii nie zaczęła dokładać do magistrali |
| **Sesja `w_toku`** | Kasowanie pojedyncze, wielokrotne i Porządki **odrzucają** wiersz `w_toku` — trzy osobne testy | Trzy drogi, trzy sposoby na skasowanie wiersza pod pracującym zapisem |
| **Punkty przeżywają** | Po skasowaniu przejazdu liczba wierszy w `punkt_odniesienia` **bez zmian** | Utrwala decyzję z §4.3 — inaczej ktoś „posprząta osierocone dane" |
| **Migracja 1 → 3** | Baza w wersji 1 po aktualizacji ma trzy tabele **i** kolumnę `chroniony` | Pilnuje osobnych `jeśli` zamiast `gdy/inaczej` — §11.2 |
| **Migracja 2 → 3** | Baza w wersji 2 dostaje kolumnę, tabel nie tworzy powtórnie | Druga gałąź drabinki |
| **Transakcyjność** | Przerwanie kasowania 100 wierszy zostawia bazę bez zmian | §11.3 |
| **Ochrona w Porządkach** | Chroniony przejazd spełniający kryterium **nie trafia** do listy i jest policzony w wierszu „pominięte" | §4.4 |
| **Ochrona nie blokuje pojedynczego** | Chroniony przejazd da się skasować pojedynczo, okno zawiera wiersz o ochronie | §4.4 — druga połowa decyzji |
| **Puste zbiory dają kreski** | Karta „od początku" bez ani jednego przejazdu: same `—`, nigdzie `0` | Zasada całego projektu |
| **Porównanie: `null`** | Pole `null` w którymkolwiek przejeździe → `—` w wartości **i** w różnicy, nigdy `0` | §6.4 |
| **Porównanie: brak ocen** | Kolumna różnicy nie zawiera `▲`, `▼`, `lepiej`, `gorzej` ani koloru zależnego od znaku | §6.5 — ten sam test co dla karty miesiąca |
| **Porównanie: VIN** | Dwa przejazdy o różnych VIN-ach → porównanie **niedostępne** | §8.3 rozszerzenia odniesienia |
| **Porównanie: kody** | Wiersze kodów **nie mają** kolumny różnicy | §6.4 |
| **Filtry** | Włączony filtr zawęża **kropki w kalendarzu i listę dnia jednocześnie** | Rozjazd między nimi to najbardziej mylący możliwy błąd tego ekranu |
| **Filtry: pusty wynik** | Komunikat, nie pusty ekran | §8 |
| **`VACUUM`** | Po skasowaniu > 50 wierszy rozmiar pliku bazy **maleje** | §11.3 — bez tego panel kłamie o zwolnionym miejscu |
| **Suma rozmiaru** | `SUM(LENGTH(...))` zgadza się z sumą po pojedynczych wierszach | Liczba, na którą użytkownik patrzy przed kasowaniem |

**Funkcje wyboru są czyste.** `ktoreDoUsuniecia(przejazdy, kryterium)` nie dotyka bazy — dostaje
listę metadanych, zwraca listę identyfikatorów. Dzięki temu wszystkie testy Porządków chodzą na
JVM, bez Androida i bez SQLite.

---

## 14. Kolejność realizacji

| Etap | Zakres | Ukończony, gdy |
|---|---|---|
| **H1** | Migracja schematu 1 → 3, kolumna `chroniony`, czyste funkcje wyboru | Testy migracji z wersji 1 i 2 zielone; `ktoreDoUsuniecia` bez Androida |
| **H2** | Kasowanie pojedyncze: przycisk, gest, okno potwierdzenia | Gest nie kasuje bez okna; `w_toku` odrzucone; punkty przeżywają |
| **H3** | Tryb zaznaczania i kasowanie wielokrotne | `Zaznacz dzień` pomija `w_toku`; okno wymienia pozycje i sumy |
| **H4** | Panel Porządki | Bez kryterium przycisk nieaktywny; chronione pominięte i policzone; `VACUUM` zwalnia miejsce |
| **H5** | Porównanie dwóch przejazdów | `null` daje `—`; kody bez różnicy; różny VIN blokuje; zero ocen |
| **H6** | Karta „od początku" i trzy filtry | Pusty zbiór daje `—`; filtr zawęża kropki i listę jednocześnie |
| **H7** | Weryfikacja w aucie | Lista z sekcji 15 przejdzie w całości |

Wchodzą **po etapie O8**. Kolejność `H1` → `H2` → `H3` jest wiążąca: `H2` i `H3` korzystają
z okna potwierdzenia zbudowanego w `H2`, a oba stoją na funkcjach z `H1`.

`H5` i `H6` są od siebie niezależne — można je zamienić miejscami.

---

## 15. Weryfikacja w aucie

Etap `H7`, na zamontowanym radiu, z prawdziwym zbiorem nagrań:

| # | Czynność | Czego szukamy |
|---|---|---|
| 1 | Przesuń pozycję w bok podczas jazdy po nierównej drodze | **Okno się otwiera, nic nie ginie.** To jest cały powód tej warstwy |
| 2 | Skasuj jeden przejazd, wróć do karty miesiąca | Liczby przeliczone; brak pustego wiersza po skasowanej pozycji |
| 3 | Skasuj przejazd, z którego powstał punkt odniesienia; otwórz przegląd | Kolumna „poprzednio" **niezmieniona** — §4.3 działa |
| 4 | Oznacz przejazd jako chroniony, uruchom Porządki z kryterium, które go łapie | Nie ma go na liście; wiersz „1 chroniony — pominięty" |
| 5 | Uruchom Porządki podczas nagrywania | Wiersz „1 nagrywana teraz — pominięta"; bieżąca sesja nietknięta |
| 6 | Skasuj > 50 przejazdów, sprawdź zajętość w ustawieniach Androida | Miejsce **naprawdę** zwolnione — `VACUUM` zadziałał |
| 7 | Porównaj dwa przejazdy o bardzo różnej długości | Dystans i czas na górze; **żadnego ostrzeżenia ani oceny** |
| 8 | Włącz filtr `z kodami` | Kropki w kalendarzu i lista dnia zawężone **zgodnie** |
| 9 | Zainstaluj wersję z `H1` na radiu z bazą sprzed rozszerzenia | Migracja przechodzi, stare przejazdy widoczne, nic nie zginęło |
| 10 | Odczytaj kartę „od początku" po miesiącu jazdy | Sumy zgadzają się z odczuciem; „bez rozgrzania" wygląda sensownie |

**Punkt 9 jest najważniejszy z całej listy.** Jest jedynym, który sprawdza migrację na
prawdziwych danych — a migracja jest jedyną częścią tego rozszerzenia, która potrafi
nieodwracalnie zepsuć to, co już zebrałeś.

---

## 16. Źródła

To rozszerzenie **nie wprowadza ani jednej liczby wymagającej źródła zewnętrznego**.

| Liczba | Rodzaj | Skąd |
|---|---|---|
| `2 / 5 / 10 / 15` min, `3 / 6 / 12` mies. | kryterium wyboru | Wybór użytkownika. Nie wchodzi do żadnej reguły, nie twierdzi niczego o aucie — §5.4 |
| `50` wierszy przed `VACUUM` | próg techniczny | Koszt operacji na bazie, nie własność samochodu — §11.3 |
| `10` pozycji przed zwinięciem listy | ograniczenie widoku | Czytelność okna, nie własność danych — §4.6 |

Wszystkie progi diagnostyczne, pasma i reguły pozostają **dokładnie takie, jak w warstwach
poprzednich**. Bibliografia `docs/zrodla.md` bez zmian.
