# i40-android — rozszerzenie objaśnień

**Data:** 2026-08-15
**Status:** zaakceptowany, gotowy do realizacji
**Rozszerza:** `2026-08-14-i40-android-design.md` (bazowy),
`2026-08-14-i40-android-diagnostyka-design.md` (diagnostyczne),
`2026-08-14-i40-android-kontekst-design.md` (kontekstowe),
`2026-08-14-i40-android-odniesienie-design.md` (odniesienia),
`2026-08-15-i40-android-historia-design.md` (historii).
Wszystkie obowiązują bez zmian poza tym, co ta specyfikacja jawnie zmienia (sekcja 11)

**Treść haseł słownika:** `docs/slownik.md` — dokument źródłowy, nie do generowania

---

## 1. Cel

Aplikacja umie już bardzo dużo powiedzieć o silniku. Problem w tym, że **mówi to językiem,
którego trzeba się najpierw nauczyć** — a nie ma gdzie.

Pięć paneli ekranu żywego to pulpity dla kogoś, kto wie, czego szuka. Ktoś, kto nie wie, widzi
trzydzieści liczb i nie ma pojęcia, na którą patrzeć ani co znaczy, że jedna z nich odstaje.
Zakładka Przegląd mówi po polsku, ale **dopiero na postoju i dopiero po uruchomieniu procedury**.

To rozszerzenie dokłada dwie rzeczy:

1. **Panel „Stan"** — jedno zdanie o tym, czy jest dobrze, i lista tego, co odstaje. Pierwszy
   w kolejności paneli, do zerknięcia w dwie sekundy.
2. **Słownik** — dotknięcie dowolnej wartości w całej aplikacji otwiera wyjaśnienie: co to jest,
   po co się na to patrzy, co znaczy wyjście poza pasmo i **czego ta liczba nie mówi**.

Rozszerzenie **nie dokłada ani jednego zapytania OBD** i **nie wprowadza ani jednego nowego
progu**. Wszystko liczy z pasm i reguł, które już istnieją.

---

## 2. Zasada: ciemny kokpit

W lotnictwie obowiązuje reguła **dark cockpit**: gdy wszystko działa, panel jest ciemny.
Każde zapalone światło znaczy *coś wymaga uwagi*.

Pierwszy szkic tego panelu wyglądał tak:

```
Silnik rozgrzany
Mieszanka w normie
Ciśnienie paliwa w normie
Wszystkie parametry w pasmach
```

Cztery linijki, które **za każdym razem wyglądają tak samo**. Po tygodniu przestaje się je
czytać — a wtedy przestaje się zauważać, gdy jedna się zmieni. To ten sam mechanizm, przez
który aplikacja ma tylko pięć alarmów, a nie trzydzieści.

**Panel Stan pokazuje wyłącznie odchylenia.** Gdy nie ma odchyleń — jedno zdanie i koniec.

---

## 3. Zrównoważenie — co to rozszerzenie obciąża

### 3.1 Magistrala: nic

**Zero nowych zapytań OBD.** Panel Stan czyta te same próbki, które już płyną. Słownik nie czyta
niczego — pokazuje wartość, która i tak jest na ekranie. Test tego pilnuje (sekcja 12).

### 3.2 Progi: ani jednego nowego

Panel Stan sprawdza **przynależność do pasm z `PasmaOdniesienia`** i używa **gotowych zdań
z siedemnastu istniejących reguł przeglądu**. Nie ma własnych progów ani własnych wniosków.

Słownik **nie zawiera ani jednej liczby wpisanej w tekst** — sekcja 7.3.

### 3.3 Ekran: szósty panel i jeden wysuwany arkusz

Zakładki są trzy i zostają trzy. Paneli robi się sześć. Słownik to arkusz wysuwany z dołu,
bez własnego miejsca w nawigacji.

### 3.4 Baza: nic

Zero nowych tabel, zero nowych kolumn. Wersja schematu zostaje **3**.

---

## 4. Panel „Stan"

### 4.1 Miejsce w kolejności — pierwszy

```
● ○ ○ ○ ○ ○   STAN  ▸ Podstawowy ▸ Mieszanka ▸ Wtrysk GDI ▸ Termika ▸ Powietrze
```

Panel podsumowujący jako pierwszy, bo przy sześciu panelach domyślny powinien być **najprostszy
z nich**. Kto chce liczb, przesuwa palcem w bok. Kto chce wiedzieć, czy jest dobrze — już wie.

To zmienia to, co widać po każdym uruchomieniu aplikacji, i jest zmianą wobec §8.1 rozszerzenia
diagnostycznego (sekcja 11).

### 4.2 Trzy stany — i trzeci jest najważniejszy

| Stan | Kiedy | Co pokazuje |
|---|---|---|
| **W normie** | Wszystkie zmierzone parametry w pasmach | Jedno zdanie |
| **Odchylenia** | Co najmniej jeden poza pasmem | Listę odchyleń, najcięższe u góry |
| **Jeszcze nie wiem** | Są parametry nieodczytane w tej sesji | Co już wiadomo + czego jeszcze nie |

**Stan „jeszcze nie wiem" jest obowiązkowy i nie wolno go pomijać.** Trzy sekundy po odpaleniu
silnika połowa parametrów nie została ani razu odczytana, a druga połowa nie może być w normie,
bo silnik jest zimny. Napis „wszystko w normie" byłby wtedy nieprawdą — i to nieprawdą
najgorszego rodzaju, bo uspokajającą.

### 4.3 Stan „w normie"

```
┌───────────────────────────────────────────┐
│ 88 °C ~  │  92 °C   │ 13,9 V   │  +3,9 %  │
│ OLEJ mod.│   PŁYN   │ NAPIĘCIE │ KOREKTA D│
│   ≥ 90   │  70–105  │ 13,0–15,0│ −10 – +10│
├──────────┴──────────┴──────────┴──────────┤
│ ● ○ ○ ○ ○ ○  STAN         04:12   4,0 Hz  │
├───────────────────────────────────────────┤
│                                           │
│                                           │
│            Wszystko w normie              │
│                                           │
│      Silnik rozgrzany · olej gotowy       │
│                                           │
│                                           │
└───────────────────────────────────────────┘
```

Jedno zdanie, duże, na środku. Druga linijka to **kontekst, nie lista** — mówi, w jakim stanie
jest silnik, bo od tego zależy, czy „w normie" w ogóle coś znaczy.

**Nie wolno dopisywać wyliczenia sprawdzonych parametrów.** To wraca do wersji, którą sekcja 2
odrzuca.

### 4.4 Stan „odchylenia"

```
├───────────────────────────────────────────┤
│ ▲ Mieszanka uboga                         │
│   korekta długa  +14 %       norma ±10    │
│                              → Mieszanka  │
│                                           │
│ ▼ Ciśnienie paliwa poniżej normy          │
│   27 bar na jałowym          norma 34–55  │
│                            → Wtrysk GDI   │
├───────────────────────────────────────────┤
│ Pozostałe w normie · silnik rozgrzany     │
└───────────────────────────────────────────┘
```

Każde odchylenie w trzech wierszach:

| Wiersz | Treść | Skąd |
|---|---|---|
| 1 | Zdanie po polsku + znacznik `▲`/`▼` | **Wniosek istniejącej reguły**, gdy któraś odpala. Inaczej nazwa parametru + „poniżej/powyżej normy" |
| 2 | Wartość i pasmo | `PasmaOdniesienia` |
| 3 | Skrót do panelu | Mapa parametr → panel, sekcja 4.7 |

**Kolejność: waga malejąco, potem kolejność z `PasmaOdniesienia`.** Wagi to `usterka`,
`uwaga`, `informacja` — te same, które ma przegląd. Odchylenie bez pasującej reguły dostaje
wagę `uwaga`, bo wyjście poza pasmo bez wniosku nadal jest czymś, na co warto spojrzeć.

**Więcej niż cztery odchylenia** → pokazujemy cztery i wiersz `… i N dalszych → Przegląd`.
Panel, którego nie da się przeczytać w dwie sekundy, przestaje być panelem podsumowującym.

### 4.5 Stan „jeszcze nie wiem"

```
├───────────────────────────────────────────┤
│                                           │
│           Silnik się rozgrzewa            │
│        płyn 48 °C · olej 31 °C ~          │
│                                           │
├───────────────────────────────────────────┤
│  Jeszcze nie zmierzone                    │
│  katalizator · ciśnienie paliwa · korekty │
└───────────────────────────────────────────┘
```

**Parametr nieodczytany ani razu w tej sesji jest „jeszcze nie zmierzony", nigdy „w normie".**

Ten stan **współistnieje z odchyleniami**: gdy coś już odstaje, a reszta jeszcze się nie
odczytała, panel pokazuje odchylenia u góry i wiersz „jeszcze nie zmierzone" pod spodem.

Wiersz znika, gdy wszystkie parametry z pasmami zostały odczytane co najmniej raz. Przy
nastawie `Zrównoważona` trwa to około **czterech sekund** — poziom wolny `C` chodzi co dwadzieścia
cykli gorących. To nie jest usterka, tylko konsekwencja zrównoważonego odpytywania i tak ma
wyglądać.

### 4.6 Wiersz kodów błędów

Nowy kod wykryty w trakcie jazdy dostaje **osobny wiersz na samej górze**, przed odchyleniami:

```
│ ✕ Nowy kod błędu: P0171          → Przegląd │
```

Jest to najcięższa rzecz, jaka może się w trakcie jazdy zdarzyć, a panel bez tego wiersza
wyglądałby na kompletny, nie będąc.

Wiersz pokazuje **surowy kod zawsze**, a opis ze słownika DTC tylko wtedy, gdy istnieje —
zgodnie z zasadą z `.cursor/rules/20-obd.mdc`: brak opisu jest uczciwszy niż zgadnięty.

### 4.7 Mapa parametr → panel

Skrót w trzecim wierszu odchylenia prowadzi do panelu, który ten parametr pokazuje **na wykresie**,
nie na kaflu:

| Parametr | Panel |
|---|---|
| `0C` obroty, `04` obciążenie, `0E` zapłon | Podstawowy |
| `06`, `07` korekty, `03` pętla, `44` lambda | Mieszanka |
| `23` ciśnienie szyny, `43` obciążenie abs., `11` przepustnica | Wtrysk GDI |
| `3C` katalizator, `05` płyn, olej (model), `0F` dolot, `46` otoczenie | Termika |
| `0B` kolektor, `33` atmosferyczne, `4C` przepustnica zadana, `49` pedał | Powietrze |
| pozostałe | Przegląd → Odczyty |

Ostatni wiersz jest obowiązkowy: nie każdy parametr z pasmem ma swój wykres, a skrót
prowadzący donikąd jest gorszy niż jego brak.

### 4.8 Panel Stan nie alarmuje

**Zero nowych dźwięków.** Alarmów zostaje pięć.

Panel Stan jest ekranem do zerknięcia, nie systemem ostrzegania. Dokładanie sygnałów
dźwiękowych do rzeczy, które nie wymagają reakcji w trakcie jazdy, to prosta droga do tego,
żeby zaczęły być ignorowane także te pięć, które wymagają.

---

## 5. Stan parametru — jedna czysta funkcja

Cały panel stoi na jednej funkcji, która nie wie nic o Androidzie:

```
stanParametru(pid, wartosc, odczytanoWTejSesji, warunkiWaznosciSpelnione) -> StanParametru

  NIE_ZMIERZONY     odczytanoWTejSesji == false
  NIEWAZNY_TERAZ    warunkiWaznosciSpelnione == false
  BEZ_PASMA         pasmo dla pid to jawne "brak"
  W_NORMIE          wartość wewnątrz pasma
  PONIZEJ           wartość poniżej dolnej granicy
  POWYZEJ           wartość powyżej górnej granicy
```

**Kolejność sprawdzeń jest obowiązkowa.** `NIE_ZMIERZONY` musi być pierwszy — parametr bez
odczytu ma `wartosc == null` i każde inne uporządkowanie skończy się porównaniem z pustką albo,
gorzej, z podstawionym zerem.

### 5.1 `NIEWAZNY_TERAZ` — czwarty stan, dołożony poprawką P1

Parametr może mieć odczyt i mimo to **nie nadawać się do porównania z pasmem**, bo warunki,
w których cokolwiek znaczy, nie są w tej chwili spełnione.

Dziś dotyczy to **korekt paliwa poza pętlą zamkniętą** — i tylko ich. `warunkiWaznosciSpelnione`
dla `0106` i `0107` to `status0103 ∈ {2, 16}`; dla wszystkich pozostałych parametrów stała
`true`. Ta sama reguła, co dla czwartego kafla (§8.5 warstwy kontekstowej) — **jedno źródło,
nie dwie kopie**.

> **Bez tego stanu aplikacja przeczyłaby sama sobie w jednym spojrzeniu.** Kafel na górze
> pokazywałby `— ○`, a panel Stan pod nim w tej samej sekundzie orzekałby „Mieszanka uboga,
> korekta +14 %" — z wartości zamrożonej przed przejściem w pętlę otwartą. Dwa widoki tej samej
> liczby muszą milczeć w tym samym momencie.

`NIEWAZNY_TERAZ` liczy się do `JESZCZE_NIE_WIEM`, **nie** do `W_NORMIE` i **nie** do
`ODCHYLENIA` — z tego samego powodu co `BEZ_PASMA`: to niewiedza, a nie dobra wiadomość.
Na liście „jeszcze nie zmierzone" pokazuje się z powodem („korekty — pętla otwarta"), żeby
nie wyglądał na zerwany odczyt.

⚠️ **Wymaga `0103`, czyli etapu K2.** Gdy status jest niedostępny, `warunkiWaznosciSpelnione`
przyjmuje `false` — zachowawczo, tak samo jak kafel.

`BEZ_PASMA` jest osobnym stanem, nie odmianą `W_NORMIE`. Parametr bez normy **nie jest
w normie** — o nim po prostu nic nie wiadomo. Zliczanie go do „wszystko w normie" byłoby
policzeniem niewiedzy jako dobrej wiadomości.

Stan panelu wynika z zestawu stanów parametrów:

```
jest choć jeden PONIZEJ/POWYZEJ                    → ODCHYLENIA
w przeciwnym razie:
  jest choć jeden NIE_ZMIERZONY lub NIEWAZNY_TERAZ → JESZCZE_NIE_WIEM
  w przeciwnym razie                               → W_NORMIE
```

---

## 6. Słownik — po co osobno

Panel Stan mówi **że** coś odstaje. Nie mówi, **czym to coś jest**.

Człowiek, który zobaczy „korekta długa +14 %", ma dwie drogi: zapytać internet albo zignorować.
Pierwsza kończy się forum, na którym ktoś twierdzi rzeczy sprzeczne z tym, co pokazuje
aplikacja. Druga kończy się tym, że cały ekran jest bezużyteczny.

Słownik jest trzecią drogą i **jedynym sposobem, żeby się tego nauczyć w trakcie używania**.

---

## 7. Hasło słownika

### 7.1 Wysuwany arkusz, nie okno

Dotknięcie dowolnej wartości — kafla, wiersza odczytów, punktu na wykresie, wiersza raportu —
wysuwa arkusz z dołu ekranu. Nie okno modalne: arkusz da się zsunąć palcem w dół, a pod nim
widać, skąd się przyszło.

**Blokada prędkościowa z §12.4 bazowego obowiązuje**: w ruchu słownik się nie otwiera.
To jest treść do czytania, a nie do zerkania.

### 7.2 Układ — cztery rubryki, zawsze te same

```
┌───────────────────────────────────────────┐
│  KOREKTA DŁUGOTERMINOWA             [ ✕ ] │
├───────────────────────────────────────────┤
│  Teraz          +3,9 %                    │
│  Norma          ±10 %                     │
│  Poprzednio     +3,8 %      (8 sierpnia)  │
├───────────────────────────────────────────┤
│  CO TO JEST                               │
│  …                                        │
│                                           │
│  PO CO NA TO PATRZEĆ                      │
│  …                                        │
│                                           │
│  GDY WYJDZIE POZA PASMO                   │
│  …                                        │
│                                           │
│  CZEGO TO NIE MÓWI                        │
│  …                                        │
├───────────────────────────────────────────┤
│  PID 0107 · poziom średni B · co 2,5 s    │
└───────────────────────────────────────────┘
```

**Rubryki są stałe i obowiązkowe.** Ten sam układ wszędzie znaczy, że po trzecim haśle wiadomo,
gdzie patrzeć — i że autor żadnej nie mógł pominąć.

**Parametry mają cztery rubryki, pojęcia trzy.** Pojęcie nie jest wielkością mierzoną, więc
rubryka o wyjściu poza pasmo nie ma do czego się odnieść; hasła pojęć mają
`CO TO JEST` / `PO CO CI TO WIEDZIEĆ` / `CZEGO TO NIE MÓWI`.

**Rubryka „czego to nie mówi" jest obowiązkowa i nie wolno jej opuszczać.** W słownikach jest
rzadka; tutaj jest konieczna, bo cała aplikacja stoi na tym, że nie udaje mądrzejszej, niż
jest. Człowiek, który nie przeczyta granicy, sam sobie dopowie diagnozę — i będzie to diagnoza,
której aplikacja nigdy nie postawiła.

Gdzie parametr nie ma pasma, rubryka „gdy wyjdzie poza pasmo" zawiera `—` z jednozdaniowym
powodem. Pusta rubryka jest zabroniona, tak samo jak pusta kolumna normy.

### 7.3 Liczby nigdy nie są wpisane w tekst

**Blok górny — `Teraz`, `Norma`, `Poprzednio` — czyta z tych samych źródeł co panele:**

| Wiersz | Źródło |
|---|---|
| `Teraz` | Bieżąca próbka; `—` gdy nieodczytana |
| `Norma` | `PasmaOdniesienia` — to samo źródło, co kolumna normy |
| `Poprzednio` | `punkt_odniesienia` — to samo źródło, co kolumna „poprzednio" |

Gdyby `±10` było wklepane w tekst hasła, przy pierwszej zmianie progu **słownik zacząłby mówić
co innego niż panel** — i nikt by tego nie zauważył, bo nikt nie sprawdza tekstów pomocy.

**Test tego pilnuje:** żaden tekst hasła nie zawiera liczby, która występuje
w `PasmaOdniesienia`. Wyjątkiem są liczby będące częścią wyjaśnienia fizyki albo nazwą
identyfikatora — jak `2,3 bara w oponie` czy `Czas do 90 °C`; wszystkie są wypisane
w `docs/slownik.md` jako **lista zamknięta**.

⚠️ **Test musi odsiać trzy rodzaje fałszywych trafień**, wypisane w `docs/slownik.md`: numery
PID-ów (`0105` zawiera `105`), nazwę adaptera (`ELM327` zawiera `27`) i stopki techniczne
z częstotliwością. Test porównujący gołe podciągi zgłosi je wszystkie przy pierwszym
uruchomieniu — sprawdzono to na gotowej treści.

### 7.4 Stopka techniczna

Ostatni wiersz: numer PID, poziom odpytywania i częstotliwość. Dla kogoś, kto chce wejść
głębiej — i dla mnie, gdy będę to za rok debugował.

Dla wartości wyliczanych zamiast numeru PID stoi **wzór**: `podciśnienie = 0133 − 010B`.

### 7.5 Nawigacja wewnątrz słownika

Pojęcia użyte w treści hasła są **odsyłaczami do swoich haseł**. „Korekta długoterminowa"
odsyła do „pętla zamknięta", ta do „sonda lambda".

**Bez głębokości większej niż trzy przeskoki wstecz.** Arkusz trzyma stos, przycisk wstecz
wraca, ale po trzech poziomach zamiast kolejnego przeskoku pojawia się przycisk `wróć do
początku`. Bez tego łatwo zgubić, po co się w ogóle tu weszło.

---

## 8. Zakres słownika — policzony, nie oszacowany

### 8.1 Co ma hasło

Kontrakt, bliźniaczy do kontraktu pasm z §9.4 rozszerzenia diagnostycznego:

> **Każda wartość wyświetlana gdziekolwiek w aplikacji ma hasło w słowniku.**

### 8.2 Parametry mierzone — 32 PID-y

Wyliczone jako **przecięcie katalogu z §9.1 bazowego i masek z §3.2, minus poprawka P1**:

```
01 03 04 05 06 07 0B 0C 0D 0E 0F 11 13 1C 1F 21 23 2E 30 31 33 3C
41 42 43 44 45 46 47 49 4A 4C
```

**`10`, `5C` i `5E` są w katalogu, ale nie w masce** — to auto ich nie ma, więc nie są
wyświetlane i haseł nie potrzebują.
**`15`, `32`, `34` i `56` są w masce, ale nie w katalogu** — katalog jest zamknięty, więc też
nie są wyświetlane.
**`2F` jest w katalogu i w masce, a mimo to nie jest wyświetlany** — jedyny taki przypadek.
Maska go zgłasza, auto zwraca zero niezależnie od stanu baku (poprawka P1, §3.2 bazowego).
To trzecia, osobna kategoria: **nie „nie ma", tylko „jest i kłamie".**

PID-y bliskoznaczne dzielą hasło z rozróżnieniem w treści: pozycje przepustnicy `11 45 47 4C`,
pozycje pedału `49 4A`, stan monitorów `01 41`. Daje to **27 haseł na 32 PID-y**.

### 8.3 Wartości wyliczane — 10 haseł

Nie są odczytem, tylko wynikiem działania na odczytach, i **każda musi to o sobie powiedzieć**:

```
temperatura oleju (model)      podciśnienie
suma korekt                    rozjazd przepustnicy
dystans                        średnia prędkość
czas do 90 °C                  czas poza pasmem w pętli zamkniętej
mediana korekty długiej        maksymalne ciśnienie szyny i obciążenie przy nim
```

### 8.4 Pojęcia — 33 hasła

Bez nich połowa haseł parametrów odsyłałaby w próżnię:

```
OBD-II                PID                    tryb zapytania
adapter ELM327        maska PID-ów           sterownik silnika
kod błędu DTC         kody: zapisany, oczekujący, trwały
kontrolka MIL         monitory gotowości     pętla zamknięta i otwarta
sonda lambda          bank                   katalizator
wtrysk bezpośredni GDI                       przedmuchiwanie zbiornika
GMP i wyprzedzenie zapłonu                   norma
„poprzednio"          model kontra pomiar    pewność modelu
kreska zamiast zera   znaczniki ▲ ▼ ~ ⌀ ○    waga wniosku
alarm i karencja      poziomy odpytywania    nastawa Zrównoważona
przejazd i sesja odzyskana                   decymacja
VIN                   blokada prędkościowa   czego ten egzemplarz nie ma
obsługiwany bez danych
```

Dwa doszły przy pisaniu treści: **blokada prędkościowa** i **czego ten egzemplarz nie
ma** były przywoływane w hasłach parametrów, a nie miały własnych — odsyłacz prowadziłby
w próżnię.

Trzecie doszło z poprawką P1: **obsługiwany bez danych**. Hasło **było
parametrem, a stało się pojęciem** — parametru nie ma już na żadnym ekranie, ale odsyłacze
do niego prowadzą z „maski PID-ów" i „czego ten egzemplarz nie ma", a samo zjawisko jest
najciekawszą rzeczą, jakiej to auto o sobie nie mówi. Skasowanie hasła zerwałoby dwa odsyłacze
i skasowało jedyny zapisany przykład tego, że **maska potrafi kłamać**.

### 8.5 Razem

| Grupa | Haseł |
|---|---|
| Parametry mierzone (32 PID-y) | **27** |
| Wartości wyliczane | **10** |
| Pojęcia | **33** |
| **Razem** | **70** |

Suma się nie zmieniła — jedno hasło przeszło z pierwszej grupy do trzeciej.

### 8.6 Treść jest w `docs/slownik.md` i pochodzi wyłącznie stamtąd

**Ani jedno zdanie treści haseł nie powstaje w Cursorze.**

Rubryki „gdy wyjdzie poza pasmo" i „czego to nie mówi" to dokładnie te miejsca, w których
zmyślone wyjaśnienie wygląda identycznie jak prawdziwe — i w których pomyłka wysyła człowieka
naprawiać nie to, co trzeba. Obowiązuje tu `.cursor/rules/30-zrodla.mdc` w pełnym brzmieniu.

`docs/slownik.md` jest **dokumentem źródłowym**, jak `docs/zrodla.md`. Zadaniem etapu S3 jest
przenieść go do zasobów aplikacji **co do zdania**, a nie napisać na nowo.

---

## 9. Nie-cele

**Samouczek przy pierwszym uruchomieniu.** Ekrany powitalne przeklikuje się bez czytania.
Wyjaśnienie w miejscu, w którym powstaje pytanie, działa; wyjaśnienie na zapas nie.

**Wyszukiwarka w słowniku.** Siedemdziesiąt haseł, wszystkie osiągalne dotknięciem tego,
o co się pyta. Wyszukiwarka byłaby przyznaniem, że wejścia kontekstowe nie działają.

**Diagnozy w słowniku.** „Powyżej +10 % oznacza nieszczelny dolot" — nie. Rubryka wymienia
możliwości i kończy się rubryką o tym, czego liczba nie rozstrzyga. Granica z `30-zrodla.mdc`
obowiązuje bez zmian.

**Instrukcje napraw.** Aplikacja mówi, co pokazuje sterownik. Nie mówi, co odkręcić.

**Tłumaczenie na inne języki.** Jedno auto, jeden użytkownik.

**Alarmy z panelu Stan.** Sekcja 4.8.

**Wyliczanie sprawdzonych parametrów w stanie „w normie".** Sekcja 2 — to jest cały sens
tego panelu.

---

## 10. Decyzje projektowe

| Decyzja | Dlaczego |
|---|---|
| **Panel pokazuje tylko odchylenia** | Lista, która zawsze wygląda tak samo, przestaje być czytana — a wtedy nie widać zmiany |
| **Panel Stan pierwszy w kolejności** | Przy sześciu panelach domyślny powinien być najprostszy |
| **Stan „jeszcze nie wiem" osobno** | Trzy sekundy po starcie „wszystko w normie" byłoby nieprawdą, i to uspokajającą |
| **`BEZ_PASMA` to nie `W_NORMIE`** | Parametr bez normy nie jest w normie — o nim nic nie wiadomo. Liczenie niewiedzy jako dobrej wiadomości to cichy fałsz |
| **Maksymalnie cztery odchylenia** | Panel, którego nie da się przeczytać w dwie sekundy, przestaje być podsumowaniem |
| **Zdania z istniejących reguł** | Siedemnaście wniosków jest już napisanych i uzasadnionych. Drugi komplet rozjechałby się z pierwszym |
| **Panel nie alarmuje** | Alarmów jest pięć i mają takie zostać |
| **Stały komplet rubryk** | Stały układ czyni hasła skanowalnymi i uniemożliwia pominięcie rubryki |
| **Rubryka „czego to nie mówi"** | Bez granicy człowiek dopowie sobie diagnozę, której aplikacja nie postawiła |
| **Liczby czytane, nie wpisywane** | Norma wklepana w tekst rozjedzie się z panelem przy pierwszej zmianie i nikt tego nie zauważy |
| **Arkusz z dołu, nie okno modalne** | Da się zsunąć, widać, skąd się przyszło. §12.4 zabrania okien modalnych w ruchu |
| **Słownik zablokowany w ruchu** | To treść do czytania, nie do zerkania |
| **Treść w `docs/slownik.md`** | Wyjaśnienia to miejsce, w którym agent zacznie zmyślać, a zmyślone wygląda jak prawdziwe |
| **Bez wyszukiwarki** | Wyszukiwarka byłaby przyznaniem, że wejścia kontekstowe nie działają |

---

## 11. Zmiany w kontraktach

| Miejsce | Zmiana |
|---|---|
| **§8.1 rozszerzenia diagnostycznego** | Paneli jest **sześć**, nie pięć. **Stan jest pierwszy**; kolejność pozostałych pięciu bez zmian |
| **§8.7 rozszerzenia diagnostycznego** | Bez zmian: przełączanie paneli dozwolone w ruchu. **Słownik nie** — podlega blokadzie parametrów |
| **§9.4 rozszerzenia diagnostycznego** | Bez zmian, dochodzi **bliźniaczy kontrakt**: każda wyświetlana wartość ma hasło słownika |
| **§10.3 progi alarmów** | **Bez zmian.** Alarmów zostaje pięć |
| **§10.4 progi reguł przeglądu** | **Bez zmian.** Panel Stan używa istniejących reguł, nie dokłada |
| **Pętla, PID-y, nastawy, pasma, baza** | **Bez żadnych zmian** |

**Czego nie zmieniamy:** pętli gorącej, jej częstotliwości, składu obowiązkowego `0D`, `05`
i `04`, żadnego progu, żadnej reguły, żadnej osi, składu żadnego poziomu odpytywania, schematu
bazy.

---

## 12. Testy

| Obszar | Test | Dlaczego akurat ten |
|---|---|---|
| **Zero nowych zapytań** | Lista odpytywanych PID-ów przed i po rozszerzeniu **identyczna** | Jedyny test pilnujący, że warstwa objaśnień nie dokłada do magistrali |
| **`NIE_ZMIERZONY` pierwszy** | Parametr z `wartosc == null` daje `NIE_ZMIERZONY`, nie `PONIZEJ` | Zła kolejność sprawdzeń kończy się porównaniem z pustką albo z podstawionym zerem |
| **`BEZ_PASMA` ≠ `W_NORMIE`** | Parametr z pasmem `—` **nie liczy się** do „wszystko w normie" | §5 — bez tego niewiedza wygląda jak dobra wiadomość |
| **`NIEWAZNY_TERAZ` przy pętli otwartej** | Korekty `0106` i `0107` przy `status0103 ∉ {2, 16}` **i przy braku odczytu `0103`** dają `NIEWAZNY_TERAZ`, nigdy `POWYZEJ`/`PONIZEJ` | §5.1. Test ma sprawdzić **ten sam moment** na kaflu i na panelu Stan: albo oba milczą, albo oba mówią. Rozjazd tych dwóch to sprzeczność widoczna w jednym spojrzeniu |
| **Stan po starcie** | Sesja z jednym odczytem daje `JESZCZE_NIE_WIEM`, **nigdy** `W_NORMIE` | §4.5 — najważniejszy test tego rozszerzenia |
| **Odchylenie ma pierwszeństwo** | Odchylenie + nieodczytane parametry → `ODCHYLENIA` z wierszem „jeszcze nie zmierzone" | §4.5 |
| **Zdania z reguł** | Wniosek panelu dla korekty +14 % jest **dosłownie** wnioskiem reguły z tabeli 10.4 | Drugi komplet zdań rozjechałby się z pierwszym |
| **Limit czterech** | Sześć odchyleń → cztery wiersze + `… i 2 dalsze` | §4.4 |
| **Kolejność odchyleń** | `usterka` przed `uwaga` przed `informacja` | §4.4 |
| **Brak alarmu** | Panel Stan nie wywołuje odtwarzacza dźwięku **ani razu** | §4.8 |
| **Kontrakt haseł** | Każdy wyświetlany parametr **i każde pojęcie użyte w treści** ma hasło | Bliźniak kontraktu pasm |
| **Komplet rubryk** | Żadne hasło nie ma pustej rubryki; parametr ma cztery, pojęcie trzy; brak pasma daje `—` z powodem | §7.2 |
| **Brak liczb w tekście** | Żaden tekst hasła nie zawiera liczby z `PasmaOdniesienia`, poza listą dozwolonych z `docs/slownik.md` | §7.3 — inaczej słownik po cichu rozjedzie się z panelem |
| **Odsyłacze** | Każde pojęcie w treści prowadzi do istniejącego hasła | §7.5 |
| **Stos odsyłaczy** | Po trzech przeskokach pojawia się `wróć do początku` | §7.5 |
| **Blokada w ruchu** | Przy `010D > 0` słownik się nie otwiera | §7.1 |
| **Zgodność z `docs/slownik.md`** | Treść w zasobach **co do zdania** zgodna z dokumentem źródłowym | §8.6 — test przechodzi oba i porównuje |

Funkcja `stanParametru` i składanie stanu panelu są **czyste** — bez Androida, bez zegara.
Wszystkie testy stanu chodzą na JVM.

---

## 13. Kolejność realizacji

| Etap | Zakres | Ukończony, gdy |
|---|---|---|
| **S1** | `stanParametru`, składanie stanu panelu, mapa parametr → panel — wszystko czyste | `NIE_ZMIERZONY` pierwszy; `BEZ_PASMA` ≠ `W_NORMIE`; **`NIEWAZNY_TERAZ` dla korekt poza pętlą zamkniętą i przy braku `0103`**; testy na JVM |
| **S2** | Panel Stan: trzy stany, wiersz kodów, limit czterech, pierwszy w kolejności | Sesja z jednym odczytem daje „jeszcze nie wiem"; zero dźwięków |
| **S3** | Słownik: struktura hasła, zasoby przeniesione z `docs/slownik.md`, kontrakt | **70 haseł**, żadnej pustej rubryki, żadnej liczby z pasm w tekście |
| **S4** | Wpięcie słownika: kafle, odczyty, wykresy, raport, odsyłacze, blokada w ruchu | Każda wyświetlana wartość otwiera hasło; w ruchu nie otwiera |
| **S5** | Weryfikacja w aucie | Lista z sekcji 14 przejdzie w całości |

Wchodzą **po etapie H7**. `S1` przed `S2` jest wiążące. `S3` jest **niezależne** od `S1` i `S2` —
słownik nie potrzebuje panelu Stan i można je robić równolegle.

---

## 14. Weryfikacja w aucie

| # | Czynność | Czego szukamy |
|---|---|---|
| 1 | Odpal zimny silnik, patrz na panel Stan przez pierwsze pół minuty | **„Jeszcze nie wiem", potem „silnik się rozgrzewa"** — ani razu „wszystko w normie" |
| 2 | Poczekaj do rozgrzania na postoju | Przejście na „wszystko w normie" z drugą linijką o rozgrzaniu |
| 3 | Zerknij na panel w ruchu, licz czas | **Da się odczytać w jednym spojrzeniu** — jeśli nie, panel jest za gęsty |
| 4 | Doprowadź do odchylenia (np. zimny silnik, wysokie obroty) | Zdanie **identyczne** z tym, które daje Przegląd |
| 5 | Spróbuj otworzyć słownik w ruchu | Nie otwiera się |
| 6 | Otwórz słownik na postoju, przejdź trzy odsyłacze w głąb | Pojawia się `wróć do początku` |
| 7 | Porównaj `Norma` w słowniku z kolumną normy w Odczytach | **Identyczne** — jedno źródło działa |
| 8 | Przejdź wszystkie 33 wiersze Odczytów, dotykając każdego | Każdy otwiera hasło; żadnej pustej rubryki |

**Punkt 1 jest najważniejszy.** Sprawdza jedyną rzecz, która w tym rozszerzeniu może być
szkodliwa: uspokajający komunikat wystawiony, zanim cokolwiek zmierzono.

---

## 15. Źródła

Rozszerzenie **nie wprowadza ani jednej nowej stałej liczbowej**. Wszystkie progi, pasma
i wnioski pochodzą z warstw wcześniejszych.

Zasady projektowe, na których stoi panel:

| Zasada | Skąd | Zastosowanie |
|---|---|---|
| **Dark cockpit** | Filozofia kokpitu Airbusa — ciemny panel znaczy „wszystko działa" | §2, §4.3 |
| **Krótkie spojrzenie** | Wytyczne NHTSA o rozpraszaniu kierowcy: pojedyncze spojrzenie ≤ 2 s. **Dobrowolne i adresowane do systemów fabrycznych** — bierzemy zasadę, nie zgodność | §4.4 limit czterech |
| **Stopniowe odsłanianie** | Progressive disclosure (Nielsen) | Stan → panel → Przegląd; hasło → rubryki → stopka |
| **Zmęczenie alarmami** | Literatura o alarmach w intensywnej terapii | §4.8 — zero nowych dźwięków |
| **Kolor nie jedynym nośnikiem** | WCAG 2.1, kryterium 1.4.1 | `▲` `▼` obok koloru — zasada już obowiązuje w §8.8 diagnostycznego |
| **Rozdzielone rodzaje dokumentacji** | Diátaxis | Słownik to **wyjaśnienie**, nie instrukcja — §9 |
| **Jedno źródło prawdy** | Zasada tego projektu | §7.3 — liczby czytane, nigdy wpisywane |

Treść haseł: `docs/slownik.md`. Bibliografia progów: `docs/zrodla.md`, **bez zmian**.
