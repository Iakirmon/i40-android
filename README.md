# headunit-obd2-logger

**OBD-II diagnostics that runs on the car's Android head unit — Hyundai i40 (2015, 2.0 GDI "Nu").**
**Diagnostyka OBD-II działająca na radiu z Androidem — Hyundai i40 (2015, 2.0 GDI „Nu").**

Repo: <https://github.com/Iakirmon/i40-android> · iOS original / pierwowzór: [`i40-check`](https://github.com/Iakirmon/ios-obd2-ble-diagnostics)

[**English**](#english) ([screens](#screens)) · [**Polski**](#polski) ([ekrany](#ekrany)) · [MIT](#license--licencja)

---

## English

### What this is, in one paragraph

An Android app that lives on the car's head unit instead of your phone. The head unit powers up with the ignition, so the app starts, connects to the ELM327 adapter and **begins logging on its own** — every trip is recorded with no phone in your hand, no cable and no START button. While you drive it shows live values; when you park it can run a full inspection; afterwards it keeps a browsable history of trips.

### Why port it from iOS at all

The iOS app works fine, but it needs you to pull the phone out, open it and press START. The head unit is already on. That is the entire reason for the port — **unattended recording**.

### The one rule about numbers

Every value is either a real measurement or an explicitly named model. Nothing is invented, nothing is rounded into existence.

- a failed read shows `—`, **never `0`**
- a modelled value (oil temperature) is prefixed with `~`
- a value that is meaningless right now (e.g. fuel trim in open loop) shows `○`

### Hardware and target

| | |
|---|---|
| Adapter | vLinker (ELM327) |
| Car bus | ISO 15765-4 CAN, 11-bit, 500 kbit/s |
| Screen | head unit 1280×720, landscape (PR9 / UIS7862) |

### How you use it

Every screen mentioned here is drawn out under [Screens](#screens).

**1. Turn the ignition on.** The head unit boots, a foreground service connects to the adapter and starts recording. There is nothing to tap.

**2. Recording tab** (the default screen). Four tiles at the top, one of six live panels below them, and a status bar at the bottom: elapsed time, poll rate in Hz, number of requests sent, and **Stop** — active only at a standstill while recording.

The four tiles are: oil temperature (model), coolant temperature, battery voltage, long-term fuel trim.

The six panels, swiped sideways:

| Panel | What it shows |
|---|---|
| `STAN` — Status | one-line verdict, or the list of out-of-band values (max 4 rows + "… and N more") |
| `PODSTAWOWY` — Basic | RPM, engine load, ignition timing — 60-second traces |
| `MIESZANKA` — Mixture | short and long fuel trim, EVAP purge, commanded lambda, closed/open loop state |
| `WTRYSK GDI` — GDI injection | fuel rail pressure against load and throttle |
| `TERMIKA` — Thermal | catalyst, coolant, oil (model), intake and ambient air |
| `POWIETRZE` — Air | computed manifold vacuum, commanded vs. actual throttle, pedal |

**3. While the car is moving.** Swiping between panels is allowed. Everything else — tabs, dictionary, inspection — is locked above 0 km/h.

**4. Tap any value to open the dictionary.** At a standstill, tapping a tile, a reading row, a chart caption or a report row slides up a sheet: *Now / Normal range / Previously*, then four plain-language sections. While moving, nothing opens.

**5. Inspection tab.** Standstill only. Runs a full pass and returns a verdict, GDI and catalyst cards, and a list of readings in *now / previously / normal* columns.

**6. History tab.** A calendar with a dot on every day that has trips, the trip list for a chosen day, session detail, side-by-side comparison of two trips, bulk cleanup by criteria, an all-time card and filters.

**7. Theme.** `NIGHT / DAY / AUTO`, night by default. Switching the theme **does not interrupt recording**.

### Safety: the speed lock

Above 0 km/h the app deliberately becomes almost inert — only panel swiping responds. Out-of-band values are flagged silently with `▲` / `▼`; crossing a band **does not beep**. Audible alarms fire for five critical conditions only.

### Symbols used everywhere

| Symbol | Meaning |
|---|---|
| `—` | no reading |
| `⌀` | read failed |
| `○` | inactive / meaningless right now (e.g. fuel trim in open loop) |
| `~` | model, not a measurement (oil) |
| `▲` `▼` | out of band (silent) |

### Build and test

```bash
./gradlew ktlintCheck
./gradlew lint
./gradlew test
```

### Project status

- Kotlin, Jetpack Compose, `minSdk` 31, `targetSdk` 34
- Everything above the transport layer is pure logic, driven by a mock: `MockI40Script`, a real capture taken from the car on 2026-08-08
- **Stage 9 — the real transport (SPP / BLE / Wi-Fi) is not written yet.** On the head unit the app currently runs on the mock transport
- Test bar: no fewer than **153** test functions (what the iOS app has). The port is already above that

### Documentation

| File | Role |
|---|---|
| `docs/spec/2026-08-14-i40-android-design.md` | base design |
| `docs/spec/*-{diagnostyka,kontekst,odniesienie,historia,objasnienia,wyglad}-*.md` | extension layers |
| `docs/slownik.md` | the 70 dictionary entries — **source text, never generated** |
| `docs/zrodla.md` | where every reference number comes from |
| `docs/weryfikacja-*.md` | checklists for the bench and for the car |
| `AGENTS.md`, `.cursor/rules/` | rules for AI agents |

What is true **right now** (poll composition, tiles, DB version): `.cursor/rules/00-projekt.mdc` → **STAN AKTUALNY**.

### Screens

Text mock-ups of the 1280×720 landscape layout, night theme. **The interface is in Polish** — the app is built for one Polish-speaking driver — so the labels below are Polish and each mock-up is followed by a translation.

#### Chrome — three tabs and the theme switch

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  Przegląd          [Nagrywanie]          Historia                            │
│  NOC   DZIEŃ   AUTO                                                          │
└──────────────────────────────────────────────────────────────────────────────┘
```

`Przegląd` = Inspection, `Nagrywanie` = Recording, `Historia` = History; `NOC / DZIEŃ / AUTO` = night / day / auto theme.

While moving, Inspection and History grey out; Recording stays.

#### Recording — tiles and the Status panel

```
┌────────────┬────────────┬────────────┬────────────┐
│  ~87 °C    │   92 °C    │  13,8 V    │   +2 %     │
│  OLEJ mod. │   PŁYN     │  NAPIĘCIE  │  KOREKTA D │
│  ≥ 80 · …  │  85–105    │  13,2–14,8 │ −10 – +10  │
└────────────┴────────────┴────────────┴────────────┘
● ○ ○ ○ ○ ○  STAN

                    Silnik rozgrzany
              wszystkie parametry w pasmach

              (albo lista odchyleń ▲ / ▼ —
               max 4 wiersze + „… i N dalsze")

┌──────────────────────────────────────────────────┐
│  12:34  4,0 Hz  842 zap.  ● NAGRYWA   Zatrzymaj  │
└──────────────────────────────────────────────────┘
```

Tiles, left to right: `OLEJ mod.` = oil (modelled), `PŁYN` = coolant, `NAPIĘCIE` = voltage, `KOREKTA D` = long-term trim. The third row of each tile is the normal band. `STAN` = Status panel; the text reads "engine warm, all parameters within their bands", otherwise a list of deviations (max 4 rows + "… and N more"). Status bar: `zap.` = requests, `NAGRYWA` = recording, `Zatrzymaj` = Stop.

Oil is a **model** (that is the `~`), not a measurement. Long-term trim outside closed loop reads `— ○`.

#### Recording — Basic panel, live charts

```
● ● ○ ○ ○ ○  PODSTAWOWY

  OBROTY ────────────────────────────────  2140
  ░░░░░░░░░░░░░░░░░/\/\/\____________░░░░   ← 60 s trace, fixed Y axis

  OBCIĄŻENIE ────────────────────────────  34 %
  ░░░░░░░░░░░░/‾‾\___________________░░░░

  ZAPŁON ────────────────────────────────  12,5°
  ░░░░░░░░░░░░░\/\___________________░░░░
```

`PODSTAWOWY` = Basic, `OBROTY` = RPM, `OBCIĄŻENIE` = load, `ZAPŁON` = ignition timing.

Grid lines are the band edges from `PasmaOdniesienia`, not round numbers. An empty field shows a scanning line — the only animation in the app.

#### Recording — the four remaining panels

```
○ ○ ● ○ ○ ○  MIESZANKA                         (Mixture)
  ● pętla zamknięta                            closed loop
  KOREKTA RAZEM  [wykres z cieniami: przedmuch / pętla otwarta]
                 combined trim, shaded while purging or in open loop
  KRÓTKA  +1,5 %     PRZEDMUCH.  12 %          short trim / EVAP purge
  DŁUGA   +2,0 %     LAMBDA ZAD. 1,000         long trim / commanded lambda

○ ○ ○ ● ○ ○  WTRYSK GDI                        (GDI injection)
  CIŚNIENIE SZYNY / OBCIĄŻENIE / PRZEPUSTNICA  rail pressure / load / throttle
  max … bar przy … % obciążenia                peak bar at … % load

○ ○ ○ ○ ● ○  TERMIKA                           (Thermal)
  KATALIZATOR / PŁYN / OLEJ (model)            catalyst / coolant / oil
  DOLOT …   OTOCZ. …                           intake … ambient …

○ ○ ○ ○ ○ ●  POWIETRZE                         (Air)
  podciśnienie (wyliczone)                     computed manifold vacuum
  przepustnica zadana · rzeczywista / pedał    commanded · actual throttle / pedal
```

#### Dictionary — the sheet that slides up

```
┌────────────────────────────────────────────┐
│  ✕                                         │
│  KOREKTA DŁUGOTERMINOWA                    │
│  Teraz      +2 %                           │
│  Norma      −10 – +10 %                    │
│  Poprzednio  +1 %  ·  3 pomiary −2–+4      │
│                                            │
│  CO TO JEST                                │
│  …tekst z docs/slownik.md…                 │
│  PO CO NA TO PATRZEĆ                       │
│  …                                         │
│  GDY WYJDZIE POZA PASMO                    │
│  …                                         │
│  CZEGO TO NIE MÓWI                         │
│  …                                         │
│  PID `0107` · poziom średni B · …          │
│                         wstecz / wróć…     │
└────────────────────────────────────────────┘
```

Header: `Teraz` = now, `Norma` = normal band, `Poprzednio` = previously (`3 pomiary` = 3 samples). The four sections are `CO TO JEST` = what it is, `PO CO NA TO PATRZEĆ` = why you would look at it, `GDY WYJDZIE POZA PASMO` = what it means when it leaves the band, `CZEGO TO NIE MÓWI` = what it does **not** tell you.

Entry text is **not generated** — it is carried over sentence for sentence from `docs/slownik.md` (70 entries).

#### Inspection

```
┌──────────────────────────────────────────────┐
│  Przegląd                                    │
│  Na postoju — pełny przegląd…                │
│  [ Uruchom przegląd ]                        │
│                                              │
│  Wszystko OK          (albo Uwaga / Usterka) │
│  KMHL…VIN…                                   │
│                                              │
│  WTRYSK GDI                                  │
│    Ciśnienie w szynie   32,1 bar             │
│    norma  30 – 40 bar                        │
│  KATALIZATOR                                 │
│    Temperatura, bank 1  420 °C               │
│  ODCZYTY                                     │
│    teraz   poprzednio    norma               │
│    Obroty  712    708    —                   │
│    … (do 33 wierszy)                         │
└──────────────────────────────────────────────┘
```

`Uruchom przegląd` = Run inspection. The verdict is one of `Wszystko OK` / `Uwaga` / `Usterka` — all clear, warning, fault. `ODCZYTY` = readings, in *teraz / poprzednio / norma* = now / previously / normal columns, up to 33 rows.

While moving the button is disabled and the speed-lock message is shown instead.

#### History — calendar and one day

```
┌──────────────────────────────────────────────┐
│  ◀  sierpień 2026  ▶     [Porządki] [Filtr]  │
│  Pn Wt Śr Cz Pt So Nd                        │
│     1  2● 3  4● 5  6  7                      │
│  …                                           │
│                                              │
│  4 sierpnia                                  │
│  ● 08:12  12,4 km  24 min                    │
│  ● 17:40   3,1 km   9 min   🔒               │
│                                              │
│  OD POCZĄTKU                                 │
│  dystans  1 240 km   …                       │
└──────────────────────────────────────────────┘
```

`Porządki` = cleanup, `Filtr` = filter, `OD POCZĄTKU` = all-time, `dystans` = distance, `🔒` = pinned and protected from cleanup.

Swiping a row sideways asks for delete confirmation — there is no undo. A session still in progress (`w_toku`) cannot be deleted.

---

## Polski

### O co chodzi — jeden akapit

Aplikacja na Androida, która siedzi w radiu samochodowym, a nie w telefonie. Radio wstaje razem z zapłonem, więc aplikacja startuje, łączy się z adapterem ELM327 i **sama zaczyna nagrywać** — każdy przejazd zapisuje się bez wyjmowania telefonu, bez kabla i bez klikania START. W trakcie jazdy pokazuje wartości na żywo, na postoju potrafi zrobić pełny przegląd, a po jeździe trzyma historię przejazdów do przeglądania.

### Po co w ogóle port z iOS

Aplikacja na iOS działa dobrze, ale wymaga wyjęcia telefonu, otwarcia i naciśnięcia START. Radio i tak już chodzi. To jest cały powód portu — **nagrywanie bez udziału kierowcy**.

### Jedna zasada o liczbach

Każda wartość to albo prawdziwy pomiar, albo jawnie nazwany model. Nic nie jest zmyślone ani „zaokrąglone do sensownej liczby".

- nieudany odczyt pokazuje `—`, **nigdy `0`**
- wartość z modelu (temperatura oleju) ma przedrostek `~`
- wartość, która teraz nic nie znaczy (np. korekta w pętli otwartej), pokazuje `○`

### Sprzęt i cel

| | |
|---|---|
| Adapter | vLinker (ELM327) |
| Magistrala | ISO 15765-4 CAN, 11-bit, 500 kbit/s |
| Ekran | radio 1280×720, poziomo (PR9 / UIS7862) |

### Jak się z tego korzysta

Każdy wymieniony tu ekran jest narysowany w rozdziale [Ekrany](#ekrany).

**1. Przekręć kluczyk.** Radio wstaje, usługa pierwszoplanowa łączy się z adapterem i wchodzi w nagrywanie. Nie trzeba nic klikać.

**2. Zakładka Nagrywanie** (ekran domyślny). U góry cztery kafle, pod nimi jeden z sześciu paneli na żywo, na dole pasek stanu: czas, częstotliwość odpytywania w Hz, liczba wysłanych zapytań i **Zatrzymaj** — aktywny tylko na postoju podczas nagrywania.

Cztery kafle to: temperatura oleju (model), temperatura płynu, napięcie, korekta długoterminowa.

Sześć paneli, przełączanych przesunięciem palca w bok:

| Panel | Co pokazuje |
|---|---|
| `STAN` | jednozdaniowy werdykt albo lista wartości poza pasmem (max 4 wiersze + „… i N dalsze") |
| `PODSTAWOWY` | obroty, obciążenie, kąt zapłonu — ślady 60-sekundowe |
| `MIESZANKA` | korekta krótko- i długoterminowa, przedmuch kanistra, lambda zadana, pętla zamknięta/otwarta |
| `WTRYSK GDI` | ciśnienie w szynie paliwowej wobec obciążenia i przepustnicy |
| `TERMIKA` | katalizator, płyn, olej (model), dolot, otoczenie |
| `POWIETRZE` | wyliczone podciśnienie, przepustnica zadana i rzeczywista, pedał |

**3. W ruchu.** Wolno przełączać panele. Reszta — zakładki, słownik, przegląd — jest zablokowana przy prędkości powyżej 0.

**4. Dotknięcie wartości otwiera słownik.** Na postoju dotknięcie kafla, wiersza odczytu, podpisu wykresu albo wiersza raportu wysuwa arkusz z dołu: *Teraz / Norma / Poprzednio*, a pod tym cztery rubryki napisane po ludzku. W ruchu nie otwiera się nic.

**5. Zakładka Przegląd.** Tylko na postoju. Uruchamia pełne badanie i zwraca werdykt, karty GDI i katalizatora oraz listę odczytów w kolumnach *teraz / poprzednio / norma*.

**6. Zakładka Historia.** Kalendarz z kropką przy każdym dniu z przejazdami, lista przejazdów wybranego dnia, szczegóły sesji, porównanie dwóch przejazdów, porządki (kasowanie według kryteriów), karta „od początku" i filtry.

**7. Motyw.** `NOC / DZIEŃ / AUTO`, domyślnie noc. Zmiana motywu **nie przerywa nagrywania**.

### Bezpieczeństwo: blokada prędkościowa

Powyżej 0 km/h aplikacja celowo staje się prawie bezwładna — reaguje tylko na przesuwanie paneli. Wartości poza pasmem są oznaczane po cichu znakami `▲` / `▼`; wyjście poza pasmo **nie piszczy**. Alarm dźwiękowy odzywa się tylko przy pięciu warunkach krytycznych.

### Znaczniki (wszędzie te same)

| Znak | Znaczenie |
|---|---|
| `—` | brak odczytu |
| `⌀` | odczyt nieudany |
| `○` | nieaktywne / bez znaczenia teraz (np. korekta w pętli otwartej) |
| `~` | model, nie pomiar (olej) |
| `▲` `▼` | poza pasmem (bez dźwięku) |

### Budowa i testy

```bash
./gradlew ktlintCheck
./gradlew lint
./gradlew test
```

### Stan projektu

- Kotlin, Jetpack Compose, `minSdk` 31, `targetSdk` 34
- Wszystko powyżej warstwy transportu to czysta logika napędzana atrapą: `MockI40Script`, prawdziwy zapis z auta z 2026-08-08
- **Etap 9 — właściwy transport (SPP / BLE / Wi-Fi) jeszcze nie powstał.** Na radiu aplikacja chodzi na razie na atrapie
- Próg testów: nie mniej niż **153** funkcje testowe (tyle ma iOS). Port jest już powyżej tego progu

### Dokumentacja

| Plik | Rola |
|---|---|
| `docs/spec/2026-08-14-i40-android-design.md` | projekt bazowy |
| `docs/spec/*-{diagnostyka,kontekst,odniesienie,historia,objasnienia,wyglad}-*.md` | warstwy rozszerzeń |
| `docs/slownik.md` | treść 70 haseł — **źródło, nie generować** |
| `docs/zrodla.md` | skąd wzięła się każda liczba odniesienia |
| `docs/weryfikacja-*.md` | checklisty na biurko i do auta |
| `AGENTS.md`, `.cursor/rules/` | zasady dla agentów AI |

Stan obowiązujący **teraz** (skład pętli, kafle, wersja bazy): `.cursor/rules/00-projekt.mdc` → **STAN AKTUALNY**.

### Ekrany

Makiety tekstowe układu 1280×720 poziomo, motyw nocny.

#### Belka — trzy zakładki i przełącznik motywu

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  Przegląd          [Nagrywanie]          Historia                            │
│  NOC   DZIEŃ   AUTO                                                          │
└──────────────────────────────────────────────────────────────────────────────┘
```

W ruchu zakładki Przegląd i Historia są wyszarzone, zostaje Nagrywanie.

#### Nagrywanie — kafle i panel Stan

```
┌────────────┬────────────┬────────────┬────────────┐
│  ~87 °C    │   92 °C    │  13,8 V    │   +2 %     │
│  OLEJ mod. │   PŁYN     │  NAPIĘCIE  │  KOREKTA D │
│  ≥ 80 · …  │  85–105    │  13,2–14,8 │ −10 – +10  │
└────────────┴────────────┴────────────┴────────────┘
● ○ ○ ○ ○ ○  STAN

                    Silnik rozgrzany
              wszystkie parametry w pasmach

              (albo lista odchyleń ▲ / ▼ —
               max 4 wiersze + „… i N dalsze")

┌──────────────────────────────────────────────────┐
│  12:34  4,0 Hz  842 zap.  ● NAGRYWA   Zatrzymaj  │
└──────────────────────────────────────────────────┘
```

Trzeci wiersz każdego kafla to pasmo normy. Olej to **model** (tylda `~`), nie pomiar. Korekta długa poza pętlą zamkniętą pokazuje `— ○`.

#### Nagrywanie — panel Podstawowy, wykresy na żywo

```
● ● ○ ○ ○ ○  PODSTAWOWY

  OBROTY ────────────────────────────────  2140
  ░░░░░░░░░░░░░░░░░/\/\/\____________░░░░   ← ślad 60 s, oś Y sztywna

  OBCIĄŻENIE ────────────────────────────  34 %
  ░░░░░░░░░░░░/‾‾\___________________░░░░

  ZAPŁON ────────────────────────────────  12,5°
  ░░░░░░░░░░░░░\/\___________________░░░░
```

Linie siatki to granice pasm z `PasmaOdniesienia`, nie „okrągłe" wartości. Puste pole pokazuje linię skanującą — jedyną animację w aplikacji.

#### Nagrywanie — pozostałe cztery panele

```
○ ○ ● ○ ○ ○  MIESZANKA
  ● pętla zamknięta
  KOREKTA RAZEM  [wykres z cieniami: przedmuch / pętla otwarta]
  KRÓTKA  +1,5 %     PRZEDMUCH.  12 %
  DŁUGA   +2,0 %     LAMBDA ZAD. 1,000

○ ○ ○ ● ○ ○  WTRYSK GDI
  CIŚNIENIE SZYNY / OBCIĄŻENIE / PRZEPUSTNICA
  max … bar przy … % obciążenia

○ ○ ○ ○ ● ○  TERMIKA
  KATALIZATOR / PŁYN / OLEJ (model)
  DOLOT …   OTOCZ. …

○ ○ ○ ○ ○ ●  POWIETRZE
  podciśnienie (wyliczone)
  przepustnica zadana · rzeczywista / pedał
```

#### Słownik — arkusz wysuwany z dołu

```
┌────────────────────────────────────────────┐
│  ✕                                         │
│  KOREKTA DŁUGOTERMINOWA                    │
│  Teraz      +2 %                           │
│  Norma      −10 – +10 %                    │
│  Poprzednio  +1 %  ·  3 pomiary −2–+4      │
│                                            │
│  CO TO JEST                                │
│  …tekst z docs/slownik.md…                 │
│  PO CO NA TO PATRZEĆ                       │
│  …                                         │
│  GDY WYJDZIE POZA PASMO                    │
│  …                                         │
│  CZEGO TO NIE MÓWI                         │
│  …                                         │
│  PID `0107` · poziom średni B · …          │
│                         wstecz / wróć…     │
└────────────────────────────────────────────┘
```

Treść haseł **nie jest generowana** — przeniesiona co do zdania z `docs/slownik.md` (70 haseł).

#### Przegląd

```
┌──────────────────────────────────────────────┐
│  Przegląd                                    │
│  Na postoju — pełny przegląd…                │
│  [ Uruchom przegląd ]                        │
│                                              │
│  Wszystko OK          (albo Uwaga / Usterka) │
│  KMHL…VIN…                                   │
│                                              │
│  WTRYSK GDI                                  │
│    Ciśnienie w szynie   32,1 bar             │
│    norma  30 – 40 bar                        │
│  KATALIZATOR                                 │
│    Temperatura, bank 1  420 °C               │
│  ODCZYTY                                     │
│    teraz   poprzednio    norma               │
│    Obroty  712    708    —                   │
│    … (do 33 wierszy)                         │
└──────────────────────────────────────────────┘
```

Werdykt to jedno z: `Wszystko OK` / `Uwaga` / `Usterka`. Odczytów do 33 wierszy. W ruchu przycisk jest nieaktywny, z komunikatem o blokadzie prędkościowej.

#### Historia — kalendarz i wybrany dzień

```
┌──────────────────────────────────────────────┐
│  ◀  sierpień 2026  ▶     [Porządki] [Filtr]  │
│  Pn Wt Śr Cz Pt So Nd                        │
│     1  2● 3  4● 5  6  7                      │
│  …                                           │
│                                              │
│  4 sierpnia                                  │
│  ● 08:12  12,4 km  24 min                    │
│  ● 17:40   3,1 km   9 min   🔒               │
│                                              │
│  OD POCZĄTKU                                 │
│  dystans  1 240 km   …                       │
└──────────────────────────────────────────────┘
```

`🔒` = przejazd przypięty, porządki go nie ruszają. Gest w bok na wierszu otwiera okno potwierdzenia kasowania, bez cofnięcia. Sesja `w_toku` nie kasuje się.

---

## License · Licencja

MIT.
