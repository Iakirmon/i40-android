# i40-android — rozszerzenie diagnostyczne

**Data:** 2026-08-14
**Wersja:** 2 — po sprawdzeniu źródeł i przeliczeniu poziomów odpytywania
**Status:** zaakceptowany, gotowy do realizacji
**Rozszerza:** `2026-08-14-i40-android-design.md` — dokument bazowy obowiązuje bez zmian
poza tym, co ta specyfikacja jawnie zmienia (sekcja 12)

> **Co zmieniło się wobec wersji 1.** Wersja 1 odmawiała dodania reguł werdyktu, bo „nie mamy
> na progi źródła". To była **odmowa poszukania, nie brak źródeł**. Po sprawdzeniu dokumentacji
> branżowej rozszerzenie zawiera **trzy reguły z podanymi wartościami odniesienia** (sekcja 10),
> a osobno wymienia to, czego nadal nie orzekamy i dlaczego (sekcja 5.1).
>
> Druga zmiana: przeliczenie poziomów odpytywania wykazało **trzy osobne kolizje faz**, z których
> najpoważniejsza istniała **w dokumencie bazowym od początku**, bez udziału tego rozszerzenia.
> Poprawki trafiły do bazowego, a rozszerzenie jest czysto addytywne. Historia w sekcji 7.4 —
> warto ją przeczytać, bo pokazuje, jak łatwo tu o błąd, którego nie widać.

---

## 1. Cel

Auto oddaje **37 PID-ów**. Aplikacja używa dwunastu, a sześć naprawdę wartościowych czyta
**raz, na postoju, i nigdy w jeździe** — czyli tam, gdzie one nic nie znaczą.

Rozszerzenie doprowadza sześć pomiarów do trzech miejsc naraz: **na żywo, w przeglądzie
i w raporcie z przejazdu**, i dokłada do nich **trzy reguły oparte na wartościach odniesienia
ze źródeł**. Nie dodaje nowego trybu pracy ani nowej zakładki.

---

## 2. Zasada: jedna wiedza, trzy horyzonty

> **Ten sam pomiar odpowiada na trzy różne pytania, zależnie od tego, kiedy się na niego patrzy.**

| Horyzont | Pytanie | Miejsce |
|---|---|---|
| **Teraz** | Co się dzieje w tej chwili? | Ekran żywy, alarmy |
| **Na postoju** | Jaki jest stan, bez jazdy? | Przegląd, werdykt |
| **Po przejeździe** | Co się działo przez czterdzieści minut? Kiedy? | Raport sesji |

Reguła projektowa całego rozszerzenia: **każdy nowy pomiar musi trafić do wszystkich trzech
miejsc.** Pomiar widoczny tylko na żywo znika razem z ekranem; pomiar tylko w raporcie nie
pomaga, gdy coś dzieje się teraz.

---

## 3. Co auto potrafi — zweryfikowane w masce

Zdekodowane z zapisu z 2026-08-08 (`4100BE3EA813`, `4120A007F011`, `4140FED00400`).
**Wszystkie poniższe są obsługiwane przez ten egzemplarz.**

### 3.1 Wchodzą do rozszerzenia

| PID | Nazwa | W zapisie (jałowy) | Po co |
|---|---|---|---|
| **`23`** | Ciśnienie w szynie wysokiego ciśnienia | `41230180` → **38,4 bar** | 2.0 GDI ma pompę wysokiego ciśnienia jako część zużywalną. Słabnięcie objawia się **niedobiciem pod obciążeniem** |
| **`3C`** | Temperatura katalizatora, bank 1, czujnik 1 | `413C1344` → **453 °C** | Jedyny czujnik widzący, co dzieje się **za** silnikiem |
| **`0B`** | Ciśnienie w kolektorze (MAP) | `410B22` → **34 kPa** | **To auto nie ma przepływomierza** (`10` nieobsługiwany), więc MAP jest podstawowym czujnikiem obciążenia |
| **`11`** | Pozycja przepustnicy | `411127` → **15,3 %** | Odniesienie dla MAP i dla ciśnienia szyny |
| **`43`** | Obciążenie absolutne | `41430039` → **22,4 %** | Porównywalne między warunkami, w przeciwieństwie do `04` |
| **`44`** | Zadany współczynnik lambda | `41448000` → **1,000** | Rozstrzyga, czy sterownik jest w pętli zamkniętej |

### 3.2 Obsługiwane, ale poza katalogiem — **wymagają formuły ze źródła**

| PID | Co mierzy | Status |
|---|---|---|
| **`15`** | Sonda tlenu 2, bank 1 — **za katalizatorem** | **Formuła nieznana projektowi.** Rodzina `14`–`1B` ma format 2-bajtowy, katalog zna tylko 4-bajtową `24`–`2B` |
| `32` | Ciśnienie par w układzie odparowania | j.w. |
| `34` | Sonda szerokopasmowa 1 z prądem | j.w. |
| `56` | Korekta wtórna sondy, bank 1 | j.w. |

**`15` jest z tej czwórki najcenniejszy** — napięcie sondy za katalizatorem to bezpośrednia
ocena jego sprawności. Wejdzie, gdy ktoś odczyta formułę z SAE J1979 i wpisze razem ze źródłem.
Do tego czasu ekrany pokazują `—` **z podanym powodem**.

---

## 4. Co mówią źródła — wartości odniesienia

Sekcja powstała po **sprawdzeniu dokumentacji**, nie po zastanowieniu się, ile powinna dawać
sprawna pompa. Każda wartość ma źródło i **jawnie podany poziom wiarygodności**.

### 4.1 Ciśnienie w szynie GDI

| Wielkość | Wartość | Źródło |
|---|---|---|
| Ciśnienie **zadane** na jałowym, GDI ogólnie | 500–800 psi = **34–55 bar** | Apex Tech Nation, *GDI Fuel System Diagnosis* |
| Ciśnienie pod obciążeniem | 2000–3500 psi = **138–241 bar** | j.w. |
| Podczas rozruchu | 200–400 psi = 14–28 bar | j.w. |
| Dopuszczalne odchylenie rzeczywistego od zadanego, **jałowy** | 100 psi = **7 bar** | j.w. |
| Dopuszczalne odchylenie, **pod obciążeniem** | 150 psi = 10 bar | j.w. |
| Ciśnienie wtrysku silnika Nu GDI | **150 bar**, pompa do **200 bar** | Opracowania o silniku Hyundai Nu |

**Zmierzone w tym aucie: 38,4 bar na jałowym.** Mieści się w podanym zakresie 34–55 bar —
to jest niezależne potwierdzenie, że i źródło, i odczyt są sensowne.

### 4.2 Temperatura katalizatora

| Wielkość | Wartość | Źródło |
|---|---|---|
| Temperatura zapłonu (light-off), minimum skuteczne | **300 °C** | Opracowania branżowe o katalizatorach |
| Normalny zakres pracy przy typowej jeździe | **650–870 °C** | j.w. |
| Typowy odczyt przed katalizatorem | 600–900 °C | j.w. |

PID `3C` to czujnik **przed** katalizatorem (bank 1, czujnik 1), więc obowiązuje zakres 600–900 °C.
Zmierzone 453 °C **na jałowym** jest poniżej tego pasma i to jest poprawne — pasmo dotyczy
typowej jazdy, a na jałowym przepływ i temperatura spalin są niskie.

### 4.3 Poziom wiarygodności — powiedziane wprost

**To są źródła branżowe i encyklopedyczne, nie fabryczne dane Hyundaia dla silnika G4NC.**

Konsekwencje, wszystkie wiążące:

- Reguły z sekcji 10 mają wagę **`uwaga`**, nigdy `usterka`. Uwaga mówi „sprawdź", usterka
  mówi „jest zepsute" — a na to drugie te źródła nie wystarczają.
- Treść każdego wniosku **podaje zakres odniesienia i jego pochodzenie**, żeby użytkownik
  wiedział, wobec czego jest porównywany.
- **Fabryczne dane Hyundaia unieważniają te wartości.** Gdy pojawi się instrukcja warsztatowa
  G4NC z konkretnymi liczbami, wchodzą one i sekcja 4 zostaje przepisana.

---

## 5. Nie-cele

### 5.1 Czego nadal nie orzekamy — i dlaczego akurat tego

Trzy reguły wchodzą. Cztery rzeczy nadal nie, i każda z osobnego powodu:

**Porównania rzeczywistego z zadanym ciśnieniem.** To jest **metoda, którą źródła wskazują jako
właściwą**: *„If actual pressure tracks desired pressure closely across all operating conditions,
the HPFP is doing its job. If actual consistently lags behind desired, or drops under load, the
HPFP is the primary suspect."* Ciśnienie **zadane** jest jednak PID-em producenckim Hyundaia,
poza trybem 01 standardu OBD-II. **Nie mamy go i nie da się go zdobyć bez UDS.** Dlatego
reguła `GDI-1` porównuje z zakresem odniesienia, a nie z zadanym — i jest to gorszy test,
co dokument mówi wprost zamiast udawać, że nie.

**Reguła „ciśnienie nie wzrosło pod obciążeniem".** Zakres 138–241 bar pod obciążeniem jest
w źródle, ale definicja „pod obciążeniem" już nie. Każdy próg w rodzaju „obciążenie > 70 %"
byłby **selektorem warunku wymyślonym przeze mnie**, a nie liczbą ze źródła. Zamiast reguły:
raport pokazuje maksimum sesji **wraz z zakresem odniesienia** i obciążeniem, przy którym
wystąpiło — sekcja 11.1. Użytkownik dostaje komplet i ocenia sam.

**Reguły dla `0B`, `11` i `43`.** Te wielkości mają sens wyłącznie w zestawieniu z innymi, nie
jako pojedyncze liczby z progiem — i dlatego w tabeli 8.8 mają odpowiednio „brak normy"
(`0B`, z uzasadnieniem) i „zakres fizyczny" (`11`, `43`). **Pokazujemy je, ale niczego na ich
podstawie nie orzekamy.**

`44` jest wyjątkiem: ma pasmo `1,000`, ale nie z reguły, tylko **z definicji mieszanki
stechiometrycznej**. Odchylenie od jedynki znaczy „sterownik wyszedł z pętli zamkniętej", a to
jest stan pracy, nie usterka — więc pasmo tak, reguła nie.

**Porównania z własną historią.** Zdanie „38 bar, typowo 41 bar dla tego auta" byłoby lepsze
niż każdy próg z zewnątrz. Wymaga linii bazowej z kilkunastu przejazdów — osobny dokument,
po zebraniu danych.

### 5.2 Pozostałe nie-cele

- **Spalanie w l/100 km.** `5E` nieobsługiwany, `10` nieobsługiwany — każda wartość musiałaby
  powstać z modelu na MAP, obrotach i zgadywanej sprawności napełnienia. Liczba wymyślona,
  stojąca obok prawdziwych i nieodróżnialna od nich.
- **Znaczniki zdarzeń w przebiegu** — dobry pomysł, osobny zakres.
- Pomiary przyspieszeń, wskaźniki powtarzające deskę rozdzielczą.

---

## 6. Decyzje projektowe

| Decyzja | Uzasadnienie |
|---|---|
| **Trzeci poziom odpytywania — pętla średnia** | Sześć nowych PID-ów nie mieści się w gorącej szóstce, a w rotacji zimnej co 2,5 s byłyby bezużyteczne dla ciśnienia zmieniającego się z gazem |
| **Rozszerzenie jest czysto addytywne — nie rusza faz** | Fazy rotacji zimnej i odczytu `03` są poprawione w dokumencie bazowym, gdzie kolizja istniała już bez pętli średniej. Rozszerzenie dokłada `n % 4 == 0` i **dowodzi rozłączności** — sekcja 7.2 |
| **Nastawa `Zrównoważona` 4 Hz zostaje domyślna i wiążąca** | Przy niej trzy poziomy mieszczą się z zapasem: szczyt 140 ms przy budżecie 250 ms. Sekcja 7.3 |
| **Pętla gorąca nietknięta** | 4 Hz i skład `0D 05 04` + trzy gniazda są wiążące z dokumentu bazowego |
| **Trzy reguły ze źródłami, wszystkie z wagą `uwaga`** | Sekcja 4.3 — źródła branżowe nie wystarczają na wagę `usterka` |
| **Warunki reguł zbudowane z istniejących pojęć projektu** | „Silnik rozgrzany" to `OilTempEstimator.silnikRozgrzany`, „silnik pracuje" to obroty > 500 z bazowego. **Zero nowych stałych warunkowych** |
| **Panele zamiast nowych zakładek** | Zakładki są trzy i mają zostać trzy |
| **Przełączanie paneli dozwolone w ruchu** | Sekcja 8.7 — świadoma zmiana reguły bezpieczeństwa, z uzasadnieniem |
| **`15` zaprojektowany, ale niezaimplementowany** | Ekran ma dla niego miejsce i jawnie mówi, że go brakuje |

---

## 7. Poziomy odpytywania — cztery, bez kolizji

### 7.1 Skład

> ⚠️ **Tabela poniżej opisuje stan z sierpnia, przed rozszerzeniem kontekstowym.** Skład
> poziomów został tam przebudowany na cztery. **Stan aktualny: sekcja STAN AKTUALNY
> w `.cursor/rules/00-projekt.mdc`.** Fazy i rachunek kolizji poniżej pozostają w mocy —
> zmienił się wyłącznie przydział PID-ów do poziomów.

**Cztery rzeczy emitują zapytania**, nie trzy. Czwartą łatwo przeoczyć, bo pochodzi
z dokumentu bazowego i nie wygląda na „poziom" — a to właśnie ona powodowała drugą kolizję.

| Poziom | Kiedy | Skład | Częstotliwość przy 4 Hz |
|---|---|---|---|
| Gorący | każdy cykl | `0D 05 04` + trzy gniazda wykresów | 4 Hz |
| **Średni** | **`n % 4 == 0`** | **`23 3C 0B 11 43 44`** | ~1 Hz |
| Zimny | **`n % 10 == 5`** | `46 1F 42 0F 07` — bez `2F`, poprawka P1 | ~0,4 Hz |
| Odczyt kodów | **`n % 200 == 150`** | `03` — jedno polecenie, nie multi-PID | ~0,02 Hz |

Wszystkie sześć PID-ów pętli średniej zweryfikowane w masce jako obsługiwane.

Trzy z czterech pozycji mają **przesuniętą fazę** i to nie jest ozdoba — sekcje 7.2 i 7.4.

### 7.2 Dlaczego czwarty poziom się nie zderza z pozostałymi

**Fazy rotacji zimnej i odczytu `03` przychodzą z dokumentu bazowego** — sekcja 10.1 bazowego
wyjaśnia, dlaczego są przesunięte i że bez tego co dwusetny cykl robił trzy zapytania.
Rozszerzenie **niczego tam nie zmienia**; dokłada czwarty poziom w istniejącą siatkę.

Zadanie jest więc jedno: **sprawdzić, że `n % 4 == 0` nie zderza się z żadną z dwóch
zajętych faz.**

```
n % 4  == 0    ⟹  n parzyste
n % 10 == 5    ⟹  n nieparzyste                      →  rozłączne z definicji

n % 200 == 150 ⟹  n % 4 stale równe 150 % 4 = 2      →  nigdy 0, rozłączne
```

Sprawdzone na 200 000 cykli, każda para osobno: **zero kolizji**. Rozkład zapytań na cykl przy
komplecie czterech poziomów:

| Zapytań w cyklu | Ile cykli na 200 000 |
|---|---|
| 1 | 129 000 |
| 2 | 71 000 |
| **3 lub więcej** | **0** |

Dla porównania, gdyby pętla średnia trafiła w niepoprawioną siatkę (`n % 10 == 0`,
`n % 200 == 0`): 9000 cykli z trzema zapytaniami i **1000 z czterema** — czyli 280 ms
w budżecie 250 ms. **Przekroczenie, nie ciasnota.**

### 7.3 Budżet przy trzech nastawach

| Nastawa | Zapytań/s | Szczyt cyklu | Budżet cyklu | Sufit |
|---|---|---|---|---|
| Oszczędna 2 Hz | 2,70 | 140 ms | 500 ms | 25/s |
| **Zrównoważona 4 Hz — domyślna** | **5,40** | **140 ms** | **250 ms** | 25/s |
| Szczegółowa ~8 Hz | 10,80 | 140 ms | 125 ms | 25/s |

Przy `Zrównoważonej` zapas jest dwukrotny w budżecie cyklu i prawie pięciokrotny wobec sufitu.

**Przy `Szczegółowej` cykle z pętlą średnią przekraczają budżet** (140 ms wobec 125 ms).
To nie jest usterka — dokument bazowy mówi wprost, że gdy obieg trwa dłużej niż interwał,
pętla idzie tak szybko, jak może, bez kolejkowania. Realna częstotliwość spada i **licznik Hz
na ekranie to pokaże**. Nastawa `Szczegółowa` z założenia oddaje, ile adapter zdoła.

**`Zrównoważona` pozostaje domyślna i to jest decyzja, nie ustawienie fabryczne do zmiany.**
Przy niej wszystkie cztery poziomy mieszczą się z zapasem, a wykresy są czytelne.

### 7.4 Jak ta siatka powstała — zapis dla potomności

Warto to zostawić, bo pokazuje, jak łatwo tu o błąd, którego nie widać.

**Pierwsza wersja tego rozszerzenia** dołożyła pętlę średnią przy `n % 4 == 0` do rotacji
zimnej stojącej wtedy na `n % 10 == 0`. Oba warunki spełniają się dla `n` podzielnych przez 20:
**co dwudziesty cykl trzy zapytania**, 210 ms w budżecie 250 ms.

**Poprawka pierwsza** przesunęła rotację zimną na `n % 10 == 5`. Wyglądało na domknięte.

**Przegląd krytyczny znalazł drugą kolizję**, przeoczoną przy pierwszej: odczyt `03` stał
na `n % 200 == 0`, a **200 dzieli się przez 4**, więc trafiał w pętlę średnią. Znów trzy
zapytania, tylko rzadziej.

**Przegląd drugi znalazł trzecią rzecz, poważniejszą od obu**: kolizja odczytu `03`
z rotacją zimną (200 dzieli się przez 10) istniała **w dokumencie bazowym od początku**,
bez żadnej pętli średniej. Rozszerzenie próbowało łatać cudzy błąd u siebie.

Dlatego obie zmiany faz **trafiły do dokumentu bazowego**, gdzie jest ich miejsce, a
rozszerzenie zostało czysto addytywne.

**Wniosek do zapamiętania:** przy trzech okresach w jednej pętli kolizje trzeba **policzyć**,
a nie ocenić na oko. Każda z tych trzech była niewidoczna do momentu wypisania rozkładu
zapytań na cykl.

### 7.5 Kolejność w cyklu

Gorący zawsze pierwszy. Gdyby kiedykolwiek doszło do zbiegu średniego i zimnego — co przy
`n % 4` i `n % 10 == 5` jest niemożliwe, ale kod nie ma prawa na tym polegać — **średni idzie
przed zimnym**, bo jest bliżej danych, na które ktoś patrzy.

**Stałe do tabeli 10.1 dokumentu bazowego:**

```
Pętla średnia   n % 4  == 0
Rotacja zimna   n % 10 == 5      (zmiana: było n % 10 == 0)
```

---

## 8. Ekran żywy — panele

> **Rozszerzenie kontekstowe dokłada piąty panel — Powietrze.** Wskaźniki w układach poniżej
> pokazują już pięć pozycji. Sam panel jest opisany w sekcji 9 tamtego dokumentu.

> **Zanim przeczytasz układy paneli: sekcja 8.8 definiuje pasma odniesienia** i obowiązuje
> nie tylko tutaj, ale też w przeglądzie (sekcja 9) i w raporcie (sekcja 11). Bez niej mockupy
> poniżej wyglądają jak zbiór przypadkowych liczb pod wartościami.

### 8.1 Układ wspólny

Listwa czterech kafli **zostaje na górze niezależnie od panelu**. Pod nią pasek panelu,
poniżej obszar zmienny. Przełączanie: **szerokie przeciągnięcie w poziomie przez obszar
wykresów**, bez małych celów i bez menu.

**Skład listwy po poprawce P1:** olej (model), płyn `0105`, napięcie `0142`,
**korekta długa `0107`**. Czwarty kafel zastąpił poziom paliwa, który na tym egzemplarzu nie
daje danych (§3.2 bazowego).

Skutek uboczny, wart odnotowania: **wszystkie cztery kafle mają teraz pasmo ze źródła.**
Wcześniej czwarty miał w wierszu normy `—`, bo poziom w baku żadnej normy nie ma. Listwa
przestała więc zawierać pozycję, przy której wiersz normy nie niesie informacji — a to akurat
ta listwa, na którą patrzy się najczęściej i najkrócej.

**Pasmo czwartego kafla to `−10 – +10 %` i nie jest nowe** — to próg reguł `ltft_lean`
i `ltft_rich` z tabeli 10.4 dokumentu bazowego, ten sam, który już stoi w tabeli 8.8. Zasada
nadrzędna z 8.8 zostaje nienaruszona: poprawka P1 **nie wprowadza ani jednego nowego pasma,
ani jednej nowej reguły, ani jednego nowego alarmu.**

### 8.2 Panel 1 — Podstawowy

Bez zmian względem dokumentu bazowego.

```
┌──────────┬──────────┬──────────┬──────────┐
│ 88 °C ~  │  92 °C   │ 13,9 V   │  +3,9 %  │
│ OLEJ mod.│   PŁYN   │ NAPIĘCIE │ KOREKTA D│
│   ≥ 90   │  70–105  │ 13,0–15,0│ −10 – +10│
├──────────┴──────────┴──────────┴──────────┤
│ ● ○ ○ ○ ○  PODSTAWOWY      04:12   4,0 Hz  │
├───────────────────────────────────────────┤
│ OBROTY          ╱‾╲___╱‾‾‾╲__      1726   │
├───────────────────────────────────────────┤
│ OBCIĄŻENIE    __╱‾╲______╱‾╲_       34 %  │
├───────────────────────────────────────────┤
│ ZAPŁON        ‾‾╲__╱‾‾╲___╱‾‾       18 °  │
└───────────────────────────────────────────┘
```

**Na tym panelu nie ma ani jednej linii odniesienia i to jest poprawne.** Obroty i obciążenie
mają wyłącznie zakres fizyczny, a wyprzedzenie zapłonu **nie ma normy w ogóle** — mapa zapłonu
zależy od obciążenia, obrotów i paliwa, więc żadna pojedyncza liczba nie byłaby prawdziwa
(sekcja 8.8).

Pasma widać natomiast w listwie kafli, która jest wspólna dla wszystkich paneli.

### 8.3 Panel 2 — Mieszanka

**Powód istnienia:** korekty paliwa są diagnostyczne przede wszystkim **pod obciążeniem**,
a dziś widać je wyłącznie na postoju. Nieszczelność dolotu daje dodatnią korektę na jałowym
i zanikającą przy gazie; słabnący wtryskiwacz odwrotnie.

```
┌──────────┬──────────┬──────────┬──────────┐
│ 88 °C ~  │  92 °C   │ 13,9 V   │  +3,9 %  │
│ OLEJ mod.│   PŁYN   │ NAPIĘCIE │ KOREKTA D│
│   ≥ 90   │  70–105  │ 13,0–15,0│ −10 – +10│
├──────────┴──────────┴──────────┴──────────┤
│ ○ ● ○ ○ ○  MIESZANKA       04:12   4,0 Hz  │
├───────────────────────────────────────────┤
│ KOREKTA RAZEM                    norma ±20│
│  +25 ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄  │
│  +20 ──────────────────────────────────   │
│    0 ─────╱‾‾╲____╱‾‾‾‾╲______╱‾‾   +3,1 %│
│  −20 ──────────────────────────────────   │
│  −25 ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄  │
├─────────────────────┬─────────────────────┤
│ KRÓTKA      −0,8 %  │ LAMBDA ZAD.   1,000 │
│      norma  —       │      norma    1,000 │
│ DŁUGA       +3,9 %  │ ZA KAT.          —  │
│      norma  ±10     │      norma       —  │
├─────────────────────┴─────────────────────┤
│ Poza pasmem ±20 %:  0:00 z 4:12           │
└───────────────────────────────────────────┘
```

**Wykres sumy ma pasmo ±20 %, nie ±10 %.** To rozróżnienie łatwo zgubić: ±10 % jest progiem
reguł dla **korekty długoterminowej samej**, a ±20 % progiem reguły dla **sumy** (tabela 10.4
bazowego). Wersja robocza tego dokumentu rysowała ±10 % na wykresie sumy — czyli pasmo cudzej
reguły. Licznik czasu poza pasmem liczy się z tego samego progu co wykres, ±20 %.

**Korekta krótkoterminowa ma `—` w miejscu normy** i to jest poprawne: żadna reguła nie dotyczy
jej osobno, dotyczą długoterminowej i sumy.

**Wykres pokazuje sumę korekt**, bo to ona mówi, jak daleko sterownik odszedł od mapy bazowej.
Składniki pod spodem jako liczby — czyta się je jako poziom, nie jako kształt.

**Pasmo ±10 % narysowane na stałe.** Linia bez odniesienia nic nie znaczy przy zerkaniu w ruchu.

**„Poza pasmem" to licznik czasu w tej sesji**, nie stan chwilowy. Odróżnia jednorazowy skok
przy wyprzedzaniu od stanu trwającego pół przejazdu.

**`ZA KAT.` pokazuje `—` z powodem**, dopóki PID `15` nie dostanie formuły ze źródła.

### 8.4 Panel 3 — Wtrysk GDI

**Powód istnienia:** pompa wysokiego ciśnienia to jedyna kosztowna część zużywalna tego układu,
a jej stan widać wyłącznie pod obciążeniem.

```
┌──────────┬──────────┬──────────┬──────────┐
│ 88 °C ~  │  92 °C   │ 13,9 V   │  +3,9 %  │
│ OLEJ mod.│   PŁYN   │ NAPIĘCIE │ KOREKTA D│
│   ≥ 90   │  70–105  │ 13,0–15,0│ −10 – +10│
├──────────┴──────────┴──────────┴──────────┤
│ ○ ○ ● ○ ○  WTRYSK GDI      04:12   4,0 Hz  │
├───────────────────────────────────────────┤
│ CIŚNIENIE SZYNY                           │
│  240 ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄  │
│  138 ─────────────────────────────────    │  ← pod obciążeniem
│   55 ─────────────────────────────────    │  ← jałowy, góra
│   34 ─────────────╱‾‾╲────────────────    │  ← jałowy, dół
│    0 ──╱‾╲_____╱      ╲____╱‾‾╲_   52 bar │
├───────────────────────────────────────────┤
│ OBCIĄŻENIE ABS. __╱‾╲___╱‾╲__╱‾╲    30 %  │
├───────────────────────────────────────────┤
│ PRZEPUSTNICA    __╱‾╲___╱‾╲__╱‾╲    18 %  │
├───────────────────────────────────────────┤
│ Max w sesji:  148 bar  przy 84 % obciąż.  │
│   norma pod obciążeniem  138 – 241 bar    │
└───────────────────────────────────────────┘
```

**Dwa pasma na jednym wykresie, bo ciśnienie ma dwie normy** zależnie od stanu pracy: 34–55 bar
na jałowym i 138–241 bar pod obciążeniem (sekcja 4.1). Obie narysowane jednocześnie — kierowca
widzi, do którego pasma krzywa zmierza, bez przełączania czegokolwiek.

**Obciążenie i przepustnica nie mają pasm** — zakres fizyczny, bez normy. Są tu jako odniesienie
dla ciśnienia, nie jako pomiary do oceny.

**Cztery linie odniesienia z sekcji 4.1 narysowane na stałe** — pasmo jałowe 34–55 bar i pasmo
obciążeniowe 138–241 bar. To jest różnica wobec wersji 1: krzywa nie wisi już w próżni,
tylko jest widziana wobec zakresu, w którym powinna się znaleźć.

**Trzy przebiegi na wspólnej osi.** Ciśnienie samo w sobie nic nie mówi — 52 bary są świetne
na luzie i fatalne przy pełnym gazie.

**Wiersz „max w sesji" podaje obciążenie**, przy którym maksimum wystąpiło. Wartość bez tego
kontekstu byłaby bezużyteczna.

**Czego tu nie ma:** ciśnienia **zadanego**. To PID producencki Hyundaia, poza standardem
OBD-II — sekcja 5.1.

### 8.5 Panel 4 — Termika

```
┌──────────┬──────────┬──────────┬──────────┐
│ 88 °C ~  │  92 °C   │ 13,9 V   │  +3,9 %  │
│ OLEJ mod.│   PŁYN   │ NAPIĘCIE │ KOREKTA D│
│   ≥ 90   │  70–105  │ 13,0–15,0│ −10 – +10│
├──────────┴──────────┴──────────┴──────────┤
│ ○ ○ ○ ● ○  TERMIKA         04:12   4,0 Hz  │
├───────────────────────────────────────────┤
│ KATALIZATOR                               │
│  870 ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄  │  ← góra normy
│  650 ─────────────────────────────────    │  ← dół normy
│  300 ─────────────────────────────────    │  ← zapłon
│    0 ___╱‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾   612 °C   │
├───────────────────────────────────────────┤
│ PŁYN                              92 °C   │
│  105 ──────────────────────────────────   │
│   70 __╱‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾    │
├───────────────────────────────────────────┤
│ OLEJ (model)  ___╱‾‾‾‾‾‾‾‾‾‾‾‾     88 °C ~│
│   90 ──────────────────────────────────   │
│               pewność: dobra              │
├─────────────────────┬─────────────────────┤
│ DOLOT       23 °C   │ OTOCZENIE     21 °C │
│      norma    —     │      norma      —   │
├─────────────────────┴─────────────────────┤
│ Płyn 90 °C po 6:24   ·   olej po 12:10    │
└───────────────────────────────────────────┘
```

**Wszystkie trzy krzywe mają pasma**, każde z innego źródła: katalizator ze źródeł branżowych
(sekcja 4.2), płyn i olej z progów istniejących reguł projektu. **Dolot i otoczenie mają `—`** —
temperatura powietrza z definicji nie ma normy.

**Trzy linie odniesienia z sekcji 4.2:** zapłon 300 °C, pasmo normalnej pracy 650–870 °C.
Krzywa poniżej 300 °C przy rozgrzanym silniku i powyżej 870 °C to dokładnie warunki reguł
`KAT-1` i `KAT-2`.

**Trzy krzywe na wspólnej osi** pokazują, czego żadna osobno nie pokaże: katalizator rozgrzewa
się pierwszy i najszybciej, płyn drugi, olej najwolniej. Odstępstwo od tej kolejności samo
w sobie jest sygnałem.

**Wiersz dolny to właściwa diagnoza termostatu** — czas dojścia płynu do 90 °C od startu
silnika, porównywalny między przejazdami.

### 8.6 Zakresy osi Y dla nowych wykresów

Niezmiennik 11 dokumentu bazowego obowiązuje: **osie są sztywne**.

| Parametr | Zakres | Skąd |
|---|---|---|
| Ciśnienie w szynie | **0 – 240 bar** | Górna granica pasma obciążeniowego z sekcji 4.1 (241 bar), zaokrąglona w dół. **Nie** z zakresu fizycznego PID-u (655 350 kPa) — to granica typu, nie silnika |
| Temperatura katalizatora | **0 – 1000 °C** | Typowy odczyt przed katalizatorem sięga 900 °C (sekcja 4.2), plus zapas. Oś do 900 °C przykleiłaby próg `KAT-2` (870 °C) do krawędzi — czyli dokładnie ten stan, dla którego się na wykres patrzy, byłby najgorzej widoczny |
| Ciśnienie w kolektorze | 0 – 110 kPa | Ciśnienie bezwzględne, atmosferyczne z zapasem |
| Obciążenie absolutne | 0 – 100 % | Jak `04` w tabeli 10.7 bazowego |
| Pozycja przepustnicy | 0 – 100 % | j.w. |
| Suma korekt paliwa | −25 – +25 % | Ten sam zakres co pojedyncza korekta — celowo, żeby kształt znaczył to samo |

**Linie odniesienia** (34, 55, 138 bar; 300, 650, 870 °C; ±10 %) rysowane wewnątrz zakresu
jako stałe znaczniki. To nie jest skalowanie — bez nich krzywa przy zerkaniu w ruchu nic
nie znaczy.

Wartość poza zakresem **przycinana do krawędzi i oznaczana**.

### 8.7 Bezpieczeństwo — zmiana reguły z dokumentu bazowego

Dokument bazowy blokuje w ruchu wszystko poza zatrzymaniem nagrywania. **Przełączanie paneli
jest z tej blokady wyłączone** — świadoma zmiana, nie przeoczenie.

Uzasadnienie: to **jedno szerokie przeciągnięcie bez celu do trafienia**, nie zmienia niczego
w nagrywaniu i jest mniej rozpraszające niż czytanie ośmiu liczb naraz — a po to panele
powstały.

```
panelSwitch      → dozwolone także w ruchu
parameterChange  → nadal zablokowane
settings, navigation, start → nadal zablokowane
```

---

### 8.8 Pasma odniesienia — jeden język dla wszystkich widoków

**Ta sekcja obowiązuje także w sekcjach 9 (przegląd) i 11 (raport).** Liczba bez pasma nic nie
mówi komuś, kto nie ma wartości w pamięci — a nikt ich nie ma. „38,4 bar" to dane; „38,4 bar
przy normie 34–55" to informacja.

#### Zasada nadrzędna

> **Pasmo pochodzi z reguły albo ze źródła. Nigdy nie powstaje na potrzeby wyświetlania.**

To nie jest formalność. Pasmo narysowane „żeby ekran ładnie wyglądał" jest **liczbą wymyśloną
pokazaną jako norma** — czyli dokładnie tym, czego zabrania `30-zrodla.mdc`, tylko w groźniejszej
postaci, bo użytkownik porównuje z nim swój samochód.

Konsekwencja: **osiem pasm normy w całym projekcie i ani jedno nie jest nowe.** Wszystkie są
progami istniejących reguł albo wartościami ze sprawdzonych źródeł z sekcji 4. Wyprowadzenie
w tabeli poniżej.

#### Pasmo to nie alarm

> **Pasmo widać zawsze. Alarm odzywa się tylko dla pięciu warunków krytycznych.**

Przekroczenie pasma **nie wydaje dźwięku i nie wibruje**. Zmienia wyłącznie znacznik przy
wartości. Alarmują wyłącznie warunki z tabeli 10.3 dokumentu bazowego plus `KAT-2` — pięć
łącznie, wszystkie wybrane dlatego, że wymagają reakcji **w trakcie jazdy**.

Bez tego rozdziału aplikacja piszczałaby przy każdym zimnym silniku, a kierowca nauczyłby się
ignorować dźwięk — łącznie z tym jednym, który znaczy „zatrzymaj się".

| | Pasmo | Alarm |
|---|---|---|
| Płyn 68 °C na zimnym silniku | znacznik ▼ | **cisza** |
| Płyn 108 °C | znacznik ▲ | **dźwięk powtarzany co 10 s** |
| Korekta +14 % | znacznik ▲ | cisza |
| Katalizator 880 °C | znacznik ▲ | dźwięk jednorazowy |

#### Trzy rodzaje pasma

| Rodzaj | Co znaczy | Jak wygląda |
|---|---|---|
| **Norma** | Zakres, w którym wartość powinna się znaleźć. Z reguły albo ze źródła | `70–105 °C` pod wartością; linie odniesienia na wykresie |
| **Fizyczny** | Granice typu z katalogu PID — mówi „czy odczyt jest w ogóle możliwy", nie „czy zdrowy" | tylko zakres osi wykresu, **bez** pasma pod wartością |
| **Brak** | Nie ma źródła na normę dla tego parametru | **`—` w miejscu pasma**, nigdy puste miejsce |

Trzeci rodzaj jest najważniejszy i najczęściej pomijany. **Puste miejsce wygląda jak
przeoczenie; kreska mówi „sprawdziliśmy i normy nie ma".** To ta sama zasada co przy nieudanym
odczycie.

#### Znaczniki

```
92 °C      wartość w paśmie          — bez znacznika
108 °C ▲   powyżej pasma
68 °C ▼    poniżej pasma
— ⌀        odczyt nieudany           — powód obok
— ○        odczyt udany, wartość nieważna w tym stanie
88 °C ~    wartość z modelu, nie pomiar
```

**`⌀` i `○` to nie to samo i mylenie ich kosztuje informację.** `⌀` znaczy *nie wiem* — nic nie
przyszło. `○` znaczy *wiem, ale ta liczba teraz nic nie mówi* — odczyt się udał, tylko warunki
jego ważności nie są spełnione. Jedyny dzisiejszy przypadek `○` to **korekta paliwa poza pętlą
zamkniętą** (kafel czwarty i panel Mieszanka); rozpoznanie stanu wymaga `0103`, więc znacznik
działa **dopiero od warstwy kontekstowej**, etap K2.

`○` ma w tym projekcie ustalone znaczenie „nieaktywne / niedostępne" — ten sam znak niesie
wskaźnik paneli `● ○ ○ ○ ○` i pasek punktu odniesienia w §10.1 warstwy odniesienia. To celowa
zgodność, nie kolizja: wszędzie znaczy **„jest miejsce, ale nie teraz"**.

**Kolor nigdy nie jest jedynym sygnałem** — niezmiennik z dokumentu bazowego obowiązuje tu tak
samo jak przy werdykcie. Każdy stan ma własny znak.

#### Tabela wszystkich parametrów

Kompletna. Parametr wyświetlany gdziekolwiek w aplikacji i nieobecny w tej tabeli jest błędem.

| Parametr | PID | Rodzaj | Pasmo | Wyprowadzenie |
|---|---|---|---|---|
| Temperatura płynu | `05` | **norma** | **70 – 105 °C** | Reguły `thermostat` (< 70) i `overheat` (> 105), tabela 10.4 bazowego |
| Temperatura oleju | model | **norma** | **≥ 90 °C** | Reguła `oil_cold`, tabela 10.4 bazowego |
| Napięcie, silnik pracuje | `42` | **norma** | **13,0 – 15,0 V** | Reguły `alternator_low` (< 13,0 przy obr. > 500) i `overcharge` (> 15,0) |
| Napięcie, silnik zgaszony | `42` | **norma** | **> 12,4 V** | Reguła `battery_weak` (12,0–12,4 przy obr. < 50) |
| Korekta długoterminowa | `07` | **norma** | **−10 – +10 %** | Reguły `ltft_lean` i `ltft_rich` |
| Suma korekt | `06`+`07` | **norma** | **−20 – +20 %** | Reguła `trim_sum` |
| Ciśnienie szyny, jałowy | `23` | **norma** | **34 – 55 bar** | Sekcja 4.1 |
| Ciśnienie szyny, obciążenie | `23` | **norma** | **138 – 241 bar** | Sekcja 4.1 |
| Temperatura katalizatora | `3C` | **norma** | **650 – 870 °C**, zapłon od **300 °C** | Sekcja 4.2 |
| Lambda zadana | `44` | **norma** | **1,000** w pętli zamkniętej | Definicja mieszanki stechiometrycznej |
| Korekta krótkoterminowa | `06` | fizyczny | −100 – +100 % | Katalog PID. **Osobnej normy brak** — reguły dotyczą długoterminowej i sumy |
| Obroty | `0C` | fizyczny | 0 – 8000 obr/min | Katalog. **Brak źródła na pasmo jałowe dla G4NC** |
| Prędkość | `0D` | fizyczny | 0 – 250 km/h | Katalog |
| Obciążenie obliczone | `04` | fizyczny | 0 – 100 % | Katalog |
| Obciążenie absolutne | `43` | fizyczny | 0 – 100 % | Katalog |
| Pozycja przepustnicy | `11` | fizyczny | 0 – 100 % | Katalog |
| Wyprzedzenie zapłonu | `0E` | **brak** | `—` | Mapa zapłonu jest własnością sterownika, zależy od obciążenia, obrotów i paliwa. **Żadna pojedyncza norma nie istnieje** |
| Ciśnienie w kolektorze | `0B` | **brak** | `—` | Zależy od obciążenia i wysokości n.p.m. Sensowne wyłącznie jako podciśnienie wobec `33` |
| Temperatura dolotu | `0F` | **brak** | `—` | Zależy od temperatury otoczenia |
| Temperatura otoczenia | `46` | **brak** | `—` | Z definicji bez normy |
| Sonda za katalizatorem | `15` | **brak** | `—` | PID bez formuły w katalogu, sekcja 3.2 |

**Cztery parametry mają jawnie zapisane „brak normy" z uzasadnieniem.** To jest uczciwsza
odpowiedź niż pasmo wzięte z niczego, a użytkownik dowiaduje się przy okazji, dlaczego akurat
przy wyprzedzeniu zapłonu żadna liczba nie byłaby prawdziwa.

#### Skąd kod bierze te wartości

**Jedno miejsce: `PasmaOdniesienia`** — obiekt z tabelą powyżej. Reguły, kafle, linie na
wykresach, karty przeglądu i raport czytają **z niego**, nie z własnych kopii.

Duplikat progu w dwóch miejscach to gwarancja, że po pierwszej zmianie ekran i reguła zaczną
mówić co innego — a użytkownik zobaczy wartość „w paśmie" obok wniosku „poza normą".

---

## 9. Przegląd — dwie nowe karty

### 9.1 Karta „Wtrysk GDI"

```
┌───────────────────────────────────────────┐
│  WTRYSK GDI                               │
├───────────────────────────────────────────┤
│  Ciśnienie w szynie          38,4 bar     │
│    norma na jałowym       34 – 55 bar     │
│  Obciążenie absolutne          22 %       │
│    norma                          —       │
│  Obroty                       926 obr/min │
│    norma                          —       │
│                                           │
│  Stan pompy widać dopiero pod             │
│  obciążeniem — zobacz panel WTRYSK GDI    │
│  podczas jazdy.                           │
└───────────────────────────────────────────┘
```

**Zakres odniesienia pod wartością** jest treścią tej karty. Liczba bez niego nie mówi nic
komuś, kto nie wie, ile powinno być.

Zdanie na dole zostaje: przegląd tego nie rozstrzygnie i ma to powiedzieć wprost.

### 9.2 Karta „Katalizator"

```
┌───────────────────────────────────────────┐
│  KATALIZATOR                              │
├───────────────────────────────────────────┤
│  Temperatura, bank 1         453 °C       │
│    norma w jeździe      650 – 870 °C      │
│    zapłon od                 300 °C       │
│    (453 °C na jałowym jest poprawne —     │
│     pasmo dotyczy typowej jazdy)          │
│  Sonda za katalizatorem          —        │
│    PID 15 obsługiwany przez auto,         │
│    brak formuły w katalogu                │
│                                           │
│  Monitor katalizatora        gotowy       │
│  Monitor sond tlenu          gotowy       │
└───────────────────────────────────────────┘
```

### 9.3 Rozszerzenie karty „Odczyty"

Do grupy „Powietrze i dolot":

```
  Ciśnienie w kolektorze         34 kPa      norma  —
  Ciśnienie atmosferyczne        99 kPa      norma  —
  Podciśnienie (wyliczone)       65 kPa      norma  —
```

**Podciśnienie jest jedyną wartością liczoną, nie mierzoną** — różnica dwóch odczytów, oznaczona.

### 9.4 Cały ekran „Odczyty" podlega sekcji 8.8

Ekran odczytów wypisuje **wszystkie** obsługiwane PID-y z przecięcia katalogu i maski — na tym
aucie kilkanaście pozycji. Kontrakt pasm obowiązuje każdą z nich:

```
  Temperatura płynu chłodzącego   92 °C      norma  70 – 105
  Napięcie sterownika           13,9 V       norma  13,0 – 15,0
  Korekta długoterminowa        +3,9 %       norma  ±10
  Korekta krótkoterminowa       −0,8 %       norma  —
  Wyprzedzenie zapłonu            18 °       norma  —
  Pozycja przepustnicy          15,3 %       norma  —
  ...
```

**Kolumna „norma" nie ma prawa być pusta w żadnym wierszu.** Albo pasmo, albo `—`. Puste miejsce
wygląda jak przeoczenie i użytkownik nie wie, czy normy nie ma, czy zapomniano ją wpisać.

---

## 10. Reguły i alarmy — trzy nowe, ze źródłami

**Wszystkie z wagą `uwaga`** — sekcja 4.3. Wszystkie warunki zbudowane z **istniejących pojęć
projektu**, bez ani jednej nowej stałej warunkowej.

### 10.0 „Silnik rozgrzany" — jedna definicja dla obu kontekstów

Reguły `GDI-1` i `KAT-1` wymagają rozgrzanego silnika. **Nie wolno tu użyć
`OilTempEstimator.silnikRozgrzany`** — model oleju całkuje po czasie i istnieje wyłącznie
w trakcie nagrywania. Przegląd jest jednorazowym skanem, więc w jego kontekście model nie ma
stanu i warunek nigdy by się nie spełnił. **To jest błąd, który wersja robocza tego dokumentu
zawierała.**

Definicja obowiązująca w obu kontekstach, złożona wyłącznie z istniejących stałych projektu:

```
silnikRozgrzany  ≡  płyn 05 ≥ 70 °C  ∧  czas pracy 1F ≥ 600 s
```

- **70 °C** — istniejąca granica z reguły termostatu w tabeli 10.4 bazowego
  („Płyn < 70 °C przy czasie pracy > 10 min → uwaga").
- **600 s** — istniejący próg „rozgrzany" z modelu oleju, sekcja 8.5 bazowego.

Obydwa PID-y (`05`, `1F`) są czytane i w przeglądzie, i w nagrywaniu. **Zero nowych stałych.**

### 10.1 `GDI-1` — ciśnienie w szynie poniżej zakresu jałowego

| | |
|---|---|
| **Rejestr** | `RuleEngine` — werdykt przeglądu |
| **Warunek** | silnik rozgrzany **∧** prędkość `0D` = 0 **∧** obroty `0C` > 500 **∧** `23` < **27 bar** |
| **Waga** | uwaga |
| **Próg** | 34 bar (dolna granica zadanego na jałowym) **−** 7 bar (dopuszczalne odchylenie rzeczywistego od zadanego) = **27 bar** |
| **Źródło** | sekcja 4.1 |

**Tytuł:** Ciśnienie w szynie poniżej zakresu jałowego

**Treść:** *Ciśnienie 24,1 bar przy zakresie odniesienia 34–55 bar dla jałowego. Możliwe
słabnięcie pompy wysokiego ciśnienia albo niedobór po stronie niskiego ciśnienia. Zakres
pochodzi z dokumentacji branżowej dla silników GDI, nie z danych fabrycznych Hyundaia.*

**Definicja „silnik rozgrzany" dla reguł — sekcja 10.0.** Obroty > 500 to istniejący próg
„silnik pracuje" z tabeli 10.4 bazowego. Prędkość = 0 odróżnia jałowy od jazdy.

### 10.2 `KAT-1` — katalizator poniżej temperatury zapłonu

| | |
|---|---|
| **Rejestr** | `RuleEngine` — werdykt przeglądu |
| **Warunek** | silnik rozgrzany **∧** `3C` < **300 °C** |
| **Waga** | uwaga |
| **Próg** | 300 °C — minimum skuteczne dla konwersji |
| **Źródło** | sekcja 4.2 |

**Tytuł:** Katalizator poniżej temperatury zapłonu

**Treść:** *Katalizator 212 °C przy rozgrzanym silniku. Poniżej 300 °C konwersja praktycznie
nie zachodzi. Możliwa niesprawność katalizatora albo czujnika temperatury.*

### 10.3 `KAT-2` — katalizator powyżej normalnego zakresu

| | |
|---|---|
| **Rejestr** | **`AlertEngine`** — alarm na żywo, karencja 60 s |
| **Warunek** | `3C` > **870 °C** |
| **Waga** | uwaga |
| **Próg** | 870 °C — górna granica normalnego zakresu pracy |
| **Źródło** | sekcja 4.2 |

**Komunikat alarmu:** *Temperatura katalizatora powyżej normalnego zakresu*

`KAT-2` jest **alarmem, nie regułą przeglądu**, bo przegrzanie zdarza się w jeździe i jest
wtedy istotne — na postoju katalizator stygnie. Trafia do `AlertEngine` z karencją 60 s,
jak pozostałe alarmy wagi „uwaga" z tabeli 10.3 bazowego.

Nie ma warunku „silnik rozgrzany" — przegrzanie jest przegrzaniem niezależnie od tego,
jak długo trwa jazda.

### 10.4 Wymagane rozszerzenia struktur wejściowych

Nowe reguły potrzebują danych, których dotychczasowe struktury nie niosą:

| Struktura | Nowe pola | Dla kogo |
|---|---|---|
| `RuleInput` | `cisnienieSzynyBar`, `predkoscKmh`, `temperaturaKatalizatoraC` | `GDI-1`, `KAT-1` |
| `Report.ruleInput` | wypełnienie powyższych z `readings` (PID `23`, `0D`, `3C`) | j.w. |
| `AlertSnapshot` | `temperaturaKatalizatoraC` | `KAT-2` |

Wszystkie jako typy opcjonalne. **Brakująca wartość pomija regułę** — zasada z bazowego
obowiązuje bez zmian: reguła, która milczy przy braku danych, jest poprawna; reguła orzekająca
na podstawie wymyślonego zera jest szkodliwa.

### 10.5 Czego te reguły nie robią

Żadna nie mówi, **co jest zepsute**. `GDI-1` wymienia dwie możliwe przyczyny i nie rozstrzyga
między nimi, bo z jednego pomiaru nie da się. To jest granica kompetencji z reguły
`30-zrodla.mdc` i obowiązuje tak samo dla nowych reguł jak dla starych.

**Wagi `usterka` nie używa żadna z nich** — sekcja 4.3.

---

## 11. Raport sesji

### 11.1 Nowe pola podsumowania

Liczone przy zamknięciu sesji z gotowego przebiegu — czyste funkcje, bez ELM.

| Pole | Skąd | Po co |
|---|---|---|
| `maxCisnienieSzynyBar` | max serii `23` | Szczyt osiągnięty w przejeździe |
| `obciazeniePrzyMaxCisnieniu` | seria `43` **w chwili najbliższej czasowo** maksimum `23` | **Bez tego maksimum jest bezużyteczne** |
| `maxTempKatalizatoraC` | max serii `3C` | Przegrzew widać tu, nie w kodach |
| `czasDo90CSekundy` | pierwsza próbka `05` ≥ 90 °C od startu sesji | Diagnoza termostatu porównywalna między przejazdami |
| `czasPozaPasmemKorektSekundy` | suma **odstępów**, gdy \|`06` + `07`\| > **20 %** | Próg z reguły `trim_sum`, ten sam co pasmo wykresu sumy — sekcja 8.8. Odróżnia skok od stanu trwałego |

`null`, gdy odpowiedniej serii nie ma — niezmiennik 1 bazowego.

### 11.2 Nagłówek raportu

```
┌───────────────────────────────────────────────────────────┐
│  Przejazd 14 sierpnia, 17:42                      42 min  │
│  38,6 km   ·   śr. 4,0 Hz   ·   10 118 próbek             │
├───────────────────────────────────────────────────────────┤
│  Max obroty        4 210     Max płyn          94 °C ▼    │
│    norma                 —     norma        70 – 105      │
│  Max prędkość    118 km/h    Napięcie   13,8 – 14,3 V     │
│    norma                 —     norma      13,0 – 15,0     │
├───────────────────────────────────────────────────────────┤
│  DIAGNOSTYKA                                              │
│  Max ciśnienie szyny   148 bar  przy 84 % obciążenia      │
│    norma pod obciążeniem       138 – 241 bar              │
│  Max temp. katalizatora        712 °C                     │
│    norma w jeździe             650 – 870 °C               │
│  Płyn 90 °C po                6:24                        │
│    norma                          —                       │
│  Korekty poza pasmem ±20 %    0:00  z 42:11               │
├───────────────────────────────────────────────────────────┤
│  Kody na starcie: brak    ·    na końcu: brak             │
└───────────────────────────────────────────────────────────┘
```

**Zakresy odniesienia pod wartościami** zastępują regułę, której nie da się napisać (sekcja 5.1).
Użytkownik widzi 148 bar wobec spodziewanych 138–241 i ocenia sam — a to jest uczciwsze niż
próg z wymyślonym selektorem warunku.

### 11.3 Stos wykresów historycznych

Trzy nowe pasma na **wspólnym suwaku odczytu**:

```
┌───────────────────────────────────────────────────────────┐
│ 17:42                    ┊                          18:24 │
├───────────────────────────────────────────────────────────┤
│ OBROTY      ╱╲__╱‾╲___╱‾┊╲__╱╲____              2 140     │
├───────────────────────────────────────────────────────────┤
│ PRĘDKOŚĆ    __╱‾‾‾‾‾╲___┊‾‾╲______                78 km/h │
├───────────────────────────────────────────────────────────┤
│ CIŚN. SZYNY __╱‾╲__╱‾‾╲_┊╱‾╲______                94 bar  │
├───────────────────────────────────────────────────────────┤
│ KATALIZATOR ___╱‾‾‾‾‾‾‾‾┊‾‾‾‾‾‾‾‾‾               648 °C   │
├───────────────────────────────────────────────────────────┤
│ KOREKTA RAZ. ‾‾╲__╱‾╲___┊__╱‾╲____                +4,2 %  │
├───────────────────────────────────────────────────────────┤
│ PŁYN        __╱‾‾‾‾‾‾‾‾‾┊‾‾‾‾‾‾‾‾‾                92 °C   │
└───────────────────────────────────────────────────────────┘
```

**To jest miejsce, w którym rozszerzenie zarabia na siebie.** Przeciągnięcie suwaka do skoku
obrotów pokazuje jednocześnie ciśnienie w szynie, katalizator i reakcję korekt.

**Nowe pasma pochodzą z pętli średniej**, czyli mają około czterech razy mniej próbek niż
gorące. Suwak musi dla każdego pasma znaleźć wartość **najbliższą czasowo** — nie wolno
zakładać wspólnej siatki czasu.

Decymacja min-max obowiązuje dla nowych pasm tak samo jak dla istniejących.

---

## 12. Zmiany w kontraktach dokumentu bazowego

Wszystko poniżej jest **jawną zmianą**. Nic więcej się nie zmienia.

| Miejsce | Zmiana |
|---|---|
| §8.5 `SampleStream` | Trzeci poziom: pętla średnia, 6 PID, `n % 4 == 0` |
| §10.1 stałe | Jeden nowy wiersz: **`Pętla średnia n % 4 == 0`**. Fazy rotacji zimnej i odczytu `03` są już poprawione w bazowym — rozszerzenie ich **nie rusza** |
| §10.2 skład zapytań | Nowa tabela poziomu średniego: `23 3C 0B 11 43 44` |
| §10.3 progi alarmów | Nowy wiersz `KAT-2`: `3C` > 870 °C, uwaga, karencja 60 s |
| §10.4 progi reguł | Dwa nowe wiersze: `GDI-1`, `KAT-1` |
| §10.7 osie Y | Sześć nowych wierszy z sekcji 8.6 |
| §8.6 podsumowanie | Pięć nowych pól z sekcji 11.1 |
| §12.2 ekran żywy | Cztery panele zamiast jednego układu; **listwa kafli zyskuje trzeci wiersz z pasmem** |
| §12.1 interfejs | **Kontrakt pasm odniesienia — sekcja 8.8.** Obowiązuje wszystkie widoki, także istniejące: kafle, ekran odczytów w przeglądzie i nagłówek raportu |
| §12.4 bezpieczeństwo | `panelSwitch` dozwolony w ruchu |
| §12.5 historia | Trzy nowe pasma w stosie |
| §8.8 przegląd | Dwie nowe karty, rozszerzenie karty „Odczyty" |

**Czego nie zmieniamy:** pętli gorącej, jej częstotliwości, składu obowiązkowego `0D`, `05` i `04`,
istniejących progów alarmów i reguł, decymacji, checkpointu ani żadnej stałej z tabel 10.5–10.6.

---

## 13. Testy

| Obszar | Test | Dlaczego akurat ten |
|---|---|---|
| **Rozłączność wszystkich czterech poziomów** | Gorący, średni `n % 4 == 0`, zimny `n % 10 == 5`, odczyt `03` `n % 200 == 150` — **żaden cykl nie wykonuje trzech zapytań**, sprawdzone na **co najmniej 20 000 cykli** (mniej nie wykryje kolizji z odczytem `03`) | Sekcje 7.2 i 7.4. Pierwsza wersja tego dokumentu naprawiła jedną kolizję i zostawiła drugą |
| Pętla średnia | Wykonuje się dokładnie co czwarty cykl | |
| Sufit zapytań | Przy **czterech** poziomach suma nie przekracza 25/s | |
| **Nietykalność gorącej** | Skład i tempo pętli gorącej identyczne przed i po dodaniu poziomu średniego | Pilnuje zakazu z sekcji 6 |
| **`GDI-1`** | Odpala się przy 24 bar, silnik rozgrzany, prędkość 0, obroty 900. **Nie odpala się** przy tym samym ciśnieniu, gdy silnik nierozgrzany albo pojazd jedzie | Warunek ma trzy człony i każdy musi działać |
| **`KAT-1`** | Odpala przy 212 °C i rozgrzanym silniku; nie odpala przy nierozgrzanym | |
| **`KAT-2`** | Odpala przy 880 °C; karencja 60 s działa jak dla pozostałych alarmów „uwaga" | |
| **Waga nowych reguł** | Żadna z trzech nie ma wagi `usterka` | Sekcja 4.3 jest wiążąca, a wagę łatwo podnieść „żeby zauważył" |
| **Treść wniosków** | Każdy zawiera zakres odniesienia i wzmiankę o pochodzeniu | Bez tego liczba nic nie mówi |
| `maxCisnienieSzynyBar` | Maksimum serii `23`, `null` gdy serii brak | |
| **`obciazeniePrzyMaxCisnieniu`** | Dopasowanie **po czasie** — test na seriach o celowo różnej liczbie próbek | Dopasowanie po indeksie zadziała, dopóki któraś seria nie zgubi próbki, a wtedy skłamie bez objawu |
| **`czasPozaPasmemKorektSekundy`** | Suma **odstępów**, nie liczba próbek — test na nierównym próbkowaniu | Przy nierównym próbkowaniu to różne wielkości |
| `czasDo90CSekundy` | `null`, gdy nigdy nie osiągnięto | |
| **Kreska przy PID `15`** | Panel mieszanki i karta katalizatora pokazują `—` **z powodem**, nigdy `0` | |
| **Suwak na rzadkich pasmach** | Zwraca wartość najbliższą czasowo dla pasma o czterokrotnie mniejszej gęstości | |
| `panelSwitch` w ruchu | Dozwolony przy prędkości > 0; `parameterChange` w tym samym teście nadal zablokowany | Żeby było widać rozróżnienie, a nie poluzowanie |
| **Kompletność pasm** | **Każdy parametr wyświetlany gdziekolwiek ma wpis w `PasmaOdniesienia`** — pasmo albo jawne brak. Test przechodzi listę wyświetlanych PID-ów i sprawdza obecność wpisu | Bez tego nowy parametr dodany za pół roku pojawi się z pustą kolumną normy |
| **Jedno źródło progów** | Reguła `GDI-1` i linia odniesienia na panelu GDI czytają **tę samą stałą**; to samo dla `KAT-1`/`KAT-2` i panelu Termika | Duplikat progu gwarantuje, że po pierwszej zmianie ekran i reguła powiedzą co innego |
| **Pasmo ≠ alarm** | Wartość poza pasmem, ale spoza pięciu warunków krytycznych, **zmienia znacznik i nie wywołuje `AlertPlayer`** | Sekcja 8.8. Bez tego aplikacja piszczy przy każdym zimnym silniku |
| **Pasmo sumy korekt** | Wykres sumy i licznik czasu poza pasmem używają **±20 %**, nie ±10 % | ±10 % to próg korekty długoterminowej, nie sumy. Wersja robocza myliła te dwa |
| **Znacznik zamiast koloru** | Wartość powyżej pasma renderuje `▲`, poniżej `▼` — sprawdzane na tekście, nie na kolorze | Niezmiennik: kolor nigdy nie jest jedynym sygnałem |

---

## 14. Kolejność realizacji

Rozszerzenie wchodzi **po etapie 8 dokumentu bazowego**.

| Etap | Zawartość | Ukończony, gdy |
|---|---|---|
| **D1** | Pętla średnia + **przesunięcie rotacji zimnej** + testy | Test rozłączności: zero cykli z trzema zapytaniami |
| **D2** | Pięć pól podsumowania + testy | Dopasowanie po czasie, nie po indeksie |
| **D3** | **`PasmaOdniesienia` — wszystkie progi w jednym miejscu** + **trzy reguły** `GDI-1`, `KAT-1`, `KAT-2` + testy | Warunki złożone działają; żadna nie ma wagi `usterka`; test kompletności pasm przechodzi |
| **D4** | Panel Mieszanka | Pasmo ±10 %, licznik czasu, `—` przy PID `15` |
| **D5** | Panel Wtrysk GDI | Cztery linie odniesienia, max sesji z obciążeniem |
| **D6** | Panel Termika | Trzy linie odniesienia, trzy krzywe, czasy dojścia |
| **D7** | Przełączanie paneli + `panelSwitch` | Dozwolony w ruchu, `parameterChange` nie |
| **D8** | Dwie karty przeglądu **oraz kontrakt pasm na całym ekranie odczytów** | Żaden wiersz nie ma pustej kolumny normy |
| **D9** | Raport: blok `DIAGNOSTYKA` + trzy pasma | Suwak działa na pasmach o różnej gęstości |
| **D10** | Weryfikacja w aucie | Sekcja 15 |

Reguły są w **D3, przed panelami** — celowo. Są czystą logiką, testowalną bez interfejsu,
a panele mają rysować linie odniesienia z tych samych stałych, nie z własnych kopii.

---

## 15. Weryfikacja w aucie

| # | Co zrobić | Czego szukamy |
|---|---|---|
| 1 | Zimny start, jazda do pełnego rozgrzania | Kolejność krzywych na panelu Termika; czas dojścia płynu do 90 °C; **kiedy katalizator przekracza 300 °C** |
| 2 | **Jedno pełne otwarcie przepustnicy** na bezpiecznym odcinku | Czy ciśnienie szyny wchodzi w pasmo 138–241 bar. To jedyny test pompy, jaki mamy bez ciśnienia zadanego |
| 3 | Jazda miejska i trasa | Czy korekty zachowują się różnie przy różnym obciążeniu; czy katalizator trzyma się pasma 650–870 °C |
| 4 | ~~Sprawdzenie `2F` przy różnym poziomie paliwa~~ **ROZSTRZYGNIĘTE — poprawka P1.** `2F` zwraca zero niezależnie od stanu baku; wypadł z odpytywania i z wyświetlania. Krok wykreślony, nie do powtarzania | — |
| 4a | **Korekta długa `0107` na czwartym kaflu — czy odświeża się w jeździe** | Nowy odbiorca po P1. Ma się zmieniać przez minuty, nie stać w miejscu. Zamrożona wartość znaczy, że kafel czyta próbkę, która nie przychodzi |
| 4b | **Kafel przy pętli otwartej — czy pokazuje `— ○`, a nie ostatnią liczbę** | Rozgrzewanie i pełny gaz to stany, w których korekta nie znaczy nic. Wymaga `0103`, więc **sprawdzalne dopiero od etapu K2** |
| 5 | Zapis prawdziwych odpowiedzi pętli średniej do `MockI40Script` | Testy na rzeczywistych danych |

**Punkt 2 jest weryfikacją źródeł, nie tylko kodu.** Jeśli to auto pod pełnym obciążeniem nie
zbliża się do 138 bar, znaczy to albo że pompa słabnie, albo że **zakres z sekcji 4.1 nie
opisuje tego silnika** — i wtedy sekcję 4 trzeba przepisać, a nie naciągać do niej odczyt.

Punkt 4 dotyczy niezmiennika 1 bazowego i nie może zostać nierozstrzygnięty.

---

## 16. Źródła

Rozszerzenie wprowadza **osiem wartości liczbowych spoza katalogu PID-ów**. Wszystkie
w sekcji 4, wszystkie z podanym źródłem i poziomem wiarygodności.

| Wartość | Sekcja | Rodzaj źródła |
|---|---|---|
| 34–55 bar, 138–241 bar, 7 bar, 10 bar | 4.1 | Dokumentacja branżowa GDI |
| 150 bar / 200 bar dla Nu GDI | 4.1 | Opracowania o silniku |
| 300 °C, 650–870 °C, 600–900 °C | 4.2 | Opracowania branżowe o katalizatorach |

**Żadne z nich nie jest fabryczną daną Hyundaia dla G4NC.** Konsekwencja jest w sekcji 4.3:
wszystkie reguły mają wagę `uwaga`, każdy wniosek podaje zakres i jego pochodzenie, a dane
fabryczne — gdyby się pojawiły — unieważniają sekcję 4 w całości.

Pełne odsyłacze w `docs/zrodla.md`, sekcja „Wartości odniesienia rozszerzenia diagnostycznego".

**Dług źródłowy bez zmian:** formuły dla PID `15`, `32`, `34` i `56`, obsługiwanych przez auto
i nieobecnych w katalogu. Do odczytania z SAE J1979.
