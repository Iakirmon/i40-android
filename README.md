# i40-android

Aplikacja diagnostyczna OBD-II na **radio z Androidem** w Hyundaiu i40 (2015, 2.0 GDI Nu).

Port działającej aplikacji [`i40-check`](https://github.com/Iakirmon/ios-obd2-ble-diagnostics) z iOS.
Powód portu jest jeden: **radio wstaje z zapłonem**, więc każdy przejazd nagrywa się sam — bez wyjmowania telefonu i bez START.

> Każda liczba pochodzi z pomiaru albo z nazwanego modelu. Nieudany odczyt mówi „—", nigdy „0".

---

## Co robi — w skrócie

| Na postoju / w ruchu | Co się dzieje |
|---|---|
| Zapłon | Usługa pierwszoplanowa łączy się z adapterem ELM327 i **sama zaczyna nagrywać** |
| Jazda | Cztery kafle + sześć paneli na żywo (wykresy 60 s); alarmy dźwiękowe tylko przy 5 krytycznych warunkach |
| Postój | Pełny **przegląd** (kody DTC, monitory, odczyty z normą i „poprzednio") |
| Po jeździe | **Historia** przejazdów, porównanie, porządki, karta „od początku" |
| Dotknięcie liczby | **Słownik** — wysuwany arkusz z wyjaśnieniem (70 haseł); w ruchu się nie otwiera |

Adapter: vLinker (ELM327). Auto: ISO 15765-4 CAN 11-bit 500 kbit/s.  
Ekran docelowy: radio 1280×720 poziomo (PR9 / UIS7862).

---

## Krok po kroku — jak z tego korzystać

### 1. Zapłon

Radio wstaje. Aplikacja (usługa pierwszoplanowa) łączy się z adapterem i wchodzi w nagrywanie. Nie trzeba nic klikać.

### 2. Zakładka **Nagrywanie** (domyślna)

To ekran „żywy". U góry cztery kafle, pod nimi jeden z sześciu paneli (przesuw palcem w bok). Na dole pasek: czas, Hz, liczba zapytań, **Zatrzymaj** (tylko na postoju, gdy nagrywa).

### 3. Panele w ruchu

Przełączanie paneli **wolno w ruchu**. Reszta interakcji (zakładki, słownik, przegląd) jest zablokowana przy prędkości > 0.

### 4. Dotknięcie wartości → słownik

Na postoju: kafel, wiersz odczytu, podpis wykresu, wiersz raportu → arkusz z dołu (Teraz / Norma / Poprzednio + rubryki). W ruchu: nic się nie otwiera.

### 5. Zakładka **Przegląd**

Tylko na postoju. Uruchom pełny przegląd → werdykt, karty GDI / katalizator, lista odczytów z kolumnami teraz / poprzednio / norma.

### 6. Zakładka **Historia**

Kalendarz z kropkami dni, lista przejazdów dnia, szczegóły sesji, porównanie dwóch, porządki (kasowanie według kryteriów), karta „od początku", filtry.

### 7. Motyw

Przełącznik **NOC / DZIEŃ / AUTO** (NOC domyślny). Zmiana motywu **nie przerywa** nagrywania.

---

## Makiety widoków (tekst)

Układ jak na radiu 1280×720. Motyw NOC (ciemne tło).

### Chrome — trzy zakładki + motyw

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  Przegląd          [Nagrywanie]          Historia                            │
│  NOC   DZIEŃ   AUTO                                                          │
└──────────────────────────────────────────────────────────────────────────────┘
```

W ruchu zakładki Przegląd / Historia są wyszarzane; Nagrywanie zostaje.

---

### Nagrywanie — kafle + panel Stan (pierwszy)

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
               max 4 wiersze + „… i N dalsze”)

┌──────────────────────────────────────────────────┐
│  12:34   4,0 Hz   842 zap.   ● NAGRYWA    Zatrzymaj │
└──────────────────────────────────────────────────┘
```

Olej jest **modelem** (tylda `~`), nie pomiarem. Korekta długa poza pętlą zamkniętą: `— ○`.

---

### Nagrywanie — panel Podstawowy (wykresy)

```
● ● ○ ○ ○ ○  PODSTAWOWY

  OBROTY ────────────────────────────────  2140
  ░░░░░░░░░░░░░░░░░/\/\/\____________░░░░   ← ślad 60 s, oś Y sztywna

  OBCIĄŻENIE ────────────────────────────  34 %
  ░░░░░░░░░░░░/‾‾\___________________░░░░

  ZAPŁON ────────────────────────────────  12,5°
  ░░░░░░░░░░░░░\/\___________________░░░░
```

Linie siatki = granice pasm z `PasmaOdniesienia`, nie „okrągłe" wartości. Puste pole: linia skanująca (jedyna animacja).

---

### Nagrywanie — Mieszanka / Wtrysk GDI / Termika / Powietrze

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
  podciśnienie (wyliczone) / przepustnica zadana·rzecz. / pedał
```

---

### Słownik (arkusz z dołu)

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

---

### Przegląd

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

W ruchu: przycisk nieaktywny + komunikat o blokadzie prędkościowej.

---

### Historia — kalendarz i dzień

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

Gest w bok na wierszu → okno potwierdzenia kasowania (bez cofnięcia). `w_toku` nie kasuje się.

---

### Znaczniki (wszędzie)

| Znak | Znaczenie |
|---|---|
| `—` | brak odczytu |
| `⌀` | odczyt nieudany |
| `○` | nieaktywne / nieważne teraz (np. korekta w pętli otwartej) |
| `~` | model, nie pomiar (olej) |
| `▲` `▼` | poza pasmem (bez dźwięku) |

Alarm dźwiękowy tylko przy pięciu warunkach krytycznych — przekroczenie pasma **nie** piszczy.

---

## Budowa i testy

```bash
./gradlew ktlintCheck
./gradlew lint
./gradlew test
```

- Kotlin, `minSdk` 31, `targetSdk` 34, Jetpack Compose  
- Powyżej transportu: czysta logika + atrapa (`MockI40Script` — zapis z auta 2026-08-08)  
- **Etap 9** (SPP / BLE / Wi-Fi) — jeszcze do zrobienia; na radiu na razie transport atrapy  

Cel testów: nie mniej niż **153** funkcji (tyle ma iOS). Port jest już powyżej tego progu.

---

## Dokumentacja projektu

| Plik | Rola |
|---|---|
| `docs/spec/2026-08-14-i40-android-design.md` | projekt bazowy |
| `docs/spec/*-diagnostyka|kontekst|odniesienie|historia|objasnienia|wyglad-*.md` | warstwy rozszerzeń |
| `docs/slownik.md` | treść 70 haseł (źródło, nie generować) |
| `docs/zrodla.md` | bibliografia liczb |
| `docs/weryfikacja-*.md` | checklisty na radio / w aucie |
| `AGENTS.md` / `.cursor/rules/` | zasady dla agentów |

Stan obowiązujący „teraz" (skład pętli, kafle, wersja bazy): `.cursor/rules/00-projekt.mdc` → **STAN AKTUALNY**.

---

## Licencja

MIT.
