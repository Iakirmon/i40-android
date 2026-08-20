# i40-android

Port aplikacji diagnostycznej OBD-II `i40-check` z iOS na radio z Androidem zamontowane
w Hyundaiu i40. Kotlin, Jetpack Compose, Android 14.

**Dokumenty nadrzędne:**

- `docs/spec/2026-08-14-i40-android-design.md` — projekt bazowy, etapy 0–10
- `docs/spec/2026-08-14-i40-android-diagnostyka-design.md` — warstwa diagnostyczna, etapy D1–D10,
  **wchodzi po etapie 8**
- `docs/spec/2026-08-14-i40-android-kontekst-design.md` — warstwa kontekstowa, etapy K1–K7,
  **wchodzi po D9**. Tlumaczy liczby zamiast dokladac nowe: status petli, przedmuchiwanie,
  panel Powietrze, karta miesiaca
- `docs/spec/2026-08-15-i40-android-wyglad-design.md` — warstwa wyglądu, etapy W1–W5, po S4.
  **Dwa motywy: NOC (domyślny) i DZIEŃ.** Tokeny koloru i typografii, pole kalibrowane,
  zakazy rysowania, jedna animacja. **Zero nowych zapytań OBD, zero nowych progów.**
- `docs/spec/2026-08-15-i40-android-objasnienia-design.md` — warstwa objaśnień, etapy S1–S5,
  po H7. Panel Stan (szósty, pierwszy w kolejności) i słownik 70 haseł. **Zero nowych zapytań
  OBD, zero nowych progów.** Treść haseł: `docs/slownik.md` — **dokument źródłowy, przenoszony
  co do zdania, nigdy generowany**.
- `docs/spec/2026-08-15-i40-android-historia-design.md` — warstwa historii, etapy H1–H7,
  po O8. Kasowanie przejazdów, panel Porządki, porównanie dwóch przejazdów, karta
  „od początku”, filtry. **Zero nowych zapytań OBD.** Podnosi schemat bazy do wersji 3.
- `docs/spec/2026-08-14-i40-android-odniesienie-design.md` — warstwa odniesienia, etapy O1–O8,
  **wchodzi po K6**. Pokazuje, ile parametr dawal w tym aucie POPRZEDNIO. **Nie dodaje ani
  jednego zapytania OBD** — liczy z probek, ktore juz plyna

Kazda warstwa jawnie wymienia, co w poprzednich zmienia

Gdy kod i spec się rozjeżdżają, wygrywa spec, a rozbieżność zgłoś.

**Bibliografia:** `docs/zrodla.md`. Liczba, której nie da się zaczepić w jednym z tych źródeł,
nie istnieje.

Pełne zasady dla Cursora są w `.cursor/rules/`. Ten plik jest skrótem dla narzędzi, które ich
nie czytają.

## Obietnica projektu

> Każda liczba pochodzi z pomiaru albo z nazwanego modelu. Nieudany odczyt mówi „—", nigdy „0".

Powodem istnienia portu jest jedna rzecz: **radio wstaje z zapłonem**, więc komplet przejazdów
powstaje sam. Na iPhonie trzeba było pamiętać, żeby wyjąć telefon i nacisnąć START. Wszystkie
różnice wobec wersji iOS wynikają z tego faktu albo z platformy.

## Przepływ prac — dwa tory

Kod (etapy 0–8) powstaje **w całości na atrapie, na PC**. Radio testuje się osobno i wcześniej
(sekcja 15.1 A). Spotykają się dopiero w **STYKU 1**: pierwsza instalacja APK na radiu
z transportem `Atrapa`, po etapie 8 — sprawdza aplikację **bez udziału Bluetootha**.
Dopiero potem etap 9 przełącza na prawdziwy transport. Sekcja 15.0 specu.

## Zasada, z której wynika architektura

> **Wszystko powyżej transportu daje się przetestować bez auta.**

Prawdziwy Bluetooth powstaje w **etapie 9**, po całej logice i całym interfejsie. Wcześniej
wszystko stoi na atrapie, która odtwarza **prawdziwy zapis z tego auta** z 2026-08-08.

> **Wierność dotyczy liczb, nie prozy.**

Formuły, progi i stałe przenosimy co do wartości. Nazwy i idiomy dostosowujemy do Kotlina.

## Siedem rozbieżności — przeczytaj sekcję 3 specu

Specyfikacje iOS i kod iOS mówią co innego w siedmiu miejscach. **Za każdym razem rację ma
kod**, bo to on jeździł samochodem. Trzy, które najczęściej psują port:

- **Timeout przeglądu to 25 s, nie 5.** `SEARCHING...` przy pierwszej negocjacji potrafi
  ciągnąć się kilkanaście sekund. Port z 5 s wywali się na pierwszym połączeniu.
- **To auto nie obsługuje PID `5C`, `5E` ani `10`** — zdekodowane z prawdziwych masek.
  Temperatura oleju to **model termiczny**, a `paliwoL` w podsumowaniu zawsze `null`.
- **`2F` jest w masce i mimo to nie daje danych** — poprawka P1. Zwraca zero niezależnie od
  stanu baku. Nie odpytywany, nigdzie nie wyświetlany; czwarty kafel to **korekta długa `0107`**.
  **Maska to deklaracja sterownika, nie obietnica danych.**
- **Zamrożona ramka (tryb 02) nie jest zaimplementowana** mimo obietnicy w specyfikacji iOS.
  Jest nie-celem, nie luką do uzupełnienia.

## Niezmienniki

1. **Nieudany odczyt to `—` z powodem, nigdy zero.** `0 °C` jest nieodróżnialne od pomiaru.
2. Warstwy `obd/`, `acquisition/`, `rules/`, `charts/` i `service/TripStateMachine` są
   **czyste**: bez `android.*`, bez I/O, bez zegara. Zegar jest wstrzykiwany.
3. `transport/` jest **jedynym** miejscem dotykającym sprzętu.
4. **Pompa poleceń stoi na `Channel`, nigdy na `Mutex`** — `Mutex` nie jest kolejką FIFO.
5. **Odpowiedź ELM327 kończy `>`, nie koniec linii.** Pojedynczego kawałka nigdy nie parsuj.
6. **Katalog PID-ów jest jedynym źródłem długości danych.** Nieznany PID kończy parsowanie.
7. **Bity `20 40 60 80` nie są PID-ami** — odejmij je ze zbioru obsługiwanych.
8. **`0D`, `05` i `04` nigdy nie wypadają z pętli gorącej** — od nich zależą blokada
   prędkościowa, alarm przegrzania i **model temperatury oleju**. Gniazd konfigurowalnych
   jest trzy.
9. **Osie Y wykresów są sztywne.** Skalowanie automatyczne to błąd projektowy, nie brak funkcji.
10. **Decymacja przez min-max, nigdy przez średnią.**
11. **Zamykanie sesji zawsze zapisuje** — a przy nagłym zabiciu procesu ratuje to
    **checkpoint co 30 s** i odzyskiwanie sesji `w_toku`. Sekcja 11.4.
11a. **Stan żywy mieszka w usłudze, nie w `ViewModel`.** Activity na radiu umiera często.
11b. **`PARTIAL_WAKE_LOCK` w stanie `Nagrywa`** — usługa pierwszoplanowa nie chroni przed
    uśpieniem procesora.
12. **Temperatura oleju jest podpisana jako model, ze skalą pewności.**
12a0. **`norma` i `poprzednio` to dwie różne rzeczy i dwie różne etykiety.** Norma mówi, ile
    powinno być; `poprzednio` mówi, ile było w tym aucie. Wartość z historii **nigdy** nie
    może zostać opisana jako norma.
12a. **Każda wyświetlana wartość ma pasmo odniesienia albo jawne `—`** — sekcja 8.8
    rozszerzenia. Pasmo pochodzi z reguły albo ze źródła, nigdy nie powstaje dla ekranu.
12b. **Pasmo to nie alarm.** Przekroczenie zmienia znacznik, nie wydaje dźwięku. Alarmuje
    pięć warunków krytycznych.
13. **Nic nie wychodzi na zewnątrz** — mimo że radio ma modem LTE.

## Czego nie wymyślasz

Formuł PID-ów, opisów kodów DTC, progów reguł i alarmów, bezpieczników pętli, stałych modelu
oleju, timeoutów, sekwencji inicjalizacji ELM, możliwości tego egzemplarza i UUID-ów adaptera.
Wszystko ma konkretne miejsca w sekcjach 9 i 10 specu.

Gdy nie wiesz: powiedz, że nie wiesz, i powiedz, czego potrzebujesz. To poprawna odpowiedź.

Szczegóły: `.cursor/rules/30-zrodla.mdc`.

## Zależności

Dozwolone: `android.*`, `java.*`, `kotlinx.coroutines`, Jetpack Compose.

Zabronione bez pytania: Room, KSP, `kotlinx-serialization`, biblioteki wykresów, Hilt, Timber,
Retrofit, OkHttp, biblioteki do dat.

Baza to `SQLiteOpenHelper`, JSON to `org.json`, wykresy to `Canvas`, daty to `java.util.Calendar`.

Compose jest **odstępstwem nazwanym wprost** w sekcji 6.1 specu — na Androidzie jest biblioteką
w APK, nie częścią systemu. Decyzja ma termin ważności do etapu 7.

## Tryb pracy

Jeden etap z sekcji 16 specu na raz. TDD obowiązkowe: najpierw test, uruchomiony i pokazany
jako czerwony, potem implementacja.

Po każdym etapie:

```
./gradlew ktlintCheck ; if ($?) { ./gradlew lint } ; if ($?) { ./gradlew test }
```

Nie twierdź, że coś jest gotowe, dopóki nie zobaczysz wyniku. Nie commituj i nie pushuj bez
wyraźnej prośby. Nie refaktoryzuj poprzednich etapów bez prośby.

**Wersja iOS ma 153 funkcje testowe w 30 plikach. Port ma mieć nie mniej.**

## Sprzęt docelowy

Radio PR9 8/128 (autonawigacje.pl, zamówienie #124599 z 2026-08-13), SoC UIS7862,
Android 14, ekran 1280×720 poziomy, Bluetooth 5.0, Wi-Fi, modem LTE.
**Wersja Androida i przekątna niepewne** — instrukcja z pudełka mówi Android 13 i 9"–10";
rozdzielczość i SoC się zgadzają. Rozstrzyga krok 5a/5b z §15.1 B bazowego.

Adapter: vLinker, ELM327 v2.2. **Wariant `MC-IOS` — wsparcie SPP niepotwierdzone,
patrz sekcja 3.4 specu.** BLE jest udowodniony po stronie adaptera, SPP i Wi-Fi po stronie
radia. Transportu domyślnego **nie deklarujemy** — rozstrzyga test z sekcji 15.1.
Wszystkie trzy powstają w etapie 9.

Auto: Hyundai i40 2015, 2.0 GDI (Nu G4NC), protokół ISO 15765-4 CAN 11-bit 500 kbit/s.
