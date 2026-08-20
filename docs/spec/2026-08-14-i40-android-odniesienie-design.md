# i40-android — rozszerzenie odniesienia

**Data:** 2026-08-14
**Status:** zaakceptowany, gotowy do realizacji
**Rozszerza:** `2026-08-14-i40-android-design.md` (bazowy),
`2026-08-14-i40-android-diagnostyka-design.md` (diagnostyczne),
`2026-08-14-i40-android-kontekst-design.md` (kontekstowe).
Wszystkie obowiązują bez zmian poza tym, co ta specyfikacja jawnie zmienia (sekcja 12)

---

## 1. Cel

Kilkanaście parametrów w tej aplikacji ma w kolumnie normy **kreskę**, bo żadna pojedyncza
liczba nie byłaby dla nich prawdziwa. Obroty, obciążenie, wyprzedzenie zapłonu, podciśnienie,
przepustnica.

Rozszerzenie nie wymyśla dla nich norm. **Pokazuje, ile te same parametry dawały w Twoim aucie
poprzednio** — w tym samym, ściśle zdefiniowanym stanie pracy.

| Zamiast | Pojawia się |
|---|---|
| `Wyprzedzenie zapłonu 12,5 °   norma —` | `Wyprzedzenie zapłonu 12,5 °   poprzednio 12,0 · 47 pomiarów 11,8–12,6` |

Nie jest to norma. Jest to **historia własnego egzemplarza** — i o to pytałeś.

---

## 2. Zasada: „poprzednio" to nie „norma"

> **Norma mówi, ile powinno być. „Poprzednio" mówi, ile było u Ciebie.**

To rozróżnienie musi przetrwać cały projekt, bo jego zatarcie byłoby cichym kłamstwem.

| | Norma | Poprzednio |
|---|---|---|
| Skąd | źródło zewnętrzne albo reguła projektu | pomiary z tego egzemplarza |
| Odpowiada na | *czy jest zdrowo* | *czy się zmieniło* |
| Gdy auto ma usterkę od dawna | wykryje ją | **zapamięta ją jako normalność** |
| Etykieta na ekranie | `norma` | `poprzednio`, `N pomiarów` |

**Nigdy nie wolno napisać `norma` przy wartości pochodzącej z historii.** Kolumna normy zostaje
kreską tam, gdzie kreską była — obok niej stoi osobna kolumna „poprzednio".

To nie jest formalność. Użytkownik, który zobaczy `norma 705–714` przy obrotach, uzna, że
tak ma być w tym modelu. Zobaczywszy `47 pomiarów 705–714`, wie, że to jego własne auto tak
robiło — i że gdyby od początku robiło źle, ta liczba by o tym nie powiedziała.

---

## 3. Zrównoważenie — co to rozszerzenie obciąża

Sprawdzone przed napisaniem reszty dokumentu.

### 3.1 Magistrala: nic

**Rozszerzenie nie dokłada ani jednego zapytania OBD.** Wszystkie parametry, które trafiają do
punktu odniesienia, **są już odpytywane** przez istniejące poziomy:

| Parametr | Skąd już płynie |
|---|---|
| `010C` obroty, `0104` obciążenie, `010E` zapłon, `0105` płyn, `010D` prędkość, `0106` korekta krótka | pętla gorąca, 4 Hz |
| `0123` ciśnienie szyny, `010B` kolektor, `0111` przepustnica, `0143` obciążenie abs. | poziom szybki A |
| `013C` katalizator, `0144` lambda, `0107` korekta długa, `0142` napięcie, `0103` status | poziom średni B |
| `011F` czas pracy, `0133` atmosferyczne, `0146` otoczenie, `010F` dolot | poziom wolny C |

Punkt odniesienia powstaje **z próbek, które i tak przechodzą przez pętlę**. Liczba zapytań na
sekundę, fazy poziomów i szczyt cyklu **pozostają dokładnie takie same**: 5,62 zapytania na
sekundę, zero cykli z trzema zapytaniami, 140 ms przy budżecie 250 ms.

**Jest to pierwsze rozszerzenie w tym projekcie, które nic nie kosztuje na magistrali** —
i jest to warunek, nie przypadek. Gdyby wymagało nowego PID-u, trzeba by rozważyć piąty poziom
i cały rachunek faz od nowa.

### 3.2 Nastawa: bez zmian

**`Zrównoważona` 4 Hz zostaje domyślna i nietknięta.** Rozszerzenie nie dotyka pętli gorącej,
jej składu ani tempa.

### 3.3 Baza: około megabajta rocznie

| Co | Rozmiar | Ile rocznie |
|---|---|---|
| Punkt odniesienia | ~300 B | kilkaset punktów, ~1 MB |
| Zapisany przegląd | ~8 kB | kilkanaście, ~100 kB |

Przy 128 GB pamięci radia to wielkości, których nie ma sensu ograniczać. **Nie wprowadzamy
limitu liczby punktów ani ich starzenia** — bo każdy taki limit byłby liczbą wymyśloną, a nie
ma czego oszczędzać.

### 3.4 Ekran: dwa panele i jedna kolumna

Zmiany dotykają **panelu Podstawowego, panelu Wtrysk GDI i ekranu Odczyty**. Pozostałe trzy
panele i cała zakładka Historia zostają bez zmian.

To jest świadome ograniczenie zakresu: porównanie na wszystkich pięciu panelach naraz byłoby
większą zmianą interfejsu niż cała reszta rozszerzenia. Rozszerzenie na kolejne panele jest
tanie i można je zrobić później, gdy będzie wiadomo, czy mechanizm się sprawdza.

---

## 4. Punkt odniesienia — definicja stanu

Porównywać można wyłącznie to samo z tym samym. Silnik na jałowym przy 52 °C i przy 92 °C to
pod względem odczytów dwa różne silniki.

Stąd jeden ściśle zdefiniowany stan:

```
JAŁOWY ROZGRZANY  ≡  obroty 010C  > 500          (silnik pracuje)
                  ∧  prędkość 010D = 0           (postój)
                  ∧  płyn 0105 ≥ 70 °C           (rozgrzany)
                  ∧  czas pracy 011F ≥ 600 s     (rozgrzany na dobre)
```

**Ani jednej nowej stałej.** Wszystkie cztery progi już istnieją w projekcie:

| Próg | Skąd |
|---|---|
| obroty > 500 | „silnik pracuje", tabela 10.4 bazowego |
| prędkość = 0 | blokada prędkościowa, sekcja 12.4 bazowego |
| płyn ≥ 70 °C ∧ czas pracy ≥ 600 s | definicja „silnik rozgrzany", sekcja 10.0 rozszerzenia diagnostycznego |

To ta sama definicja, której używają reguły `GDI-1` i `KAT-1`. **Jedno pojęcie, trzy
zastosowania** — nie trzy podobne definicje, które kiedyś się rozjadą.

### 4.1 Ten stan zachodzi sam, kilka razy na przejazd

Nie trzeba niczego robić specjalnie. Stan spełnia się przy:

- **każdym świetle** po dziesięciu minutach jazdy,
- **zaparkowaniu** przed zgaszeniem silnika,
- postoju w korku,
- przeglądzie wykonanym na rozgrzanym aucie.

Po dwóch–trzech przejazdach punktów jest kilkanaście. **To jest cała odpowiedź na pytanie
„ile trzeba czekać": tyle, co dwa przejazdy.**

---

## 5. Nie-cele

**Nazywanie tego normą.** Sekcja 2. Kolumna `norma` zostaje kreską tam, gdzie nią była.

**Ocenianie odchylenia.** Aplikacja pokazuje `12,5 °` obok `poprzednio 12,0`. **Nie mówi, czy
pół stopnia to dużo** — bo nie wie. Żadnej strzałki wartościującej, żadnego koloru, żadnej
reguły werdyktu opartej na historii.

**Porównywanie różnych stanów.** Pomiar spoza stanu `JAŁOWY ROZGRZANY` nie tworzy punktu i nie
jest z niczym zestawiany.

**Uśrednianie sezonowe, korekty na temperaturę otoczenia, wygładzanie trendu.** Wszystko to
wymagałoby modelu, którego nie mam. Zakres z surowych pomiarów jest szeroki i **ma taki być** —
bo tyle naprawdę wynosi rozrzut.

**Limity liczby punktów i ich starzenie.** Sekcja 3.3.

**Rozszerzenie porównania na pozostałe trzy panele.** Sekcja 3.4 — tanie, ale później.

---

## 6. Decyzje projektowe

| Decyzja | Uzasadnienie |
|---|---|
| **Jedna definicja stanu, wspólna z regułami `GDI-1` i `KAT-1`** | Trzy podobne definicje rozjechałyby się przy pierwszej zmianie |
| **Mediana z okna, nie pojedyncza próbka** | Odczyty na jałowym drgają. Pojedyncza próbka zapisałaby drganie jako punkt odniesienia |
| **Minimalne okno: jeden pełny obieg poziomu wolnego** | Wymóg mechaniczny, nie próg: przed nim część parametrów **nie została jeszcze odpytana** w tym oknie |
| **Zakres pokazujemy od drugiego punktu, nie od trzeciego** | „Od trzeciego" wymagałoby liczby 3, której nie ma skąd wziąć. Zakres z dwóch punktów to po prostu te dwie wartości |
| **Punkty i przeglądy kluczowane po VIN** | Podłączenie adaptera do innego auta nie może zmieszać historii. Naprawa dziury znalezionej przy burzy mózgów |
| **Dwie tabele: punkty i przeglądy** | Różne ziarnistości: punkty powstają kilka razy na przejazd i są liczbowe, przeglądy są rzadkie i zawierają kody oraz monitory |
| **Panel Podstawowy przełącza się na porównanie** | Przy stojącym aucie wykresy są płaskimi kreskami. Ta sama przestrzeń użyta na coś, co niesie treść |
| **Wejście w tryb porównania powolne, wyjście natychmiastowe** | Sekcja 10.2. Bezpieczeństwo: gdy ruszasz, wykresy mają wrócić od razu |
| **Zero nowych zapytań** | Sekcja 3.1 — warunek, nie przypadek |

---

## 7. Zbieranie punktów

### 7.1 Przebieg

```
stan zaczyna zachodzić
  → otwórz okno, zbieraj próbki wszystkich parametrów
stan przestaje zachodzić  (ruszenie, spadek obrotów, koniec sesji)
  → jeśli okno objęło co najmniej jeden pełny obieg poziomu wolnego:
        policz MEDIANĘ każdego parametru z okna
        zapisz jeden punkt odniesienia
    w przeciwnym razie:
        odrzuć okno, punkt nie powstaje
```

**Minimalne okno to jeden pełny obieg poziomu wolnego** — czyli dwadzieścia cykli gorących,
5 sekund przy 4 Hz. Sprawdzone: **każde okno dwudziestu kolejnych cykli zawiera dokładnie jeden
strzał poziomu wolnego** (bo `n % 20 == 13` trafia raz na dwadzieścia liczb). Powód jest mechaniczny: poziom wolny odpytuje `011F`, `0133`, `0146`
i `010F` raz na dwadzieścia cykli, więc **przed upływem tego czasu tych parametrów po
prostu nie ma w oknie**. Punkt zapisany wcześniej byłby niekompletny.

To nie jest próg wybrany dla wygody — to jest długość, po której komplet danych fizycznie
istnieje.

### 7.2 Ile próbek ma który parametr

Poziomy odpytywania mają różne tempa, więc w tym samym oknie zbierają różne liczby próbek:

| Okno | Gorący | Szybki A | Średni B | Wolny C |
|---|---|---|---|---|
| **minimalne**, 20 cykli, 5 s | 20 | 5 | 2 | **1** |
| światła, 240 cykli, 60 s | 240 | 60 | 24 | 12 |
| postój 5 min, 1200 cykli | 1200 | 300 | 120 | 60 |

**Przy oknie minimalnym parametry z poziomu wolnego mają po jednej próbce**, więc ich „mediana"
jest tą jedną próbką. Trzeba to powiedzieć wprost, zamiast udawać, że każda wartość w punkcie
ma taką samą wiarygodność.

Nie jest to jednak problem, i to nie przez przypadek: **na poziomie wolnym są dokładnie te
parametry, które nie drgają** — temperatura otoczenia, ciśnienie atmosferyczne, temperatura
dolotu i czas pracy. Pojedynczy odczyt jest dla nich w zupełności wystarczający.

Parametry, które faktycznie drgają na jałowym — obroty, korekty, ciśnienie szyny — siedzą
w pętli gorącej i na poziomie A, gdzie nawet minimalne okno daje kilkanaście do dwudziestu
próbek.

**Pole `probek` w tabeli zapisuje liczbę próbek okna**, żeby dało się odróżnić punkt z pięciu
sekund od punktu z pięciu minut. Widoczne na ekranie diagnostycznym.

### 7.3 Mediana, nie średnia

Odczyty na jałowym drgają: obroty oscylują wokół zadanych, korekty pracują, ciśnienie szyny
pulsuje z pracą pompy. **Pojedyncza próbka zapisałaby drganie**, a średnia dałaby się przesunąć
jednym wyskokiem przy włączeniu klimatyzacji albo wentylatora.

Mediana z okna pokazuje **wartość typową dla tego postoju** i to jest to, co chcemy porównywać.

### 7.4 Ile punktów na przejazd

Tyle, ile było postojów spełniających warunki. Czterdziestominutowa jazda po mieście da ich
kilka, trasa autostradą — jeden, na końcu.

**Nie ograniczamy tego.** Więcej punktów to węższy i uczciwszy zakres.

### 7.5 Punkt zawiera to, co akurat było odpytywane

Trzy z sześciu parametrów pętli gorącej to **gniazda konfigurowalne**. Gdy użytkownik zmieni
gniazdo, dotychczasowy parametr **przestaje być odpytywany** — i wtedy nie ma go też w nowych
punktach odniesienia.

Zachowanie jest spójne i celowo nie próbujemy tego obchodzić:

```
parametr jest w pętli   →  trafia do punktu, ma wykres i ma porównanie
parametr wypadł z pętli →  nie trafia do punktu, nie ma wykresu i nie ma porównania
```

Panel Podstawowy pokazuje w trybie porównania **te same trzy parametry, które rysuje na
wykresach**. Jeśli któryś zniknie z pętli, zniknie z obu miejsc naraz — a nie zostanie
w porównaniu jako wartość, która się nie odświeża.

Starych punktów **nie usuwamy**: zawierają dane, które kiedyś zebrano, i pozostają ważne dla
parametrów, które nadal są odpytywane.

---

## 8. Zapis

### 8.1 Punkty odniesienia

```sql
CREATE TABLE punkt_odniesienia (
    id       TEXT PRIMARY KEY,
    kiedy    INTEGER NOT NULL,      -- epoch ms
    vin      TEXT NOT NULL,
    stan     TEXT NOT NULL,         -- jalowy_rozgrzany
    zrodlo   TEXT NOT NULL,         -- przejazd | przeglad
    probek   INTEGER NOT NULL,      -- ile próbek weszło do mediany
    odczyty  BLOB NOT NULL          -- JSON: numer PID -> mediana
);
CREATE INDEX idx_punkt_vin_kiedy ON punkt_odniesienia (vin, kiedy);
```

Pole `stan` istnieje mimo jednej możliwej wartości — **żeby dołożenie drugiego stanu
(np. `postoj_zimny`) nie wymagało migracji schematu**.

Pole `probek` jest widoczne na ekranie diagnostycznym i pozwala odróżnić punkt z pięciu
sekund postoju od punktu z pięciu minut.

**Czas pracy `011F` zapisujemy, ale nigdy nie porównujemy** — jest za każdym razem inny
z definicji, bo mierzy, jak długo silnik chodzi od uruchomienia. W kolumnie „poprzednio"
ma kreskę. Zapisujemy go, bo jest częścią warunku stanu i bywa potrzebny przy diagnozie
samego mechanizmu.

### 8.2 Zapisane przeglądy

```sql
CREATE TABLE przeglad (
    id      TEXT PRIMARY KEY,
    kiedy   INTEGER NOT NULL,
    vin     TEXT,
    stan    TEXT,                   -- jalowy_rozgrzany albo null
    raport  BLOB NOT NULL           -- JSON, pełny Report
);
CREATE INDEX idx_przeglad_vin_kiedy ON przeglad (vin, kiedy);
```

Przegląd zapisuje się **zawsze**, także gdy warunki nie były spełnione — bo kody błędów
i monitory gotowości porównuje się niezależnie od temperatury silnika. Pole `stan` mówi,
czy **odczyty liczbowe** z tego przeglądu nadają się do porównania.

### 8.3 Ochrona przed pomyleniem samochodów

VIN czytany jest przy każdym przeglądzie i na starcie każdej sesji nagrywania.

```
VIN zgodny z ostatnio znanym    → normalna praca
VIN inny                        → pytanie do użytkownika:
                                  „To nie jest auto, którego historię masz zapisaną.
                                   Prowadzić osobną historię dla tego VIN-u?"
VIN nieodczytany                → punkty NIE powstają; przegląd zapisuje się bez VIN-u
                                  i nie bierze udziału w porównaniach
```

**Ostatni wiersz jest istotny.** Punkt bez VIN-u trafiłby do wspólnego worka i zatruł zakresy
obu aut. Lepiej go nie tworzyć.

---

## 9. Przegląd — warunki i porównanie

### 9.1 Nagłówek: czy to porównywalne

```
┌───────────────────────────────────────────────────────────┐
│  Hyundai i40 · 2015 · Ulsan            KMHLC41DAFU066558  │
│  GGVF-EE5AFS01600 · ECM-EngineControl                     │
├───────────────────────────────────────────────────────────┤
│                    ✓   WSZYSTKO OK                        │
│               sprawdzone 14 sierpnia, 17:38               │
├───────────────────────────────────────────────────────────┤
│  Warunki    ● jałowy rozgrzany                            │
│             płyn 92 °C · postój · 712 obr/min · 14 min    │
│  Porównanie z 12 lipca — ten sam stan                 ✓   │
└───────────────────────────────────────────────────────────┘
```

Gdy warunki nie były spełnione:

```
├───────────────────────────────────────────────────────────┤
│  Warunki    ○ silnik nierozgrzany, płyn 52 °C             │
│  Porównanie liczbowe niedostępne — poprzedni przegląd     │
│  był na rozgrzanym silniku, wartości nieporównywalne      │
│  Kody błędów i monitory porównane mimo to             ✓   │
└───────────────────────────────────────────────────────────┘
```

**Ostatni wiersz jest ważny.** Pojawienie się kodu błędu nie zależy od temperatury silnika,
więc ta część porównania działa zawsze. Aplikacja mówi wprost, **co porównała, a czego nie** —
zamiast po cichu pominąć jedno albo pokazać drugie jako pełne.

### 9.2 Ekran Odczyty — kolumna „poprzednio"

```
┌───────────────────────────────────────────────────────────┐
│  ◀  ODCZYTY            teraz   poprzednio    norma        │
│                               12 lipca                    │
├───────────────────────────────────────────────────────────┤
│  SILNIK                                                   │
│  Obroty            712 obr/min      708         —         │
│  Obciążenie             18,0 %     17,6         —         │
│  Wyprzedzenie            12,5 °    12,0         —         │
│  Czas pracy              14 min       —         —         │
├───────────────────────────────────────────────────────────┤
│  TEMPERATURY                                              │
│  Płyn chłodzący           92 °C      91     70 – 105      │
│  Olej (model)             91 °C ~    90       ≥ 90        │
│  Katalizator             612 °C     598    650 – 870      │
│  Powietrze dolotowe       23 °C      19         —         │
│  Otoczenie                21 °C      18         —         │
├───────────────────────────────────────────────────────────┤
│  MIESZANKA                                                │
│  Status układu     pętla zamknięta   ta sama               │
│  Korekta długa          +3,9 %     +3,8       ±10         │
│  Korekta krótka         −0,8 %     −0,6        —          │
│  Lambda zadana           1,000     1,000     1,000        │
│  Przedmuchiwanie             0 %       0        —         │
├───────────────────────────────────────────────────────────┤
│  POWIETRZE I WTRYSK                                       │
│  Ciśnienie szyny       38,4 bar     38,1    34 – 55       │
│  Ciśn. w kolektorze      34 kPa       35        —         │
│  Ciśn. atmosferyczne     99 kPa      101   50 – 110       │
│  Podciśnienie            65 kPa       66        —         │
│  Przepustnica rzeczyw.   15,3 %     15,1        —         │
│  Przepustnica zadana     15,3 %     15,1        —         │
│  Pedał                   14,5 %     14,3        —         │
├───────────────────────────────────────────────────────────┤
│  INSTALACJA                                               │
│  Napięcie              14,2 V       14,2   13,0 – 15,0    │
└───────────────────────────────────────────────────────────┘
```

Cała wartość tej funkcji jest w wierszach, gdzie **norma to `—`, a „poprzednio" ma liczbę**.
Nie wiem, ile powinno być wyprzedzenia zapłonu — ale wiem, że Twoje auto dawało 12,0 ° miesiąc
temu, a dziś daje 12,5 °.

Wiersze, których nie da się porównać, mają w kolumnie `—`: czas pracy jest za każdym razem inny,
a status pętli nie jest liczbą, więc porównuje się go słowem („ta sama"), nie różnicą.

**Poziomu paliwa na tej karcie nie ma i nie będzie** — poprawka P1 (§3.2 bazowego). Czujnik
zwraca zero niezależnie od stanu baku, więc wiersz `0 % ⌀ · 0 ⌀ · —` byłby trzema kolumnami
niczego. **Parametr bez danych usuwamy z widoku, a nie pokazujemy jako pusty** — inaczej karta
uczy, że kreski są normalne, i przestaje się je zauważać tam, gdzie coś znaczą.

### 9.3 Zmiany od poprzedniego przeglądu

Osobny blok, bo dotyczy rzeczy, które **nie są liczbami**:

```
┌───────────────────────────────────────────────────────────┐
│  ZMIANY OD 12 LIPCA                                       │
├───────────────────────────────────────────────────────────┤
│  ⊕  P0171   Mieszanka zbyt uboga, bank 1                  │
│       kod pojawił się od poprzedniego przeglądu           │
│  ⊖  P0442   Mała nieszczelność układu odparowania         │
│       kodu już nie ma                                     │
│  ⊖  Monitor układu odparowania — był gotowy, teraz nie    │
│  ⊕  Kontrolka MIL — była zgaszona, teraz świeci           │
└───────────────────────────────────────────────────────────┘
```

Gdy nic się nie zmieniło:

```
│  ZMIANY OD 12 LIPCA                                       │
│  Bez zmian: te same kody, te same monitory, ta sama       │
│  kontrolka.                                               │
```

**Ten blok nie wymaga żadnej liczby.** Pojawienie się kodu albo utrata gotowości monitora to
fakt, nie ocena — i jest to jedyna część całego rozszerzenia, która działa **niezależnie od
stanu silnika**.

---

## 10. Panel Podstawowy

### 10.1 Dwa tryby

```
W RUCHU — bez zmian względem dzisiejszego stanu
┌──────────┬──────────┬──────────┬──────────┐
│ 88 °C ~  │  92 °C   │ 13,9 V   │  +3,9 %  │
│ OLEJ mod.│   PŁYN   │ NAPIĘCIE │ KOREKTA D│
│   ≥ 90   │  70–105  │ 13,0–15,0│ −10 – +10│
├──────────┴──────────┴──────────┴──────────┤
│ ● ○ ○ ○ ○  PODSTAWOWY     04:12   4,0 Hz  │
├───────────────────────────────────────────┤
│ ○ w ruchu — punkt odniesienia niedostępny │
├───────────────────────────────────────────┤
│ OBROTY          ╱‾╲___╱‾‾‾╲__      1726   │
├───────────────────────────────────────────┤
│ OBCIĄŻENIE    __╱‾╲______╱‾╲_       34 %  │
├───────────────────────────────────────────┤
│ ZAPŁON        ‾‾╲__╱‾‾╲___╱‾‾       18 °  │
└───────────────────────────────────────────┘


NA POSTOJU, ROZGRZANY — po pięciu sekundach w tym stanie
┌──────────┬──────────┬──────────┬──────────┐
│ 91 °C ~  │  92 °C   │ 14,2 V   │  +3,9 %  │
│ OLEJ mod.│   PŁYN   │ NAPIĘCIE │ KOREKTA D│
│   ≥ 90   │  70–105  │ 13,0–15,0│ −10 – +10│
├──────────┴──────────┴──────────┴──────────┤
│ ● ○ ○ ○ ○  PODSTAWOWY     14:02   4,0 Hz  │
├───────────────────────────────────────────┤
│ ● JAŁOWY ROZGRZANY — punkt odniesienia    │
├───────────────────────────────────────────┤
│ OBROTY              712                   │
│   poprzednio 708  ·  47 pomiarów 705–714  │
├───────────────────────────────────────────┤
│ OBCIĄŻENIE        18,0 %                  │
│   poprzednio 17,6  ·  47 pomiarów 17,2–18,1│
├───────────────────────────────────────────┤
│ ZAPŁON            12,5 °                  │
│   poprzednio 12,0  ·  47 pomiarów 11,8–12,6│
└───────────────────────────────────────────┘
```

Wiersz stanu stoi **nad treścią**, tak jak wiersz statusu pętli na panelu Mieszanka. Ten sam
język w całej aplikacji: **stan, który rozstrzyga o sensie tego, co poniżej, stoi wyżej niż to.**

### 10.2 Wejście powolne, wyjście natychmiastowe

| Przejście | Kiedy | Dlaczego tak |
|---|---|---|
| wykresy → porównanie | po **pełnym obiegu poziomu wolnego** w stanie (5 s przy 4 Hz) | Wcześniej część parametrów nie została odpytana, a migotanie przy każdym zatrzymaniu byłoby męczące |
| porównanie → wykresy | **natychmiast**, gdy stan przestaje zachodzić | Ruszasz — wykresy mają wrócić od razu, bez opóźnienia |

Asymetria jest celowa. **Wolno wchodzić w tryb, w którym nie ma wykresów; nie wolno w nim
zostać, gdy auto rusza.**

### 10.3 Gdy punktów jeszcze nie ma

```
│ ● JAŁOWY ROZGRZANY — punkt odniesienia    │
├───────────────────────────────────────────┤
│ OBROTY              712                   │
│   pierwszy pomiar — brak porównania       │
```

Po pierwszym punkcie:

```
│   poprzednio 708                          │
```

Po drugim i dalszych — dochodzi zakres. **Nie czekamy na trzeci pomiar**, bo „trzy" byłoby
liczbą wziętą znikąd; zakres z dwóch punktów to po prostu te dwie wartości.

---

## 11. Panel Wtrysk GDI — jeden wiersz

```
├───────────────────────────────────────────┤
│ Max w sesji:  148 bar  przy 84 % obciąż.  │
│   norma pod obciążeniem  138 – 241 bar    │
├───────────────────────────────────────────┤
│ Na jałowym rozgrzanym:        38,4 bar    │
│   poprzednio 38,1  ·  47 pomiarów 37,9–38,6│
└───────────────────────────────────────────┘
```

Ciśnienie w szynie na rozgrzanym jałowym jest **najlepszym powtarzalnym wskaźnikiem stanu
pompy, jaki mamy bez ciśnienia zadanego**. Osuwanie się tej wartości przez kolejne miesiące
byłoby pierwszym sygnałem — wcześniejszym niż jakikolwiek kod błędu.

Panel nie przełącza trybów; to jest **dodatkowy wiersz**, widoczny tylko wtedy, gdy stan
zachodzi. Poza nim wiersz znika, a nie pokazuje starych danych.

---

## 12. Zmiany w kontraktach

| Miejsce | Zmiana |
|---|---|
| Baza | **Dwie nowe tabele**: `punkt_odniesienia`, `przeglad` |
| §8.6 bazowego | Bez zmian — punkty i przeglądy są osobne od przejazdów |
| §8.8 rozszerzenia diagnostycznego | **Nowa kolumna „poprzednio"** obok istniejącej kolumny normy. Kontrakt pasm bez zmian: `norma` nadal pochodzi wyłącznie z reguły albo źródła |
| §8.8 rozszerzenia diagnostycznego | Nowy znacznik: **`poprzednio`** i **`N pomiarów A–B`**, wizualnie odróżnione od pasma |
| §12.2 ekran żywy | Panel Podstawowy dostaje dwa tryby; panel Wtrysk GDI dodatkowy wiersz |
| §8.8 przeglądu w bazowym | Nagłówek z warunkami; blok „zmiany od poprzedniego przeglądu" |
| Sekcja 10.0 rozszerzenia diagnostycznego | Definicja „silnik rozgrzany" **zyskuje trzeciego użytkownika** — bez zmiany treści |

**Czego nie zmieniamy:** pętli gorącej i pozostałych poziomów, faz, liczby zapytań, nastawy
`Zrównoważona`, progów alarmów, reguł werdyktu, kontraktu pasm, checkpointu, decymacji.

**Rozszerzenie nie wprowadza ani jednej nowej stałej liczbowej.**

---

## 13. Testy

| Obszar | Test | Dlaczego akurat ten |
|---|---|---|
| **Zero nowych zapytań** | Liczba zapytań na cykl i na sekundę **identyczna przed i po** wdrożeniu rozszerzenia | Sekcja 3.1 jest warunkiem, nie deklaracją. Bez tego testu ktoś dopisze odpytanie „bo brakowało parametru" |
| **Definicja stanu** | Cztery warunki; **każdy pojedynczo złamany wyłącza stan** — cztery osobne przypadki | Warunek złożony z czterech członów to cztery okazje do pomyłki |
| **Wspólna definicja** | Stan używa **tej samej stałej** co reguły `GDI-1` i `KAT-1`, nie własnej kopii | Trzy kopie rozjadą się przy pierwszej zmianie |
| **Minimalne okno** | Okno krótsze niż pełny obieg poziomu wolnego **nie tworzy punktu** | Punkt niekompletny jest gorszy niż jego brak |
| **Mediana, nie średnia** | Okno z jednym skrajnym wyskokiem — wynik ma się różnić od średniej | Test napisany tak, żeby implementacja na średniej go oblała |
| **Rozdział po VIN** | Punkty z dwóch różnych VIN-ów **nie mieszają się** w zakresach | |
| **Brak VIN-u** | Gdy VIN nieodczytany, **punkt nie powstaje** | Punkt bez VIN-u zatruwa zakresy obu aut |
| **Porównywalność przeglądów** | Przegląd spoza stanu **nie pokazuje porównania liczbowego**, ale **pokazuje porównanie kodów i monitorów** | Dwie różne odpowiedzi w jednym ekranie — najłatwiej zrobić z tego jedną |
| **Zmiany kodów** | Kod pojawiający się, kod znikający, monitor tracący gotowość, MIL zmieniający stan — cztery przypadki | |
| **Progresja wyświetlania** | 0 punktów → „pierwszy pomiar"; 1 → „poprzednio X"; 2+ → zakres | Zakres od **drugiego**, nie od trzeciego |
| **Etykieta** | Wartość z historii renderuje się jako `poprzednio` albo `N pomiarów`, **nigdy jako `norma`** | Sekcja 2. Test na tekście, nie na kolorze |
| **Przełączanie panelu** | Wejście dopiero po pełnym obiegu poziomu wolnego; **wyjście natychmiast** po ruszeniu | Asymetria jest celowa i łatwo ją zgubić, implementując oba przejścia tym samym opóźnieniem |
| **Wiersz na panelu GDI** | Widoczny tylko w stanie; poza nim **znika**, a nie pokazuje starej wartości | |

---

## 14. Kolejność realizacji

Wchodzi **po etapie K6** rozszerzenia kontekstowego.

| Etap | Zawartość | Ukończony, gdy |
|---|---|---|
| **O1** | Definicja stanu jako czysta funkcja + testy | Cztery warunki, każdy łamany osobno; stała wspólna z `GDI-1` |
| **O2** | Okno, mediana, tabela `punkt_odniesienia`, rozdział po VIN | Okno za krótkie nie tworzy punktu; mediana oblewa test średniej |
| **O3** | Tabela `przeglad`, zapis raportu, porównanie kodów i monitorów | Cztery rodzaje zmian wykrywane; działa też spoza stanu |
| **O4** | Ekran Odczyty: kolumna „poprzednio" | Etykieta nigdy nie brzmi `norma` |
| **O5** | Nagłówek przeglądu: warunki i porównywalność | Spoza stanu: liczby nie, kody tak |
| **O6** | Panel Podstawowy: dwa tryby | Wejście powolne, wyjście natychmiastowe |
| **O7** | Panel Wtrysk GDI: wiersz odniesienia | Poza stanem wiersz znika |
| **O8** | Weryfikacja w aucie | Sekcja 15 |

**O1 i O2 przed wszystkim** — bez punktów nie ma czego wyświetlać. **O3 jest niezależne** od
reszty i można je zrobić równolegle, bo porównanie kodów nie potrzebuje punktów odniesienia.

---

## 15. Weryfikacja w aucie

| # | Co zrobić | Czego szukamy |
|---|---|---|
| 1 | Przejazd 15 min zakończony postojem z pracującym silnikiem | **Czy powstał punkt odniesienia.** Sprawdź na ekranie diagnostycznym: liczba punktów i liczba próbek |
| 2 | Postój na światłach po 10 min jazdy | Czy panel Podstawowy przełącza się po ~5 s i wraca natychmiast po ruszeniu |
| 3 | Drugi przejazd tego samego dnia | Czy pojawiło się `poprzednio`, a po nim zakres |
| 4 | Przegląd na rozgrzanym silniku | Nagłówek pokazuje `● jałowy rozgrzany`, porównanie dostępne |
| 5 | Przegląd na zimnym silniku | Nagłówek pokazuje `○`, **liczby nieporównane, kody porównane** |
| 6 | Po tygodniu | Czy zakres jest wąski, czy szeroki — i czy ma to sens |

**Punkt 6 jest sprawdzianem sensu rozszerzenia.** Jeśli po tygodniu zakres obrotów na jałowym
wynosi 690–760, to jest zbyt szeroki, żeby cokolwiek znaczyć — i wtedy trzeba albo zawęzić
definicję stanu, albo uznać, że dla tego parametru nie da się zrobić sensownego odniesienia.
**Wynik negatywny też się zapisuje.**

---

## 16. Źródła

**Rozszerzenie nie korzysta z żadnego źródła zewnętrznego i nie wprowadza żadnej nowej stałej.**

Cztery progi definiujące stan pochodzą z dokumentów wcześniejszych: `> 500` i `= 0`
z dokumentu bazowego, `≥ 70 °C` i `≥ 600 s` z sekcji 10.0 rozszerzenia diagnostycznego.

Wartości pokazywane w kolumnie „poprzednio" **pochodzą wyłącznie z pomiarów tego egzemplarza**
i są tak opisane na ekranie. Nie są normą i nie wolno ich tak nazwać — sekcja 2.
