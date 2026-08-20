# i40-android — rozszerzenie kontekstowe

**Data:** 2026-08-14
**Status:** zaakceptowany, gotowy do realizacji
**Rozszerza:** `2026-08-14-i40-android-design.md` (bazowy) oraz
`2026-08-14-i40-android-diagnostyka-design.md` (diagnostyczne). Oba obowiązują bez zmian poza
tym, co ta specyfikacja jawnie zmienia (sekcja 11)

---

## 1. Cel

Trzy rzeczy, jeden wspólny mianownik: **nie dokładają nowych liczb, tylko tłumaczą te, które
już pokazujemy.**

| Co | Problem, który rozwiązuje |
|---|---|
| **Status pętli i przedmuchiwanie** na panelu Mieszanka | Korekty paliwa skaczą z **normalnych powodów**, a aplikacja pokazuje to jak usterkę |
| **Panel Powietrze** | Widać skutek (uboga mieszanka), nie widać przyczyny (nieszczelność dolotu, zacinająca się przepustnica) |
| **Karta miesiąca** | Jeden przejazd nie pokazuje trendu. Termostat psuje się przez miesiące, nie w jeden poranek |

Pięć nowych PID-ów, wszystkie **już obsługiwane przez to auto i już obecne w katalogu**.
Cztery z nich mają gotowe wzory; jeden — PID `0103` — wymaga dekodera, którego formuła
pochodzi ze standardu (sekcja 4).

---

## 2. Zasada: liczba bez kontekstu bywa gorsza niż jej brak

Rozszerzenie diagnostyczne dało każdej wartości **pasmo** — czyli odpowiedź na pytanie
„ile powinno być". To rozszerzenie daje **warunki** — odpowiedź na pytanie „czy w tej chwili
ta wartość w ogóle coś znaczy".

> **Korekta paliwa +14 % przy otwartej pętli nie znaczy nic. Ta sama liczba przy zamkniętej
> pętli i wyłączonym przedmuchiwaniu znaczy: coś jest nie tak.**

Aplikacja, która pokazuje samą liczbę, w pierwszym przypadku **kłamie przez przemilczenie**.
Użytkownik widzi znacznik ▲, wnioskuje, że silnik ma problem, i albo się niepotrzebnie martwi,
albo — gorzej — uczy się ignorować znaczniki.

To jest dokładnie ten sam błąd co zero zamiast kreski, tylko subtelniejszy: **wartość jest
prawdziwa, a wniosek fałszywy.**

---

## 3. Pięć nowych PID-ów

Wszystkie zweryfikowane w masce tego egzemplarza jako obsługiwane
(`4100BE3EA813`, `4120A007F011`, `4140FED00400`).

| PID | Nazwa | Wzór | W katalogu? | Po co |
|---|---|---|---|---|
| **`0103`** | Status układu paliwowego | **enumeracja, sekcja 4** | jest, ale jako **surowe bajty** — bez znaczenia | Rozstrzyga, czy korekty w ogóle działają |
| **`012E`** | Zadane przedmuchiwanie zbiornika węglowego | `A × 100 / 255`, % | **tak, gotowy** | Tłumaczy skoki korekt |
| **`014C`** | Zadana pozycja przepustnicy | `A × 100 / 255`, % | **tak, gotowy** | Porównanie z rzeczywistą — stan korpusu przepustnicy |
| **`0149`** | Pozycja pedału przyspieszenia D | `A × 100 / 255`, % | **tak, gotowy** | Odniesienie: czego chciał kierowca |
| **`0133`** | Ciśnienie atmosferyczne | `A`, kPa | **tak, gotowy** | Z `010B` daje podciśnienie |

**Cztery z pięciu nie wymagają ani jednej nowej linii dekodera.** Katalog PID-ów już je zna —
były po prostu nieużywane w jeździe.

### 3.1 Ostrzeżenie o nazewnictwie — czytaj uważnie

W tym projekcie **`03` znaczy dwie zupełnie różne rzeczy**:

| Zapis | Co to jest |
|---|---|
| **tryb `03`** | Polecenie „podaj zapisane kody błędów". Wysyłane co `n % 200 == 150` |
| **PID `0103`** | Pytanie trybu 01 o status układu paliwowego. Nowość tego rozszerzenia |

**W całym projekcie PID-y trybu 01 zapisujemy z prefiksem trybu: `0103`, `012E`, `010D`.**
Tryby zapisujemy słownie: „tryb 03", „tryb 09".

Bez tej dyscypliny ktoś prędzej czy później wyśle `03` zamiast `0103` i dostanie listę kodów
błędów tam, gdzie spodziewał się statusu pętli — a odpowiedź da się sparsować, więc **błąd
przejdzie bez objawu**.

---

## 4. Co mówi źródło — enumeracja PID `0103`

Dwa bajty. **Bajt A** to układ paliwowy nr 1, **bajt B** to układ nr 2 (ten silnik ma jeden
bank, więc B jest nieistotny). Oba kodowane identycznie:

| Wartość | Znaczenie | Co to znaczy dla korekt |
|---|---|---|
| `0` | Silnik wyłączony | — |
| `1` | **Pętla otwarta** — niewystarczająca temperatura silnika | Korekty **nie działają**, sterownik jedzie z mapy |
| **`2`** | **Pętla zamknięta** — sprzężenie zwrotne sondy tlenu | **Korekty działają i są wiarygodne** |
| `4` | **Pętla otwarta** — obciążenie silnika albo odcięcie paliwa przy zwalnianiu | Korekty **nie działają** |
| **`8`** | **Pętla otwarta — awaria układu** | **Sterownik zgłasza usterkę** |
| **`16`** | **Pętla zamknięta, ale awaria sprzężenia** | **Sterownik zgłasza usterkę sondy** |

Źródło: SAE J1979, wykaz PID-ów trybu 01. Odsyłacz w `docs/zrodla.md`.

**Wartości `8` i `16` są jakościowo inne od pozostałych.** To nie jest próg, który ktoś ustalił —
**sterownik sam nazywa ten stan awarią**. Stąd reguła `MIX-1` w sekcji 11.2, w pełni oparta
na źródle, bez ani jednej wymyślonej liczby.

---

## 5. Nie-cele

**Reguła na podciśnienie.** Podciśnienie na jałowym jest klasycznym testem szczelności dolotu,
ale **nie mam na jego wartość źródła** — ani ogólnego, ani dla G4NC. Panel je pokazuje,
pasmo ma `—`, reguły nie ma. To ta sama dyscyplina co przy ciśnieniu szyny w rozszerzeniu
diagnostycznym.

**Reguła na rozjazd przepustnicy zadanej i rzeczywistej.** „O ile mogą się różnić" to liczba,
której nie znam. Panel pokazuje obie krzywe jedna na drugiej — **rozjazd widać gołym okiem
i nie potrzeba do tego progu**. Gdy pojawią się dane fabryczne, regułę można dopisać.

**Werdykt na karcie miesiąca.** Karta pokazuje ten miesiąc i poprzedni obok siebie plus różnicę
jako liczbę. **Nie mówi, czy różnica jest zła** — bo „czy 28 sekund dłużej to problem" wymaga
progu, którego nie mam. Sekcja 10.3.

**Wykrywanie zimnego startu jako osobnej kategorii.** Kuszące, ale „zimny" wymagałoby progu
temperatury. Zamiast tego karta miesiąca liczy przejazdy, **w których płyn w ogóle nie
osiągnął 90 °C** — a to nie wymaga żadnego progu, bo pole `czasDo90CSekundy` jest wtedy `null`.
Sekcja 10.2.

**Cokolwiek wysyłającego dane na zewnątrz.** Bez zmian.

---

## 6. Decyzje projektowe

| Decyzja | Uzasadnienie |
|---|---|
| **Przebudowa poziomów odpytywania na trzy zamiast dwóch** | Pięć nowych PID-ów nie mieści się w dwóch pełnych szóstkach. Sekcja 7 |
| **Podział poziomów według tego, jak szybko wartość się zmienia** — nie według panelu | Nagrywanie **nie może zależeć od tego, który panel jest na ekranie**. Inaczej sesje przestają być porównywalne, a to jest fundament karty miesiąca i przyszłej linii bazowej |
| **Trzeci poziom przy `n % 20 == 13`** | Jedyna wolna faza rozłączna z pozostałymi trzema. Dowód w sekcji 7.2 |
| **PID-y trybu 01 zawsze z prefiksem: `0103`, nie `03`** | Sekcja 3.1. Kolizja nazw z trybem 03 daje błąd bez objawu |
| **Reguła `MIX-1` tylko dla wartości `8` i `16`** | To jedyne dwie, przy których **sterownik sam mówi „awaria"**. Reszta to normalne stany pracy |
| **Licznik czasu poza pasmem liczy tylko czas w pętli zamkniętej** | Korekta przy otwartej pętli nie znaczy nic, więc jej liczenie zawyża wynik. Sekcja 8.4 |
| **Karta miesiąca w nagłówku zakładki Historia** | Zakładki są trzy i mają zostać trzy. Kalendarz już ma przewijanie miesięcy — karta zmienia się razem z nim, bez nowej nawigacji |
| **Karta miesiąca pokazuje różnicę jako liczbę, bez oceny** | Sekcja 10.3 |
| **Piąty panel zamiast rozbudowy istniejących** | Panel Mieszanka jest już gęsty. Powietrze to osobna historia diagnostyczna |

---

## 7. Przebudowa poziomów odpytywania

### 7.1 Nowy układ

Dotychczas dwa poziomy poza gorącym nie mieściły wszystkiego. Nowy podział — **według tego,
jak szybko wartość się zmienia**, a nie według tego, który panel jej potrzebuje:

| Poziom | Faza | Skład | Częstotliwość przy 4 Hz |
|---|---|---|---|
| **Gorący** | każdy cykl | `010D 0105 0104` + **trzy** gniazda wykresów | 4 Hz |
| **Szybki A** | `n % 4 == 0` | `0123 010B 0111 014C 0149 0143` | ~1 Hz |
| **Średni B** | `n % 10 == 5` | `013C 0144 012E 0103 0107 0142` | ~0,4 Hz |
| **Wolny C** | **`n % 20 == 13`** | `011F 0146 010F 0133` (**dwa** miejsca wolne — `012F` wypadł poprawką P1) | ~0,2 Hz |
| Kody błędów | `n % 200 == 150` | **tryb 03** — jedno polecenie | ~0,02 Hz |

**Szybki A** to wszystko, co reaguje na pedał gazu: ciśnienie szyny, ciśnienie w kolektorze,
obie pozycje przepustnicy, pedał i obciążenie. Te wartości mają sens **jako kształt**, więc
potrzebują tempa.

**Średni B** to chemia i stan: katalizator, lambda, przedmuchiwanie, status pętli, korekta
długoterminowa, napięcie. Zmieniają się w sekundach.

**Wolny C** to otoczenie: czas pracy, temperatura otoczenia i dolotu, ciśnienie atmosferyczne.
Zmieniają się w minutach albo wcale. Cztery PID-y przy limicie sześciu — **dwa miejsca zostają
wolne i mają takie zostać**, dopóki nie zgłosi się parametr z odbiorcą.

### 7.2 Dowód rozłączności

Trzeci poziom musi nie zderzać się z trzema już zajętymi fazami. Przeszukane wszystkie reszty
dla okresów 20 i 25; **`n % 20 == 13` jest wolna**:

```
n % 20 == 13  ⟹  n % 4  jest stale równe 13 % 4  = 1   → nigdy 0   (rozłączne z A)
n % 20 == 13  ⟹  n % 10 jest stale równe 13 % 10 = 3   → nigdy 5   (rozłączne z B)
n % 200 == 150 ⟹ n % 20 jest stale równe 150 % 20 = 10 → nigdy 13  (rozłączne z trybem 03)
```

Sprawdzone na 200 000 cykli, komplet czterech poziomów plus gorący:

| Zapytań w cyklu | Ile cykli na 200 000 |
|---|---|
| 1 | 119 000 |
| 2 | 81 000 |
| **3 lub więcej** | **0** |

Zapytań na sekundę: **5,62** przy suficie 25. Szczyt cyklu: **140 ms** przy budżecie 250 ms.

### 7.3 Co zwolniło i dlaczego to dopuszczalne

Przebudowa przenosi cztery PID-y na wolniejsze poziomy. **Każde spowolnienie jest świadome
i zapisane:**

| PID | Było | Jest | Skutek |
|---|---|---|---|
| `013C` katalizator | 1 Hz | 0,4 Hz | Alarm `KAT-2` wykryje przegrzanie do 2,5 s później. Przegrzew narasta przez dziesiątki sekund — **dopuszczalne** |
| `0144` lambda | 1 Hz | 0,4 Hz | Wartość praktycznie stała w pętli zamkniętej — **bez znaczenia** |
| `011F` czas pracy | 0,4 Hz | 0,2 Hz | Nowy przejazd wykryty do 5 s później; model oleju resetuje się z takim opóźnieniem — **dopuszczalne**, bo drugim wyzwalaczem podziału jest 30 s postoju |
| ~~`012F` poziom paliwa~~ | 0,4 Hz | **nie odpytywany** | Poprawka P1 — zwraca zero niezależnie od stanu baku. Nie spowolniony, tylko usunięty |

**Czego nie zwolniono:** wszystkiego, od czego zależy alarm pilny albo bezpieczeństwo.
Temperatura płynu `0105` i prędkość `010D` zostają w pętli gorącej na 4 Hz.

### 7.4 Ograniczenie, które trzeba znać

**Przedmuchiwanie `012E` jest próbkowane co 2,5 s.** Cykl przedmuchiwania trwa zwykle
kilkanaście sekund i zostanie złapany, ale **krótkie impulsy poniżej ~3 s mogą umknąć**.

Konsekwencja: skok korekty wywołany bardzo krótkim przedmuchiwaniem **nie zostanie
wytłumaczony** i pokaże się jako niewyjaśniony. To jest znane ograniczenie, nie usterka —
i panel ma o tym mówić, gdy pokazuje niewyjaśniony skok.

Podniesienie `012E` do poziomu A oznaczałoby zdegradowanie czegoś innego. Do rozważenia
dopiero wtedy, gdy okaże się to realnym problemem w danych z auta.

---

## 8. Panel Mieszanka — przebudowany

### 8.1 Układ

```
┌──────────┬──────────┬──────────┬──────────┐
│ 88 °C ~  │  92 °C   │ 13,9 V   │  +3,9 %  │
│ OLEJ mod.│   PŁYN   │ NAPIĘCIE │ KOREKTA D│
│   ≥ 90   │  70–105  │ 13,0–15,0│ −10 – +10│
├──────────┴──────────┴──────────┴──────────┤
│ ○ ● ○ ○ ○  MIESZANKA      04:12   4,0 Hz  │
├───────────────────────────────────────────┤
│ ● PĘTLA ZAMKNIĘTA          korekty ważne  │
├───────────────────────────────────────────┤
│ KOREKTA RAZEM                    norma ±20│
│  +25 ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄  │
│  +20 ──────────────────────────────────   │
│    0 ────╱‾‾╲▓▓▓▓▓╱‾‾╲______╱‾‾    +3,1 % │
│  −20 ──────────────────────────────────   │
│  −25 ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄  │
│      ▓ przedmuchiwanie   ░ pętla otwarta  │
├─────────────────────┬─────────────────────┤
│ KRÓTKA      −0,8 %  │ PRZEDMUCH.      0 % │
│      norma      —   │      norma      —   │
│ DŁUGA       +3,9 %  │ LAMBDA ZAD.   1,000 │
│      norma    ±10   │      norma    1,000 │
│ ZA KAT.         —   │                     │
│      norma      —   │                     │
├─────────────────────┴─────────────────────┤
│ Poza pasmem ±20 %:  0:00 z 3:48           │
│               (czas w pętli zamkniętej)   │
└───────────────────────────────────────────┘
```

### 8.2 Wiersz statusu pętli — najważniejszy element panelu

Stoi **nad wykresem**, nie pod nim, bo rozstrzyga, czy w ogóle warto na wykres patrzeć.

| Wartość `0103` | Wyświetlane | Znaczek |
|---|---|---|
| `2` | **PĘTLA ZAMKNIĘTA** — korekty ważne | `●` |
| `1` | PĘTLA OTWARTA — silnik za zimny | `○` |
| `4` | PĘTLA OTWARTA — pełne obciążenie lub hamowanie silnikiem | `○` |
| `8` | **PĘTLA OTWARTA — awaria układu** | `✕` |
| `16` | **PĘTLA ZAMKNIĘTA — awaria sondy** | `⚠` |
| `0` | Silnik wyłączony | `—` |
| inne | Nieznany status (`0x..`) | `?` |

Ostatni wiersz jest obowiązkowy. **Wartość spoza enumeracji pokazujemy jako nieznaną razem
z surową liczbą**, nigdy nie zgadujemy najbliższej pasującej.

### 8.3 Cieniowanie wykresu — kontekst wprost na krzywej

Dwa rodzaje tła pod krzywą sumy korekt:

- **`▓` przedmuchiwanie aktywne** (`012E` > 0) — skok korekty w tym miejscu **ma wyjaśnienie**
- **`░` pętla otwarta** (`0103` ≠ `2` i ≠ `16`) — korekta w tym miejscu **nie znaczy nic**

To jest cała treść punktu 1 tego rozszerzenia. Zamiast liczby, przy której użytkownik zgaduje,
czy to problem — **liczba z zaznaczonym powodem**.

Cieniowanie rysowane pod krzywą, w kolorze tła o niskim kontraście, **z legendą pod wykresem** —
bo kolor nigdy nie jest jedynym sygnałem, a tu jest to szczególnie ważne, skoro oba rodzaje
tła znaczą co innego.

### 8.4 Licznik czasu poza pasmem — zmiana definicji

**Było:** suma odstępów, gdy `|0106 krótka + 0107 długa| > 20 %`, dzielone przez cały czas sesji.

**Jest:** suma odstępów, gdy warunek zachodzi **i pętla jest zamknięta**, dzielone przez
**czas spędzony w pętli zamkniętej**.

Powód: korekta przy otwartej pętli to zamrożona wartość sprzed przejścia w tryb otwarty.
Liczenie jej **zawyża albo zaniża licznik zależnie od tego, w jakim stanie sterownik akurat
zamarł** — czyli dodaje szum niezwiązany ze stanem silnika.

Mianownik też się zmienia, bo inaczej stosunek przestaje mieć sens: `0:30 z 40:00` przy
trzydziestu minutach otwartej pętli to zupełnie co innego niż `0:30 z 10:00`.

**Nazwa pola zmienia się na `czasPozaPasmemWPetliZamknietejSekundy`**, a doń dochodzi drugie:
`czasWPetliZamknietejSekundy`. Nazwy są długie celowo — mają nie dać się pomylić ze starymi.

### 8.5 Czwarty kafel — ten sam warunek ważności, tylko na górze ekranu

Poprawka P1 (§3.2 bazowego) oddała czwarty kafel **korekcie długiej `0107`**. Kafel widać
na każdym panelu, więc obowiązuje go dokładnie to samo zastrzeżenie co wykres pod nim —
tylko że tam status pętli stoi wypisany słowami, a na kaflu nie ma na to miejsca.

Rozstrzygnięcie: **liczbę pokazujemy wyłącznie po pozytywnym potwierdzeniu pętli zamkniętej.**

| Wartość `0103` | Kafel | Dlaczego |
|---|---|---|
| `2` pętla zamknięta | **liczba** | korekty działają i są wiarygodne |
| `16` zamknięta, awaria sondy | **liczba** | korekty **są** stosowane; awarię zgłasza `MIX-1` i panel Stan, nie kafel |
| `1` otwarta, zimny silnik | `— ○` | wartość zamrożona |
| `4` otwarta, obciążenie / hamowanie | `— ○` | wartość zamrożona |
| `8` otwarta, awaria układu | `— ○` | wartość zamrożona; awarię zgłasza `MIX-1` |
| `0` silnik wyłączony | `— ○` | nie ma czego korygować |
| brak odczytu `0103` | `— ○` | **domyślnie zachowawczo** |
| wartość spoza enumeracji | `— ○` | j.w. |

Ostatnie dwa wiersze są sednem tej tabeli. **Domyślną odpowiedzią jest kreska, nie liczba** —
kafel milczy, dopóki `0103` nie potwierdzi pętli zamkniętej. Odwrotny domyślny wybór
(„pokazuj, chyba że wiadomo, że nie wolno") daje przy zerwanym odczycie `0103` liczbę wyglądającą
na wiarygodną, i to dokładnie w chwili, gdy aplikacja wie o silniku najmniej.

**Wiersz normy zostaje `−10 – +10` także przy kresce.** Norma nie przestaje istnieć dlatego,
że odczyt jest chwilowo nieważny — a pusty wiersz normy jest w tym projekcie zakazany (§8.8
rozszerzenia diagnostycznego).

⚠️ **Do etapu K2 kafel nie ma jak tego rozpoznać**, bo `0103` dochodzi dopiero z tym
rozszerzeniem. Etapy wcześniejsze pokazują liczbę bez zastrzeżenia — to stan przejściowy,
zapisany świadomie, żeby nikt go później nie wziął za docelowy.

---

## 9. Panel Powietrze — nowy

### 9.1 Układ

```
┌──────────┬──────────┬──────────┬──────────┐
│ 88 °C ~  │  92 °C   │ 13,9 V   │  +3,9 %  │
│ OLEJ mod.│   PŁYN   │ NAPIĘCIE │ KOREKTA D│
│   ≥ 90   │  70–105  │ 13,0–15,0│ −10 – +10│
├──────────┴──────────┴──────────┴──────────┤
│ ○ ○ ○ ○ ●  POWIETRZE      04:12   4,0 Hz  │
├───────────────────────────────────────────┤
│ PODCIŚNIENIE  (wyliczone)         65 kPa  │
│  110 ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄  │
│    0 ‾‾╲___╱‾‾‾╲______╱‾‾‾‾               │
│                            norma    —     │
├───────────────────────────────────────────┤
│ PRZEPUSTNICA        zadana ── rzeczyw. ┈┈ │
│  100 ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄  │
│    0 __╱‾╲___╱‾╲__      zad. 18 %  rz. 18 %│
│                            rozjazd  0 pkt │
├───────────────────────────────────────────┤
│ PEDAŁ         __╱‾╲___╱‾╲__        14 %   │
├───────────────────────────────────────────┤
│ atmosferyczne  99 kPa  ·  kolektor 34 kPa │
└───────────────────────────────────────────┘
```

### 9.2 Co ten panel pokazuje, czego nie widać nigdzie indziej

**Podciśnienie** = `0133` atmosferyczne − `010B` kolektor. To **jedyna wartość liczona
z dwóch pomiarów** w całym projekcie i musi być tak oznaczona.

Uwaga na różne tempa: `010B` jest w poziomie A (~1 Hz), a `0133` w poziomie C (~0,2 Hz).
Podciśnienie liczy się **z ostatniej znanej wartości atmosferycznej**, przetrzymywanej między
odczytami. Jest to dopuszczalne, bo ciśnienie atmosferyczne zmienia się z pogodą i wysokością,
czyli w skali minut i kilometrów, a nie sekund. **Gdy atmosferycznego jeszcze nie odczytano —
podciśnienie jest `null`, nie liczone z założonych 101 kPa.**

Nieszczelność dolotu podnosi ciśnienie w kolektorze, czyli **obniża podciśnienie**. Dzisiejsza
aplikacja pokazuje wyłącznie skutek — dodatnią korektę paliwa. Ten panel pokazuje przyczynę.

**Zadana kontra rzeczywista przepustnica.** Sterownik każe przepustnicy ustawić się w danym
położeniu (`014C`) i odczytuje, gdzie faktycznie jest (`0111`). Trwały rozjazd oznacza
zabrudzony albo zacinający się korpus przepustnicy — częsta przypadłość silników
z bezpośrednim wtryskiem, bo nie ma strumienia paliwa, który by dolot przemywał.

**Obie krzywe rysowane jedna na drugiej.** Zdrowa przepustnica: krzywe się pokrywają.
Chora: rozjeżdżają się widocznie. **Nie potrzeba do tego progu** — dlatego reguły nie ma
(sekcja 5), a wiersz „rozjazd" podaje po prostu różnicę w punktach procentowych.

**Pedał** jest odniesieniem: czego chciał kierowca. Rozjazd pedał → zadana to co innego niż
zadana → rzeczywista; pierwsze jest normalne (sterownik filtruje i ogranicza), drugie nie.

### 9.3 Pasma

| Parametr | Rodzaj | Pasmo | Powód |
|---|---|---|---|
| Podciśnienie | **brak** | `—` | Zależy od obciążenia i wysokości n.p.m.; **brak źródła na wartość jałową** |
| Przepustnica zadana i rzeczywista | fizyczny | 0 – 100 % | Katalog |
| Rozjazd | **brak** | `—` | Brak źródła na dopuszczalną różnicę |
| Pedał | fizyczny | 0 – 100 % | Katalog |
| Ciśnienie atmosferyczne | fizyczny | 50 – 110 kPa | Katalog |

**Cztery z pięciu pozycji mają `—`.** To panel, który **pokazuje i nie orzeka** — i to jest
uczciwa odpowiedź, dopóki nie ma źródeł.

### 9.4 Zakresy osi Y

| Wykres | Zakres | Skąd |
|---|---|---|
| Podciśnienie | 0 – 110 kPa | Maksimum to ciśnienie atmosferyczne na poziomie morza |
| Przepustnica (obie krzywe) | 0 – 100 % | Katalog |
| Pedał | 0 – 100 % | Katalog |

---

## 10. Karta miesiąca

### 10.1 Gdzie jest

**W nagłówku zakładki Historia, nad siatką kalendarza.** Kalendarz już ma przewijanie miesięcy
strzałkami — karta zmienia się razem z nim. Zero nowej nawigacji, zero nowej zakładki.

```
┌───────────────────────────────────────────────────────────┐
│  ◀   SIERPIEŃ 2026   ▶                                    │
├───────────────────────────────────────────────────────────┤
│  Przejazdy              23        lipiec 19        +4     │
│  Dystans             412 km       lipiec 388 km   +24 km  │
│  Czas za kierownicą  14 h 20      lipiec 12 h 05  +2 h 15 │
├───────────────────────────────────────────────────────────┤
│  ROZGRZEWANIE                                             │
│  Bez rozgrzania       7 z 23      lipiec 5 z 19           │
│  Mediana do 90 °C       6:40      lipiec 6:12     +28 s   │
├───────────────────────────────────────────────────────────┤
│  TRENDY                                                   │
│  Mediana korekty długiej  +4,1 %  lipiec +3,8 %   +0,3 pp │
│  Najwyższa temp. płynu      97 °C lipiec 96 °C            │
│  Najniższe napięcie       13,6 V  lipiec 13,7 V           │
├───────────────────────────────────────────────────────────┤
│  [ pon ] [ wt ] [ śr ] ... siatka kalendarza poniżej      │
└───────────────────────────────────────────────────────────┘
```

### 10.2 „Bez rozgrzania" — wiersz bez ani jednego progu

To jest najciekawszy wiersz tej karty, bo **nie zawiera żadnej wymyślonej liczby**.

Pole `czasDo90CSekundy` jest `null` dokładnie wtedy, gdy płyn **nigdy nie osiągnął 90 °C**
w danym przejeździe. Zliczenie takich sesji nie wymaga zdefiniowania, co znaczy „zimny",
„krótki" ani „za krótki" — **wystarczy policzyć wartości `null`**.

A odpowiada na pytanie, którego inaczej nie da się zadać: *ile razy w tym miesiącu uruchomiłem
silnik i zgasiłem go, zanim się rozgrzał.* Krótkie przejazdy są tym, co silnik zużywa
najbardziej, i jest to jedyna rzecz w całej aplikacji, na którą kierowca ma bezpośredni wpływ.

### 10.3 Karta nie orzeka — i to jest decyzja, nie przeoczenie

Kolumna różnicy pokazuje **liczbę**: `+28 s`, `+0,3 pp`, `+4`. Nie pokazuje strzałki oceniającej,
nie koloruje na czerwono, nie pisze „pogorszenie".

Powód: **nie wiem, czy 28 sekund to problem.** Może termostat zaczyna siadać, a może w lipcu
było cieplej na dworze. Żeby to rozstrzygnąć, potrzeba albo źródła, albo linii bazowej
z kilkunastu miesięcy — a rozszerzenie ma jedno i drugie przed sobą, nie za sobą.

**Liczby obok siebie to informacja. Strzałka to wniosek.** Pierwsze mogę dać uczciwie, drugiego
nie.

### 10.4 Mediana, nie średnia

Wszystkie wartości zbiorcze liczone jako **mediana**, nie średnia arytmetyczna.

Jeden nietypowy przejazd — postój w korku z włączonym silnikiem, jazda w upał, pięciominutowy
skok do sklepu — potrafi przesunąć średnią na tyle, że trend z miesiąca na miesiąc przestaje
być widoczny. **Mediana ignoruje pojedyncze wybryki i pokazuje typową jazdę**, a o typową
jazdę tu chodzi.

### 10.5 Nowe pole podsumowania

Karta miesiąca liczy się z pól, które już są — poza jednym:

| Pole | Skąd | Po co |
|---|---|---|
| `medianaKorektyDlugoterminowej` | mediana serii `0107` w przejeździe | Trend miesiąc do miesiąca; pojedynczy odczyt nic nie mówi |

`null`, gdy serii `0107` w przebiegu nie ma.

**Reszta karty liczy się z istniejących pól:** `dystansKm`, `czasTrwaniaS`, `czasDo90CSekundy`,
`maxPlynC`, `minNapiecie`.

---

## 11. Zmiany w kontraktach

| Miejsce | Zmiana |
|---|---|
| §10.1 stałe bazowego | Trzeci poziom: **`Wolny C — n % 20 == 13`** |
| §10.2 i §8.5 bazowego | **`04` obciążenie dołącza do PID-ów obowiązkowych pętli gorącej** — wejście modelu oleju. Gniazd konfigurowalnych jest trzy, nie cztery. Naprawa błędu wykrytego przy przeglądzie tego rozszerzenia, sekcja 11.2 |
| §10.2 skład zapytań | **Przebudowa wszystkich trzech poziomów poza gorącym** — sekcja 7.1 |
| §10.4 progi reguł bazowego | Nowy wiersz: `MIX-1` |
| §8.6 podsumowanie bazowego | `medianaKorektyDlugoterminowej`; **`czasPozaPasmemKorektSekundy` zastąpione przez `czasPozaPasmemWPetliZamknietejSekundy` i `czasWPetliZamknietejSekundy`** |
| §8.8 tabela pasm rozszerzenia diagnostycznego | Sześć nowych wierszy: `0103`, `012E`, `014C`, `0149`, `0133`, podciśnienie |
| §8 rozszerzenia diagnostycznego | **Pięć paneli zamiast czterech**; wskaźnik `● ○ ○ ○ ○` |
| §8.3 rozszerzenia diagnostycznego | Panel Mieszanka: status pętli, cieniowanie, nowy licznik |
| §12.5 historia bazowego | Karta miesiąca nad kalendarzem |
| Katalog PID-ów | `0103` dostaje **dekoder enumeracji** zamiast surowych bajtów |

**Czego nie zmieniamy:** pętli gorącej, jej częstotliwości, składu `010D` i `0105`, faz
poziomów A, B i trybu 03, istniejących progów alarmów, decymacji, checkpointu.

### 11.1 Naprawa błędu w dokumencie bazowym: obciążenie `0104`

**Znalezione przy przeglądzie krytycznym tego rozszerzenia, dotyczy dokumentu bazowego.**

Model temperatury oleju liczy `target = płyn + 25 × (obciążenie/100)`. Obciążenie `0104`
było w bazowym **zwykłym gniazdem wykresu**, konfigurowalnym przez użytkownika.

Wystarczyłoby, żeby użytkownik zmienił parametr na jednym z wykresów, a **model oleju cicho
zamarłby**: kafel pokazywałby ostatnią wyliczoną wartość, skala pewności przestałaby rosnąć,
i nic w interfejsie by o tym nie powiedziało.

To jest awaria dokładnie tego rodzaju, którego ten projekt nie dopuszcza — **wartość wygląda
poprawnie i jest nieaktualna**.

**Poprawka: `0104` dołącza do obowiązkowych `010D` i `0105`.** Gniazd konfigurowalnych jest
odtąd trzy. Zmiana trafia do dokumentu bazowego, bo błąd tam powstał.

**Panel Podstawowy nic nie traci.** Obciążenie jest teraz w pętli **na stałe**, więc nadal da
się je narysować na wykresie — „gniazdo" oznacza wyłącznie to, że parametr **dołącza do
zapytania gorącego**, a nie że jest jedynym, który wolno pokazać. Układ trzech wykresów
Podstawowego (obroty, obciążenie, zapłon) zostaje bez zmian.

Realna zmiana dla użytkownika: konfigurowalnych parametrów jest trzy zamiast czterech —
i jest to cena za to, żeby model oleju nie mógł zostać wyłączony przez przypadek.

### 11.2 Reguła `MIX-1`

| | |
|---|---|
| **Rejestr** | `RuleEngine` — werdykt przeglądu |
| **Warunek** | `0103` bajt A ∈ {`8`, `16`} |
| **Waga** | uwaga |
| **Źródło** | Enumeracja z sekcji 4 — **sterownik sam nazywa ten stan awarią** |

**Tytuł:** Sterownik zgłasza awarię układu regulacji mieszanki

**Treść przy wartości `8`:** *Układ paliwowy pracuje w pętli otwartej z powodu awarii —
tak raportuje sterownik. Korekty paliwa są w tym stanie nieaktywne, więc silnik jedzie
z mapy bazowej bez korygowania składu mieszanki.*

**Treść przy wartości `16`:** *Układ pracuje w pętli zamkniętej, ale sterownik zgłasza usterkę
sprzężenia zwrotnego z sondy tlenu.*

**Nie orzeka, co jest zepsute.** Podaje to, co powiedział sterownik — a to jest cała
i jedyna treść tej reguły.

---

## 12. Testy

| Obszar | Test | Dlaczego akurat ten |
|---|---|---|
| **Rozłączność czterech poziomów** | `n % 4 == 0`, `n % 10 == 5`, `n % 20 == 13`, `n % 200 == 150` — **żaden cykl nie ma trzech zapytań**, na co najmniej 200 000 cykli | Trzeci poziom to trzecia okazja do kolizji. Dwie poprzednie zostały znalezione dopiero rachunkiem |
| Faza poziomu C | Wykonuje się dokładnie co dwudziesty cykl | |
| **Nietykalność gorącej** | Skład i tempo identyczne przed i po przebudowie | Przebudowa dotyka trzech poziomów — łatwo zawadzić o czwarty |
| **Dekoder `0103`** | Każda z sześciu wartości z tabeli 4 daje właściwy opis; **wartość spoza enumeracji daje „nieznany" z surową liczbą**, nie najbliższą pasującą | Zgadywanie najbliższej wartości to wymyślanie danych |
| **`MIX-1`** | Odpala się dla `8` i `16`; **nie odpala dla `1` i `4`**, bo to normalne stany pracy | Najłatwiejszy błąd: potraktować każdą otwartą pętlę jako usterkę |
| **Cieniowanie przedmuchiwania** | Region `▓` pokrywa się czasowo z próbkami, gdzie `012E` > 0 | |
| **Cieniowanie otwartej pętli** | Region `░` tam, gdzie `0103` ∉ {`2`, `16`} | Uwaga: `16` to pętla **zamknięta** mimo awarii — nie może trafić do cieniowania „otwarta" |
| **Licznik w pętli zamkniętej** | Czas poza pasmem liczony **tylko** w pętli zamkniętej; mianownik to czas w pętli zamkniętej, nie czas sesji | Zmiana definicji względem poprzedniej wersji — stary test przestaje obowiązywać i musi zostać przepisany |
| **Podciśnienie** | Różnica `0133 − 010B`; `null`, gdy któregokolwiek brak | Wartość liczona z dwóch pomiarów — brak jednego unieważnia wynik |
| **Rozjazd przepustnicy** | Różnica `014C − 0111` w punktach procentowych; bez progu i bez wagi | |
| **`medianaKorektyDlugoterminowej`** | Mediana, **nie średnia**; `null` gdy serii brak; test na serii z jednym skrajnym odstającym punktem | Mediana i średnia różnią się dokładnie tam, gdzie to ma znaczenie |
| **Karta miesiąca: „bez rozgrzania"** | Liczy sesje z `czasDo90CSekundy == null`, **nie stosuje żadnego progu temperatury** | Jedyny wiersz karty bez wymyślonej liczby — test to utrwala |
| **Karta miesiąca nie orzeka** | Kolumna różnicy zawiera **wyłącznie liczbę ze znakiem**, nigdy słowa oceniającego ani strzałki wartościującej | Sekcja 10.3 |
| **Pusty miesiąc** | Miesiąc bez przejazdów pokazuje `—` we wszystkich polach, **nie zera** | Zero przejazdów to co innego niż zero kilometrów |
| **Kompletność pasm** | Sześć nowych parametrów ma wpisy w `PasmaOdniesienia` | Kontrakt z §8.8 rozszerzenia diagnostycznego |
| **Czwarty kafel milczy domyślnie** | Kafel daje `— ○` dla `1`, `4`, `8`, `0`, **przy braku odczytu `0103`** i przy wartości spoza enumeracji; liczbę daje **wyłącznie** dla `2` i `16` | §8.5. Najłatwiejszy błąd to warunek `if (petlaOtwarta) kreska` — przy zerwanym `0103` przepuszcza liczbę, czyli myli się w jedyną stronę, w którą nie wolno |
| **Wiersz normy kafla nigdy nie pusty** | Przy `— ○` trzeci wiersz kafla nadal pokazuje `−10 – +10` | §8.8. Kreska w wartości nie kasuje normy |
| **`012F` nie jest odpytywany** | Zbiór PID-ów poziomu C **nie zawiera** `012F` na żadnej nastawie tempa | Poprawka P1. Test pilnuje, żeby nie wrócił przy zapełnianiu wolnych miejsc |
| **Obowiązkowość `0104`** | Skład pętli gorącej zawiera `0104` **przy każdej konfiguracji trzech gniazd**, także gdy użytkownik wybierze trzy zupełnie inne parametry | Sekcja 11.1. Bez tego testu regresja wraca, a objawem jest zamrożony kafel oleju — czyli nic, co rzuca się w oczy |
| **Model oleju dostaje obciążenie** | Test end-to-end: po zmianie wszystkich trzech gniazd model oleju **nadal się aktualizuje**, a skala pewności rośnie | Test sprawdzający skutek, nie tylko skład listy |

---

## 13. Kolejność realizacji

Wchodzi **po etapie D9** rozszerzenia diagnostycznego.

| Etap | Zawartość | Ukończony, gdy |
|---|---|---|
| **K1** | Przebudowa poziomów: A, B, C + faza `n % 20 == 13` + testy | Test rozłączności na 200 000 cykli: zero cykli z trzema zapytaniami |
| **K2** | Dekoder enumeracji `0103` + reguła `MIX-1` + **warunek ważności czwartego kafla (§8.5)** + testy | Sześć wartości rozpoznanych; wartość spoza enumeracji daje „nieznany"; **kafel milczy przy braku potwierdzenia pętli zamkniętej** |
| **K3** | Panel Mieszanka: wiersz statusu, cieniowanie, nowy licznik | Cieniowanie pokrywa się z próbkami; licznik liczy w pętli zamkniętej |
| **K4** | Panel Powietrze + piąty punkt we wskaźniku paneli | Dwie krzywe przepustnicy jedna na drugiej; podciśnienie oznaczone jako liczone |
| **K5** | `medianaKorektyDlugoterminowej` + zmiana pól licznika + testy | Mediana odporna na wartość odstającą |
| **K6** | Karta miesiąca nad kalendarzem | Pusty miesiąc daje `—`, nie zera; kolumna różnicy bez ocen |
| **K7** | Weryfikacja w aucie | Sekcja 14 |

**K1 przed wszystkim**, bo bez trzeciego poziomu żaden z nowych PID-ów nie jest odpytywany.
**K2 przed K3**, bo panel rysuje cieniowanie na podstawie dekodera.

---

## 14. Weryfikacja w aucie

| # | Co zrobić | Czego szukamy |
|---|---|---|
| 1 | Zimny start, obserwacja panelu Mieszanka | Status przechodzi z **`1` pętla otwarta, silnik za zimny** na **`2` zamknięta**. Kiedy? Powinno po kilkudziesięciu sekundach |
| 2 | Jazda ustabilizowana, 10–15 min | **Czy widać cieniowanie przedmuchiwania** i czy skoki korekt na nie przypadają. To weryfikacja całego punktu 1 |
| 3 | Mocne przyspieszenie | Status ma przejść na **`4` — pełne obciążenie**. Korekty w tym czasie zamierają |
| 4 | Hamowanie silnikiem, noga z gazu | Też **`4`** — odcięcie paliwa. Dobry sposób na sprawdzenie, czy dekoder rozróżnia stany |
| 5 | Panel Powietrze na jałowym | Podciśnienie ustabilizowane; **zadana i rzeczywista przepustnica pokrywają się** |
| 6 | Panel Powietrze przy zmianach gazu | Czy krzywe zadanej i rzeczywistej nadążają za sobą, czy jedna zostaje w tyle |
| 7 | Po miesiącu jazdy | Karta miesiąca ma sensowne liczby; **wiersz „bez rozgrzania" zgadza się z odczuciem** |

**Punkt 2 jest sprawdzianem sensu całego rozszerzenia.** Jeśli w danych z auta okaże się,
że przedmuchiwanie nigdy nie przypada na skoki korekt, to znaczy, że albo próbkowanie 0,4 Hz
jest za wolne (sekcja 7.4), albo hipoteza była błędna — i trzeba to zapisać, a nie naciągać.

**Punkt 1 przy okazji rozstrzyga rzecz z poprzedniego rozszerzenia:** ile trwa dojście do pętli
zamkniętej, czyli od kiedy korekty w ogóle mają sens.

---

## 15. Źródła

Rozszerzenie wprowadza **jedną nową wartość spoza katalogu PID-ów**: enumerację statusu układu
paliwowego (sekcja 4). Pochodzi z SAE J1979, wykaz PID-ów trybu 01.

Cztery pozostałe PID-y — `012E`, `014C`, `0149`, `0133` — mają wzory **już obecne w katalogu
projektu**, przeniesione z wersji iOS. Żaden nie wymaga nowej stałej.

**Rozszerzenie nie wprowadza ani jednego nowego progu.** Reguła `MIX-1` opiera się na
enumeracji, w której **sterownik sam nazywa swój stan awarią** — to nie jest wartość graniczna
ustalona przez kogokolwiek, tylko odczyt.

Odsyłacze w `docs/zrodla.md`, sekcja „Enumeracja statusu układu paliwowego".
