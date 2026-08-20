# i40-android — prompty dla Cursora: rozszerzenie kontekstowe

Siedem etapów `K1`–`K7`, **jeden nowy czat na etap**. Wchodzą **po etapie D9** rozszerzenia
diagnostycznego.

---

## Jak to prowadzić

**Do każdego promptu dołącz trzy dokumenty:**

```
@docs/spec/2026-08-14-i40-android-design.md
@docs/spec/2026-08-14-i40-android-diagnostyka-design.md
@docs/spec/2026-08-14-i40-android-kontekst-design.md
```

**PRZECZYTAJ SEKCJĘ 3.1 SPECU KONTEKSTOWEGO, ZANIM NAPISZESZ LINIĘ KODU.** W tym projekcie
`03` znaczy dwie różne rzeczy: **tryb 03** to odczyt kodów błędów, **PID `0103`** to status
układu paliwowego. Pomylenie ich daje odpowiedź, którą **da się sparsować** — więc błąd
przejdzie bez żadnego objawu, a aplikacja pokaże listę kodów jako status pętli.

**Zasada zapisu w całym projekcie:** PID-y trybu 01 **zawsze z prefiksem trybu** — `0103`,
`012E`, `010D`. Tryby słownie — „tryb 03", „tryb 09".

**Po każdym etapie:**

```
./gradlew ktlintCheck ; if ($?) { ./gradlew lint } ; if ($?) { ./gradlew test }
```

**Cztery pytania kontrolne:**

*„Czy pętla gorąca jest nietknięta?"* — przebudowa dotyka trzech poziomów, łatwo zawadzić
o czwarty.

*„Skąd ta liczba?"* — to rozszerzenie **nie wprowadza ani jednego nowego progu**. Jeśli
w kodzie pojawia się nowa stała graniczna, jest wymyślona.

*„Czy ta wartość w tej chwili w ogóle coś znaczy?"* — to jest myśl przewodnia całego
rozszerzenia. Korekta przy otwartej pętli nie znaczy nic.

*„Czy to jest liczba, czy wniosek?"* — karta miesiąca pokazuje liczby, nie oceny.

---

## Etap K1 — przebudowa poziomów odpytywania

```
Etap K1 z sekcji 13 specu kontekstowego. Kontrakt: sekcja 7.

Piec nowych PID-ow nie miesci sie w dwoch pelnych szostkach. Przebudowujemy poziomy —
podzial WEDLUG TEGO, JAK SZYBKO WARTOSC SIE ZMIENIA, nie wedlug tego, ktory panel
jej potrzebuje.

DLACZEGO NIE WEDLUG PANELU: nagrywanie NIE MOZE zalezec od tego, ktory panel jest
na ekranie. Inaczej sesje przestaja byc porownywalne, a to fundament karty miesiaca
i przyszlej linii bazowej.

1. Nowy uklad — sekcja 7.1:

   goracy    kazdy cykl     010D 0105 0104 + TRZY gniazda wykresow  4 Hz
   szybki A  n % 4  == 0    0123 010B 0111 014C 0149 0143          ~1 Hz
   sredni B  n % 10 == 5    013C 0144 012E 0103 0107 0142          ~0,4 Hz
   wolny C   n % 20 == 13   011F 0146 010F 0133       (+2 wolne)   ~0,2 Hz
   kody      n % 200 == 150 tryb 03                                ~0,02 Hz

   UWAGA: 0103 w poziomie B to PID statusu ukladu paliwowego, NIE tryb 03.
   Tryb 03 zostaje tam, gdzie byl, przy n % 200 == 150.

2. FAZA n % 20 == 13 jest jedyna wolna. Dowod:
     n % 20 == 13   =>  n % 4  stale = 1   -> nigdy 0   (rozlaczne z A)
     n % 20 == 13   =>  n % 10 stale = 3   -> nigdy 5   (rozlaczne z B)
     n % 200 == 150 =>  n % 20 stale = 10  -> nigdy 13  (rozlaczne z trybem 03)
   Sprawdzone na 200 000 cykli: zero cykli z trzema zapytaniami.
   Zapytan na sekunde 5,62 przy sufcie 25. Szczyt 140 ms przy budzecie 250 ms.

3. TRZY PID-Y ZWALNIAJA i kazde zwolnienie jest swiadome — sekcja 7.3:
     013C katalizator  1 Hz -> 0,4 Hz   alarm KAT-2 do 2,5 s pozniej, dopuszczalne
     0144 lambda       1 Hz -> 0,4 Hz   praktycznie stala, bez znaczenia
     011F czas pracy   0,4  -> 0,2 Hz   nowy przejazd do 5 s pozniej, dopuszczalne
   NIE ZWALNIAJ niczego, od czego zalezy alarm pilny. 0105 i 010D zostaja w goracej.

3a. 012F NIE ZWALNIA — 012F ZNIKA (poprawka P1). Nie jest odpytywany na zadnej
   nastawie tempa. Poziom C ma przez to CZTERY PID-y przy limicie szesciu, czyli
   DWA MIEJSCA WOLNE. Maja zostac wolne.

ZAKAZ: nie dotykaj faz poziomow A, B i trybu 03 ani tempa petli goracej.

UWAGA — JEDNA ZMIANA W GORACEJ JEST W TYM ETAPIE ZAMIERZONA: 0104 obciazenie dolacza
do PID-ow OBOWIAZKOWYCH obok 010D i 0105, a gniazd konfigurowalnych jest odtad TRZY.
Powod w sekcji 11.1 specu kontekstowego: model oleju liczy target = plyn + 25*(obciazenie/100),
wiec bez obciazenia CICHO ZAMIERA — kafel pokazuje stara wartosc, pewnosc przestaje rosnac
i nic o tym nie mowi. To naprawa bledu z dokumentu bazowego, nie nowa funkcja.

4. Testy:
   - TEST ROZLACZNOSCI CZTERECH POZIOMOW na co najmniej 200 000 cykli: zaden cykl
     nie wykonuje trzech zapytan. To trzecia okazja do kolizji w tym projekcie —
     dwie poprzednie zostaly znalezione dopiero rachunkiem, nie na oko.
   - poziom C wykonuje sie dokladnie co dwudziesty cykl,
   - suma zapytan nie przekracza sufitu 25/s,
   - sklad i tempo petli goracej identyczne przed i po przebudowie.
```

**Ukończone, gdy:** test rozłączności czterech poziomów przechodzi na 200 000 cykli.

---

## Etap K2 — dekoder statusu układu paliwowego i reguła MIX-1

```
Etap K2 z sekcji 13 specu kontekstowego. Kontrakt: sekcja 4 i 11.1.

1. Dekoder enumeracji PID 0103 w obd/. Dwa bajty; bajt A to uklad nr 1,
   bajt B to uklad nr 2 (ten silnik ma jeden bank, wiec B jest nieistotny).

   Wartosci ze zrodla — sekcja 4 specu:
      0   silnik wylaczony
      1   petla otwarta — niewystarczajaca temperatura silnika
      2   PETLA ZAMKNIETA — sprzezenie zwrotne sondy tlenu
      4   petla otwarta — obciazenie silnika albo odciecie paliwa przy zwalnianiu
      8   PETLA OTWARTA — AWARIA UKLADU
      16  PETLA ZAMKNIETA — AWARIA SPRZEZENIA

   PULAPKA: wartosc SPOZA tej listy zwraca "nieznany status" WRAZ Z SUROWA LICZBA.
   NIE dopasowuj do najblizszej pasujacej wartosci — to jest wymyslanie danych.

   PULAPKA: 16 to petla ZAMKNIETA mimo awarii. Latwo wrzucic ja do worka "otwarta",
   bo brzmi jak usterka. Ma znaczenie pozniej, przy cieniowaniu wykresu w K3.

2. Regula MIX-1 w rules/RuleEngine.kt — sekcja 11.1.
   Warunek: bajt A ze zbioru {8, 16}.  Waga: UWAGA.

   To NIE JEST prog, ktory ktos ustalil — to stan, ktory STEROWNIK SAM NAZYWA AWARIA.
   Stad waga uwaga mimo braku danych fabrycznych: nie orzekamy nic ponad to,
   co powiedzialo ECU.

   NIE odpalaj reguly dla wartosci 1 ani 4. To normalne stany pracy: zimny silnik
   i pelne obciazenie. Potraktowanie kazdej otwartej petli jako usterki to
   najlatwiejszy blad tego etapu.

   Dwie rozne tresci wniosku dla 8 i dla 16 — wzory w sekcji 11.1.
   Zaden nie orzeka, CO jest zepsute.

3. Rozszerz RuleInput o statusUkladuPaliwowego (typ opcjonalny) i wypelnij
   go w Report.ruleInput z odczytu 0103.

3a. WARUNEK WAZNOSCI CZWARTEGO KAFLA — sekcja 8.5 specu kontekstowego.

   Czwarty kafel to korekta dluga 0107 (poprawka P1). Od tego etapu kafel
   pokazuje liczbe TYLKO po POZYTYWNYM potwierdzeniu petli zamknietej:

      2   -> LICZBA
      16  -> LICZBA   (petla zamknieta mimo awarii sondy; awarie zglasza MIX-1)
      1   -> "— ○"
      4   -> "— ○"
      8   -> "— ○"
      0   -> "— ○"
      brak odczytu 0103        -> "— ○"
      wartosc spoza enumeracji -> "— ○"

   NAPISZ TO JAKO:  if (status == 2 || status == 16) liczba else kreska
   NIE JAKO:        if (petlaOtwarta) kreska else liczba

   Roznica jest cala trescia tego punktu. Druga wersja przy ZERWANYM odczycie
   0103 przepusci liczbe — czyli pomyli sie dokladnie w te strone, w ktora nie
   wolno, i to wtedy, gdy aplikacja wie o silniku najmniej.

   TRZECI WIERSZ KAFLA (norma) zostaje "-10 – +10" TAKZE przy kresce.
   Kreska w wartosci nie kasuje normy. Pusty wiersz normy jest zakazany.

   ○ to znak "nieaktywne / niedostepne", ten sam co we wskazniku paneli.
   NIE MYL z ⌀, ktore znaczy ODCZYT NIEUDANY. ⌀ mowi "nie wiem",
   ○ mowi "wiem, ale ta liczba teraz nic nie znaczy".

4. Testy: kazda z szesciu wartosci daje wlasciwy opis; wartosc spoza enumeracji
   daje "nieznany" z surowa liczba; MIX-1 odpala dla 8 i 16 i NIE odpala dla 1 i 4.

4a. Test kafla, OSIEM PRZYPADKOW z punktu 3a — z naciskiem na dwa ostatnie:
   brak odczytu 0103 i wartosc spoza enumeracji MUSZA dac kreske.
   Plus: zbior PID-ow poziomu C nie zawiera 012F na zadnej nastawie tempa.
```

**Ukończone, gdy:** sześć wartości rozpoznanych, wartość spoza enumeracji daje „nieznany",
`MIX-1` milczy przy `1` i `4`, a **czwarty kafel milczy przy braku potwierdzenia pętli
zamkniętej** — łącznie z przypadkiem zerwanego odczytu `0103`.

---

## Etap K3 — panel Mieszanka: status, cieniowanie, nowy licznik

```
Etap K3 z sekcji 13 specu kontekstowego. Uklad: sekcja 8.

To jest etap, w ktorym rozszerzenie zarabia na siebie: zamiast liczby, przy ktorej
uzytkownik zgaduje, czy to problem — liczba z zaznaczonym powodem.

1. WIERSZ STATUSU PETLI nad wykresem, nie pod nim. Rozstrzyga, czy w ogole warto
   na wykres patrzec, wiec stoi wyzej niz to, co opisuje.
   Tabela stanow i znaczkow: sekcja 8.2.

2. CIENIOWANIE POD KRZYWA — dwa rodzaje, sekcja 8.3:
     ▓  przedmuchiwanie aktywne   (012E > 0)      skok korekty MA wyjasnienie
     ░  petla otwarta             (0103 nie w {2, 16})   korekta NIE ZNACZY NIC

   PULAPKA: 16 to petla ZAMKNIETA (z awaria sondy). NIE moze trafic do cieniowania
   "otwarta". Sprawdz to testem — pomylka jest cicha, bo wykres i tak sie narysuje.

   Legenda pod wykresem OBOWIAZKOWA. Kolor nigdy nie jest jedynym sygnalem,
   a tu jest to wazniejsze niz zwykle, bo dwa rodzaje tla znacza co innego.

3. LICZNIK CZASU POZA PASMEM — ZMIANA DEFINICJI, sekcja 8.4.

   Bylo:  czas gdy |0106 + 0107| > 20 %,  dzielone przez caly czas sesji
   Jest:  czas gdy warunek zachodzi I PETLA JEST ZAMKNIETA,
          dzielone przez CZAS W PETLI ZAMKNIETEJ

   POWOD: korekta przy otwartej petli to zamrozona wartosc sprzed przejscia w tryb
   otwarty. Liczenie jej dodaje szum niezwiazany ze stanem silnika.
   Mianownik tez sie zmienia, bo inaczej stosunek traci sens: 0:30 z 40:00 przy
   trzydziestu minutach otwartej petli to co innego niz 0:30 z 10:00.

   NOWE NAZWY POL: czasPozaPasmemWPetliZamknietejSekundy oraz
   czasWPetliZamknietejSekundy. Dlugie celowo — maja nie dac sie pomylic ze starymi.
   STARY TEST czasPozaPasmemKorektSekundy PRZESTAJE OBOWIAZYWAC i musi zostac
   przepisany, nie usuniety po cichu.

4. Wskaznik paneli zmienia sie z czterech na PIEC kropek. Panel Powietrze powstaje
   w K4, ale miejsce we wskazniku robimy juz teraz.

5. Testy: cieniowanie ▓ pokrywa sie czasowo z probkami gdzie 012E > 0;
   cieniowanie ░ tam gdzie 0103 nie w {2, 16} — z osobnym przypadkiem dla 16;
   licznik liczy wylacznie w petli zamknietej, licznie i w liczniku, i w mianowniku.
```

**Ukończone, gdy:** cieniowanie rozróżnia `16` od stanów otwartych, a licznik liczy tylko w pętli zamkniętej.

---

## Etap K4 — panel Powietrze

```
Etap K4 z sekcji 13 specu kontekstowego. Uklad: sekcja 9.

1. Piaty panel. Listwa kafli bez zmian, wskaznik ○ ○ ○ ○ ●.

2. PODCISNIENIE = 0133 atmosferyczne − 010B kolektor.
   JEDYNA wartosc liczona z dwoch pomiarow w calym projekcie — MUSI byc tak oznaczona
   na ekranie ("wyliczone").
   null, gdy KTOREGOKOLWIEK z dwoch brak. Brak jednego uniewaznia wynik.

3. PRZEPUSTNICA: dwie krzywe JEDNA NA DRUGIEJ — zadana 014C i rzeczywista 0111.
   Zdrowa przepustnica: krzywe sie pokrywaja. Chora: rozjezdzaja sie widocznie.
   Wiersz "rozjazd" podaje roznice w punktach procentowych.

   BEZ PROGU I BEZ REGULY — sekcja 5 specu. Nie znam liczby "o ile moga sie roznic",
   a rozjazd widac golym okiem. Gdy pojawia sie dane fabryczne, regule mozna dopisac.

4. PEDAL 0149 jako trzecia krzywa, ponizej. To odniesienie: czego chcial kierowca.
   Rozjazd pedal -> zadana jest NORMALNY (sterownik filtruje i ogranicza).
   Rozjazd zadana -> rzeczywista nie jest.

5. PASMA — sekcja 9.3. CZTERY Z PIECIU pozycji maja "—":
     podcisnienie      brak — zalezy od obciazenia i wysokosci n.p.m., brak zrodla
     rozjazd           brak — brak zrodla na dopuszczalna roznice
     przepustnica      fizyczny 0–100 %
     pedal             fizyczny 0–100 %
     atmosferyczne     fizyczny 50–110 kPa
   To panel, ktory POKAZUJE I NIE ORZEKA. Nie wymyslaj mu pasm.

6. Osie Y sztywne — sekcja 9.4.

Testy: podcisnienie null gdy brak ktoregokolwiek skladnika; rozjazd liczony
w punktach procentowych; wszystkie piec pozycji ma wpis w PasmaOdniesienia.
```

**Ukończone, gdy:** dwie krzywe przepustnicy na jednym wykresie, podciśnienie oznaczone jako wyliczone, cztery pasma to `—`.

---

## Etap K5 — mediana korekty i zmiana pól licznika

```
Etap K5 z sekcji 13 specu kontekstowego. Kontrakt: sekcja 10.5 i 8.4.

1. Nowe pole podsumowania: medianaKorektyDlugoterminowej — MEDIANA serii 0107.

   MEDIANA, NIE SREDNIA. Jeden nietypowy przejazd — postoj w korku, jazda w upal,
   pieciominutowy skok do sklepu — przesuwa srednia na tyle, ze trend z miesiaca
   na miesiac przestaje byc widoczny. Mediana ignoruje wybryki i pokazuje typowa jazde.

   null, gdy serii 0107 w przebiegu nie ma.

2. Zamiana pol licznika (jesli nie zrobione w K3):
     czasPozaPasmemKorektSekundy  ->  czasPozaPasmemWPetliZamknietejSekundy
                                  +   czasWPetliZamknietejSekundy

3. Testy:
   - mediana na serii z JEDNYM skrajnym punktem odstajacym — wynik ma sie
     roznic od sredniej. Napisz ten test tak, zeby implementacja na sredniej go oblala.
   - null gdy serii brak,
   - oba nowe pola czasu liczone poprawnie na sztucznym przebiegu z naprzemienna
     petla otwarta i zamknieta.
```

**Ukończone, gdy:** test mediany oblewa implementację na średniej.

---

## Etap K6 — karta miesiąca

```
Etap K6 z sekcji 13 specu kontekstowego. Uklad: sekcja 10.

1. Karta W NAGLOWKU ZAKLADKI HISTORIA, nad siatka kalendarza. Kalendarz juz ma
   przewijanie miesiecy strzalkami — karta zmienia sie razem z nim.
   ZERO nowej nawigacji, ZERO nowej zakladki.

2. Wiersze i zrodla — sekcja 10.1. Wszystko liczone z ISTNIEJACYCH pol podsumowania
   plus mediana z K5.

3. WIERSZ "BEZ ROZGRZANIA" — najwazniejszy i jedyny bez zadnej wymyslonej liczby.
   Liczy sesje, w ktorych czasDo90CSekundy == null, czyli plyn NIGDY nie osiagnal 90 °C.

   NIE definiuj "zimnego startu", NIE wprowadzaj progu temperatury, NIE licz
   "krotkich przejazdow" wedlug kilometrow. Wystarczy policzyc wartosci null.

   Odpowiada na pytanie, ktorego inaczej nie da sie zadac: ile razy w tym miesiacu
   uruchomilem silnik i zgasilem go, zanim sie rozgrzal.

4. KARTA NIE ORZEKA — sekcja 10.3. Kolumna roznicy zawiera WYLACZNIE liczbe ze znakiem:
   "+28 s", "+0,3 pp", "+4". BEZ strzalek wartosciujacych, BEZ czerwonego koloru,
   BEZ slow "pogorszenie" czy "lepiej".

   POWOD: nie wiem, czy 28 sekund to problem. Moze termostat siada, a moze w lipcu
   bylo cieplej. Liczby obok siebie to informacja, strzalka to wniosek.

5. WSZYSTKIE WARTOSCI ZBIORCZE JAKO MEDIANA, nie srednia — sekcja 10.4.

6. PUSTY MIESIAC pokazuje "—" we wszystkich polach, NIE zera.
   Zero przejazdow to co innego niz zero kilometrow.

Testy: pusty miesiac daje "—" a nie zera; kolumna roznicy nie zawiera zadnego
slowa oceniajacego (test na tekscie); wiersz "bez rozgrzania" liczy null-e
i nie stosuje zadnego progu.
```

**Ukończone, gdy:** pusty miesiąc daje `—`, a test tekstowy potwierdza brak słów oceniających.

---

## Etap K7 — weryfikacja w aucie

```
Etap K7 z sekcji 13 specu kontekstowego. Kontrakt: sekcja 14.

Przygotuj docs/weryfikacja-kontekst.md z miejscem na wynik kazdego punktu:

1. Zimny start — kiedy status przechodzi z 1 (za zimny) na 2 (zamknieta).
2. Jazda ustabilizowana 10–15 min — CZY WIDAC CIENIOWANIE PRZEDMUCHIWANIA
   i czy skoki korekt na nie przypadaja.
3. Mocne przyspieszenie — status ma przejsc na 4.
4. Hamowanie silnikiem — tez 4, odciecie paliwa.
5. Panel Powietrze na jalowym — zadana i rzeczywista przepustnica pokrywaja sie.
6. Panel Powietrze przy zmianach gazu — czy jedna krzywa zostaje w tyle.
7. Po miesiacu — karta miesiaca ma sensowne liczby.

PUNKT 2 JEST SPRAWDZIANEM SENSU CALEGO ROZSZERZENIA. Jesli w danych okaze sie,
ze przedmuchiwanie nigdy nie przypada na skoki korekt, to znaczy ALBO ze probkowanie
0,4 Hz jest za wolne (sekcja 7.4), ALBO ze hipoteza byla bledna.
W obu przypadkach TO SIE ZAPISUJE, a nie naciaga do niej danych.

Po powrocie dopisz prawdziwe odpowiedzi poziomow A, B i C do MockI40Script.
```

**Ukończone, gdy:** atrapa zawiera prawdziwe odpowiedzi trzech nowych poziomów, a punkt 2 ma zapisany wynik — także gdy wynik jest negatywny.

---

## Prompt kontrolny — po rozszerzeniu kontekstowym

```
Przeczytaj trzy specy i przejrzyj repozytorium. Nie pisz kodu. Odpowiedz z dowodem
z pliku i numeru linii:

1. Czy petla goraca ma nadal szesc PID-ow, 4 Hz i obowiazkowe 010D oraz 0105?

2. Czy istnieje test dowodzacy, ze zaden cykl nie wykonuje trzech zapytan,
   i na ilu cyklach dziala? Powinno byc co najmniej 200 000.

3. Czy gdziekolwiek w kodzie wysylane jest polecenie "03" tam, gdzie powinno byc
   "0103"? To blad bez objawu — odpowiedz da sie sparsowac.

4. Czy dekoder 0103 dla wartosci spoza enumeracji zwraca "nieznany" z surowa liczba,
   czy dopasowuje do najblizszej?

5. Czy MIX-1 odpala sie dla wartosci 1 albo 4? Nie powinien — to normalne stany pracy.

6. Czy wartosc 16 trafia do cieniowania "petla otwarta"? Nie powinna — 16 to petla
   ZAMKNIETA z awaria sondy.

7. Czy licznik czasu poza pasmem liczy tylko w petli zamknietej — i w liczniku,
   i w mianowniku?

8. Czy medianaKorektyDlugoterminowej jest mediana, czy srednia? Pokaz kod.

9. Czy wiersz "bez rozgrzania" na karcie miesiaca uzywa jakiegokolwiek progu
   temperatury? Nie powinien — ma liczyc wartosci null.

10. Czy kolumna roznicy na karcie miesiaca zawiera gdziekolwiek slowo oceniajace
    albo strzalke wartosciujaca?

11. Czy to rozszerzenie wprowadzilo JAKIKOLWIEK nowy prog liczbowy? Nie powinno.
    Wypisz wszystkie nowe stale i pokaz, ze kazda jest albo enumeracja ze zrodla,
    albo wzorem z katalogu.

12. Czy piec nowych parametrow ma wpisy w PasmaOdniesienia?
```
