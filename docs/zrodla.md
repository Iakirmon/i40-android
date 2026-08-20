# Bibliografia — i40-android

Liczba, której nie da się zaczepić w jednym z tych źródeł, **nie istnieje**.

Przy rozbieżności obowiązuje hierarchia z `.cursor/rules/30-zrodla.mdc`: kod iOS wygrywa
ze specyfikacją iOS, bo to on jeździł samochodem.

---

## 1. Kod źródłowy wersji iOS — źródło pierwszego wyboru

**`C:\Macrologic\SVN\priv\_github\ios-obd2-ble-diagnostics`**
Publicznie: `github.com/Iakirmon/ios-obd2-ble-diagnostics` (MIT)

46 plików źródłowych Swift (około 7900 linii) i 30 plików testowych ze **153 funkcjami testowymi**. Aplikacja działa na iPhonie
i była weryfikowana na tym konkretnym samochodzie.

Pliki, z których pochodzą wartości liczbowe tego projektu:

| Plik | Co z niego bierzemy |
|---|---|
| `OBD/PIDCatalog.swift` | Tablica definicji PID-ów — sekcja 9.1 specu |
| `OBD/PID.swift` | Funkcje dekodujące i zakresy fizyczne |
| `OBD/DTC.swift` + `Resources/DTCDictionary.json` | Dekodowanie kodów i słownik 41 opisów |
| `OBD/VIN.swift` | Kodowanie rocznika i tablica zakładów |
| `OBD/Readiness.swift` | Układ bitów monitorów gotowości |
| `OBD/SupportedPIDs.swift` | Maski i odejmowanie bitów kontynuacji |
| `ELM/ELMSession.swift` | Ramkowanie, pompa poleceń, ponowienia |
| `Acquisition/SampleStream.swift` | **Wszystkie bezpieczniki pętli — tabela 10.1** |
| `Acquisition/OilTempEstimator.swift` | **Stałe modelu oleju — sekcja 8.5** |
| `Alerts/AlertEngine.swift` | **Progi alarmów i karencje — tabela 10.3** |
| `Rules/RuleEngine.swift` | **Progi reguł przeglądu — tabela 10.4** |
| `Charts/Decimator.swift` | Algorytm decymacji min-max |
| `Storage/TrackBlob.swift`, `SessionSummaryCalculator.swift` | Format przebiegów i podsumowanie |
| `Transport/GATTPairFinder.swift` | Kolejność wyboru pary charakterystyk |
| `App/AppState.swift` | **Sekwencja przeglądu i timeout 25 s — sekcja 8.8** |

---

## 2. Zapis z prawdziwego auta — źródło prawdy o tym egzemplarzu

**`Transport/MockI40Script.swift`**, zapis z **2026-08-08**, vLinker MC-IOS, ELM327 v2.2.

Powstał przez „Diagnostyka → Kopiuj zapis z auta" w działającej aplikacji na iPhonie.
To jest odpowiednik złotego korpusu z `fa3-check`: **bez niego atrapa jest makietą, a testy
sprawdzają wyobrażenie zamiast rzeczywistości.**

Kluczowe odpowiedzi:

| Zapytanie | Odpowiedź | Co z niej wiemy |
|---|---|---|
| `0100` | `4100BE3EA813` | maska PID-ów `01`–`20` |
| `0120` | `4120A007F011` | maska PID-ów `21`–`40` |
| `0140` | `4140FED00400` | maska `41`–`60`, **bez bitu kontynuacji** |
| `ATDPN` | `A6` | ISO 15765-4 CAN 11-bit 500 kbit/s |
| `ATRV` | `14.2V` | napięcie na pinie 16 |
| `0902` | `KMHLC41DAFU066558` | VIN |
| `0904` | `GGVF-EE5AFS01600` | identyfikator kalibracji |
| `090A` | `ECM-EngineControl` | nazwa sterownika |
| `015C` | `NO DATA` | **temperatura oleju nie istnieje na tym ECU** |
| `0A` | `NO DATA` | **kody trwałe nieobsługiwane** |

Maski zdekodowane w sekcji 3.2 specu. **`5C`, `5E` i `10` nie są obsługiwane; `2F` jest —
w masce.**

⚠️ **`012F` jest w masce, a mimo to nie daje danych.** Zwraca zero niezależnie od stanu baku:
tak w zapisie z auta z 8 sierpnia, tak w wersji iOS, i tak potwierdza właściciel pojazdu
(2026-08-16). To **obserwacja, nie pomiar z dokumentu** — i jedyne miejsce w tej bibliografii,
gdzie maska rozmija się z rzeczywistością. Konsekwencje: poprawka P1 w `.cursor/rules/00-projekt.mdc`.

---

## 3. Specyfikacje wersji iOS — źródło kontekstu, nie wartości

**`C:\Macrologic\SVN\priv\i40-check\docs\superpowers\specs\`**

| Dokument | Zawartość |
|---|---|
| `2026-08-06-obd-i40-design.md` | Pierwotny projekt PWA. ⚠️ **Stąd — i wyłącznie stąd — pochodzi twierdzenie o „dual-mode SPP + BLE". Niesprawdzone, patrz niżej** |
| `2026-08-07-i40-check-ios-design.md` | Aplikacja bazowa: przegląd, reguły, katalog PID-ów |
| `2026-08-08-i40-check-rejestrator-design.md` | Rejestrator: pętla, alarmy, historia, decymacja |

⚠️ **Ostrzeżenie.** Te dokumenty **rozjeżdżają się z kodem w siedmiu miejscach** — sekcja 3.1
specu. Trzy najgroźniejsze: obiecują zamrożoną ramkę, której nie ma; podają timeout 5 s zamiast
25 s; nie wiedzą, że PID `5C` na tym aucie nie odpowiada.

Używaj ich do zrozumienia **dlaczego** coś jest, nigdy do ustalenia **ile**.

---

## 4. Standardy

**SAE J1979** — OBD-II Diagnostic Test Modes. Źródło formuł przeliczeniowych trybu 01
i struktury kodów DTC.

Dostęp jest płatny, dlatego **operacyjnym źródłem formuł jest `PIDCatalog.swift`**, który został
z niego wyprowadzony i przetestowany na aucie. Formuła spoza tej tablicy nie wchodzi do projektu
bez potwierdzenia.

**ISO 15765-4** — CAN diagnostyczny, 11-bit, 500 kbit/s. Protokół potwierdzony odpowiedzią
`ATDPN` = `A6`. Istotny o tyle, że **ramka CAN mieści 7 bajtów danych**, więc odpowiedź na sześć
PID-ów jest prawie zawsze wieloramkowa.

**Karta katalogowa ELM327 v2.2** (Elm Electronics) — polecenia `AT`, komunikaty tekstowe,
znak zachęty `>`, brak potokowania. Zachowanie adaptera weryfikowane zapisem z punktu 2.

---

## 5. Sprzęt docelowy

**Radio:** zamówienie **#124599** z 2026-08-13, autonawigacje.pl, 1549 zł.
Karta produktu: `RADIO NAWIGACJA GPS HYUNDAI I40 2011-2017 ANDROID 8/128 GB`,
SKU `PR9 8/128 RDS1 HY 259N`.

Parametry z karty produktu: UIS7862, 8 × Cortex-A55 @ 2,0 GHz, GPU PowerVR Rogue GE8322,
Android 14, 8 GB RAM / 128 GB eMMC, ekran 8" 1280 × 720, 4 × 48 W, tuner TDA7708,
Wi-Fi / GPS / Bluetooth 5.0 / 4G na kartę SIM, 2 × USB, brak slotu SD, gwarancja 24 miesiące.

Zastrzeżenie ze sprzedawcy: plug & play dla wersji z fabrycznym radiem; wersje z fabryczną
kamerą cofania albo nagłośnieniem **Infinity** wymagają adaptera CANBUS (200 zł).

**⚠️ Drugie źródło o tym samym sprzęcie, niezgodne z pierwszym.** Instrukcja obsługi dołączona
do radia — marka **SMART-AUTO**, stopka **FSC © 2022** — podaje:

| Parametr | Karta produktu | Instrukcja z pudełka |
|---|---|---|
| System | Android **14** | Android **13** |
| Przekątna | **8"** | **9" albo 10"** |
| SoC | UIS7862, 8 × A55 @ 2,0 GHz | 8 × A55 @ 2,0 GHz — **zgodne** |
| Pamięć | 8 / 128 GB | 8 / 128 GB — **zgodne** |
| Rozdzielczość | 1280 × 720 | 1280 × 720 — **zgodne** |

Hipoteza, **nie ustalenie**: radia tej klasy pochodzą od jednego producenta i są sprzedawane pod
wieloma markami, a instrukcja opisuje rodzinę produktów, nie egzemplarz.

**Żadne z tych dwóch źródeł nie jest urządzeniem.** Rozstrzygają kroki **5a** i **5b** z §15.1 B
dokumentu bazowego — odczyt gęstości i wersji systemu na prawdziwym radiu. Do tego czasu obowiązuje
hierarchia z `.cursor/rules/30-zrodla.mdc`: **pomiar bije kartę produktu, karta produktu bije
instrukcję** (nowsza i dotyczy konkretnego SKU).

Instrukcja wnosi też dwa **ustalenia funkcjonalne**, których karta produktu nie ma:

- *„Wciśnięcie przycisku Powrót do ekranu głównego zminimalizuje aplikację […] aplikacja będzie
  działać w tle"* — **to radio pozwala aplikacjom pracować w tle**; największe ryzyko projektu
  jest przynajmniej wstępnie potwierdzone jako wykonalne.
- *„Powrót do poprzedniego ekranu **zamyka aplikację**"* — przycisk wstecz kończy aplikację,
  a nie cofa nawigację. Jeśli to się potwierdzi (krok 6a), **odzyskiwanie sesji przestaje być
  ścieżką awaryjną i staje się codzienną**.

**Adapter:** vLinker, ELM327 v2.2. UUID-y GATT odczytane z urządzenia: usługa `18F0`,
zapis `2AF1`, notyfikacja `2AF0`.

⚠️ **Nazwa wariantu jest w źródłach niespójna.** `MockI40Script.swift`, `BLETransport.swift`
i `DetailViews.swift` mówią **`MC-IOS`**; teksty interfejsu mówią `MC+`. Rozgłoszeniowa nazwa
prawdziwego urządzenia zawiera `MC-IOS` — inaczej punktacja kandydatów w `BLETransport.swift:282`
nie miałaby sensu. Twierdzenie o „dual-mode SPP + BLE" pochodzi wyłącznie ze specyfikacji
z 6 sierpnia, pisanej dla PWA na MacBooku **przed** scharakteryzowaniem adaptera, i nigdy nie
zostało sprawdzone. Sekcja 3.4 specu.

**Auto:** Hyundai i40, rocznik 2015, 2.0 GDI (Nu G4NC), VIN `KMHLC41DAFU066558`,
kalibracja `GGVF-EE5AFS01600`.

---

## 6. Dokumentacja platformy

Android Developers — części, które faktycznie zmieniają projekt:

| Temat | Dlaczego istotne |
|---|---|
| Foreground service types | Android 14 wymaga `foregroundServiceType`; bez `connectedDevice` `startForeground` rzuca wyjątkiem |
| Ograniczenia startu usług z `BOOT_COMPLETED` | Część typów jest wykluczona; `connectedDevice` nie jest — **do zweryfikowania na tym radiu** |
| Uprawnienia Bluetooth od Androida 12 | `BLUETOOTH_CONNECT` wystarcza, gdy aplikacja **nie skanuje** |
| `BluetoothDevice.createRfcommSocketToServiceRecord` | SPP; UUID `00001101-0000-1000-8000-00805F9B34FB` |
| `AudioManager.requestAudioFocus` | `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` — alarm przycisza nawigację zamiast ją zabijać |

**Uwaga:** dokumentacja opisuje czysty Android. Producenci radi samochodowych modyfikują
zachowanie usług w tle i autostartu, więc **wszystko z tej tabeli podlega weryfikacji na
sprzęcie** — sekcja 15 specu.

---

## 7. Wartości odniesienia rozszerzenia diagnostycznego

Osiem wartości liczbowych spoza katalogu PID-ów, użytych w regułach `GDI-1`, `KAT-1` i `KAT-2`
oraz jako linie odniesienia na wykresach. Sekcja 4 rozszerzenia
`2026-08-14-i40-android-diagnostyka-design.md`.

### 7.1 Ciśnienie w szynie GDI

**Apex Tech Nation — „GDI Fuel System Diagnosis"**
<https://apextechnation.com/articles/gdi-fuel-system-diagnosis>

Odczytane wprost z artykułu:

- *„At idle on most GDI systems, desired pressure runs 500-800 PSI."* → **34–55 bar**
- *„Under load, it ramps up to 2,000-3,500 PSI."* → **138–241 bar**
- *„During cranking or prime, it may start lower (around 200-400 PSI)"* → 14–28 bar
- Progi diagnostyczne: na jałowym *„actual more than 100 PSI below desired"* → **7 bar**;
  pod obciążeniem *„actual drops or fluctuates more than 150 PSI"* → 10 bar

**Zdanie, które ukształtowało sekcję 5.1 rozszerzenia:**

> *„If actual pressure tracks desired pressure closely across all operating conditions, the HPFP
> is doing its job. If actual consistently lags behind desired, or drops under load, the HPFP
> is the primary suspect."*

Właściwą metodą jest porównanie **rzeczywistego z zadanym**. Ciśnienie zadane to PID
producencki Hyundaia, poza trybem 01 — więc tej metody **nie mamy** i reguła `GDI-1` jest
świadomie gorszym zastępnikiem.

**Uzupełniająco:**
<https://apextechnation.com/articles/gasoline-direct-injection-gdi> ·
<https://www.vehicleservicepros.com/service-repair/article/55329104/modern-gdi>

### 7.2 Silnik Hyundai Nu GDI

<https://en.wikipedia.org/wiki/Hyundai_Nu_engine> ·
<https://www.motor-car.net/innovation/engines/1304-kia-engines/19996-hyundai-kia-nu>

Ciśnienie wtrysku **150 bar**, pompa wysokiego ciśnienia do **200 bar**. Zgodne z pasmem
obciążeniowym z 7.1 i z odczytem 38,4 bar na jałowym z tego egzemplarza.

### 7.3 Temperatura katalizatora

<https://www.aa1car.com/library/converter.htm> ·
<https://engineerskill.blog/how-hot-does-a-catalytic-converter-get>

- Temperatura zapłonu, minimum skuteczne dla konwersji: **300 °C**
- Normalny zakres pracy przy typowej jeździe: **650–870 °C** (1200–1600 °F)
- Typowy odczyt **przed** katalizatorem: 600–900 °C; za katalizatorem 300–400 °C

PID `3C` to czujnik przed katalizatorem (bank 1, czujnik 1).

### 7.4 Poziom wiarygodności — powiedziane wprost

**To są źródła branżowe i encyklopedyczne, nie fabryczne dane Hyundaia dla G4NC.**
Konsekwencje są wiążące i opisane w sekcji 4.3 rozszerzenia:

- wszystkie trzy reguły mają wagę **`uwaga`**, nigdy `usterka`,
- każdy wniosek podaje zakres odniesienia **i jego pochodzenie**,
- **dane fabryczne unieważniają sekcję 4 rozszerzenia w całości** — gdy pojawi się instrukcja
  warsztatowa G4NC z konkretnymi liczbami, wchodzą one, a nie te.

Instrukcja warsztatowa G4NC istnieje jako produkt komercyjny
(<https://mdjc-manuals.com/downloads/workshop-manual-mechanical-repair-engine-control-fuel-system-hyundai-g4nc-nu-2-0-l-dohc-gdi-engine/>)
i **jest to najbardziej opłacalne uzupełnienie tego projektu** — zamienia osiem wartości
branżowych na fabryczne.

---

## 8. Enumeracja statusu układu paliwowego — PID `0103`

<https://en.wikipedia.org/wiki/OBD-II_PIDs> — wykaz PID-ów trybu 01 według SAE J1979.

Dwa bajty; bajt A to układ nr 1, bajt B to układ nr 2. Wartości:

| Wartość | Znaczenie |
|---|---|
| `0` | Silnik wyłączony |
| `1` | Pętla otwarta — niewystarczająca temperatura silnika |
| `2` | **Pętla zamknięta** — sprzężenie zwrotne sondy tlenu |
| `4` | Pętla otwarta — obciążenie silnika albo odcięcie paliwa przy zwalnianiu |
| `8` | **Pętla otwarta — awaria układu** |
| `16` | **Pętla zamknięta — awaria sprzężenia** |

Ta sama strona potwierdziła też, że **w całym trybie 01 nie ma PID-u poziomu oleju** — jedyna
pozycja ze słowem `oil` to `5C`, temperatura. Stąd model termiczny zamiast pomiaru.

Wartości `8` i `16` są podstawą reguły `MIX-1`. **Nie są progiem ustalonym przez kogokolwiek —
sterownik sam nazywa ten stan awarią**, a aplikacja to jedynie powtarza.

---

## 9. Czego nie wolno traktować jako źródła

- własnej pamięci o formułach OBD-II,
- artykułów o „standardowych PID-ach" i list PID-ów z internetu,
- dokumentacji innych aplikacji diagnostycznych (Torque, Car Scanner, OBD Fusion),
- forów o ELM327 i klonach,
- treści reguł innych narzędzi diagnostycznych.

Materiałów wtórnych wolno użyć do **jednej** rzeczy: żeby znaleźć, gdzie w kodzie iOS albo
w standardzie szukać. Do treści stałej — nigdy.
