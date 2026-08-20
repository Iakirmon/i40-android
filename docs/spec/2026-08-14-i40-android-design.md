# i40-android — projekt techniczny

**Data:** 2026-08-14
**Status:** zaakceptowany, gotowy do realizacji
**Adresat:** dokument jest samowystarczalny — realizacja od zera, bez dostępu do kodu Swift
**Rozszerzony przez sześć dokumentów**, każdy wchodzi po poprzednim i jawnie wymienia, co zmienia:

1. `2026-08-14-i40-android-diagnostyka-design.md` — pasma odniesienia, panele ekranu żywego,
   karty przeglądu, blok diagnostyki w raporcie. Po etapie 8.
2. `2026-08-14-i40-android-kontekst-design.md` — status pętli, przedmuchiwanie, panel Powietrze,
   karta miesiąca. Po D9.
3. `2026-08-14-i40-android-odniesienie-design.md` — punkty odniesienia, kolumna poprzednio,
   porównanie przeglądów. Po K6.
4. `2026-08-15-i40-android-historia-design.md` — kasowanie przejazdów, panel Porządki,
   porównanie dwóch przejazdów, karta „od początku”, filtry. Po O8.
5. `2026-08-15-i40-android-objasnienia-design.md` — panel Stan i słownik 70 haseł. Po H7.
6. `2026-08-15-i40-android-wyglad-design.md` — dwa motywy, tokeny, zasady rysowania. Po S4.

⚠️ **Stan aktualny po wszystkich trzech jest w `.cursor/rules/00-projekt.mdc`, sekcja
STAN AKTUALNY.** Tabele w tym dokumencie opisują stan **przed** rozszerzeniami.

Port aplikacji `i40-check` z iOS na radio z Androidem zamontowane w Hyundaiu i40.
Wersja iOS działa i jest opublikowana; ten dokument opisuje, co przenosimy dosłownie,
co zmieniamy z powodu platformy i co zmieniamy z powodu tego, że urządzenie siedzi w aucie
na stałe.

---

## 1. Cel

Aplikacja na radio z Androidem, która **sama nagrywa każdy przejazd** i pokazuje parametry
silnika na żywo, a na postoju wykonuje pełny przegląd diagnostyczny przez adapter OBD-II.

Na iPhonie trzeba było pamiętać: wyjąć telefon, odpalić aplikację, nacisnąć START. Radio
wstaje z zapłonem i widzi adapter, zanim kierowca zapnie pasy. **To jest jedyny powód, dla
którego ten port ma sens** — i z niego wynikają wszystkie różnice projektowe wobec wersji iOS.

Obietnica, którą repozytorium ma udowadniać swoją zawartością:

> **Każda liczba pochodzi z pomiaru albo z nazwanego modelu. Nieudany odczyt mówi „—",
> nigdy „0".**

---

## 2. Zasada, z której wynika cała architektura

> **Wszystko powyżej transportu daje się przetestować bez auta.**

Zasada przeniesiona z wersji iOS i obowiązująca bez zmian. Konsekwencje są dwie i obie są
wiążące dla kolejności realizacji:

**Prawdziwy Bluetooth powstaje na końcu.** Dekodery, pętla próbkowania, zapis, alarmy,
maszyna stanów przejazdu i cały interfejs powstają na atrapie transportu, gdzie wynik jest
sprawdzalny bez radia, bez adaptera i bez samochodu. Fragment, którego nie da się przetestować
automatycznie, jest wtedy pojedynczym plikiem dopisywanym do gotowej i zweryfikowanej reszty.

**Atrapa nie jest makietą — jest zapisem z auta.** `MockI40Script` zawiera prawdziwe
odpowiedzi Twojego i40 z 8 sierpnia 2026, łącznie z pociętymi kawałkami i echem poleceń.
Testy odzwierciedlają rzeczywistość, a nie wyobrażenie o niej.

Druga zasada, specyficzna dla portu:

> **Wierność dotyczy liczb, nie prozy.**

Formuły PID-ów, progi alarmów, stałe modelu oleju, bezpieczniki pętli i algorytm decymacji
przenosimy **co do wartości**. Nazwy klas, układ plików i idiomy języka dostosowujemy do
Kotlina. Przepisanie progu „z pamięci" daje błąd, którego nie widać na ekranie — dokładnie ten
rodzaj, którego narzędzie diagnostyczne nie ma prawa popełniać.

---

## 3. Co zostało sprawdzone w źródłach

**Pierwsza wersja tego dokumentu opierała się na dwóch specyfikacjach iOS. Po przeczytaniu
kodu okazało się, że w siedmiu miejscach kod i specyfikacja mówią co innego** — i za każdym
razem rację ma kod, bo to on jeździł samochodem. Tabela jest tutaj dlatego, że agent, który
tych faktów nie zna, popełni te same błędy co pierwsza wersja specu.

### 3.1 Rozbieżności między specyfikacją iOS a kodem iOS

| # | Ustalenie | Skutek dla portu |
|---|---|---|
| 1 | **Zamrożona ramka (tryb 02) nie jest zaimplementowana.** Pole `freezeFrame` jest zawsze `nil`, nigdzie nie ma przypisania, a `DetailViews.swift` ma dla niej martwy blok renderujący | Obie specyfikacje iOS ją obiecują. Port „wierny specyfikacji" napisałby kod, którego oryginał nie ma. **Sekcja 4 — nie-cel** |
| 2 | **Alarm „nowy kod błędu" nigdy się nie odpala.** `LiveRecordingHost` podaje do `AlertSnapshot.from` puste `dtcsAtStart` i `dtcsNow` | Reguła istnieje i ma testy, ale jest odłączona od danych. **Domykamy — sekcja 8.7** |
| 3 | **Kafel poziomu paliwa `2F` nigdy nie dostaje danych.** Rotacja zimna go nie zawiera; w kodzie stoi komentarz „bez 5C / 2F" | ~~Domykamy — `2F` wchodzi do rotacji~~ **Zamknięte inaczej: poprawka P1.** Maska zgłasza `2F`, ale auto zwraca zero niezależnie od stanu baku. Kafla nie da się naprawić — **zastąpiony korektą długą `0107`**, sekcja 12.2 |
| 4 | **Istnieją dwie różne rotacje zimne i domyślna nigdy nie działa.** `SessionRecorder.defaultColdPIDs` ma 8 PID-ów, ale `LiveRecordingHost.beginRecorder` podaje własną listę 5 PID-ów, która ją przesłania | Kto przepisze „domyślną", przepisze kod martwy. **Definiujemy jedną listę, sekcja 10.2** |
| 5 | **Timeouty nie są takie jak w specyfikacji.** Spec mówi 5 s / 2 ponowienia dla wszystkiego. Kod: przegląd **25 s / 2**, nagrywanie **5 s / 1**, atrapa **5 s / 0** | 25 s jest konieczne: `ATZ` trwa około sekundy, a `SEARCHING...` przy pierwszej negocjacji potrafi ciągnąć się kilkanaście. **Port z 5 s wywali się na pierwszym połączeniu z autem** |
| 6 | **Sekwencja startowa jest dłuższa niż w specyfikacji.** Dochodzą `ATI` i `AT@1`, a `ATDPN` wykonywany jest **dwa razy** — przed negocjacją (odpowiedź `A0`) i po niej (`A6`) | Bez pierwszego `ATDPN` odczyt protokołu bierze stan sprzed negocjacji i raportuje `A0` = „automat, jeszcze nic nie wybrano" |
| 7 | **Rotacja zimna odpytuje PID-y, których to auto nie ma.** `5E` i `10` są w liście, a maski ich nie zgłaszają — patrz 3.2 | Dwa z ośmiu zapytań rotacji szły w próżnię. **Nowa rotacja to sześć obsługiwanych PID-ów, czyli jedno zapytanie na obieg** |

### 3.2 Maski obsługiwanych PID-ów — zdekodowane z zapisu z auta

Zapis z 2026-08-08 zawiera trzy odpowiedzi maskowe. Rozłożone na bity:

| Zapytanie | Odpowiedź | Obsługiwane PID-y |
|---|---|---|
| `0100` | `4100BE3EA813` | `01 03 04 05 06 07 0B 0C 0D 0E 0F 11 13 15 1C 1F` (+ bit kontynuacji `20`) |
| `0120` | `4120A007F011` | `21 23 2E 2F 30 31 32 33 34 3C` (+ bit kontynuacji `40`) |
| `0140` | `4140FED00400` | `41 42 43 44 45 46 47 49 4A 4C 56` — **bez bitu kontynuacji**, więc `0160` nie jest wysyłane |

Z tego wynikają cztery fakty, wszystkie policzone, nie zgadnięte:

| PID | Stan | Konsekwencja |
|---|---|---|
| `5C` temperatura oleju | **nieobsługiwany** | Bajt `00` w zakresie `59`–`60`. Potwierdza `NO DATA` w zapisie. Temperatura oleju to **model**, sekcja 8.5 |
| `5E` chwilowe zużycie paliwa | **nieobsługiwany** | Ten sam bajt. **`SessionSummary.fuelLitres` na tym aucie nigdy się nie policzy** — pole zostaje dla innych aut, ale spec ma to mówić wprost |
| `10` przepływ masowy powietrza | **nieobsługiwany** | Bajt `3E`, bit `10` wyzerowany. Silnik jest GDI z czujnikiem ciśnienia — MAF-u fizycznie nie ma |
| `2F` poziom paliwa | **w masce, bez danych** | Bajt `07`, bit ustawiony — a mimo to odczyt zwraca zero niezależnie od stanu baku. **Poprawka P1**, akapit niżej |

Sprawdzone również: wszystkie sześć PID-ów pętli gorącej (`0D 05 0C 04 0E 06`) jest
obsługiwanych, więc pętla gra w całości bez degradacji.

#### Poprawka P1 — `012F` w masce, a bez danych

Dokument w pierwotnej wersji zakładał, że rozbieżność 3 da się zamknąć „jednym wpisem do
rotacji". **Nie da się.** Trzy niezależne obserwacje mówią to samo:

| Źródło | Co pokazuje |
|---|---|
| Zapis z auta z 2026-08-08 | `2F` zwraca `0 %` |
| Kod iOS | kafel nigdy nie dostał danych — ale bo `2F` nie był odpytywany, więc nic nie rozstrzyga |
| Właściciel pojazdu, 2026-08-16 | poziomu paliwa **nie ma**, potwierdzenie z użytkowania |

Wniosek wiążący: **bit w masce jest deklaracją sterownika, nie obietnicą danych.** `012F` wypada
z odpytywania (sekcja 10.2) i z wyświetlania (sekcja 12.2). Czwarty kafel przejmuje
**korekta długoterminowa `0107`**.

> To jedyny znany taki przypadek na tym egzemplarzu. Wystarczy jeden, żeby maski nie traktować
> jako dowodu, że parametr coś zwróci — tak samo jak katalog PID nie jest dowodem, że auto
> parametr ma. **Zbiór z maski i zbiór odpytywany to dwie różne listy.**

`15`, `32`, `34` i `56` są obsługiwane przez auto, ale **nie ma ich w katalogu PID-ów** — nie są
odczytywane i to jest poprawne zachowanie, nie luka. Katalog opisuje to, co umiemy zdekodować;
maska opisuje to, co auto umie oddać. Lista pól powstaje z przecięcia i jest krótsza od obu.

### 3.3 Detale kodu, które łatwo przeoczyć, a są celowe

| Detal | Dlaczego tak |
|---|---|
| **Bity kontynuacji `20 40 60 80` są odejmowane** ze zbioru obsługiwanych PID-ów | To znaczniki „jest następna maska", nie parametry pomiarowe. Bez odjęcia aplikacja próbuje je odczytać |
| **Napięcie spada z PID `42` na `ATRV`** z pinu 16, gdy PID-u brak | Dwa niezależne pomiary tej samej wielkości; rozbieżność między nimi sama jest sygnałem |
| **`permanentDTCs` jest typem opcjonalnym** | `null` znaczy „tryb `0A` nieobsługiwany" (tak jest na tym aucie), pusta lista znaczy „obsługiwany, brak kodów". Zwinięcie tego do pustej listy kłamie |
| **`MultiPID.parse` przerywa na nieznanym PID-zie**, zamiast zgadywać długość | Katalog jest jedynym źródłem długości. Zgadnięcie przesuwa wszystkie kolejne bajty |
| **Bufor ELM jest konsumowany tylko, gdy ktoś czeka na odpowiedź** | Zapis kończący się szybciej niż zawieszenie na oczekiwaniu zjadłby ramkę, a polecenie wisiałoby do timeoutu. Wyścig realny przy BLE i przy atrapie |
| **Atrapa symuluje stan echa adaptera** (`ATZ`/`ATE1` włączają, `ATE0` wyłącza) i liczy powtórzenia polecenia | Drugie `0101` w przeglądzie dostaje drugi wpis skryptu. Bez tego atrapa rozjeżdża się z prawdziwym adapterem |
| **Model oleju resetuje się przy spadku PID `1F`** o więcej niż 2 s | To wykrycie nowego startu silnika. Ten sam sygnał rozdziela przejazdy — sekcja 11 |

### 3.4 Adapter — czego o nim **nie** wiemy

Ta sekcja powstała przy przeglądzie krytycznym pierwszej wersji tego dokumentu i **unieważnia
jej główną rekomendację techniczną**. Zapisana w całości, bo dotyczy założenia, na którym
opierał się wybór transportu.

**Co twierdzi specyfikacja.** `2026-08-06-obd-i40-design.md` podaje: *„vLinker MC+ — Bluetooth
4.0 dual-mode (SPP + BLE)"*. Na tej podstawie pierwsza wersja tego specu uczyniła **SPP
transportem głównym**, argumentując, że jest prostszy i pewniejszy na tanim radiu.

**Co mówi kod.** Nazwa urządzenia w źródłach jest niespójna:

| Miejsce | Nazwa |
|---|---|
| `MockI40Script.swift`, nagłówek zapisu z auta | **`vLinker MC-IOS`** |
| `BLETransport.swift:282` — punktacja kandydatów przy skanie | `score += 1000` dla nazwy zawierającej **`MC-IOS`** |
| `DetailViews.swift` — opis urządzenia w podglądzie | **`vLinker MC-IOS (nagranie)`** |
| `CheckupView.swift`, `DevicePickerView.swift` — teksty dla użytkownika | `vLinker MC+` |

**Nazwa rozgłoszeniowa prawdziwego urządzenia zawiera `MC-IOS`** — inaczej punktacja
w `BLETransport` nie miałaby sensu, a zapis z auta nie byłby nią podpisany. `MC+` pojawia się
wyłącznie w tekstach interfejsu, czyli tam, gdzie nikt tego nie weryfikował.

**Wniosek.** Wariant `MC-IOS` jest z nazwy przeznaczony dla iOS, a na iOS jedyną dostępną drogą
jest BLE. **Wsparcie SPP przez ten egzemplarz jest niepotwierdzone i prawdopodobnie nie
istnieje.** Zdanie o „dual-mode" pochodzi ze specyfikacji pisanej dla PWA na MacBooku, zanim
adapter został scharakteryzowany — i nigdy nie zostało sprawdzone, bo wersja iOS i tak nie
mogła użyć SPP.

**Co z tego wynika dla projektu:**

| | Adapter | Radio |
|---|---|---|
| **BLE** | **działa — udowodnione**, tak powstał zapis z auta | ryzyko: czy stos GATT jest widoczny aplikacjom |
| **SPP** | **niepotwierdzone, wariant `MC-IOS` sugeruje brak** | bezpieczne: SPP działa nawet na radiach z modułem BT na UART |
| **Wi-Fi** | wymaga innego adaptera (~150 zł) | bezpieczne: gniazdo TCP działa zawsze |

Ryzyko nie leży więc po jednej stronie, tylko w **macierzy dwóch niewiadomych**. Żaden transport
nie jest z góry pewny.

**Decyzja: kolejności nie deklarujemy w specyfikacji — rozstrzyga ją test z sekcji 15.1.**
Wszystkie trzy transporty powstają w etapie 9, domyślny wybiera się po teście, a kod powyżej
`Transport` nie wie o niczym. To jest dokładnie ten scenariusz, dla którego wymienna warstwa
transportu została zaprojektowana, więc architektura tę korektę przechodzi bez zmian — zmienia
się jedna wartość domyślna w ustawieniach.

**Pierwsza czynność po sparowaniu adaptera:** sprawdzić `device.uuids` i poszukać
`00001101-0000-1000-8000-00805F9B34FB`. Obecny → SPP istnieje. Brak → adapter jest BLE-only
i cała nadzieja leży w stosie GATT radia.

### 3.5 Środowisko docelowe — z karty produktu, jeszcze nie z urządzenia

| Element | Ustalenie |
|---|---|
| Radio | Zamówienie #124599 z 2026-08-13, autonawigacje.pl, 1549 zł |
| Model | PR9 8/128, SKU `PR9 8/128 RDS1 HY 259N` |
| SoC | UIS7862, 8 × ARM Cortex-A55 @ 2,0 GHz, GPU PowerVR Rogue GE8322 |
| System | Android 14 |
| Pamięć | 8 GB RAM / 128 GB eMMC |
| Ekran | 1280 × 720, orientacja pozioma na stałe. **Przekątna niepewna** — karta produktu 8", instrukcja 9"–10"; rozdzielczość ta sama |
| Porty | 2 × USB, brak slotu kart SD, brak napędu |
| Łączność | Wi-Fi, Bluetooth 5.0, GPS, modem LTE na kartę SIM |
| Gwarancja | 24 miesiące |
| Montaż | Plug & play dla wersji z fabrycznym radiem, bez Infinity i bez fabrycznej kamery |

Kod dopasowania `HY 259N` jest identyczny we wszystkich wariantach oferty na i40, więc część
samochodowa (ramka, wiązka, CANBUS) jest ta sama niezależnie od przekątnej.

> ⚠️ **Ta tabela pochodzi z karty produktu, nie z urządzenia.** Instrukcja obsługi dołączona
> do radia (marka SMART-AUTO, FSC 2022) podaje **Android 13** i przekątne **9" i 10"**. SoC,
> pamięć i rozdzielczość zgadzają się co do znaku.
>
> Prawdopodobne wyjaśnienie — **hipoteza, nie ustalenie**: te radia pochodzą od jednego
> producenta i są sprzedawane pod wieloma markami, a instrukcja opisuje rodzinę produktów.
>
> **Rozdzielczość 1280 × 720 jest wspólna dla wszystkich wariantów, więc układy są bezpieczne.**
> Niewiadomą jest gęstość raportowana Androidowi — rozstrzyga ją krok 5a z sekcji 15.1 B.

---

## 4. Nie-cele

Świadomie poza zakresem. Każdy punkt to osobna decyzja, zapisana, żeby nie wróciła przypadkiem.

- **Zamrożona ramka, tryb 02.** Rozbieżność 1 z sekcji 3.1: wersja iOS jej nie ma, mimo że
  specyfikacja ją obiecuje. Dołożenie jej tutaj oznacza nowy tryb, własny parser, własny ekran
  i — co ważniejsze — **brak zapisu z auta, na którym dałoby się to przetestować**. Wchodzi
  wtedy, gdy powstanie zapis odpowiedzi trybu 02 z tego egzemplarza.
- **Kasowanie kodów, tryb 04.** Tryb 04 obok kodów czyści monitory gotowości, których odbudowa
  wymaga kilkudziesięciu kilometrów jazdy w określonych warunkach. Skasowanie kodu przed
  badaniem technicznym spowodowałoby jego oblanie.
- **Tryb 06** — wyniki testów pokładowych. Wartościowy, ale poszerza zakres o własny format
  odpowiedzi i własny ekran.
- **UDS i PID-y producenckie Hyundaia.** Wymagają reverse-engineeringu per rocznik i silnik.
- **Poziom oleju.** Nie istnieje w standardzie OBD-II, a spora część egzemplarzy nie ma nawet
  czujnika. Temperatura oleju wchodzi jako model, nie jako pomiar — sekcja 8.5.
- **GPS i mapa trasy**, mimo że radio ma odbiornik. Osobny zbiór danych, osobny ekran, osobne
  pytanie o prywatność.
- **Nakładanie dwóch sesji na jednym wykresie**, eksport sesji do pliku, synchronizacja
  z telefonem, wysyłka czegokolwiek na zewnątrz.
- **Android Auto i CarPlay jako cel projekcji.** Google i Apple dopuszczają tam wyłącznie
  multimedia, komunikatory i nawigację — kategoria diagnostyczna nie istnieje. Aplikacja jest
  zwykłym APK na radiu i to jest cała odpowiedź na ten problem.
- **Praca na telefonie z Androidem** jako wspierany scenariusz. Kod będzie działał, ale
  automatyka przejazdu zakłada stałe zasilanie i start z zapłonem.

---

## 5. Co przenosimy — inwentarz wersji iOS

Źródło: `_github/ios-obd2-ble-diagnostics`, **46 plików źródłowych Swift** (około 7900 linii)
i **30 plików testowych ze 153 funkcjami testowymi**. Podział na to, co jest merytoryką, i to, co jest platformą.

| Warstwa | Pliki iOS | Charakter | Co się dzieje |
|---|---|---|---|
| Transport | `Transport`, `BLETransport`, `GATTPairFinder`, `MockTransport`, `MockI40Script` | platforma + zapis z auta | Przepisujemy; skrypt atrapy **dosłownie** |
| ELM | `ELMSession`, `ELMResponse`, `MultiFrame` | logika | Przenosimy; pompa na `Channel` zamiast `AsyncStream` |
| OBD | `PID`, `PIDCatalog`, `MultiPID`, `SupportedPIDs`, `DTC`, `VIN`, `Readiness` | **czysta logika** | Przenosimy jeden do jednego, łącznie z formułami |
| Pozyskiwanie | `PIDBatchReader`, `SampleStream`, `RingBuffer`, `OilTempEstimator` | **czysta logika** | Przenosimy ze stałymi |
| Reguły | `RuleEngine`, `Insight` | **czysta logika** | Przenosimy z progami |
| Alarmy | `AlertEngine` | **czysta logika** | Przenosimy z progami i karencją |
| Alarmy | `AlertPlayer` | platforma | Piszemy od nowa na `AudioManager` |
| Zapis | `TrackBlob`, `DriveSession`, `SessionRecorder`, `SessionSummaryCalculator` | logika + platforma | SwiftData → SQLite, format blobu własny |
| Wykresy | `Decimator` | **czysta logika** | Przenosimy z algorytmem min-max |
| Raport | `Report` | logika | Przenosimy z regułą werdyktu |
| Orkiestracja | `AppState` | logika + platforma | Rozbijamy: przegląd → `CheckupOrchestrator`, reszta → `DriveService` |
| Interfejs | `UI/*` | platforma | Piszemy od nowa w Compose |
| **Automatyka** | — | **nowe** | `TripStateMachine`, `DriveService`, `BootReceiver` |

Warstwy oznaczone „czysta logika" to około 1100 linii, które przenoszą się mechanicznie i mają
w oryginale komplet testów. To one niosą wartość tego projektu i to na nich nie wolno
improwizować.

---

## 6. Decyzje projektowe

| Decyzja | Uzasadnienie |
|---|---|
| **Transportu głównego nie deklarujemy — rozstrzyga test** | Sekcja 3.4. Wsparcie SPP przez ten egzemplarz adaptera jest **niepotwierdzone**, a BLE jest udowodnione po stronie adaptera i niepewne po stronie radia. Dwie niewiadome, żadna nie do rozstrzygnięcia z dokumentacji |
| **Trzy transporty w projekcie od początku**, nie „gdyby co" | Wprost z powyższego. Wi-Fi kosztuje kilkadziesiąt linii i zdejmuje obie niewiadome naraz. Wynik testu na biurku ma zmieniać **jedną wartość domyślną w ustawieniach**, nie kształt kodu |
| **Adapter sparowany raz w ustawieniach systemu**, aplikacja nie skanuje | Znika uprawnienie `BLUETOOTH_SCAN` i cała ceremonia z lokalizacją. Na tanim radiu to usuwa najczęstszą przyczynę awarii, a wybór urządzenia i tak robi się raz w życiu |
| **Sygnałem startu przejazdu są obroty, nie zapłon** | Gniazdo OBD w i40 bywa zasilane na stałe, więc obecność adaptera niczego nie dowodzi. `01 0C` > 0 dowodzi |
| **Przegląd zamyka i zapisuje bieżącą sesję**, po nim startuje nowa | Adapter przyjmuje jednego klienta. Wstrzymanie pętli dałoby sesję z minutową dziurą, na której całkowanie dystansu i średnia częstotliwość zaczynają kłamać. Przegląd robi się na postoju, więc rozdzielenie przejazdu nic nie kosztuje |
| **Usługa pierwszoplanowa typu `connectedDevice`** | Android 14 wymaga typu; bez niego `startForeground` rzuca wyjątkiem przy pierwszym uruchomieniu. Ten typ **nie jest** na liście wykluczonych ze startu z `BOOT_COMPLETED` — ale producenci radi modyfikują te reguły, więc **do zweryfikowania w `logcat`** przy pierwszym wgraniu, sekcja 15.1 |
| **Pełny port trzech zakładek**, mimo że przegląd działa na iPhonie | Konsekwencja jednego klienta: skoro radio trzyma adapter, przegląd na iPhonie i tak przestaje być dostępny. Rezygnacja z niego oznaczałaby utratę działającej funkcji |
| **Jetpack Compose** mimo reguły zero zależności | Odstępstwo nazwane wprost — sekcja 6.1 |
| **Surowy SQLite przez `SQLiteOpenHelper`, nie Room** | Schemat to jedna tabela z dwiema kolumnami BLOB. Room ciągnie KSP, czyli prawdziwą zależność budowania, i jest więcej konfiguracji niż kodu |
| **Własny format binarny przebiegów**, nie serializacja biblioteczna | `PropertyListEncoder` nie ma odpowiednika. Kolumnowy zapis float32 przez `DataOutputStream` to trzydzieści linii i test obiegu w obie strony |
| **Podsumowanie sesji jako JSON przez `org.json`** | `org.json` jest we frameworku. Podsumowanie jest małe i będzie ewoluować, więc format czytelny wygrywa nad binarnym |
| **Wykresy rysowane na `Canvas`**, bez biblioteki | Osie są **sztywne** (sekcja 12.3), więc cała trudność biblioteki wykresów — skalowanie — jest tu niepotrzebna. Zostaje ścieżka przez punkty |
| **Pompa poleceń na `Channel`, nie na `Mutex`** | `Mutex` w Kotlinie szereguje dostęp, ale **nie gwarantuje kolejności** — czekające korutyny nie tworzą kolejki FIFO. W narzędziu diagnostycznym polecenia wykonane w innej kolejności niż wysłane są nie do zdiagnozowania. `Channel` jest FIFO |
| **Timeout przeglądu 25 s, nagrywania 5 s** | Rozbieżność 5 z sekcji 3.1. Wartości z działającego kodu, nie ze specyfikacji |
| **Rotacja zimna to dokładnie sześć PID-ów** | Maksimum jednego zapytania. Osiem PID-ów kosztowałoby dwa zapytania na obieg, a dwa z nich i tak nie istnieją na tym aucie (sekcja 3.2) |
| **Identyfikatory po polsku w domenie, po angielsku w platformie** | Sekcja 6.2 |
| **Zapis z auta wendorowany w repozytorium** | Bez niego atrapa jest makietą, a testy sprawdzają wyobrażenie. To jest odpowiednik złotego korpusu z `fa3-check` |

### 6.1 Compose — odstępstwo nazwane wprost

Reguła z projektu iOS brzmi: **zero zależności zewnętrznych**. Na Androidzie granica
„platforma kontra biblioteka" leży gdzie indziej i uczciwie trzeba to powiedzieć.

Na iOS SwiftUI jest częścią systemu. Na Androidzie **Compose jest biblioteką pakowaną do
APK** — formalnie *jest* zależnością zewnętrzną. Ścisłym odpowiednikiem SwiftUI byłyby
klasyczne widoki `android.widget`, które faktycznie siedzą we frameworku.

Wybieram Compose i zapisuję powód: wykresy przesuwne i stos wykresów historycznych
w klasycznych widokach oznaczają własne `onDraw`, ręczne zarządzanie unieważnianiem
i własną obsługę gestów dla wspólnego suwaka. Compose `Canvas` i `pointerInput` dają to samo
w kilkunastu liniach każde.

**Ta decyzja ma termin ważności: do etapu 7.** Po napisaniu interfejsu jej cofnięcie oznacza
przepisanie połowy projektu, więc jeśli ma zostać cofnięta, to przed nim.

Dozwolone łącznie: `android.*`, `java.*`, `kotlinx.coroutines`, Jetpack Compose.
Zabronione bez pytania: Room, kotlinx-serialization, jakakolwiek biblioteka wykresów, Hilt,
Timber, Retrofit, OkHttp.

### 6.2 Język identyfikatorów

Domena jest polska w źródle: `Przeglad`, `Werdykt`, `Zastrzezenie`, `Alarm`, `Przejazd`.
Platforma jest angielska z definicji: `onCreate`, `StateFlow`, `BluetoothSocket`.

Zasada: **typy i funkcje domenowe po polsku, styk z Androidem po angielsku.** Skróty
z dokumentów technicznych zostają w oryginale — `PID`, `DTC`, `VIN`, `ELM`, `MIL`, `MAF`.
Komentarze i dokumentacja po polsku, jak w `i40-check` i `fa3-check`.

Wyjątek: pliki będące dosłownym portem czystej logiki zachowują nazwy oryginału
(`SampleStream`, `TrackBlob`, `Decimator`), żeby dało się je zestawić linia po linii ze
Swiftem przy szukaniu rozbieżności.

---

## 7. Architektura

```
ui/            ekrany Compose, motyw                     ← platforma
  ↑
service/       DriveService, TripStateMachine, Boot      ← automatyka przejazdu
checkup/       CheckupOrchestrator                       ← przegląd na postoju
  ↑
alerts/        AlertEngine (czysty) + AlertPlayer        
storage/       SQLite, TrackBlob, SessionRecorder        
acquisition/   SampleStream, RingBuffer, OilTemp         ← czysta logika
rules/         RuleEngine                                ← czysta logika
obd/           PID · DTC · VIN · maski · MultiPID        ← czysta logika, zero I/O
  ↑
elm/           pompa poleceń, ramkowanie, komunikaty
  ↑
transport/     Spp · Ble · Wifi · Mock                   ← jedyny styk ze sprzętem
```

**Zależności idą w jedną stronę.** Warstwa `obd/` nie wie o istnieniu `elm/`, `elm/` nie wie
o istnieniu transportu poza interfejsem, a `ui/` nie wie, którym kablem płyną dane.

### 7.1 Struktura katalogów

```
i40-android/
├── AGENTS.md
├── LICENSE
├── .cursor/
│   ├── rules/
│   │   ├── 00-projekt.mdc          niezmienniki, kod, zależności, testy
│   │   ├── 10-transport-elm.mdc    kontrakt transportu i sesji ELM
│   │   ├── 20-obd.mdc              kontrakt dekoderów i katalogu PID
│   │   └── 30-zrodla.mdc           zakaz wymyślania formuł, progów i opisów
│   └── skills/
│       └── etap/SKILL.md
├── docs/
│   ├── spec/
│   │   ├── 2026-08-14-i40-android-design.md      ten dokument
│   │   └── 2026-08-14-cursor-prompty.md          prompty, jeden czat na etap
│   └── zrodla.md                                 bibliografia
└── app/                                          projekt Android (powstaje w etapie 0)
    └── src/
        ├── main/kotlin/pl/i40/android/
        │   ├── transport/    Transport, Spp, Ble, Wifi, Mock, MockI40Script, GattPairFinder
        │   ├── elm/          ElmSession, ElmResponse, MultiFrame
        │   ├── obd/          Pid, PidCatalog, MultiPid, SupportedPids, Dtc, Vin, Readiness
        │   ├── acquisition/  PidBatchReader, SampleStream, RingBuffer, OilTempEstimator
        │   ├── rules/        RuleEngine, Insight
        │   ├── alerts/       AlertEngine, AlertPlayer
        │   ├── storage/      TrackBlob, DriveSessionDao, SessionRecorder, SummaryCalculator
        │   ├── charts/       Decimator
        │   ├── checkup/      CheckupOrchestrator, Report
        │   ├── service/      DriveService, TripStateMachine, BootReceiver
        │   └── ui/           Theme, LiveScreen, RollingChart, HistoryScreen, CheckupScreen
        ├── main/res/raw/     dtc_dictionary.json
        └── test/kotlin/      testy jednostkowe (JVM, bez emulatora)
```

**Testy stoją w `test/`, nie w `androidTest/`.** Cała warstwa czysta jest testowalna na JVM,
więc emulator nie jest potrzebny do niczego poza transportem, a transport i tak jest
weryfikowany na sprzęcie.

---

## 8. Kontrakty modułów

### 8.1 `transport/`

```kotlin
interface Transport {
    suspend fun open()
    suspend fun close()
    suspend fun write(bytes: ByteArray)
    val chunks: Flow<ByteArray>
    val disconnects: Flow<Unit>
}
```

Odpowiednik protokołu ze Swifta jeden do jednego. **To jedyny kontrakt między sprzętem
a resztą aplikacji** i jedyne miejsce, w którym wolno dotknąć `BluetoothSocket`,
`BluetoothGatt` i `java.net.Socket`.

**`SppTransport` — najprostszy, ale wsparcie po stronie adaptera niepotwierdzone (sekcja 3.4).**
Urządzenie z `adapter.bondedDevices`, nie ze skanowania.
`createRfcommSocketToServiceRecord(UUID("00001101-0000-1000-8000-00805F9B34FB"))`.
Przed `connect()` zawsze `adapter.cancelDiscovery()` — bez tego połączenie potrafi zawisnąć.
Odczyt w korutynie na `Dispatchers.IO`, `InputStream.read` do bufora, każdy kawałek do `chunks`.
Ponowienie co 5 s bez limitu prób: adapter budzi się razem z autem, więc czekanie jest
właściwym zachowaniem, nie błędem.

**`BleTransport` — jedyny udowodniony po stronie adaptera.** Tak powstał zapis z auta i tak
działa aplikacja na iPhonie. Prawdziwe UUID-y, odczytane z urządzenia:

| Element | UUID |
|---|---|
| Usługa | `18F0` |
| Zapis | `2AF1` |
| Notyfikacja | `2AF0` |

**Nie hardkodować wyłącznie tych UUID-ów.** Kolejność wyboru pary, przenoszona z
`GATTPairFinder`: najpierw znana kombinacja Vgate `2AF1`+`2AF0`, potem pierwsza para
zapis+notyfikacja w obrębie jednej usługi, w ostateczności para z różnych usług. Wybór wraca
z uzasadnieniem, które trafia na ekran diagnostyczny. **To jest czysta funkcja i ma własne
testy bez Androida.**

Zapis przez `WRITE_TYPE_NO_RESPONSE`, gdy charakterystyka to obsługuje — klony ELM327 zwykle
tego wymagają.

**`WifiTransport` — plan awaryjny.** Gniazdo TCP, domyślnie `192.168.0.10:35000`,
konfigurowalne. Potrzebne, gdy radio nie wystawi Bluetootha aplikacjom.

**`MockTransport` — atrapa.** Odtwarza `MockI40Script`. Nie jest prostym „polecenie → tekst":

- porównanie polecenia **bez względu na wielkość liter i spacje**,
- **symulacja stanu echa**: `ATZ` i `ATE1` włączają echo, `ATE0` wyłącza; wpis nagrany z echem
  odtwarzany przy echu wyłączonym ma je obcięte i odwrotnie,
- **licznik powtórzeń**: drugie `0101` w przeglądzie dostaje drugi pasujący wpis skryptu,
- odpowiedź na nieznane polecenie to `?\r\r>`, jak w prawdziwym adapterze,
- `drop()` budzi `disconnects` — to inna ścieżka niż `close()` i testy rozłączenia w trakcie
  nagrywania z niej korzystają.

### 8.2 `elm/ElmSession`

**Pompa poleceń.** ELM327 nie obsługuje potokowania: drugie polecenie wysłane przed odpowiedzią
na pierwsze psuje oba. Wymagane jest ścisłe „jedno polecenie w powietrzu" **oraz zachowanie
kolejności**.

```
Channel<Zadanie>(UNLIMITED)  ──►  jedna długo żyjąca korutyna
      ▲                                    │
   send()                          wyślij → czekaj na pełną odpowiedź
   zwraca                          → spełnij CompletableDeferred
   Deferred                        → dopiero potem pobierz następne
```

`Mutex` **nie wystarczy** — szereguje dostęp, ale korutyny czekające na mutex nie tworzą
kolejki FIFO. Test szeregowania jest obowiązkowy: wiele poleceń wysłanych współbieżnie musi
trafić do transportu pojedynczo i w kolejności wysłania.

**Ramkowanie.** Odpowiedź kończy znak zachęty `>` (0x3E), **nie** koniec linii. Kawałki
przychodzą pocięte: przy BLE zgodnie z MTU (około 20 bajtów), przy SPP zgodnie z tym, co
oddał `InputStream`. Jedna odpowiedź to zwykle kilka kawałków, a bywa, że dwie odpowiedzi
trafiają w jeden.

Bufor akumulować do napotkania `>`. **Pojedynczego kawałka nigdy nie parsować.**

Detal z rozbieżności 3.3: **bufor konsumujemy tylko wtedy, gdy ktoś czeka na odpowiedź.**
Bez tego zapis kończący się szybciej niż zawieszenie na oczekiwaniu zjada ramkę i polecenie
wisi do timeoutu.

**Ponowienia.** Ponawiać wyłącznie po **timeoucie** oraz po `STOPPED` i `BUFFER FULL`.
Pozostałe komunikaty przerywają — ponawianie `UNABLE TO CONNECT` to pętla na głuchym łączu.

### 8.3 `elm/ElmResponse` i `elm/MultiFrame`

`ElmResponse` rozpoznaje komunikaty tekstowe **po dokładnym dopasowaniu linii**, nie po
podłańcuchu:

| Komunikat | Reakcja |
|---|---|
| `NO DATA` | Oznacz pole niedostępne, kontynuuj |
| `SEARCHING...` | Czekaj, właściwa odpowiedź może dojść później |
| `UNABLE TO CONNECT` | Przerwij — zapłon wyłączony lub adapter nie w gnieździe |
| `BUS INIT: ERROR`, `CAN ERROR` | Przerwij |
| `STOPPED`, `BUFFER FULL` | Ponów |
| `?` | Błąd w kodzie — zaloguj |

`MultiFrame` skleja odpowiedzi wieloramkowe: linie `N:HEX` sortowane po `N` i łączone,
z pominięciem wiodącej długości całkowitej (`014`). **Wspólne dla trybu 09 i dla odpowiedzi
wielo-PID** — odpowiedź na sześć PID-ów ma około piętnastu bajtów, a ramka CAN mieści siedem,
więc jest prawie zawsze wieloramkowa.

### 8.4 `obd/`

Warstwa **czysta: bez I/O, bez zegara, bez Androida.** Cała testowalna na JVM.

`Pid` niesie: numer, nazwę po polsku, liczbę bajtów, jednostkę, **fizyczny zakres dopuszczalny**
i funkcję dekodującą. Wartość spoza zakresu jest oznaczana jako podejrzana i pokazywana
z ostrzeżeniem, nie jako fakt.

`PidCatalog` to tablica definicji — **jedyne źródło długości danych PID-u**. `MultiPid.parse`
czyta numer, sprawdza długość w katalogu i konsumuje tyle bajtów; **nieznany PID kończy
parsowanie**, bo zgadnięcie długości przesunęłoby wszystkie kolejne bajty.

`SupportedPids` parsuje maski. Bit 7 bajtu A to pierwszy PID zakresu, bit 0 bajtu D to
ostatni — i ten ostatni oznacza wyłącznie „jest następna maska". **Bity `20 40 60 80` trzeba
odjąć ze zbioru wynikowego.**

`Dtc` dekoduje dwubajtowe kody i opisuje je ze słownika. Rodziny: bity 7–6 pierwszego bajtu
to `P`/`C`/`B`/`U`, bity 5–4 to druga cyfra, bity 3–0 trzecia, drugi bajt to czwarta i piąta.
Kody `P0xxx` i `P2xxx` są generyczne; `P1xxx` oraz `P30xx`–`P33xx` producenckie.

`Vin` wyprowadza z numeru **wyłącznie to, co pewne**: znaki 1–3 producent, znak 10 rok modelowy
(kodowanie pomija `I O Q U Z`, więc `F` = 2015), znak 11 zakład (**osobna tablica** — `U` = Ulsan
jest poprawne, mimo że jako kod rocznika `U` nie występuje). Znaków 4–8 **nie dekodować** —
Hyundai tego nie publikuje.

`Readiness` dekoduje PID `01`: bajt A bit 7 to kontrolka MIL, bity 6–0 liczba kodów; bajt B
monitory ciągłe i typ zapłonu; bajty C i D monitory nieciągłe. **Monitor nieobsługiwany przez
to auto nie jest usterką i nie ma się pojawiać jako brak.**

### 8.5 `acquisition/`

**`PidBatchReader`** — sonda i odczyt. Sonda wysyła jedno zapytanie o `0C` i `0D`; odpowiedź
uznajemy za dowód wsparcia **tylko gdy da się z niej odczytać co najmniej dwa** PID-y z sondy.
Samotna odpowiedź na pierwszy oznacza brak wsparcia → tryb pojedynczy i komunikat na ekranie.

**`SampleStream`** — pętla gorąca i rotacja zimna. **Bez timera**: następne zapytanie po
odebraniu poprzedniej odpowiedzi, z celowaniem w interwał z nastawy. Gdy obieg trwa dłużej
niż interwał — zero snu, zapytania **nie kolejkują się**. Stałe w sekcji 10.

Skład szóstki gorącej, w tej kolejności: **najpierw obowiązkowe `0D`, `05` i `04`** (nie wolno
ich wypchnąć), potem trzy gniazda wykresów, na końcu uzupełnienie z domyślnych.

**`RingBuffer`** — bufor kołowy stałej pojemności, źródło wykresów przesuwnych, niezależny od
zapisu sesji. 60 s przy 4 Hz = 240 próbek.

**`OilTempEstimator`** — **model termiczny, nie pomiar.** PID `5C` na tym aucie nie istnieje
(sekcja 3.2). Model przenosimy ze stałymi:

```
tauBase = 300 s              jedyny parametr do kalibracji termometrem IR na misce

reset(coolant):              estimate = coolant; runSeconds = 0

update(dt, coolant, load, ambient):
    target        = coolant + 25 * (load/100)
    loadFactor    = 0,6 + 0,8 * (load/100)          0,6 na jałowym … 1,4 pod pełnym
    ambientFactor = 1 + (20 - ambient) / 60         1,0 przy 20 °C … 1,33 przy −20 °C
    tau           = clamp(tauBase * ambientFactor / loadFactor, 150, 900)
    alpha         = 1 - exp(-dt / tau)
    estimate     += alpha * (target - estimate)
```

Brak `ambient` → przyjmij 20 °C. Obciążenie przycinane do 0…100.
**Spadek PID `1F` o więcej niż 2 s** względem poprzedniego odczytu → nowy start silnika → reset.

Pewność modelu, pokazywana **zawsze** obok wartości: `< 180 s` niska, `180–600 s` średnia,
`> 600 s` dobra. Silnik rozgrzany przy estymacie ≥ 90 °C i czasie pracy ≥ 600 s.

Wartość jest podpisana na ekranie jako model. To jest różnica między narzędziem diagnostycznym
a ładnie wyglądającą liczbą.

### 8.6 `storage/`

> ⚠️ **Wersja schematu wynosi dziś 3, nie 1.** Rozszerzenie odniesienia dołożyło tabele
> `punkt_odniesienia` i `przeglad`, historii — kolumnę `chroniony`. Pełna drabinka wersji
> i `onUpgrade` są w **§11.2 warstwy historii**; poniższa tabela opisuje wyłącznie wersję 1.

**Baza — pierwsza tabela, `SQLiteOpenHelper`, wersja schematu 1:**

```sql
CREATE TABLE przejazd (
    id          TEXT PRIMARY KEY,
    poczatek    INTEGER NOT NULL,      -- epoch ms
    koniec      INTEGER,
    status      TEXT NOT NULL,         -- w_toku | zamkniety | odzyskany
    vin         TEXT,
    notatka     TEXT NOT NULL DEFAULT '',
    podsumowanie BLOB NOT NULL,        -- JSON (org.json)
    przebieg    BLOB NOT NULL          -- format binarny poniżej
);
CREATE INDEX idx_przejazd_poczatek ON przejazd (poczatek);
CREATE INDEX idx_przejazd_status   ON przejazd (status);
```

**Wiersz powstaje przy STARCIE sesji, nie przy zamknięciu**, ze statusem `w_toku` — patrz
sekcja 11.4. Bez tego przerwany przejazd nie zostawia po sobie śladu.

**Nie tworzyć wiersza na próbkę.** Dwudziestominutowa sesja przy 4 Hz to blisko pięć tysięcy
próbek na serię. Lista przejazdów czyta wyłącznie metadane i podsumowanie; `przebieg` wczytuje
się dopiero przy wejściu w szczegóły.

**Format przebiegów — kolumnowy, nie wierszowy.** Pętla gorąca i rotacja zimna próbkują z różną
częstotliwością i nie da się ich ułożyć we wspólne wiersze.

```
magic       4 bajty   "I40T"
wersja      int32     = 1
liczbaSerii int32
  dla każdej serii:
    pid     uint8
    n       int32
    czasy   n × float32    sekundy od startu sesji
    wartosci n × float32   wartość po przeliczeniu, w jednostce z katalogu
```

`DataOutputStream`, big-endian. Około pół megabajta na dwudziestominutową sesję z sześcioma
seriami — kompresja niepotrzebna. **Test obiegu w obie strony jest obowiązkowy.**

**Podsumowanie** liczone przy zatrzymaniu z gotowego przebiegu — czysta funkcja bez ELM.
Dla sesji odzyskanej (sekcja 11.4) liczone z przebiegu **częściowego**, bez odczytu `03` na
końcu; `kodyNaKoncu` zostaje wtedy pustą listą, a nie zgadywaną kopią `kodyNaStarcie`:

| Pole | Skąd |
|---|---|
| `czasTrwaniaS` | większa z dwóch: rozpiętość czasów w przebiegu, różnica znaczników |
| `dystansKm` | całkowanie prędkości `0D` regułą trapezów |
| `maxObroty`, `srednieObroty` | seria `0C` |
| `maxPredkoscKmh` | seria `0D` |
| `maxPlynC` | seria `05` |
| `minNapiecie`, `maxNapiecie` | seria `42` |
| `paliwoL` | całkowanie `5E` — **na tym aucie zawsze `null`**, patrz 3.2 |
| `kodyNaStarcie`, `kodyNaKoncu` | dwa odczyty `03` |
| `liczbaProbek`, `sredniaHz` | z przebiegu i czasu trwania |

Pole `paliwoL` zostaje w formacie mimo że tego auta nie dotyczy — usunięcie go z formatu
zamknęłoby drogę innym egzemplarzom, a `null` jest uczciwą odpowiedzią.

### 8.7 `service/` — automatyka przejazdu

To jedyna część projektu bez odpowiednika w wersji iOS. Szczegóły w sekcji 11.

**`TripStateMachine` jest czystą funkcją stanu i zdarzeń** — żadnego Androida, pełne pokrycie
testami. To miejsce, w którym najłatwiej o błąd, którego nie widać do chwili, gdy zabraknie
nagrania z ważnego przejazdu.

**Usługa jest właścicielem stanu żywego** — buforów kołowych, gromadzonego przebiegu, modelu
oleju i karencji alarmów. Interfejs czyta z niej `StateFlow` i nie przechowuje nic własnego.
Do tego: checkpoint co 30 s, odzyskiwanie sesji `w_toku` przy starcie i `PARTIAL_WAKE_LOCK`
w stanie `Nagrywa`. Wszystko w sekcji 11.4 — **to nie jest opcjonalne dopracowanie, tylko
warunek, żeby zdanie „nigdy nie tracić danych" było prawdziwe**.

**Domknięcie rozbieżności 2:** alarm „nowy kod błędu" dostaje dane. Kody zapisane odczytujemy
poleceniem `03` **na starcie sesji i przy `n % 200 == 150`** (około 50 s przy 4 Hz; faza
przesunięta — sekcja 10.1), i to jest
`dtcsNow` podawane do `AlertEngine`. Bez tego reguła jest kodem martwym, jak w wersji iOS.

### 8.8 `checkup/` — przegląd na postoju

Sekwencja poleceń, **przeniesiona z działającego kodu, nie ze specyfikacji** (rozbieżność 6):

```
ATZ        reset, ~1 s, zwraca baner z wersją
ATI        wersja firmware
AT@1       opis adaptera
ATE0       echo wyłączone
ATL0       bez linefeedów
ATS0       bez spacji
ATH0       bez nagłówków CAN
ATSP0      automatyczne wykrywanie protokołu
ATRV       napięcie na pinie 16
ATDPN      protokół PRZED negocjacją            → w zapisie: A0
0100       pierwsze prawdziwe zapytanie, wymusza negocjację
ATDPN      protokół PO negocjacji               → w zapisie: A6
0120 0140 0160   łańcuchowo, póki bit kontynuacji
0902 0904 090A   VIN, kalibracja, nazwa sterownika
0101       kontrolka MIL i monitory
03 07 0A   kody zapisane, oczekujące, trwałe
01XX       pętla po PID-ach z przecięcia katalogu i maski
```

**Timeout 25 s, dwa ponowienia.** `ATZ` trwa około sekundy, a `SEARCHING...` przy pierwszej
negocjacji potrafi ciągnąć się kilkanaście. Port z 5 s wywali się na pierwszym połączeniu.

**Werdykt** — kolejność jest istotna i przenoszona dosłownie:

```
kontrolka MIL świeci LUB są kody zapisane            → USTERKA
któryś wniosek ma wagę usterki                       → USTERKA
są kody oczekujące LUB monitory niegotowe            → UWAGA
któryś wniosek ma wagę uwagi                         → UWAGA
w pozostałych przypadkach                            → OK
```

**Kolor nigdy nie jest jedynym sygnałem** — każdy stan niesie własny znak graficzny.

Napięcie do reguł: PID `42`, a gdy go brak — `ATRV` z pinu 16.

### 8.9 `ui/`

Szczegóły w sekcji 12. Kontrakt jednozdaniowy: **interfejs nie wie, którym transportem płyną
dane, i nie liczy niczego, co dałoby się policzyć w warstwie czystej.**

---

## 9. Katalog PID-ów i słownik kodów

### 9.1 Formuły trybu 01

Źródło: SAE J1979. `A`, `B`, `C`, `D` to kolejne bajty danych. **Tablica jest zamknięta —
PID spoza niej nie istnieje.**

| PID | Nazwa | Bajty | Formuła | Jednostka | Zakres |
|---|---|---|---|---|---|
| `01` | Stan monitorów | 4 | pole bitowe | — | — |
| `03` | Status układu paliwowego | 2 | pole bitowe | — | — |
| `04` | Obliczone obciążenie silnika | 1 | A × 100 / 255 | % | 0…100 |
| `05` | Temperatura płynu chłodzącego | 1 | A − 40 | °C | −40…150 |
| `06` | Korekta krótkoterminowa, bank 1 | 1 | A / 1,28 − 100 | % | −100…100 |
| `07` | Korekta długoterminowa, bank 1 | 1 | A / 1,28 − 100 | % | −100…100 |
| `0B` | Ciśnienie w kolektorze dolotowym | 1 | A | kPa | 0…255 |
| `0C` | Obroty silnika | 2 | (256A + B) / 4 | obr/min | 0…8000 |
| `0D` | Prędkość pojazdu | 1 | A | km/h | 0…250 |
| `0E` | Wyprzedzenie zapłonu | 1 | A / 2 − 64 | ° przed GMP | −64…64 |
| `0F` | Temperatura powietrza dolotowego | 1 | A − 40 | °C | −40…100 |
| `10` | Przepływ masowy powietrza | 2 | (256A + B) / 100 | g/s | 0…100 |
| `11` | Pozycja przepustnicy | 1 | A × 100 / 255 | % | 0…100 |
| `13` | Zamontowane sondy tlenu | 1 | pole bitowe | — | — |
| `1C` | Norma OBD | 1 | kod | — | — |
| `1F` | Czas pracy od uruchomienia | 2 | 256A + B | s | 0…65535 |
| `21` | Przebieg z zapaloną kontrolką | 2 | 256A + B | km | 0…65535 |
| `23` | Ciśnienie w szynie wysokiego ciśnienia | 2 | (256A + B) × 10 | kPa | 0…655350 |
| `24`–`2B` | Sondy tlenu 1–8 | 4 | λ = (256A+B)/32768; U = (256C+D)/8192 | — / V | — |
| `2C` | Zadana wartość EGR | 1 | A × 100 / 255 | % | 0…100 |
| `2D` | Uchyb EGR | 1 | A / 1,28 − 100 | % | −100…100 |
| `2E` | Zadane przedmuchiwanie zbiornika | 1 | A × 100 / 255 | % | 0…100 |
| `2F` | Poziom paliwa w zbiorniku ⚠️ **nie wyświetlany — poprawka P1** | 1 | A × 100 / 255 | % | 0…100 |
| `30` | Rozgrzania od skasowania kodów | 1 | A | — | 0…255 |
| `31` | Przebieg od skasowania kodów | 2 | 256A + B | km | 0…65535 |
| `33` | Ciśnienie atmosferyczne | 1 | A | kPa | 50…110 |
| `3C`–`3F` | Temperatura katalizatora 1–4 | 2 | (256A + B) / 10 − 40 | °C | −40…6513,5 |
| `41` | Gotowość monitorów w bieżącym cyklu | 4 | pole bitowe | — | — |
| `42` | Napięcie sterownika | 2 | (256A + B) / 1000 | V | 0…20 |
| `43` | Obciążenie absolutne | 2 | (256A + B) × 100 / 255 | % | 0…100 |
| `44` | Zadany współczynnik lambda | 2 | (256A + B) / 32768 | — | 0…2 |
| `45` | Względna pozycja przepustnicy | 1 | A × 100 / 255 | % | 0…100 |
| `46` | Temperatura otoczenia | 1 | A − 40 | °C | −40…60 |
| `47`, `48` | Bezwzględna pozycja przepustnicy B, C | 1 | A × 100 / 255 | % | 0…100 |
| `49`–`4B` | Pozycja pedału D, E, F | 1 | A × 100 / 255 | % | 0…100 |
| `4C` | Pozycja zadana przepustnicy | 1 | A × 100 / 255 | % | 0…100 |
| `4D` | Czas pracy z zapaloną kontrolką | 2 | 256A + B | min | 0…65535 |
| `4E` | Czas od skasowania kodów | 2 | 256A + B | min | 0…65535 |
| `51` | Rodzaj paliwa | 1 | kod | — | — |
| `5A` | Względna pozycja pedału | 1 | A × 100 / 255 | % | 0…100 |
| `5C` | Temperatura oleju silnikowego | 1 | A − 40 | °C | −40…160 |
| `5D` | Kąt wtrysku paliwa | 2 | ((256A+B) − 26880) / 128 | ° | −210…301,992 |
| `5E` | Chwilowe zużycie paliwa | 2 | (256A + B) / 20 | l/h | 0…3276,75 |
| `61`, `62` | Moment żądany / wydany | 1 | A − 125 | % | −125…130 |
| `63` | Moment odniesienia silnika | 2 | 256A + B | N·m | 0…65535 |

`5C`, `5E` i `10` zostają w katalogu, mimo że **to auto ich nie obsługuje** — katalog opisuje
standard, maska opisuje egzemplarz, a lista pól w interfejsie powstaje z ich **przecięcia**.

PID-y `08` i `09` (bank 2) nie odpowiedzą — silnik jest rzędową czwórką z jednym bankiem.
Brak odpowiedzi jest poprawnym zachowaniem, nie usterką.

### 9.2 Słownik kodów DTC

41 wpisów, przenoszony z `DTCDictionary.json` **bez zmian i bez uzupełnień**. Zakres:
`P0100`–`P0104`, `P0110`–`P0113`, `P0115`–`P0118`, `P0120`–`P0123`, `P0130`–`P0135`, `P0171`,
`P0172`, `P0300`–`P0304`, `P0325`, `P0335`, `P0340`, `P0401`, `P0420`, `P0442`, `P0455`,
`P0500`, `P0505`, `P0562`, `P0563`.

Kod spoza słownika:

| Rodzaj | Opis |
|---|---|
| `P1xxx`, `P30xx`–`P33xx` | **„kod producencki Hyundai, opis nieznany"** + surowy kod |
| `P0xxx`, `P2xxx` | „Kod generyczny — brak wpisu w słowniku" |
| pozostałe | „Brak opisu w słowniku" |

**Nie wymyślać opisów.** Brak opisu jest uczciwszy niż zgadnięty, a zgadnięty wygląda dokładnie
tak samo jak prawdziwy.

---

## 10. Stałe, progi i bezpieczniki

Wszystkie wartości liczbowe projektu w jednym miejscu, żeby dało się je zweryfikować bez
czytania kodu. **Przeniesione z działającej wersji iOS. Zmiana którejkolwiek wymaga pytania.**

### 10.1 Pętla próbkowania

| Parametr | Wartość |
|---|---|
| Nastawa oszczędna | 2 Hz — odstęp 500 ms |
| **Nastawa zrównoważona — domyślna** | **4 Hz — odstęp 250 ms** |
| Nastawa szczegółowa | bez celowania, tylko bezpieczniki |
| Minimalna przerwa między poleceniami | 20 ms |
| Twardy sufit zapytań na sekundę | 25 |
| Maksimum PID-ów w jednym zapytaniu | 6 |
| Rotacja zimna | **`n % 10 == 5`** — co dziesiąty cykl, **z przesunięciem fazy** |
| Odczyt `03` | **`n % 200 == 150`** (≈ 50 s przy 4 Hz), **z przesunięciem fazy** |
| Pustych odczytów przed zatrzymaniem | 10 |
| Mnożnik zwalniania po pustym odczycie | × 1,5, sufit 8,0 |
| Mnożnik przyspieszania po udanym | × 0,9, podłoga 1,0 |
| Pojemność bufora kołowego | 240 próbek (60 s × 4 Hz) |
| Okno liczenia realnej częstotliwości | 2 s |
| **Checkpoint przebiegu do bazy** | **co 30 s** |

**Dlaczego rotacja zimna i odczyt `03` mają przesuniętą fazę.** Naiwne `n % 10 == 0`
i `n % 200 == 0` zbiegają się co dwusetny cykl, bo **200 dzieli się przez 10** — i wtedy jeden
cykl wykonuje **trzy zapytania** zamiast dwóch. Przy 4 Hz to około 210 ms w budżecie 250 ms.
Policzone: **1000 takich cykli na 200 000**.

Przesunięcie czyni zbieg niemożliwym:

```
n % 200 == 150   ⟹   n % 10 jest stale równe 0, nigdy 5
```

Sprawdzone na 200 000 cykli: **żaden cykl nie wykonuje więcej niż dwóch zapytań**.

To nie jest mikrooptymalizacja. Trzy zapytania w cyklu zjadają 84 % budżetu, a przy wolniejszym
adapterze wywracają nastawę — i objawia się to wyłącznie spadkiem licznika Hz, bez żadnego
błędu. Rozszerzenie diagnostyczne dokłada czwarty poziom w tę samą siatkę faz.

### 10.2 Skład zapytań

> ⚠️ **Składy poziomów w tej sekcji opisują stan przed rozszerzeniami.** Rozszerzenie
> diagnostyczne dołożyło poziom średni, kontekstowe przebudowało wszystkie trzy poziomy poza
> gorącym. **Stan aktualny: sekcja STAN AKTUALNY w `.cursor/rules/00-projekt.mdc`.**

**Pętla gorąca — dokładnie sześć PID-ów, jedno zapytanie:**

```
obowiązkowe:   0D prędkość          (blokada prędkościowa)
               05 temperatura płynu (alarm przegrzania)
               04 obciążenie        (wejście modelu oleju)
gniazda:       0C obroty
               0E wyprzedzenie zapłonu
               06 korekta krótkoterminowa
```

Gniazda są konfigurowalne; obowiązkowych **nie wolno wypchnąć**.

**Dlaczego `04` jest obowiązkowe, a nie jest gniazdem.** Model temperatury oleju liczy
`target = płyn + 25 × (obciążenie/100)` — **bez obciążenia nie ma czego liczyć**. Gdyby `04`
było zwykłym gniazdem, wystarczyłaby zmiana parametru na wykresie, żeby model **cicho zamarł**:
kafel pokazywałby ostatnią wartość, pewność przestałaby rosnąć, i nic by o tym nie
powiedziało. To jest dokładnie ten rodzaj awarii, którego ten projekt nie dopuszcza.

Konsekwencja: **gniazd konfigurowalnych jest trzy, nie cztery.** Zmiana parametru na wykresie
przebudowuje zapytanie gorące — to jedyne miejsce, w którym konfiguracja ekranu wpływa na
warstwę pozyskiwania danych, i musi być jawne.

**Rotacja zimna — pięć PID-ów, jedno zapytanie przy `n % 10 == 5`** (co dziesiąty
cykl, z przesunięciem fazy — sekcja 10.1)**:**

```
46  temperatura otoczenia    ← wejście modelu oleju
1F  czas pracy silnika       ← model oleju + wykrycie nowego przejazdu
42  napięcie sterownika      ← kafel + alarm niskiego napięcia
0F  temperatura dolotu
07  korekta długoterminowa   ← KAFEL (poprawka P1) + reguły ltft_lean / ltft_rich
```

**Pięć, nie sześć — jedno miejsce zostaje wolne.** Limit jednego zapytania to sześć PID-ów;
`2F` wypadł poprawką P1 (sekcja 3.2) i **nic go nie zastępuje**, bo dokładanie parametru tylko
po to, żeby zapełnić miejsce, jest odpytywaniem bez odbiorcy.

**Bez `5C`, `5E` i `10` — to auto ich nie obsługuje** (sekcja 3.2). Kto je tu dopisze,
zamieni jedno zapytanie na dwa i dostanie w zamian pustkę.

### 10.3 Progi alarmów

| Warunek | Waga | Sygnał | Karencja |
|---|---|---|---|
| Temperatura płynu > **105 °C** | pilny | powtarzany | **10 s** |
| Napięcie < **13,0 V** przy obrotach > **500** | uwaga | jednorazowy | **60 s** |
| Nowy kod błędu w trakcie sesji | uwaga | jednorazowy | **60 s** |
| Temperatura oleju < **90 °C** przy obrotach > **4000** | informacja | jednorazowy | **60 s** |

Karencja liczona **per typ alarmu**, żeby jeden warunek nie zagłuszał innego.

### 10.4 Progi reguł przeglądu

| Warunek | Waga | Wniosek |
|---|---|---|
| Kontrolka MIL świeci | usterka | Sterownik zgłasza potwierdzony problem |
| Kody zapisane obecne | usterka | Sterownik zapisał potwierdzone kody |
| Kody oczekujące obecne | uwaga | Wykryte, jeszcze niepotwierdzone |
| Korekta długoterminowa > **+10 %** | uwaga | Mieszanka uboga |
| Korekta długoterminowa < **−10 %** | uwaga | Mieszanka bogata |
| \|krótkoterminowa + długoterminowa\| > **20 %** | usterka | Poważne odchylenie od mapy bazowej |
| Płyn < **70 °C** przy czasie pracy > **10 min** | uwaga | Możliwy zablokowany termostat |
| Płyn > **105 °C** | usterka | Przegrzewanie |
| Napięcie < **13,0 V** przy obrotach > **500** | uwaga | Alternator nie doładowuje |
| Napięcie > **15,0 V** | uwaga | Przeładowanie, podejrzenie regulatora |
| Napięcie **12,0–12,4 V** przy obrotach < **50** | uwaga | Akumulator słabo naładowany |
| Monitory niegotowe | uwaga | Auto nie przejdzie badania emisji |
| Przebieg od skasowania < **100 km** przy zgaszonej kontrolce | informacja | Kody skasowano niedawno |
| Temperatura oleju < **90 °C** | informacja | Silnik nie w pełni rozgrzany |

Silnik uznajemy za pracujący przy obrotach **> 500**, za zgaszony przy **< 50**.

### 10.5 Timeouty

| Kontekst | Timeout | Ponowienia |
|---|---|---|
| **Przegląd** | **25 s** | 2 |
| Nagrywanie | 5 s | 1 |
| Atrapa w testach | 5 s | 0 |

### 10.6 Automat przejazdu i usługa

| Parametr | Wartość | Uwaga |
|---|---|---|
| Ponowienie połączenia w stanie `Rozlaczony` | co **5 s**, bez limitu prób | Adapter budzi się razem z autem |
| Odpytywanie `0C` w stanie `Czuwanie` | co **2 s** | Tylko wykrycie, że silnik ruszył |
| Start nagrywania | obroty **> 0** | Nie zapłon — gniazdo OBD bywa zasilane na stałe |
| Zamknięcie sesji po postoju | obroty **= 0 przez 30 s** | ⚠️ **To nie jest ta sama trzydziestka co checkpoint** |
| Nowy przejazd zamiast ciągu dalszego | spadek PID `1F` o **> 2 s** | Ten sam sygnał resetuje model oleju |
| **Checkpoint przebiegu do bazy** | **co 30 s** | ⚠️ Niezależny od progu powyżej — zbieżność wartości jest przypadkowa |
| Blokada czuwania | `PARTIAL_WAKE_LOCK` tylko w stanie `Nagrywa` | Zwalniany w `finally` |

Dwie trzydziestki w tej tabeli **nie mają ze sobą nic wspólnego** i wolno je zmieniać
niezależnie. Pierwsza mówi, jak długo silnik ma stać, żeby uznać przejazd za skończony; druga,
jak często zabezpieczamy dane przed zabiciem procesu.

### 10.7 Sztywne zakresy osi Y

| Parametr | Zakres |
|---|---|
| Obroty | 0 – 7000 obr/min |
| Prędkość | 0 – 200 km/h |
| Obciążenie | 0 – 100 % |
| Wyprzedzenie zapłonu | −10 – +50 ° |
| Korekta paliwa | −25 – +25 % |
| MAF | 0 – 150 g/s |
| Temperatura płynu | 0 – 130 °C |
| Temperatura oleju | 0 – 150 °C |

Wartość poza zakresem jest **przycinana do krawędzi i oznaczana** — nie rozciąga osi.

---

## 11. Automatyczny cykl przejazdu

### 11.1 Usługa

`DriveService` — usługa pierwszoplanowa typu `connectedDevice`, ze stałym powiadomieniem
pokazującym stan: *rozłączony · czeka na silnik · nagrywa 04:12 · 4,0 Hz*.

Uruchamiana przez `BootReceiver` na `BOOT_COMPLETED`, ręcznie z aplikacji oraz ponownie przez
system (`START_STICKY`). **Nie zakładać, że `BOOT_COMPLETED` przychodzi punktualnie** — usługa
musi znosić start spóźniony o minutę.

### 11.2 Maszyna stanów

`TripStateMachine` — czysta funkcja stanu i zdarzeń, zero Androida, pełne pokrycie testami.

| Stan | Wejście | Zachowanie |
|---|---|---|
| `Rozlaczony` | start usługi, utrata połączenia | próba połączenia co 5 s, **bez limitu prób** |
| `Czuwanie` | transport otwarty, ELM zainicjalizowany | odpytywanie `0C` co 2 s |
| `Nagrywa` | obroty > 0 | pełna pętla, gromadzenie przebiegu |
| `Zamykanie` | obroty = 0 przez **30 s**, rozłączenie, brak miejsca, żądanie przeglądu | odczyt `03`, podsumowanie, zapis, powrót do `Czuwanie` |

**Sygnałem startu są obroty, nie zapłon.** Gniazdo OBD w i40 bywa zasilane na stałe, więc
obecność adaptera niczego nie dowodzi. `01 0C` > 0 dowodzi.

**Nowy przejazd, a nie ciąg dalszy:** spadek PID `1F` o więcej niż 2 s zamyka bieżącą sesję
i otwiera nową. To ten sam sygnał, który resetuje model oleju. Postój z zgaszonym silnikiem
i ponownym odpaleniem daje dwa przejazdy, nie jeden z dziurą.

**Zamykanie zawsze zapisuje.** Utrata połączenia, brak miejsca, ubicie usługi przez system —
sesja zostaje zamknięta i zapisana z tym, co zebrano. **Nigdy nie tracić danych.**

### 11.3 Przegląd kontra nagrywanie

Adapter przyjmuje jednego klienta, a przegląd wymaga wyłączności. Automat startuje nagrywanie,
gdy tylko silnik ruszy, więc bez rozstrzygnięcia przegląd byłby **nieosiągalny**.

**Rozstrzygnięcie: żądanie przeglądu przechodzi przez stan `Zamykanie`.** Bieżąca sesja jest
zamykana normalnie — z odczytem `03`, podsumowaniem i zapisem — i dopiero potem rusza przegląd.
Po jego zakończeniu automat wraca do `Czuwanie` i przy pracującym silniku otwiera **nową**
sesję.

Uzasadnienie: wstrzymanie pętli dałoby jeden przejazd z minutową dziurą, na której całkowanie
dystansu i średnia częstotliwość zaczynają kłamać. Przegląd wykonuje się na postoju, więc
rozdzielenie przejazdu na dwa nic nie kosztuje.

**Przy prędkości powyżej zera przegląd jest zablokowany** razem z resztą interakcji — sekcja 12.4.

### 11.4 Przeżycie minimalizacji, tła i zabicia procesu

Na radiu w aucie aplikacja **prawie nigdy nie jest na wierzchu**. Kierowca przełącza się na
nawigację, radio, CarPlay z telefonu. Nagrywanie musi tego nie zauważyć — i to jest jedyny
powód istnienia tego portu, więc ta sekcja jest wiążąca, nie opisowa.

Trzy poziomy, o rosnącej dotkliwości:

| Zdarzenie | Co ginie | Odpowiedź |
|---|---|---|
| Użytkownik przełącza się na inną aplikację | nic — Activity idzie w `onStop` | Usługa nagrywa dalej |
| System niszczy Activity (pamięć, obrót, zmiana konfiguracji) | widok | Po powrocie ekran odtwarza się **z buforów usługi**, nie od zera |
| **System zabija cały proces** | **usługa i cała jej pamięć** | `START_STICKY` wskrzesza usługę, ale **przebieg z RAM jest bezpowrotnie stracony** — ratuje go wyłącznie checkpoint |

Trzeci wiersz jest realny. Producenci tanich radi mają agresywne zarządzanie zadaniami
i usługa pierwszoplanowa **nie daje przed nim gwarancji** — zmniejsza tylko prawdopodobieństwo.

#### Stan żywy mieszka w usłudze

Bufory kołowe, gromadzony przebieg, stan modelu oleju, karencje alarmów i znacznik startu
sesji **są polami `DriveService`**, nie `ViewModel`. `ViewModel` jest cienką nakładką czytającą
`StateFlow` wystawiony przez usługę.

Activity może umrzeć i wrócić dowolną liczbę razy — nagrywanie tego nie zauważa. Odwrotny
układ, w którym bufory siedzą w `ViewModel`, wygląda naturalniej w Compose i jest tutaj
**błędem**: pierwsze zniszczenie Activity kasuje okno wykresów, a przy nieszczęśliwym
rozwiązaniu również przebieg.

#### Checkpoint — właściwa siatka bezpieczeństwa

**Wiersz przejazdu jest wstawiany przy starcie sesji** ze statusem `w_toku`, a nie przy
zamknięciu. Co **30 sekund** gromadzony przebieg jest zapisywany do tego wiersza. Przy
normalnym zamknięciu: ostatni zapis, podsumowanie, `koniec`, status `zamkniety`.

Bez tego zdanie „nigdy nie tracić danych" z sekcji 11.2 jest nieprawdziwe: czterdziestominutowy
przejazd ubity w trzydziestej dziewiątej minucie znika w całości.

Koszt: dwudziestominutowa sesja to około czterdziestu zapisów o rosnącym rozmiarze, razem
rzędu dziesięciu megabajtów na przejazd. Na eMMC 128 GB to niezauważalne. **Nie optymalizuj
tego zapisem przyrostowym** — format kolumnowy się do tego nie nadaje, a złożoność kupiłaby
oszczędność, której nie widać.

Zapis idzie na `Dispatchers.IO` i **nie dotyka pompy poleceń**. Kopia przebiegu robiona pod tym
samym zamkiem co dopisywanie próbek, sam zapis do bazy już poza zamkiem.

#### Odzyskiwanie po zabiciu procesu

Przy **każdym starcie usługi**, jeszcze przed próbą połączenia: znajdź wiersze ze statusem
`w_toku`, policz podsumowanie z tego, co zdążyło się zapisać, ustaw `koniec` na czas ostatniego
checkpointu i zamknij je statusem **`odzyskany`**.

W historii taka sesja jest oznaczona jako **przerwana**. To nie jest kosmetyka: brakuje jej
ostatnich sekund i odczytu kodów na końcu, więc podawanie jej jako kompletnej byłoby
kłamstwem tego samego rodzaju co zero zamiast `—`.

#### Blokada czuwania

`PARTIAL_WAKE_LOCK` trzymany **wyłącznie w stanie `Nagrywa`**, zwalniany przy przejściu do
`Zamykanie` — w bloku `finally`, żeby wyjątek go nie zostawił.

Usługa pierwszoplanowa **nie zwalnia z tego obowiązku**: chroni przed ubiciem procesu, nie przed
uśpieniem procesora. Bez blokady jądro taniego radia potrafi zasnąć między odpytaniami i pętla
4 Hz degraduje się do nieregularnych skoków, których nie widać inaczej niż przez licznik Hz.

#### Warstwy obrony przed firmware, od najsłabszej

1. `START_STICKY` — system wskrzesza usługę po zabiciu.
2. Powiadomienie nieusuwalne, ze stanem na żywo — utrudnia ubicie i informuje użytkownika.
3. `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` — wymaga zgody użytkownika, prosimy o nią raz.
4. Ręczne dopisanie do autostartu w ustawieniach radia — poza zasięgiem kodu, do instrukcji.

**Żadna z nich nie jest gwarancją.** Gwarancją jest checkpoint, a te cztery zmniejszają tylko
częstość zdarzenia.

#### Powrót do aplikacji

Ekran żywy odtwarza się z buforów kołowych usługi i **pokazuje ostatnie 60 sekund od razu**,
nie pustą siatkę czekającą na napełnienie. Licznik czasu liczy się od znacznika startu sesji
w usłudze, nie od momentu pojawienia się Activity — inaczej po każdym powrocie pokazywałby zero.

Ustawienia — gniazda wykresów, kafle, nastawa tempa, wybór transportu — w `SharedPreferences`.
Przeżywają wszystko, łącznie z reinstalacją, jeśli kopia zapasowa jest włączona.

---

## 12. Interfejs

### 12.1 Układ

Trzy zakładki: **Przegląd**, **Nagrywanie**, **Historia**. Orientacja pozioma — radio nie ma
innej, więc wymuszanie orientacji z wersji iOS **odpada w całości**.

Motyw **NOC jest domyślny**. Decyzja z kontekstu użycia: ekran w desce rozdzielczej świeci
w nocy prosto w oczy.

> ⚠️ **Motywy są dwa, nie jeden.** Rozszerzenie wyglądu dołożyło motyw **DZIEŃ** — ten sam
> ciemny motyw w pełnym słońcu jest nieczytelny. Uzasadnienie powyżej dotyczy jazdy po ciemku
> i obowiązuje bez zmian. Pełne palety obu motywów: **§3 warstwy wyglądu**.

Wartości liczbowe czcionką o stałej szerokości cyfr, żeby nie skakały przy odświeżaniu
(**JetBrains Mono**, cyfry tabelaryczne — §4 warstwy wyglądu).
**Pole nieodczytane pokazuje `—` z powodem. Nigdy zera** — `0 °C` jest nieodróżnialne od
prawdziwego pomiaru i w narzędziu diagnostycznym to najgorsza możliwa decyzja.

### 12.2 Ekran żywy

Zasada nadrzędna: **nie powtarzać deski rozdzielczej.** Prędkość i obroty kierowca ma przed
sobą; pokazywanie ich na radiu zmusza do patrzenia w bok po informację, którą już widzi.

Podział: **liczba dla wartości czytanej jako poziom, wykres dla wartości czytanej jako kształt.**

| Kafle liczbowe | Wykresy przesuwne |
|---|---|
| Temperatura oleju — **model**, ze znacznikiem pewności | Obroty `0C` |
| Temperatura płynu `05` | Obciążenie `04` |
| Napięcie `42` | Wyprzedzenie zapłonu `0E` |
| **Korekta długoterminowa `07`** (poprawka P1) | Korekta krótkoterminowa `06` |

Kafle czytają ostatnią dostępną próbkę, obojętnie z której pętli. Napięcie i korekta długa
siedzą w rotacji zimnej i odświeżają się co około 2,5 s — **to nie jest usterka**, dla wartości
zmieniających się przez minuty jest w zupełności wystarczające.

**Czwarty kafel przy pętli otwartej pokazuje kreskę, nigdy liczby.** Korekta paliwa ma sens
wyłącznie w pętli zamkniętej; poza nią wartość jest zamrożona i nie opisuje bieżącego stanu.
Rozpoznanie stanu pętli wymaga PID-u `0103`, który **dokłada dopiero warstwa kontekstowa** —
do tego czasu kafel pokazuje wartość bez zastrzeżenia, a pełne zachowanie ze znacznikiem
definiuje sekcja 8.8 warstwy diagnostycznej.

```
┌──────────┬──────────┬──────────┬──────────┐
│ 88 °C ~  │  92 °C   │ 13,9 V   │  +3,9 %  │
│ OLEJ mod.│   PŁYN   │ NAPIĘCIE │ KOREKTA D│
├──────────┴──────────┴──────────┴──────────┤
│ OBROTY          ╱‾╲___╱‾‾‾╲__       1726  │
├───────────────────────────────────────────┤
│ OBCIĄŻENIE    __╱‾╲______╱‾╲_        34 % │
├───────────────────────────────────────────┤
│ ZAPŁON        ‾‾╲__╱‾‾╲___╱‾‾        18 ° │
├───────────────────────────────────────────┤
│ 04:12   4,0 Hz   993 zap.   ● NAGRYWA     │
└───────────────────────────────────────────┘
```

**Nie ma przycisku START** — nagrywanie zaczyna się samo. Jest przycisk **zatrzymania**, na
wypadek gdyby ktoś nie chciał zapisu z konkretnego przejazdu.

Każdy wykres ma bieżącą wartość wypisaną po prawej — kształt i liczba w jednym miejscu.

### 12.3 Wykresy przesuwne

**Okno sześćdziesięciu sekund**, przewijane w lewo, najnowsze przy prawej krawędzi. Przy 4 Hz
to 240 punktów na serię — źródłem jest bufor kołowy, decymacja niepotrzebna.

Rysowanie: `Canvas`, `Path` przez punkty. Bez biblioteki.

**Zakresy osi Y są sztywne** (sekcja 10.7). To nie drobiazg: przy skalowaniu automatycznym
spokojna jazda na stałych obrotach wyprodukuje dramatyczną, szarpaną krzywą, bo oś zawęzi się
do szumu. Ekran, na który zerka się w ruchu, musi mieć stałe odniesienie — wtedy kształt
zawsze znaczy to samo i rozpoznaje się go w ułamku sekundy.

### 12.4 Bezpieczeństwo w ruchu

- **Blokada prędkościowa:** gdy `01 0D` pokaże cokolwiek powyżej zera, działa wyłącznie
  zatrzymanie nagrywania. Zmiana parametrów, ustawienia, przegląd i nawigacja po zakładkach
  są zablokowane. Wracają po zatrzymaniu pojazdu.
- `FLAG_KEEP_SCREEN_ON` na czas nagrywania, zdejmowana po zakończeniu.
- Żadnych okien modalnych ani przewijania w trakcie jazdy.
- Duże pola dotykowe — palec w ruchu, nie mysz.

**Nagrywanie leci niezależnie od tego, co jest na ekranie. Patrzenie nigdy nie jest konieczne,
żeby mieć dane.** Dotyczy to także przełączenia się na nawigację i zniszczenia Activity przez
system — sekcja 11.4. Po powrocie wykresy pokazują ostatnie 60 s od razu, a licznik czasu
liczy od startu sesji, nie od pojawienia się ekranu.

### 12.5 Historia

Siatka miesięczna (`LazyVerticalGrid` + `java.util.Calendar`, bez bibliotek). Dni z nagraniami
oznaczone kropką. Dotknięcie dnia rozwija listę przejazdów: godzina, czas trwania, dystans.
Przesunięcie w bok kasuje sesję. Strzałki przełączają miesiąc.

**Sesja o statusie `odzyskany` jest oznaczona jako przerwana** — brakuje jej ostatnich sekund
i odczytu kodów na końcu (sekcja 11.4). Pokazywanie jej jak każdej innej byłoby kłamstwem tego
samego rodzaju co zero zamiast `—`.

Szczegóły sesji: nagłówek z podsumowaniem, pod nim **stos małych wykresów na wspólnej osi
czasu** — nie jeden wykres z wieloma seriami. Nakładanie obrotów na napięcie jest nieczytelne,
a druga oś Y nie rozwiązuje problemu, tylko go ukrywa.

**Wspólny suwak odczytu to najważniejsza interakcja.** Przeciągnięcie palcem przesuwa pionową
linię przez **wszystkie** wykresy naraz, a każdy pokazuje swoją wartość z tej chwili. Dopiero
to pozwala odpowiedzieć na pytanie „co zrobiła korekta paliwa, gdy skoczyły obroty" — czyli na
jedyne pytanie, dla którego warto było te dane zbierać.

### 12.6 Decymacja do wykresów historycznych

Pięć tysięcy próbek wrzuconych wprost w `Canvas` da szarpiące się przewijanie. Redukcja do
szerokości wykresu — ale **przez minimum i maksimum w koszyku, nigdy przez średnią**.
Uśrednianie wygładza dokładnie te skoki, dla których się na wykres patrzy.

```
decymuj(czasy, wartosci, koszyki, zakresCzasu) -> [(Float, Float)]
  1. podziel widoczny zakres czasu na `koszyki` równych przedziałów
  2. dla każdego niepustego koszyka wyemituj DWA punkty:
     (czas minimum, minimum) i (czas maksimum, maksimum), w kolejności czasowej
  3. koszyki puste pomiń
  4. gdy minimum i maksimum to ta sama próbka — jeden punkt
```

`koszyki` równe szerokości wykresu w pikselach, przeliczane przy zmianie powiększenia.

---

## 13. Bezpieczeństwo, uprawnienia i prywatność

### 13.1 Uprawnienia — minimalny zestaw

```xml
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.INTERNET" />   <!-- tylko WifiTransport -->

<service
    android:name=".service.DriveService"
    android:foregroundServiceType="connectedDevice"
    android:exported="false" />
```

**Nie ma `BLUETOOTH_SCAN` i nie ma uprawnień do lokalizacji.** Adapter paruje się raz
w ustawieniach systemu radia, aplikacja czyta go z `bondedDevices`. Skanowanie jest jedynym
powodem, dla którego te uprawnienia byłyby potrzebne, a skanowania nie ma.

`INTERNET` wchodzi wyłącznie dla `WifiTransport` — gniazdo TCP do adaptera w sieci lokalnej.
**Nie ma żadnego ruchu poza tym.**

### 13.2 Prywatność

Wszystko zostaje na urządzeniu. VIN, kody błędów i przebiegi **nie opuszczają radia**, mimo że
ma ono modem LTE i kartę SIM. Nie ma backendu, nie ma telemetrii, nie ma analityki.

Baza jest lokalna. Sesja wychodzi na zewnątrz wyłącznie wtedy, gdy użytkownik sam ją
wyeksportuje — a eksport jest nie-celem tej wersji.

**Repozytorium publiczne zawiera wyłącznie kod i zapis techniczny adaptera.** VIN pojawia się
w specyfikacji i w podglądach, bo jest identyfikatorem tego egzemplarza i bez niego zapis
z auta traci sens jako materiał testowy — ale nie ma w repozytorium żadnych danych
osobowych ani lokalizacyjnych.

### 13.3 Czego nie logujemy

W logu wyłącznie: znacznik czasu, polecenie ELM, długość odpowiedzi, czas obiegu.
**Treść odpowiedzi trafia do logu tylko na ekranie diagnostycznym, na żądanie użytkownika** —
tam jest potrzebna, bo służy do wklejenia w rozmowę przy diagnozowaniu adaptera.

---

## 14. Testy

JUnit 5, **na JVM, bez emulatora**. Cała warstwa czysta jest testowalna offline; transport jest
weryfikowany na sprzęcie w etapie 10.

Wersja iOS ma **153 funkcje testowe w 30 plikach**. **Port ma mieć nie mniej.**

| Obszar | Test | Dlaczego akurat ten |
|---|---|---|
| Dekodery PID-ów | tabela: hex → wartość i jednostka | Pomyłka w mnożniku daje cichy, wiarygodnie wyglądający błąd |
| Dekoder DTC | rozróżnienie generycznych i producenckich | Wymyślony opis wygląda jak prawdziwy |
| Składanie VIN-a | także z odpowiedzi pociętej między ramkami | |
| Maski PID-ów | łańcuch czterech zakresów **oraz odjęcie bitów `20 40 60 80`** | Bez odjęcia aplikacja odpytuje znaczniki |
| Maski — regresja | `4100BE3EA813` daje dokładnie zbiór z sekcji 3.2 | **Pilnuje, że dekoder zgadza się z prawdziwym autem** |
| Monitory gotowości | rozróżnienie „nieobsługiwany" i „niegotowy" | |
| **Ramkowanie strumienia** | odpowiedź pocięta w złych miejscach daje jedną całość; dwie odpowiedzi w jednym kawałku rozdzielają się | Pułapka nr 1 tego projektu |
| **Szeregowanie pompy** | wiele poleceń współbieżnie → do transportu pojedynczo i **w kolejności** | `Mutex` przeszedłby część tego testu i oblał kolejność |
| Budowa zapytania wielo-PID | poprawny ciąg dla zadanego zestawu | |
| Rozbiór wielo-PID | wieloramkowy oraz przypadek mniejszej liczby PID-ów niż w zapytaniu | |
| Sonda multi/single | samotna odpowiedź na pierwszy PID **nie** dowodzi wsparcia | |
| **Skład pętli gorącej** | `0D`, `05` i `04` nie wypadają nawet przy komplecie trzech gniazd wykresów | Brak `0D` wyłącza blokadę prędkościową, brak `05` alarm przegrzania, brak `04` **cicho zamraża model oleju** |
| Tempo pętli | przy 4 Hz i obiegu 50 ms nie przekracza sufitu; przy obiegu 400 ms **nie kolejkuje** | |
| Zwalnianie przy błędach | seria `NO DATA` obniża częstotliwość; dziesiąty zatrzymuje | |
| Bufor kołowy | nadpisywanie po zapełnieniu, kolejność czasowa zachowana | |
| **Model oleju** | reset przy spadku `1F`; zbieżność do `płyn + 25·obciążenie`; progi pewności | |
| `TrackBlob` | obieg w obie strony bez utraty danych | |
| Podsumowanie | dystans całkowany z prędkości; **`paliwoL` jest `null`, gdy serii `5E` nie ma** | Pilnuje uczciwości pola, którego to auto nie wypełni |
| Reguły alarmów | każdy warunek z tabeli 10.3, w tym karencja per typ | |
| **Nowy kod DTC** | alarm odpala się, gdy `dtcsNow` zawiera kod spoza `dtcsAtStart` | **Domknięcie rozbieżności 2 — w wersji iOS ten tor był martwy** |
| Reguły przeglądu | każdy warunek z tabeli 10.4 | |
| Werdykt | kolejność z sekcji 8.8, w tym pierwszeństwo usterki nad uwagą | |
| **Decymacja** | koszyki min-max zachowują skok, którego średnia by nie zachowała | To jest test pilnujący sensu całego algorytmu |
| Wybór pary GATT | znana para Vgate wygrywa; para z różnych usług jako ostatnia deska | |
| Atrapa | stan echa `ATE0`/`ATE1`; drugie `0101` dostaje drugi wpis | Bez tego atrapa rozjeżdża się z adapterem |
| **Maszyna stanów przejazdu** | start z obrotów, koniec po 30 s postoju, nowy przejazd przy spadku `1F`, zapis przy rozłączeniu, przejście przez `Zamykanie` przy żądaniu przeglądu | Błąd tutaj widać dopiero, gdy zabraknie nagrania |
| **Checkpoint** | sesja `w_toku` jest czytelna z bazy **w trakcie** nagrywania, a jej przebieg rośnie między checkpointami | Bez tego testu dziura z 11.4 wraca niezauważona |
| **Odzyskiwanie** | wiersz zostawiony jako `w_toku` zostaje przy starcie usługi zamknięty jako `odzyskany`, z podsumowaniem z częściowego przebiegu i pustym `kodyNaKoncu` | Symuluje zabicie procesu — jedyny sposób, żeby to przetestować bez ubijania procesu |
| **Właściciel stanu żywego** | bufory i przebieg są polami usługi; zniszczenie i odtworzenie warstwy widoku nie zeruje okna wykresów | Pilnuje, żeby nikt nie „uprościł" tego, przenosząc bufory do `ViewModel` |
| Sesja end-to-end na atrapie | od połączenia do zapisanego przejazdu z podsumowaniem | |

**Zegar pętli jest wstrzykiwany**, żeby testy tempa nie czekały w czasie rzeczywistym.

Zasady dyscypliny, przeniesione z `fa3-check`:

- **TDD obowiązkowe.** Najpierw test, uruchomiony i pokazany jako czerwony, potem implementacja.
  Test, którego nikt nie widział czerwonego, nie jest testem — jest życzeniem.
- **Nigdy nie osłabiaj asercji, żeby test przeszedł.** Jeśli test nie przechodzi, zepsuty jest
  kod albo zepsute jest założenie testu — rozstrzygnij które, powiedz wprost i napraw właściwą
  rzecz.
- **Nie dobieraj danych testowych pod implementację.** Dane pochodzą z zapisu z auta albo
  z tablic w sekcji 9, nie z tego, co akurat zwraca kod.

---

## 15. Przepływ prac i weryfikacja na sprzęcie

### 15.0 Dwa tory, dwa punkty styku

Najczęstsze nieporozumienie przy tym projekcie brzmi: „najpierw atrapa, potem połączenie, potem
instalacja". **To nie jest jeden łańcuch.** Atrapa nie jest krokiem, który się wykonuje i mija —
jest **środowiskiem, w którym powstaje cała aplikacja**, na komputerze, bez radia i bez auta.
Radio to osobny tor, który da się przetestować, zanim powstanie linijka kodu.

```
TOR A — kod na PC                    TOR B — radio na biurku
─────────────────────                ────────────────────────
etap 0  atrapa + zapis z auta        (paczka w drodze)
etap 1  ELM                                    │
etap 2  OBD                          radio przychodzi
etap 3  pętla                        15.1 A — testy bez aplikacji
etap 4  zapis                                  │
etap 5  alarmy + usługa              wynik: która pozycja
etap 6  przegląd                     transportu jest domyślna
etap 7  interfejs                              │
etap 8  historia                               │
   │                                           │
   └───────────────┬───────────────────────────┘
                   ▼
      STYK 1 — pierwsza instalacja APK z atrapą (15.1 B)
                   ▼
      STYK 2 — etap 9: przełączenie na prawdziwy transport
                   ▼
      montaż w aucie → 15.2 → etap 10
```

**Styk 1 jest ważniejszy, niż wygląda.** Po etapie 8 aplikacja jest kompletna i chodzi na
zapisie z auta. Wgrana na radio z transportem `Atrapa` odpowiada na pytania, które **nie mają
nic wspólnego z Bluetoothem**: czy APK się instaluje, czy usługa pierwszoplanowa startuje bez
wyjątku, czy układ mieści się na ekranie, czy przeżywa restart, czy nic nie zamula przy 4 Hz.

To jest cały powód, dla którego domyślnym transportem jest `Atrapa`, a nie któryś prawdziwy:
**rozdzielenie pytania „czy aplikacja działa na tym radiu" od pytania „czy Bluetooth działa na
tym radiu".** To dwa różne problemy z różnymi rozwiązaniami i mieszanie ich kosztuje dni.

### 15.1 Na biurku, zanim cokolwiek zostanie wkręcone w auto

Radio na zasilaczu 12 V: żółty stałe B+, czerwony ACC, czarny masa.

**Uwaga praktyczna:** vLinker bierze zasilanie z gniazda OBD, więc na biurku sam nie wstanie.
Kroki wymagające adaptera zrobisz albo podając mu 12 V na pin 16 i masę na piny 4/5, albo przy
aucie. Kroki bez adaptera — od razu po rozpakowaniu.

#### A. Przy dostawie radia — aplikacja niepotrzebna

| # | Krok | Co robi | Czego dowodzi | Adapter |
|---|---|---|---|---|
| 1 | **nRF Connect**, skan BLE | Skanuje otoczenie aplikacją zewnętrzną | **Czy stos BLE jest w ogóle widoczny dla aplikacji.** Pustka → firmware trzyma Bluetooth dla siebie i BLE odpada na zawsze | nie — wystarczy telefon obok |
| 2 | Parowanie vLinkera w ustawieniach systemu | Dodaje adapter do `bondedDevices` | Aplikacja nie będzie skanować — stąd brak `BLUETOOTH_SCAN` w manifeście | tak |
| 2a | **Odczyt `device.uuids`** | Wypisuje profile sparowanego urządzenia | **Czy adapter umie SPP.** Jest `00001101-0000-1000-8000-00805F9B34FB` → SPP istnieje i jest transportem domyślnym. Brak → wariant `MC-IOS` jest BLE-only (sekcja 3.4) i domyślnym zostaje BLE | tak |
| 3 | **Car Scanner** + vLinker | Cudza, dojrzała aplikacja czyta PID-y | **Że cała ścieżka działa.** Test rozstrzygający winę: gdy Car Scanner nie umie, nie ma czego debugować w Kotlinie | tak |
| 4 | Opcje programisty → **ADB po sieci** | `adb connect <ip>:5555` | Kanał wgrywania buildów bez biegania do auta z pendrivem | nie |

Kolejność nie jest przypadkowa. **Car Scanner idzie przed własną aplikacją celowo** — jest
punktem odniesienia, który oddziela usterkę sprzętu od usterki kodu.

**Zielone 1–3 → montaż. Czerwone 1 → zwrot w 14 dni albo adapter ELM327 po Wi-Fi za ~150 zł.**
Radio ma modem LTE, więc przy transporcie Wi-Fi nie traci internetu — to jedyny minus tej drogi
i on tu nie występuje.

#### B. Po etapie 8 — pierwsza instalacja, wciąż na atrapie

| # | Krok | Czego dowodzi |
|---|---|---|
| 5 | Wgranie APK, transport ustawiony na **`Atrapa`** | Aplikacja instaluje się i uruchamia na tym SoC |
| 6 | Uruchomienie usługi | `startForeground` z typem `connectedDevice` nie rzuca wyjątkiem na Androidzie 14 |
| 5a | **Odczyt `densityDpi` i `smallestScreenWidthDp`** | **Ile `dp` naprawdę mamy na ekranie.** Karta produktu mówi 8", instrukcja z pudełka 9" i 10" — rozdzielczość jest ta sama, gęstość nie. Wynik wpisuje się do §7 warstwy wyglądu |
| 5b | **Odczyt wersji Androida** | Karta produktu mówi 14, instrukcja 13. Na 13 jest **łatwiej** — Android 14 zaostrzył wymagania wobec typów usług pierwszoplanowych |
| 6a | **Przycisk „wstecz" w trakcie nagrywania** | Instrukcja radia mówi, że **wstecz zamyka aplikację**, a nie cofa nawigację. Jeśli tak jest, **ubicie aplikacji to normalna ścieżka, nie awaria** — odzyskiwanie sesji będzie się uruchamiać codziennie |
| 6b | **Uśpienie radia w trakcie nagrywania** | Radio ma przycisk uśpienia. Czy usługa przeżywa — nie da się zgadnąć. Jeśli ginie, przejazd urywa się przy każdym postoju z wygaszonym ekranem |
| 7 | Przejście przez trzy zakładki | Układ mieści się na ekranie, kafle i wykresy czytelne z fotela, nic nie zamula przy 4 Hz |
| 8 | **Restart radia** | Usługa wstaje sama po `BOOT_COMPLETED` |

**Krok 6a jest najważniejszy z całej listy B.** Zmienia status mechanizmu odzyskiwania sesji
z zabezpieczenia awaryjnego na codzienne działanie — a wtedy jego jakość decyduje o tym, czy
zbiór nagrań jest kompletny, czy dziurawy.

**Czerwone 8 nie blokuje projektu** — zostaje ręczne uruchomienie usługi z aplikacji plus
autostart w ustawieniach radia. Sprawdź wtedy `logcat`: komunikat odmowy nie trafia na ekran.

Dopiero po zielonym B przechodzisz do etapu 9 i przełączasz transport na wynik kroku 2a.
**Pierwsze prawdziwe połączenie też robi się na biurku, nie w desce.**

### 15.2 W aucie

| # | Krok | Co sprawdzamy |
|---|---|---|
| 1 | Krótki przejazd | Realna częstotliwość na ekranie; sesja zapisana; podsumowanie sensowne |
| 2 | Postój, zgaszenie i ponowne odpalenie | **Dwa przejazdy w historii, nie jeden** |
| 3 | Przegląd przy pracującym silniku | Sesja zamknięta i zapisana; po przeglądzie nowa |
| 4 | Uzupełnienie `MockI40Script` | Prawdziwe odpowiedzi wielo-PID trafiają do atrapy |
| 5 | **Kalibracja modelu oleju** | Termometr IR na misce olejowej po 20 min jazdy → korekta `tauBase` |

Krok 4 jest ważniejszy, niż wygląda: dopiero po nim testy pętli gorącej działają na prawdziwych
odpowiedziach wielo-PID, a nie na wygenerowanej fali.

---

## 16. Kolejność realizacji

Rządzi tym zasada z sekcji 2: **prawdziwy Bluetooth powstaje na końcu.** Każdy etap kończy się
kompilującym się projektem i przechodzącymi testami.

| Etap | Zawartość | Ukończony, gdy |
|---|---|---|
| **0** | Projekt Android, manifest, uprawnienia, CI, atrapa + `MockI40Script`, słownik DTC | Atrapa oddaje zapisane odpowiedzi; testy atrapy zielone |
| **1** | `elm/` — komunikaty, ramkowanie, pompa, timeouty, ponowienia | **Test szeregowania i test ramkowania przechodzą** |
| **2** | `obd/` — PID, katalog, maski, DTC, VIN, gotowość, MultiFrame, MultiPID | Test regresji masek daje dokładnie zbiór z sekcji 3.2 |
| **3** | `acquisition/` — sonda, pętla, bufor kołowy, model oleju | Tempo i zwalnianie działają na sztucznym zegarze |
| **4** | `storage/` — TrackBlob, SQLite, rejestrator, podsumowanie, **checkpoint i odzyskiwanie** | Obieg blobu w obie strony; `paliwoL` jest `null`; sesja `w_toku` czytelna w trakcie i odzyskiwalna |
| **5** | `alerts/` + `service/` — reguły alarmów, maszyna stanów, usługa jako właściciel stanu żywego, `PARTIAL_WAKE_LOCK`, `BootReceiver` | Maszyna stanów pokryta testami; alarm nowego DTC się odpala; usługa startuje na emulatorze. **Restart prawdziwego radia weryfikuje się w 8½** |
| **6** | `checkup/` — orkiestracja przeglądu, reguły, werdykt, raport | Pełny przegląd na atrapie od `ATZ` do werdyktu |
| **7** | `ui/` — motyw, ekran żywy, kafle, wykresy `Canvas`, blokada prędkościowa | Wszystkie stany widoczne na atrapie |
| **8** | `charts/` + historia — decymacja, kalendarz, stos wykresów, wspólny suwak | Suwak przesuwa linię przez wszystkie wykresy naraz |
| **8½** | **STYK 1 — pierwsza instalacja APK na radiu, transport `Atrapa`** (sekcja 15.1 B) | Aplikacja chodzi na radiu, usługa wstaje po restarcie. **Bez udziału Bluetootha** |
| **9** | **Transporty prawdziwe** — wszystkie trzy. Kolejność pisania dowolna; **domyślną wybiera test `device.uuids` z kroku 2a sekcji 15.1**, nie ten dokument | Aplikacja łączy się z adapterem na biurku |
| **10** | Weryfikacja w aucie, uzupełnienie atrapy o prawdziwe odpowiedzi, kalibracja modelu oleju | Sekcja 15.2 |

Po każdym etapie:

```
./gradlew ktlintCheck ; if ($?) { ./gradlew lint } ; if ($?) { ./gradlew test }
```

**Nie twierdź, że etap jest gotowy, dopóki nie zobaczysz wyniku na własne oczy.**

---

## 17. Ryzyka

| Ryzyko | Prawdopodobieństwo | Reakcja |
|---|---|---|
| Radio nie wystawia Bluetootha aplikacjom | średnie | Test nRF Connect **przed montażem**; `WifiTransport` w projekcie od początku |
| **Adapter `MC-IOS` nie obsługuje SPP** | **średnie–wysokie** | Sekcja 3.4. Sprawdzić `device.uuids` po sparowaniu. Wtedy zostaje BLE — udowodniony po stronie adaptera — albo Wi-Fi |
| SPP niedostępny **i** BLE GATT niewidoczny dla aplikacji | niskie | Oba naraz odpadają tylko przy pechu. Wtedy adapter ELM327 po Wi-Fi za ~150 zł, a modem LTE ratuje internet |
| `BOOT_COMPLETED` nie przychodzi na tym firmware | średnie | Ręczne uruchomienie usługi + autostart w ustawieniach radia |
| **Producent radia ubija cały proces w trakcie przejazdu** | **średnie** | Cztery warstwy obrony z 11.4, ale **właściwą siatką jest checkpoint co 30 s**. Strata ograniczona do ostatnich 30 s, sesja oznaczona jako `odzyskany` |
| Jądro radia usypia procesor między odpytaniami | średnie | `PARTIAL_WAKE_LOCK` w stanie `Nagrywa`; objaw widoczny wyłącznie na liczniku Hz |
| Android 14 blokuje typ usługi z `BOOT_COMPLETED` | niskie | Zweryfikować w `logcat` przy pierwszym wgraniu — komunikat odmowy nie trafia na ekran |
| Realna częstotliwość niższa od nastawy | **wysokie** | Licznik Hz mówi prawdę. **Nie udawać 4 Hz, gdy są 2** |
| Model oleju rozjeżdża się z rzeczywistością | średnie | Skala pewności widoczna zawsze; kalibracja `tauBase` termometrem IR |
| Adapter nie obsługuje wielu PID-ów w zapytaniu | niskie | Sonda przy starcie; tryb pojedynczy i komunikat na ekranie; częstotliwość spada do ~1,5 Hz |
| Konflikt z aplikacją na iPhonie o adapter | **pewne** | Jeden klient naraz. Przegląd przenosi się na radio — dlatego zakres v1 obejmuje wszystkie trzy zakładki |
| Sterownik zapisze kod komunikacji po długiej sesji | niskie | Odczyt `03` na starcie i końcu ujawni to od razu |

---

## 18. Źródła

Pełna bibliografia w `docs/zrodla.md`. Skrótowo:

| Rodzaj | Źródło |
|---|---|
| Formuły PID-ów | SAE J1979, przez katalog w `i40-check` i `PIDCatalog.swift` |
| Zachowanie ELM327 | Karta katalogowa ELM327 v2.2 + **zapis z prawdziwego adaptera** |
| Progi reguł i alarmów | `2026-08-07-i40-check-ios-design.md`, `2026-08-08-...-rejestrator-design.md` |
| **Wartości ostateczne** | **Kod `_github/ios-obd2-ble-diagnostics`** — przy rozbieżności wygrywa kod |
| Maski i możliwości egzemplarza | `MockI40Script.swift` — zapis z 2026-08-08 |
| Sprzęt docelowy | Zamówienie #124599, autonawigacje.pl, karta produktu PR9 8/128 |

**Reguła nadrzędna:** liczba, której nie da się zaczepić w jednym z tych źródeł, nie istnieje.
Gdy specyfikacja iOS i kod iOS mówią co innego — **wygrywa kod**, bo to on jeździł samochodem,
a rozbieżność trafia do sekcji 3.1 tego dokumentu.
