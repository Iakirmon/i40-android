# i40-android — prompty dla Cursora

Jedenaście etapów, **jeden nowy czat na etap**. Prompty są też dostępne jako skill —
`/etap 3` robi to samo, co wklejenie promptu etapu 3.

---

## Jak to prowadzić

**Nowy czat na każdy etap.** Kontekst się zapycha, a pierwszym objawem jest to, że agent
zaczyna wpisywać stałe „z pamięci" zamiast z sekcji 10 specu.

**Do każdego promptu dołącz spec:** `@docs/spec/2026-08-14-i40-android-design.md`

**Przeczytaj sekcję 3 specu, zanim zaczniesz cokolwiek.** Zawiera siedem rozbieżności między
specyfikacją iOS a kodem iOS oraz zdekodowane maski PID-ów tego konkretnego auta. Agent, który
ich nie zna, napisze kod odpytujący PID-y, których ten samochód nie ma, i port z timeoutem 5 s,
który wywali się na pierwszym połączeniu.

**Po każdym etapie sam uruchom weryfikację:**

```
./gradlew ktlintCheck ; if ($?) { ./gradlew lint } ; if ($?) { ./gradlew test }
```

**Cztery pytania kontrolne, które warto zadawać często:**

*„Skąd wziąłeś tę liczbę?"* — musi być z sekcji 9 albo 10 specu. Jeśli odpowiedź brzmi
„standardowo przyjmuje się", stała jest wymyślona.

*„Czy to auto obsługuje ten PID?"* — sekcja 3.2. `5C`, `5E` i `10` **nie**. Odpytywanie ich to
zmarnowane zapytanie i pewna pustka.

*„Czy ten test widziałeś jako czerwony?"* — jeśli nie, to nie jest test, tylko życzenie.

*„Czy to jest kod, którego oryginał nie ma?"* — sekcja 3.1 wymienia trzy funkcje obiecane przez
specyfikację iOS, a niezaimplementowane. Dwie domykamy świadomie, trzecia (zamrożona ramka) jest
nie-celem.

---

## Etap 0 — projekt, atrapa i zapis z auta

```
Przeczytaj @docs/spec/2026-08-14-i40-android-design.md, szczególnie sekcje 2, 3 i 6.
Realizujemy etap 0 z sekcji 16. W tym etapie NIE piszemy jeszcze logiki OBD.

1. Projekt Android: Kotlin, minSdk 31, targetSdk 34, compileSdk 34.
   Pakiet pl.i40.android. Jetpack Compose włączony.

   ZALEŻNOŚCI — sekcja 6.1 specu jest wiążąca. Dozwolone: android.*, java.*,
   kotlinx.coroutines, Jetpack Compose. ZABRONIONE bez pytania: Room, ksp,
   kotlinx-serialization, jakakolwiek biblioteka wykresów, Hilt, Timber, Retrofit, OkHttp.
   Jeśli uważasz, że któraś jest potrzebna — zapytaj i uzasadnij.

2. AndroidManifest z uprawnieniami z sekcji 13.1 — DOKŁADNIE tymi, ani jednego więcej.

   UWAGA: NIE dodawaj BLUETOOTH_SCAN ani ACCESS_FINE_LOCATION. Adapter paruje się raz
   w ustawieniach systemu radia, aplikacja czyta go z bondedDevices i nie skanuje.
   To jest decyzja projektowa z sekcji 6, nie przeoczenie.

3. LICENSE (MIT), .gitignore, .editorconfig, konfiguracja ktlint.

4. transport/Transport.kt — interfejs z sekcji 8.1, jeden do jednego.

5. transport/MockTransport.kt — atrapa. NIE jest to proste "polecenie → tekst".
   Sekcja 8.1 wymienia pięć zachowań, wszystkie obowiązkowe:
   - porównanie polecenia bez względu na wielkość liter i spacje,
   - symulacja stanu echa: ATZ i ATE1 włączają, ATE0 wyłącza; wpis nagrany z echem
     odtwarzany przy echu wyłączonym ma je obcięte i odwrotnie,
   - licznik powtórzeń: drugie 0101 dostaje drugi pasujący wpis skryptu,
   - nieznane polecenie → "?\r\r>",
   - drop() budzi disconnects — inna ścieżka niż close().

6. transport/MockI40Script.kt — zapis z prawdziwego auta z 2026-08-08.
   Przenieś DOSŁOWNIE z i40-check, łącznie z:
   - odpowiedzią na 0100 pociętą na trzy kawałki: "SEARCHING...\r", "4100BE3EA813\r", "\r>",
   - echem poleceń przy ATZ, ATI, AT@1, ATE0 (recordedWithEcho = true),
   - dwoma wpisami dla 0101 (przegląd pyta dwa razy),
   - 015C → "NO DATA\r\r>" oraz 0A → "NO DATA\r\r>".
   To NIE jest makieta. Nie "popraw" żadnej odpowiedzi.

7. res/raw/dtc_dictionary.json — 41 wpisów, sekcja 9.2. Bez uzupełnień.

8. Testy atrapy: echo włączone/wyłączone, powtórzone polecenie, nieznane polecenie,
   drop() budzi disconnects.

TDD: najpierw testy, uruchom, pokaż mi czerwone, potem implementacja.

Na koniec pokaż mi:
- listę uprawnień z manifestu,
- listę zależności z build.gradle.kts,
- wynik ./gradlew test.
```

**Ukończone, gdy:** atrapa oddaje zapisane odpowiedzi, testy echa zielone, w manifeście nie ma
`BLUETOOTH_SCAN` ani uprawnień do lokalizacji.

---

## Etap 1 — warstwa ELM

```
Etap 1 z sekcji 16 specu. Kontrakt: sekcja 8.2 i 8.3.

To jest najtrudniejszy etap projektu i jedyny, w którym błąd objawia się jako
"czasem nie działa". Nie spiesz się.

1. elm/ElmResponse.kt — rozpoznawanie komunikatów, tabela z sekcji 8.3.

   PUŁAPKA: dopasowanie po DOKŁADNEJ treści linii po przycięciu białych znaków,
   NIE po podłańcuchu. "NO DATA" jako podłańcuch trafiłoby w dane heksadecymalne
   zawierające przypadkiem te znaki.

2. elm/MultiFrame.kt — składanie odpowiedzi wieloramkowych.
   Linie N:HEX sortowane po N i sklejane, z pominięciem wiodącej długości całkowitej.
   Wspólne dla trybu 09 i dla wielo-PID.

3. elm/ElmSession.kt — pompa poleceń.

   PUŁAPKA NR 1, sekcja 6 specu: Mutex w Kotlinie szereguje dostęp, ale NIE gwarantuje
   kolejności — korutyny czekające na mutex nie tworzą kolejki FIFO. Użyj
   Channel<Zadanie>(UNLIMITED) i JEDNEJ długo żyjącej korutyny konsumującej.
   Channel jest FIFO i to jest cała przyczyna tego wyboru.

   PUŁAPKA NR 2: odpowiedź kończy znak '>' (0x3E), NIE koniec linii. Kawałki przychodzą
   pocięte. Akumuluj bufor do napotkania '>'. Pojedynczego kawałka nigdy nie parsuj.

   PUŁAPKA NR 3, przeniesiona z działającego kodu iOS: bufor konsumuj TYLKO wtedy,
   gdy ktoś czeka na odpowiedź. Bez tego zapis kończący się szybciej niż zawieszenie
   na oczekiwaniu zjada ramkę i polecenie wisi do timeoutu. Ten wyścig jest realny
   i przy atrapie, i przy BLE.

   Ponowienia: WYŁĄCZNIE po timeoucie oraz po STOPPED i BUFFER FULL. Pozostałe
   komunikaty przerywają — ponawianie UNABLE TO CONNECT to pętla na głuchym łączu.

   Timeouty konfigurowalne, wartości domyślne z sekcji 10.5.

4. Testy — dwa są obowiązkowe i mają być napisane PRZED implementacją:

   test ramkowania: odpowiedź pocięta w złych miejscach (np. "41", "05", "5C\r\r>")
   musi dać jedną całość; dwie odpowiedzi w jednym kawałku muszą się rozdzielić.

   test szeregowania: wyślij 10 poleceń współbieżnie, sprawdź że do transportu trafiły
   pojedynczo I W KOLEJNOŚCI wysłania. To jest test, który przy implementacji na Mutex
   przechodzi częściowo i oblewa kolejność — jeśli u Ciebie przechodzi cały za pierwszym
   razem, sprawdź, czy naprawdę testuje współbieżność.

TDD obowiązkowe. Pokaż mi czerwone testy przed implementacją.
```

**Ukończone, gdy:** test szeregowania i test ramkowania przechodzą, a Ty widziałeś je czerwone.

---

## Etap 2 — warstwa OBD

```
Etap 2 z sekcji 16 specu. Kontrakt: sekcja 8.4. Tablice: sekcja 9.

Ta warstwa jest CZYSTA: bez I/O, bez zegara, bez Androida. Jeśli w importach pojawia się
cokolwiek z android.*, coś poszło nie tak.

1. obd/Pid.kt — definicja: numer, nazwa po polsku, liczba bajtów, jednostka,
   zakres fizyczny, funkcja dekodująca.

2. obd/PidCatalog.kt — tablica z sekcji 9.1 specu, WSZYSTKIE pozycje.

   NIE WYMYŚLAJ formuł. Tablica jest zamknięta — PID spoza niej nie istnieje.
   Zostaw w katalogu 5C, 5E i 10, mimo że to auto ich nie obsługuje: katalog opisuje
   standard, maska opisuje egzemplarz, a lista pól powstaje z ich przecięcia.

3. obd/SupportedPids.kt — maski.

   PUŁAPKA: bity 20, 40, 60, 80 to znaczniki "jest następna maska", NIE PID-y pomiarowe.
   Trzeba je ODJĄĆ ze zbioru wynikowego. Bez tego aplikacja próbuje je odczytać.

4. obd/MultiPid.kt — budowa zapytania i rozbiór odpowiedzi.

   PUŁAPKA: parser czyta numer PID-u, sprawdza długość W KATALOGU i konsumuje tyle bajtów.
   Nieznany PID albo brak bajtów KOŃCZY parsowanie. Nie zgaduj długości — zgadnięcie
   przesuwa wszystkie kolejne bajty i daje wiarygodnie wyglądające bzdury.

   Odpowiedź na sześć PID-ów ma ~15 bajtów, a ramka CAN mieści 7 — więc jest prawie zawsze
   wieloramkowa i idzie przez MultiFrame z etapu 1.

5. obd/Dtc.kt — dekodowanie i słownik z res/raw.
   Kod spoza słownika: tabela z sekcji 9.2. NIE WYMYŚLAJ opisów.

6. obd/Vin.kt — tryb 09 i pola VIN-u.

   PUŁAPKA: kodowanie rocznika (znak 10) pomija litery I, O, Q, U, Z — więc F = 2015.
   Ale tablica zakładów (znak 11) jest OSOBNA i te litery jej nie dotyczą: U = Ulsan
   jest poprawne. Znaków 4-8 NIE DEKODUJ — Hyundai tego nie publikuje.

7. obd/Readiness.kt — monitory gotowości z PID 01.
   Monitor nieobsługiwany przez to auto nie jest usterką i nie ma się pojawiać jako brak.

8. Testy dla każdego z powyższych, plus JEDEN obowiązkowy test regresji:

   maski "4100BE3EA813", "4120A007F011", "4140FED00400" muszą dać DOKŁADNIE zbiór
   z sekcji 3.2 specu. W szczególności test ma potwierdzić, że 5C, 5E i 10 NIE są
   w zbiorze, a 2F JEST. Ten test pilnuje zgodności dekodera z prawdziwym autem.

   UWAGA: 2F zostaje w oczekiwaniach TEGO testu, mimo poprawki P1, ktora wyrzuca
   go z odpytywania. Ten test sprawdza DEKODER MASEK, a maska naprawde zglasza 2F.
   Usuniecie 2F z oczekiwan zamaskowaloby blad dekodera.
   Zbior z maski i zbior odpytywany to DWIE ROZNE LISTY.
```

**Ukończone, gdy:** test regresji masek daje zbiór z sekcji 3.2, a bity kontynuacji są odjęte.

---

## Etap 3 — pozyskiwanie danych

```
Etap 3 z sekcji 16 specu. Kontrakt: sekcja 8.5. Stałe: sekcja 10.1 i 10.2.

1. acquisition/PidBatchReader.kt — sonda i odczyt w trybie multi/single.

   PUŁAPKA: odpowiedź uznajemy za dowód wsparcia TYLKO gdy da się z niej odczytać
   CO NAJMNIEJ DWA PID-y z sondy. Samotna odpowiedź na pierwszy PID oznacza brak
   wsparcia, nie sukces.

2. acquisition/SampleStream.kt — pętla gorąca i rotacja zimna.

   BEZ TIMERA. Następne zapytanie po odebraniu poprzedniej odpowiedzi, z celowaniem
   w interwał z nastawy. Gdy obieg trwa dłużej niż interwał — ZERO SNU, zapytania
   nie kolejkują się.

   Skład szóstki gorącej — kolejność jest obowiązkowa: najpierw 0D i 05 (nie wolno ich
   wypchnac), potem TRZY gniazda wykresow, na koncu uzupelnienie z domyslnych.
   Obowiazkowe to 0D predkosc, 05 plyn i 04 obciazenie — to ostatnie jest wejsciem
   modelu oleju i bez niego model CICHO ZAMIERA.
   0D odpowiada za blokadę prędkościową, 05 za alarm przegrzania.

   Rotacja zimna: DOKŁADNIE pięć PID-ów z sekcji 10.2 — 46, 1F, 42, 0F, 07.
To jest skład NA TEN ETAP i jest poprawny. Warstwy D i K przebudują go później
(rotacja zimna staje się poziomem wolnym C na innej fazie) — nie wyprzedzaj tego,
etap 3 buduje wersję bazową.
   PIEC, nie szesc. Maksimum jednego zapytania to szesc, wiec JEDNO MIEJSCE ZOSTAJE
   WOLNE i ma zostac wolne. Nie dokladaj PID-a, zeby je zapelnic — odpytywanie bez
   odbiorcy to zapytanie zabrane czemus innemu.
   BEZ 5C, 5E i 10 — sekcja 3.2, to auto ich nie obsługuje.
   BEZ 2F — POPRAWKA P1. Maska zglasza 2F jako obslugiwany, ale auto zwraca zero
   niezaleznie od stanu baku (potwierdzone przez wlasciciela 2026-08-16). Rozbieznosc 3
   z sekcji 3.1 jest zamknieta jako NIENAPRAWIALNA, a nie "naprawiona wpisem do rotacji",
   jak mowila pierwotna wersja specu.

   Wszystkie bezpieczniki z tabeli 10.1, co do wartości.
   Zegar WSTRZYKIWANY, żeby testy tempa nie czekały w czasie rzeczywistym.

3. acquisition/RingBuffer.kt — bufor kołowy, pojemność 240 (60 s × 4 Hz).

4. acquisition/OilTempEstimator.kt — MODEL TERMICZNY, nie pomiar.
   Wzory i stałe: sekcja 8.5, przepisz co do wartości.
   Reset przy spadku PID 1F o więcej niż 2 s.
   Progi pewności: <180 s niska, 180-600 s średnia, >600 s dobra.

   To nie jest miejsce na "ulepszenie modelu". Stałe pochodzą z działającej wersji
   i będą kalibrowane termometrem IR w etapie 10.

5. Testy: tempo przy 4 Hz i obiegu 50 ms nie przekracza sufitu; przy obiegu 400 ms
   nie kolejkuje; seria NO DATA obniża częstotliwość a dziesiąty zatrzymuje;
   sklad petli goracej nie wypycha 0D, 05 ani 04 nawet przy komplecie trzech gniazd;
   model oleju resetuje się przy spadku 1F i zbiega do płyn + 25·obciążenie.
```

**Ukończone, gdy:** tempo i zwalnianie działają na sztucznym zegarze, `0D`, `05` i `04` nie wypadają.

---

## Etap 4 — zapis

```
Etap 4 z sekcji 16 specu. Kontrakt: sekcja 8.6.

1. storage/TrackBlob.kt — format binarny z sekcji 8.6.
   DataOutputStream, big-endian, magic "I40T", kolumnowo.
   Zapis KOLUMNOWY, nie wierszowy: pętla gorąca i rotacja zimna próbkują z różną
   częstotliwością i nie da się ich ułożyć we wspólne wiersze.

2. storage/DriveSessionDao.kt — SQLiteOpenHelper, jedna tabela z sekcji 8.6.
   NIE Room. Schemat to jedna tabela z dwiema kolumnami BLOB — Room ciągnie KSP
   i jest więcej konfiguracji niż kodu.

   NIE twórz wiersza na próbkę. Lista przejazdów czyta wyłącznie metadane
   i podsumowanie; przebieg wczytuje się dopiero przy wejściu w szczegóły.

3. storage/SummaryCalculator.kt — czysta funkcja z gotowego przebiegu, bez ELM.
   Dystans i paliwo całkowane regułą trapezów.

   UWAGA: paliwoL na tym aucie będzie ZAWSZE null, bo PID 5E nie jest obsługiwany
   (sekcja 3.2). To jest poprawne zachowanie. Napisz test, który to potwierdza —
   pilnuje uczciwości pola, którego ten egzemplarz nie wypełni.

4. storage/SessionRecorder.kt — gromadzenie próbek, DTC na brzegach sesji.

5. CHECKPOINT I ODZYSKIWANIE — sekcja 11.4 specu. To NIE jest dopracowanie na później,
   tylko warunek, żeby zdanie "nigdy nie tracić danych" było prawdziwe.

   Wiersz przejazdu wstawiaj przy STARCIE sesji, ze statusem w_toku. Co 30 s zapisuj
   do niego gromadzony przebieg. Przy zamknięciu: ostatni zapis, podsumowanie, koniec,
   status zamkniety.

   PUŁAPKA: bez tego czterdziestominutowy przejazd ubity przez firmware w 39. minucie
   znika W CAŁOŚCI. Usługa pierwszoplanowa chroni przed ubiciem, ale nie daje gwarancji —
   tanie radia mają agresywne zarządzanie zadaniami.

   Odzyskiwanie: przy KAŻDYM starcie usługi znajdź wiersze w_toku, policz podsumowanie
   z tego, co zapisane, ustaw koniec na czas ostatniego checkpointu i zamknij statusem
   odzyskany. W historii oznacz jako przerwany — brakuje im ostatnich sekund i odczytu
   kodów na końcu, więc podawanie ich jako kompletnych byłoby kłamstwem.

   NIE optymalizuj zapisem przyrostowym. Format kolumnowy się do tego nie nadaje,
   a 10 MB na przejazd jest na eMMC 128 GB niezauważalne.

   Zapis na Dispatchers.IO, kopia przebiegu pod tym samym zamkiem co dopisywanie próbek,
   sam zapis do bazy już poza zamkiem. NIE dotykaj pompy poleceń.

6. Testy: obieg TrackBlob w obie strony bez utraty danych; podsumowanie z dystansem
   całkowanym z prędkości; paliwoL null gdy serii 5E nie ma; zapis i odczyt sesji z bazy;
   sesja w_toku czytelna Z BAZY W TRAKCIE nagrywania i rosnąca między checkpointami;
   wiersz zostawiony jako w_toku zamyka się przy starcie jako odzyskany, z podsumowaniem
   z częściowego przebiegu i PUSTYM kodyNaKoncu (nie kopią kodyNaStarcie).
```

**Ukończone, gdy:** obieg blobu w obie strony czysty, `paliwoL` jest `null` w teście.

---

## Etap 5 — alarmy i automatyka przejazdu

```
Etap 5 z sekcji 16 specu. Kontrakt: sekcja 8.7 i 11. Progi: sekcja 10.3.

1. alerts/AlertEngine.kt — CZYSTE reguły z tabeli 10.3 plus karencja.
   Karencja liczona PER TYP alarmu, żeby jeden warunek nie zagłuszał innego.
   Pilny (płyn) powtarzany co 10 s, pozostałe co 60 s.

2. service/TripStateMachine.kt — CZYSTA funkcja stanu i zdarzeń, ZERO Androida.
   Cztery stany z sekcji 11.2.

   PUŁAPKA: sygnałem startu są OBROTY, nie zapłon. Gniazdo OBD w i40 bywa zasilane
   na stałe, więc obecność adaptera niczego nie dowodzi. 01 0C > 0 dowodzi.

   PUŁAPKA: spadek PID 1F o więcej niż 2 s zamyka sesję i otwiera nową. To ten sam
   sygnał, który resetuje model oleju. Postój ze zgaszeniem i ponownym odpaleniem
   ma dać DWA przejazdy, nie jeden z dziurą.

   Żądanie przeglądu przechodzi przez stan Zamykanie — sekcja 11.3. Sesja jest
   zamykana normalnie, z odczytem 03 i podsumowaniem, i dopiero potem rusza przegląd.

   Zamykanie ZAWSZE zapisuje. Utrata połączenia, brak miejsca, ubicie usługi —
   sesja zostaje zapisana z tym, co zebrano. NIGDY nie tracić danych.

3. DOMKNIĘCIE ROZBIEŻNOŚCI 2 (sekcja 3.1): kody zapisane odczytuj poleceniem 03
   na starcie sesji ORAZ przy n % 200 == 150, i podawaj jako dtcsNow do AlertEngine.

   PRZESUNIECIE FAZY JEST OBOWIAZKOWE, nie kosmetyczne. Rotacja zimna chodzi przy
   n % 10 == 5. Gdyby odczyt 03 byl przy n % 200 == 0, oba trafialyby w ten sam cykl
   (200 dzieli sie przez 10) i co dwusetny cykl robilby TRZY zapytania zamiast dwoch —
   210 ms w budzecie 250 ms. Policzone: 1000 takich cykli na 200 000.
   Przy n % 200 == 150 mamy n % 10 stale rowne 0, wiec nigdy 5. Zero kolizji.

   Napisz test: na 200 000 cykli zaden nie wykonuje trzech zapytan.
   W wersji iOS ten tor był martwy — LiveRecordingHost podawał puste zbiory, więc
   alarm "nowy kod błędu" nigdy się nie odpalał mimo dwunastu testów reguły.

4. service/DriveService.kt — usługa pierwszoplanowa I WŁAŚCICIEL STANU ŻYWEGO.

   Sekcja 11.4 specu. Bufory kołowe, gromadzony przebieg, stan modelu oleju, karencje
   alarmów i znacznik startu sesji są POLAMI USŁUGI, nie ViewModelu. Interfejs czyta
   StateFlow wystawiony przez usługę i nie przechowuje nic własnego.

   PUŁAPKA: odwrotny układ wygląda naturalniej w Compose i jest tutaj BŁĘDEM.
   Na radiu aplikacja prawie nigdy nie jest na wierzchu — kierowca przełącza się na
   nawigację. Pierwsze zniszczenie Activity skasowałoby okno wykresów.

   PARTIAL_WAKE_LOCK trzymany WYŁĄCZNIE w stanie Nagrywa, zwalniany w bloku finally.
   Usługa pierwszoplanowa chroni przed ubiciem procesu, NIE przed uśpieniem procesora —
   bez blokady jądro taniego radia zasypia między odpytaniami i pętla 4 Hz degraduje się
   do nieregularnych skoków, których nie widać inaczej niż przez licznik Hz.
   foregroundServiceType="connectedDevice" — bez tego startForeground rzuca wyjątkiem
   przy pierwszym uruchomieniu na Androidzie 14.
   START_STICKY. Stałe powiadomienie ze stanem.

5. service/BootReceiver.kt — start po BOOT_COMPLETED.
   Nie zakładaj, że przychodzi punktualnie — usługa musi znosić start spóźniony o minutę.

6. alerts/AlertPlayer.kt — AudioManager z AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK.
   Alarm PRZYCISZA nawigację, zamiast ją zabijać. Ognisko zwalniane po odtworzeniu.
   Wibracja przez VibrationEffect; brak silniczka w radiu NIE jest błędem.

7. Testy maszyny stanów — wszystkie przejścia z tabeli 11.2, plus test alarmu
   "nowy kod DTC" odpalającego się, gdy dtcsNow zawiera kod spoza dtcsAtStart.
```

**Ukończone, gdy:** maszyna stanów pokryta testami, alarm nowego DTC się odpala, usługa startuje
na emulatorze bez wyjątku. **Restart prawdziwego radia sprawdza się dopiero w STYKU 1**, po
etapie 8 — na tym etapie nie masz jeszcze czego wgrać.

---

## Etap 6 — przegląd na postoju

```
Etap 6 z sekcji 16 specu. Kontrakt: sekcja 8.8. Progi reguł: sekcja 10.4.

1. checkup/CheckupOrchestrator.kt — sekwencja poleceń z sekcji 8.8.

   PRZEPISZ JĄ DOKŁADNIE. Jest dłuższa niż w specyfikacji iOS (rozbieżność 6):
   dochodzą ATI i AT@1, a ATDPN leci DWA RAZY — przed negocjacją i po niej.
   Bez pierwszego ATDPN odczyt protokołu bierze stan sprzed negocjacji i raportuje A0
   zamiast A6.

   TIMEOUT 25 SEKUND, dwa ponowienia (sekcja 10.5). NIE 5 sekund — ATZ trwa około
   sekundy, a SEARCHING... przy pierwszej negocjacji potrafi ciągnąć się kilkanaście.
   Port z 5 s wywali się na pierwszym połączeniu z autem. To jest rozbieżność 5
   z sekcji 3.1: specyfikacja iOS mówi 5 s, działający kod używa 25 s.

   Maski 0120/0140/0160 łańcuchowo, PÓKI bit kontynuacji. Na tym aucie 0140 nie ma
   bitu kontynuacji, więc 0160 się nie wysyła — i to jest poprawne.

   Tryb 0A na tym aucie zwraca NO DATA. permanentDTCs ma wtedy być NULL, nie pustą
   listą: null znaczy "tryb nieobsługiwany", pusta lista znaczy "obsługiwany, brak kodów".

2. rules/RuleEngine.kt — CZYSTE reguły z tabeli 10.4, co do progów.
   Silnik pracuje przy obrotach > 500, zgaszony przy < 50.

3. checkup/Report.kt — migawka przeglądu i werdykt.

   Kolejność werdyktu z sekcji 8.8 jest ISTOTNA — usterka wygrywa z uwagą.
   Napięcie do reguł: PID 42, a gdy go brak — ATRV z pinu 16.

   NIE implementuj zamrożonej ramki (tryb 02). Jest nie-celem z sekcji 4:
   wersja iOS jej nie ma mimo obietnicy w specyfikacji, a przede wszystkim nie ma
   zapisu z auta, na którym dałoby się to przetestować.

4. Testy: każda reguła z tabeli 10.4; kolejność werdyktu; pełny przegląd na atrapie
   od ATZ do werdyktu.
```

**Ukończone, gdy:** pełny przegląd na atrapie kończy się werdyktem, a `permanentDTCs` jest `null`.

---

## Etap 7 — interfejs, ekran żywy

```
Etap 7 z sekcji 16 specu. Kontrakt: sekcja 12.

DECYZJA O COMPOSE MA TERMIN WAŻNOŚCI DO TEGO ETAPU (sekcja 6.1). Jeśli ma zostać
cofnięta na rzecz klasycznych widoków, powiedz to TERAZ, przed napisaniem pierwszego
ekranu. Później to przepisywanie połowy projektu.

1. ui/Theme.kt — motyw wyłącznie ciemny. Czcionka o stałej szerokości cyfr dla wartości.

2. ui/LiveScreen.kt — układ z sekcji 12.2: cztery kafle liczbowe, trzy wykresy, pasek stanu.

   SKLAD CZTERECH KAFLI — POPRAWKA P1:
      1. temperatura oleju   MODEL, nie pomiar
      2. temperatura plynu   0105
      3. napiecie            0142
      4. korekta dluga       0107     <-- NIE poziom paliwa

   Czwarty kafel to KOREKTA DLUGOTERMINOWA 0107, pasmo -10 do +10 %.
   Pasmo NIE JEST NOWE — to prog regul ltft_lean / ltft_rich z tabeli 10.4.
   NIE WYMYSLAJ go i nie zaokraglaj; czytaj z jednego miejsca (PasmaOdniesienia
   dochodzi w warstwie D, do tego czasu stala nazwana, nie liczba w kodzie UI).

   Poziomu paliwa 2F NIE MA na zadnym ekranie. Maska go zglasza, auto zwraca zero
   niezaleznie od stanu baku. Kto go doda z powrotem, doda pole, ktore zawsze klamie.

   NIE MA PRZYCISKU START. Nagrywanie zaczyna się samo. Jest przycisk zatrzymania.

   Kafel temperatury oleju MUSI być podpisany jako model, ze znacznikiem pewności.
   To nie jest ozdoba — to różnica między narzędziem diagnostycznym a ładną liczbą.

   Pole nieodczytane pokazuje "—" Z POWODEM. Nigdy zera. 0 °C jest nieodróżnialne
   od prawdziwego pomiaru.

3. ui/RollingChart.kt — Canvas, Path przez punkty, okno 60 s.

   OSIE Y SĄ SZTYWNE, wartości z tabeli 10.7. To nie jest brak funkcji, tylko decyzja:
   przy skalowaniu automatycznym spokojna jazda na stałych obrotach produkuje dramatyczną,
   szarpaną krzywą, bo oś zawęża się do szumu. Wartość poza zakresem PRZYCINAJ do krawędzi
   i oznacz — nie rozciągaj osi.

4. Blokada prędkościowa z sekcji 12.4: przy 01 0D > 0 działa wyłącznie zatrzymanie
   nagrywania. Zmiana parametrów, ustawienia, przegląd i nawigacja po zakładkach
   zablokowane. FLAG_KEEP_SCREEN_ON na czas nagrywania.

5. Testy: formatowanie kafli (wartość niedostępna daje "—"), przycinanie do zakresu osi,
   reguły blokady prędkościowej (to czysta funkcja — przetestuj ją bez UI).

   PLUS test: zbior PID-ow rotacji zimnej NIE ZAWIERA 2F, a czwarty kafel czyta 0107.
   Ten test istnieje po to, zeby 2F nie wrocil przy jakimkolwiek pozniejszym
   "zapelnianiu wolnego miejsca".
```

**Ukończone, gdy:** wszystkie stany widoczne na atrapie, oś nie skaluje się automatycznie.

---

## Etap 8 — historia

```
Etap 8 z sekcji 16 specu. Kontrakt: sekcja 12.5 i 12.6.

1. charts/Decimator.kt — koszyki MIN-MAX, algorytm z sekcji 12.6.

   NIGDY przez średnią. Uśrednianie wygładza dokładnie te skoki, dla których się
   na wykres patrzy. Test tego pilnujący jest obowiązkowy i jest testem sensu
   całego algorytmu: seria z pojedynczym ostrym skokiem po decymacji MUSI ten skok
   zachować, a po uśrednieniu by go straciła.

2. ui/HistoryScreen.kt — siatka miesięczna, LazyVerticalGrid + java.util.Calendar.
   Bez bibliotek do dat.

3. ui/SessionDetailScreen.kt — STOS małych wykresów na wspólnej osi czasu.
   NIE jeden wykres z wieloma seriami: nakładanie obrotów na napięcie jest nieczytelne,
   a druga oś Y tego nie rozwiązuje, tylko ukrywa.

   WSPÓLNY SUWAK ODCZYTU to najważniejsza interakcja tego ekranu. Przeciągnięcie palcem
   przesuwa pionową linię przez WSZYSTKIE wykresy naraz, a każdy pokazuje swoją wartość
   z tej chwili. To jest jedyny powód, dla którego warto było te dane zbierać.

4. Testy: decymacja zachowuje skok; siatka miesięczna dla lutego roku przestępnego
   i dla miesiąca zaczynającego się w niedzielę; wyszukiwanie wartości serii dla
   zadanego czasu suwaka.
```

**Ukończone, gdy:** suwak przesuwa linię przez wszystkie wykresy naraz, test decymacji zielony.

> **Tu wypada STYK 1 z sekcji 15.0 specu.** Aplikacja jest kompletna i chodzi na zapisie z auta.
> Wgraj APK na radio z transportem `Atrapa` i przejdź listę 15.1 B: instalacja, usługa
> pierwszoplanowa, układ na ekranie radia, restart radia. **To wszystko sprawdza się bez Bluetootha** —
> rozdzielasz „czy aplikacja działa na tym radiu" od „czy Bluetooth działa na tym radiu".
> Dopiero po zielonym idź do etapu 9.

---

## Etap 9 — transporty prawdziwe

```
Etap 9 z sekcji 16 specu. Kontrakt: sekcja 8.1.

Dopiero teraz dotykamy sprzętu. Wszystko powyżej jest już przetestowane na atrapie.

0. NAJPIERW USTAL, CZY SPP W OGÓLE ISTNIEJE — sekcja 3.4 specu.
   Sparuj adapter i wypisz device.uuids. Szukasz 00001101-0000-1000-8000-00805F9B34FB.
   Wariant MC-IOS jest prawdopodobnie BLE-only. Wynik decyduje, który transport jest
   domyślny — ale WSZYSTKIE TRZY i tak powstają w tym etapie.

1. transport/SppTransport.kt
   UUID 00001101-0000-1000-8000-00805F9B34FB.
   Urządzenie z adapter.bondedDevices, NIE ze skanowania.

   PUŁAPKA: przed connect() zawsze adapter.cancelDiscovery(). Bez tego połączenie
   potrafi zawisnąć bez błędu.

   PUŁAPKA: kawałki z InputStream są dowolnej długości, dokładnie jak notyfikacje BLE.
   Ramkowanie na '>' z etapu 1 obowiązuje tak samo.

   Ponowienie co 5 s BEZ LIMITU prób. Adapter budzi się razem z autem, więc czekanie
   jest właściwym zachowaniem, nie błędem.

2. transport/GattPairFinder.kt — CZYSTA funkcja wyboru pary, z testami bez Androida.
   Kolejność: najpierw znana para Vgate 2AF1+2AF0, potem pierwsza para write+notify
   w obrębie jednej usługi, w ostateczności para z różnych usług.
   Wynik wraca z uzasadnieniem — trafia na ekran diagnostyczny.

3. transport/BleTransport.kt — jedyny udowodniony po stronie adaptera.
   Prawdziwe UUID-y vLinkera: usługa 18F0, zapis 2AF1, notyfikacja 2AF0.
   NIE hardkoduj wyłącznie ich — użyj GattPairFinder.
   Zapis przez WRITE_TYPE_NO_RESPONSE, gdy charakterystyka to obsługuje.

4. transport/WifiTransport.kt — plan awaryjny. Gniazdo TCP, domyślnie 192.168.0.10:35000,
   adres konfigurowalny.

5. Ustawienia: wybór transportu, cztery pozycje (SPP, BLE, Wi-Fi, Atrapa),
   zapamiętywany w SharedPreferences. Domyślną ustaw po wyniku kroku 0.

6. Ekran diagnostyczny: wykryte usługi i charakterystyki, wybrana para z uzasadnieniem,
   wynegocjowany protokół z ATDPN, napięcie z ATRV, surowy log z przyciskiem kopiowania.
```

**Ukończone, gdy:** aplikacja łączy się z adapterem na biurku, ekran diagnostyczny pokazuje
wybraną parę.

---

## Etap 10 — weryfikacja na sprzęcie

```
Etap 10 z sekcji 16 specu. Kontrakt: sekcja 15.

Ten etap wymaga człowieka, radia i samochodu. Nie da się go zrobić w Cursorze —
Twoim zadaniem jest przygotowanie listy kontrolnej i przyjęcie wyników.

Przygotuj:

1. Listę kontrolną biurkową z sekcji 15.1 jako plik docs/weryfikacja.md,
   z miejscem na wynik każdego kroku.

2. Instrukcję uzupełnienia MockI40Script o prawdziwe odpowiedzi wielo-PID
   z auta — dokładnie który log skopiować i gdzie go wkleić.

3. Arkusz kalibracji modelu oleju: co zmierzyć termometrem IR, po ilu minutach jazdy,
   jak z pomiaru wyliczyć korektę tauBase.

Po powrocie z auta przyjmiesz ode mnie logi i:
- dopiszesz prawdziwe odpowiedzi do MockI40Script,
- uruchomisz testy na nowych danych i pokażesz wynik,
- skorygujesz tauBase, jeśli pomiar tego wymaga.

NIE zmieniaj żadnej innej stałej na podstawie jednego przejazdu.
```

**Ukończone, gdy:** atrapa zawiera prawdziwe odpowiedzi wielo-PID, testy na nich zielone.

---

## Prompt kontrolny — po całości

```
Przeczytaj @docs/spec/2026-08-14-i40-android-design.md i przejrzyj całe repozytorium.
Nie pisz kodu. Odpowiedz na pytania, każde z dowodem z pliku i numeru linii:

1. Czy któraś stała w kodzie różni się od tabeli w sekcji 10 specu? Wypisz wszystkie
   rozbieżności albo napisz, że nie ma.

2. Czy gdziekolwiek odpytujemy PID 5C, 5E albo 10? Sekcja 3.2 mówi, że to auto ich
   nie obsługuje.

3. Czy pętla gorąca może zgubić 0D albo 05? Pokaż kod składania szóstki.

4. Czy któryś nieudany odczyt zamienia się gdzieś w zero zamiast w "—"? Przeszukaj
   wszystkie miejsca, gdzie wartość opcjonalna dostaje wartość domyślną.

5. Czy pompa poleceń stoi na Channel, czy gdzieś wkradł się Mutex?

6. Czy oś Y któregoś wykresu skaluje się automatycznie?

7. Czy decymacja gdziekolwiek używa średniej?

8. Czy alarm "nowy kod DTC" ma podłączone dane, czy dostaje puste zbiory jak w wersji iOS?

9. Czy w manifeście pojawiło się BLUETOOTH_SCAN albo uprawnienie do lokalizacji?

10. Czy wiersz przejazdu powstaje przy STARCIE sesji ze statusem w_toku, czy dopiero
    przy zamknięciu? Pokaż miejsce wstawienia.

11. Gdzie mieszkają bufory kołowe i gromadzony przebieg — w DriveService czy w ViewModelu?
    Pokaż deklaracje pól.

12. Czy PARTIAL_WAKE_LOCK jest zwalniany w bloku finally? Pokaż kod.

13. Czy jest test, który zostawia wiersz jako w_toku i sprawdza, że przy starcie usługi
    zamyka się jako odzyskany?

14. Czy liczba testów jest nie mniejsza niż 153? Podaj wynik ./gradlew test.

Na koniec: czy jest w repozytorium kod, którego wersja iOS nie ma, a spec go nie wymaga?
Sekcja 4 wymienia nie-cele — sprawdź, czy któryś nie wszedł tylnymi drzwiami.
```
