# i40-android — rozszerzenie wyglądu

**Data:** 2026-08-15
**Status:** zaakceptowany, gotowy do realizacji
**Rozszerza:** wszystkie sześć wcześniejszych dokumentów.
Obowiązują bez zmian poza tym, co ta specyfikacja jawnie zmienia (sekcja 12)

---

## 1. Cel

Cały projekt poświęca wyglądowi **trzy zdania**: motyw wyłącznie ciemny, cyfry o stałej
szerokości, kolor nigdy jako jedyny sygnał. Etap 7 mówi `ui/ — motyw` i nie mówi, **jaki**.

To znaczy, że wygląd powstanie w Cursorze przez przypadek. Domyślna paleta Compose to fiolet
Material — kompletnie obcy samochodowi. A jedyny „oczywisty" kierunek dla aplikacji OBD to
neonowe zegary na czerni, którymi wygląda **każda** aplikacja na rynku.

To rozszerzenie ustala **system tokenów**: dwa motywy, paletę, skalę typograficzną, siatkę,
pola dotykowe i zasady rysowania wykresów. Nie zmienia żadnego układu — sześć paneli, przegląd
i historia są już zaprojektowane. Daje im język wizualny.

**Zero nowych zapytań OBD. Zero nowych progów. Zero zmian w bazie.**

---

## 2. Kierunek: rejestrator, nie zegar

### 2.1 Czego świadomie nie robimy

Każda aplikacja OBD na rynku rysuje **skeuomorficzne tarcze**: obrotomierz z czerwonym polem,
igła, chrom, neonowa poświata na czerni. To jest zarazem najczęstszy wygląd, jaki produkuje
generator — ciemne tło plus jeden jaskrawy akcent.

Pójście tam nie byłoby decyzją, tylko jej brakiem.

### 2.2 Czym ta aplikacja naprawdę jest

**Ona nie pokazuje chwilowych wskazań.** Próbkuje 4 razy na sekundę, zapisuje przebieg
i przy decymacji **zachowuje minima i maksima, nigdy średnią** — bo dla skoków się na to patrzy.

To jest opis **rejestratora taśmowego**, nie zegara. Ślad na kalibrowanym papierze.

Ta metafora jest jednocześnie uczciwa wobec mechanizmu i niepodobna do niczego na rynku.

### 2.3 Element sygnaturowy: pole kalibrowane

**Linie siatki pod wykresem to granice pasm** — nie okrągłe liczby co dwadzieścia jednostek.

```
  KATALIZATOR
   870 ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄   ← górna granica pasma
   650 ────────────────────────────────────    ← dolna granica pasma
   300 ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─     ← zapłon katalizatora
     0 ___╱‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾    612 °C
```

Norma jest **wydrukowana na papierze**, po którym idzie ślad. Nie sprawdzasz, ile powinno
być — widzisz to pod krzywą.

**Gdzie parametr nie ma pasma, pole jest puste — bez linii.** Brak normy staje się widoczny
jako brak nadruku. Ta sama zasada co kreska zamiast zera, tylko narysowana.

### 2.4 Ryzyko, które podejmujemy

**Parametr jeszcze niezmierzony dostaje puste pole kalibrowane, a nie znika z ekranu.**

```
  CIŚNIENIE SZYNY
   241 ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄
   138 ────────────────────────────────────
    55 ────────────────────────────────────
    34 ────────────────────────────────────
     0                         ┆        — ⌀
                               ↑ linia skanująca
```

Widzisz siatkę bez śladu i wolno przesuwającą się linię. Każda normalna aplikacja by ten wiersz
ukryła. Ta **rysuje nieobecność** — bo cała reszta projektu robi dokładnie to samo słowami.

---

## 3. Dwa motywy

### 3.1 Dlaczego dwa, skoro bazowy mówi „wyłącznie ciemny"

Uzasadnienie z dokumentu bazowego brzmi: *ekran w desce rozdzielczej świeci* — i dotyczy
**jazdy po ciemku**. Jest słuszne i zostaje.

Ale **ten sam ciemny motyw w ostrym słońcu jest nieczytelny**. Ciemne pole odbija otoczenie
i przy pełnym słońcu widzisz w nim swoje odbicie zamiast wykresu. Jasne tło wygrywa na zewnątrz,
bo podświetlenie ekranu dodaje się do jasnego pola zamiast walczyć z odbiciem.

To nie jest kwestia gustu, tylko fizyki wyświetlacza w aucie.

**Motyw NOC zostaje domyślny.** Motyw DZIEŃ dochodzi obok niego.

### 3.2 Motyw NOC — domyślny

```
tło-głębia    #0E1114    podłoże ekranu
pole          #171B1F    kalibrowane pole wykresu, powierzchnia kafla
siatka        #2A3138    linie granic pasm nadrukowane na polu
odczyt        #E8E4DC    wartości i ślady
przygasłe     #8A9299    etykiety, jednostki, opisy drugoplanowe
model         #7FA8B8    WYŁĄCZNIE wartości z tyldą
uwaga         #E0A030    waga `uwaga`, znacznik ▲ ▼
usterka       #D9433A    waga `usterka` i alarm powtarzany
```

**Podłoże nie jest czarne.** Czysta czerń na matrycy IPS zamontowanej w desce smuży przy
przewijaniu i pokazuje każdy odcisk palca. `#0E1114` jest ciemne i lekko chłodne — ekran
wygląda na wyłączony tam, gdzie nic nie ma.

**`odczyt` jest ciepłą bielą, nie białą.** Zegary w i40 podświetlone są ciepło; wartości
w tej aplikacji mają siedzieć obok nich, nie krzyczeć.

### 3.3 Motyw DZIEŃ

```
tło-głębia    #ECE7DF    podłoże — ciepła jasność, nie biel
pole          #F7F4EF    kalibrowane pole wykresu, powierzchnia kafla
siatka        #C3BCB0    linie granic pasm
odczyt        #16181A    wartości i ślady
przygasłe     #5E6468    etykiety, jednostki, opisy drugoplanowe
model         #2A5A6B    WYŁĄCZNIE wartości z tyldą
uwaga         #A85E00    waga `uwaga`, znacznik ▲ ▼
usterka       #B3261E    waga `usterka` i alarm powtarzany
```

**Podłoże nie jest białe.** Czysta biel w pełnym słońcu na ekranie w desce oślepia i wypala
kontrast tekstu. `#ECE7DF` jest jasne bez łuny.

**Rola koloru jest identyczna w obu motywach.** Bursztyn zostaje bursztynem, tylko przyciemnionym
tak, żeby trzymał kontrast na jasnym polu. To ten sam język, inaczej oświetlony.

### 3.4 Krytyka własna tego wyboru

Ciepłe jasne tło to jeden z najczęstszych domyślnych wyglądów, jakie produkuje generator.
Sprawdziłem to świadomie i zostawiam, bo:

- powód jest **funkcjonalny** — ograniczenie łuny w słońcu, nie moda,
- nie ma tu ani szeryfowego kroju wystawowego, ani terakotowego akcentu, czyli reszty tamtego
  szablonu,
- bursztyn i czerwień **nie zostały dobrane do tła** — są przeniesione z motywu nocnego, gdzie
  wzięły się z konwencji motoryzacyjnej.

To jest ta sama tożsamość przy innym świetle, nie druga skórka.

### 3.5 Kolor koduje coś prawdziwego

Bursztyn i czerwień nie są ozdobą. Odwzorowują **jeden do jednego istniejący system wag**:

| Waga wniosku | Kolor | Skąd konwencja |
|---|---|---|
| `informacja` | `przygasłe` | brak wyróżnienia |
| `uwaga` | `uwaga` (bursztyn) | motoryzacyjna konwencja ostrzeżenia |
| `usterka` | `usterka` (czerwień) | motoryzacyjna konwencja zatrzymania |

**Czerwień jest zarezerwowana.** Poza wagą `usterka` używa jej **wyłącznie alarm powtarzany**
(płyn powyżej progu). Nic innego w całej aplikacji nie ma prawa być czerwone — inaczej czerwony
przestaje znaczyć „zjedź".

**`model` ma w całej aplikacji dokładnie jedno znaczenie.** Stalowy błękit pojawia się wyłącznie
przy wartościach z tyldą — czyli praktycznie tylko przy temperaturze oleju. Zobaczenie go
gdziekolwiek indziej jest błędem.

### 3.6 Przełączanie motywu

```
Automatycznie   sygnał dzień/noc z radia (podświetlenie), gdy dostępny
Ręcznie         przełącznik w ustawieniach: NOC / DZIEŃ / AUTOMATYCZNIE
Domyślnie       NOC
```

**Gdy sygnał z radia jest niedostępny, automat wybiera NOC** — nie zgaduje z zegara. Zegar
w tym radiu bywa nieustawiony, a fałszywe przełączenie na jasny motyw w tunelu jest gorsze
niż ciemny motyw w dzień.

**Przełączenie motywu jest natychmiastowe i nie przerywa nagrywania.** Test tego pilnuje —
motyw to warstwa rysowania, a nagrywanie żyje w usłudze.

### 3.7 Kontrast — sprawdzany, nie deklarowany

Obie palety przechodzą **próg 4,5 : 1 dla tekstu** (WCAG 2.1, kryterium 1.4.3) na swoim polu.
Jest na to test liczący kontrast z wartości tokenów, nie oceniający na oko.

Kontrast **nie zastępuje znaczników.** `▲ ▼ ~ ⌀ ○` obowiązują bez zmian — w pełnym słońcu kolory
bledną, a strzałka zostaje.

---

## 4. Typografia

### 4.1 Trzy role, dwa kroje

| Rola | Krój | Dlaczego akurat ten |
|---|---|---|
| **Dane** | JetBrains Mono | Cyfry tabelaryczne o stałej szerokości |
| **Tekst** | Inter | Wnioski, słownik, opisy |
| **Etykiety** | Inter, wersaliki, rozstrzelone | Nazwy paneli, nagłówki kolumn, jednostki |

**Cyfry tabelaryczne są wymaganiem, nie preferencją.** Przy 4 Hz wartość zmienia się cztery razy
na sekundę. Krój o zmiennej szerokości cyfr sprawia, że liczba **drga w poziomie** przy każdej
zmianie — to najbardziej męcząca rzecz, jaka może się przydarzyć odczytowi na żywo, i widać ją
z fotela natychmiast.

**Świadomie nie Roboto**, mimo że jest w Androidzie i nic nie waży. Jest krojem domyślnym, czyli
niewidocznym — aplikacja w nim wygląda jak każda inna androidowa. Inter i JetBrains Mono są na
licencji **SIL OFL 1.1**, więc wolno je wbudować w APK. Koszt to jakieś pół megabajta przy
128 GB pamięci.

### 4.2 Skala

Wyprowadzona z odległości: ekran w desce, czytany **z fotela kierowcy, w ruchu**.

```
kafel-wartość    44sp    cztery kafle górnego paska
ślad-wartość     28sp    liczba przy prawej krawędzi wykresu
stan-zdanie      34sp    jedno zdanie panelu Stan
tekst            17sp    wnioski, słownik, wiersze odczytów
etykieta         13sp    wersaliki, rozstrzelenie +0,08em
oś               12sp    liczby na osi wykresu
```

**17sp to podłoga, nie propozycja.** Androidowe domyślne 14–15sp jest projektowane na
telefon trzymany w dłoni. Na wyciągnięcie ręki, przy drganiach, to za mało — i tego nie da się
nadrobić kontrastem.

**Skala nie skaluje się z ustawieniami systemowymi ponad 1,3×.** Radio bywa dostarczane
z podkręconą czcionką systemową; powyżej tego mnożnika układy paneli przestają się mieścić.
Ograniczenie jest jawne i udokumentowane, nie ciche.

---

## 5. Siatka, odstępy i pola dotykowe

```
jednostka        8dp     wszystkie odstępy są jej wielokrotnością
margines-ekranu  16dp
odstęp-kafli     8dp
odstęp-sekcji    24dp
promień-rogu     4dp     ledwie zaokrąglone — to przyrząd, nie widżet
```

**Pola dotykowe:**

| Element | Minimum | Powód |
|---|---|---|
| Cokolwiek używane **w ruchu** | **56 × 56dp** | Palec w drgającym aucie, nie mysz |
| Reszta (postój) | 48 × 48dp | Wytyczna Material, podłoga Androida |
| Odstęp między celami | 8dp | Żeby sąsiad nie łapał dotknięcia |

Elementów używanych w ruchu jest dokładnie trzy: **przełączanie paneli, zatrzymanie nagrywania,
zamknięcie alarmu**. Wszystko inne blokada prędkościowa i tak wyłącza.

---

## 6. Rysowanie wykresów

### 6.1 Warstwy pola kalibrowanego

Kolejność rysowania jest obowiązkowa — od spodu:

```
1. pole            tło wykresu
2. cieniowanie     ▓ przedmuchiwanie, ░ pętla otwarta   (rozszerzenie kontekstowe §8.3)
3. siatka          linie granic pasm + podpisy wartości
4. ślad            krzywa
5. wartość         liczba przy prawej krawędzi
```

**Cieniowanie idzie pod siatkę, nie nad ślad.** Nad śladem zasłaniałoby dane, których dotyczy.

### 6.2 Ślad

```
grubość            2dp
łączenia           ostre, bez wygładzania rogów
kolor              odczyt
antyaliasing       włączony na krzywej, WYŁĄCZONY na siatce
```

**Ostre łączenia są celowe.** Decymacja minimum-maksimum istnieje po to, żeby zachować skoki;
zaokrąglanie rogów krzywej zjadałoby je z powrotem na poziomie rysowania.

**Siatka bez antyaliasingu** — linie poziome na całych pikselach są ostre, a rozmyta linia
granicy pasma wygląda jak niepewność, którą nie jest.

### 6.3 Czego nie rysujemy

| Zakazane | Dlaczego |
|---|---|
| **Wypełnienie pod krzywą** | Sugeruje całkę, czyli sumę. Żadna z tych wielkości nie sumuje się sensownie |
| **Gradienty** | Nic nie znaczą, a maskują odczyt wartości z pozycji |
| **Cienie i wypukłości** | To przyrząd, nie pulpit |
| **Wygładzanie krzywej** (spline) | Rysuje punkty, których nie zmierzono. To ta sama nieprawda co zero zamiast kreski |
| **Automatyczne skalowanie osi** | Osie są sztywne. Ruchoma oś kłamie o skali zmiany |
| **Igły, tarcze, obrotomierze** | Sekcja 2.1 |

**Zakaz wygładzania krzywej jest tym samym niezmiennikiem, co kreska zamiast zera** — tylko
przeniesionym na piksele. Spline między dwiema próbkami rysuje wartość, której czujnik nigdy
nie podał.

### 6.4 Linia skanująca

Jedyna animacja w całej aplikacji.

```
gdzie          puste pole kalibrowane (parametr jeszcze niezmierzony)
wygląd         pionowa linia 1dp w kolorze siatka, przesuw w prawo
okres          2,4 s na przebieg pola
znika          po pierwszym odczycie tego parametru
```

**Niesie informację, której nic innego nie niesie:** *aplikacja żyje, po prostu jeszcze nie
dostała danych* — w odróżnieniu od *zawiesiła się*. Bez niej puste pole i zamrożona aplikacja
wyglądają identycznie.

**Przy włączonym ograniczeniu ruchu w systemie** linia zamienia się w statyczną kreskowaną
krawędź. Informacja zostaje, ruch znika.

**Nic innego się nie animuje.** Wartości zmieniają się skokowo, przejścia paneli są natychmiastowe,
wykresy nie wjeżdżają. Ekran, który się rusza podczas jazdy, odciąga wzrok od drogi — a to jest
jedyny koszt, którego ta aplikacja nie ma prawa generować.

---

## 7. Gęstość ekranu — niewiadoma do zmierzenia

**Rozdzielczość jest pewna: 1280 × 720.** Przekątna nie — karta produktu mówi 8", instrukcja
z pudełka wymienia 9" i 10".

Dla układu **przekątna nie ma znaczenia**: 1280 × 720 to ta sama liczba pikseli, a większy ekran
znaczy tylko, że wszystko jest fizycznie większe i czytelniejsze.

**Znaczenie ma gęstość, którą radio raportuje Androidowi**, bo od niej zależy, ile `dp` mieści
się na ekranie:

| Raportowana gęstość | Szerokość w `dp` | Skutek dla układu |
|---|---|---|
| 160 dpi (`mdpi`) | 1280 dp | Bardzo dużo miejsca, układy z zapasem |
| 213 dpi (`tvdpi`) | ~961 dp | Wygodnie |
| 240 dpi (`hdpi`) | 853 dp | Ciasno — panele trzeba sprawdzić |

**Etap W1 zaczyna się od odczytania `smallestScreenWidthDp` na prawdziwym radiu i zapisania
wyniku w tym dokumencie.** Nie zgadujemy — jeden odczyt rozstrzyga.

Do czasu odczytu obowiązuje założenie **`sw600dp`** (najciaśniejszy realny wariant) i żaden
układ nie ma prawa zakładać więcej.

---

## 8. Kontrakt

| Zasada | Sprawdzana przez |
|---|---|
| **Żaden kolor spoza tabel z §3.2 i §3.3** | Test przechodzi źródła `ui/` i szuka literałów koloru poza plikiem motywu |
| **Każda wartość liczbowa cyframi tabelarycznymi** | Test przechodzi komponenty wartości |
| **Oba motywy przechodzą 4,5 : 1 dla tekstu** | Test liczy kontrast z tokenów |
| **Czerwień tylko dla `usterka` i alarmu powtarzanego** | Test przechodzi użycia tokenu `usterka` |
| **`model` tylko przy wartościach z tyldą** | Test przechodzi użycia tokenu `model` |
| **Zero animacji poza linią skanującą** | Test szuka `animate*` poza jednym komponentem |
| **Znaczniki `▲ ▼ ~ ⌀ ○` niezależne od koloru** | Test istniejący; `○` dochodzi z poprawką P1 i podlega mu tak samo |
| **Cele dotykowe w ruchu ≥ 56dp** | Test przechodzi trzy elementy z §5 |

---

## 9. Nie-cele

**Motyw jasny jako domyślny.** NOC zostaje domyślny — auto jeździ też po ciemku, a wtedy jasny
ekran w desce oślepia.

**Personalizacja kolorów.** Paleta koduje wagi wniosków. Przestawialny kolor rozbija to znaczenie.

**Material You / kolory dynamiczne z tapety.** Kolor niesie tu znaczenie diagnostyczne; wyprowadzanie
go z tapety systemowej byłoby dokładnie odwrotnością tej zasady.

**Ikony parametrów.** Trzydzieści trzy parametry to trzydzieści trzy ikony do wymyślenia
i nauczenia. Nazwa po polsku jest jednoznaczna, ikona nie.

**Ekran powitalny i animacja uruchomienia.** Aplikacja ma zacząć nagrywać, nie się przedstawiać.

**Tryb pełnoekranowy bez paska radia.** Górny pasek radia jest systemowy i przez niego wraca się
do innych aplikacji. Ukrywanie go w aucie to pułapka.

---

## 10. Decyzje projektowe

| Decyzja | Dlaczego |
|---|---|
| **Rejestrator, nie zegar** | Aplikacja zapisuje przebieg z zachowaniem skoków — tarcza opisuje coś, czym ona nie jest |
| **Siatka = granice pasm** | Norma wydrukowana pod krzywą; nie trzeba jej nigdzie sprawdzać |
| **Brak pasma = brak siatki** | Brak normy staje się widoczny, tak jak kreska zamiast zera |
| **Puste pole zamiast ukrycia wiersza** | Nieobecność danych jest informacją i ma być narysowana |
| **Podłoże nie czarne, tło dzienne nie białe** | Czerń smuży i zbiera odciski; biel w słońcu oślepia |
| **Czerwień zarezerwowana** | Jeśli czerwony znaczy wiele rzeczy, przestaje znaczyć „zjedź" |
| **`model` jeden kolor, jedno znaczenie** | Zobaczenie go gdziekolwiek indziej ma być błędem |
| **Nie Roboto** | Krój domyślny jest niewidoczny — aplikacja wygląda wtedy jak każda inna |
| **Cyfry tabelaryczne** | Przy 4 Hz zmienna szerokość cyfr powoduje drganie liczby |
| **17sp jako podłoga tekstu** | Androidowe 14–15sp projektowano na telefon w dłoni |
| **56dp w ruchu** | Palec w drgającym aucie |
| **Bez wygładzania krzywej** | Spline rysuje wartości, których nie zmierzono |
| **Bez wypełnienia pod krzywą** | Sugeruje sumę; żadna z tych wielkości się nie sumuje |
| **Jedna animacja** | Linia skanująca odróżnia „czekam" od „zawiesiłem się". Reszta odciąga wzrok od drogi |
| **Automat bez sygnału wybiera NOC** | Zegar w radiu bywa nieustawiony; jasny motyw w tunelu jest gorszy niż ciemny w dzień |
| **Ograniczenie skali czcionki do 1,3×** | Powyżej tego panele przestają się mieścić — lepiej jawnie niż po cichu |

---

## 11. Weryfikacja w aucie

| # | Czynność | Czego szukamy |
|---|---|---|
| 1 | Odczytaj `smallestScreenWidthDp` i gęstość, wpisz do §7 | Rozstrzygnięcie niewiadomej z §7 |
| 2 | Zaparkuj w pełnym słońcu, przełącz na DZIEŃ | **Wykres czytelny** — jeśli nie, jasne tło jest za ciemne albo ślad za cienki |
| 3 | To samo miejsce, motyw NOC | Porównaj — potwierdza, po co są dwa motywy |
| 4 | Jazda nocą | Ekran nie oślepia w lusterku ani kątem oka |
| 5 | Przełącz motyw w trakcie nagrywania | **Nagrywanie nie przerywa się** |
| 6 | Zerknij na wartość zmieniającą się przy 4 Hz | **Liczba nie drga w poziomie** — cyfry tabelaryczne działają |
| 7 | Przełącz panel w ruchu, palcem, na nierównej drodze | Trafiasz za pierwszym razem |
| 8 | Odpal zimny silnik, patrz na puste pola | Linia skanująca widoczna, znika po pierwszym odczycie |
| 9 | Podkręć czcionkę systemową do maksimum | Układ trzyma się do 1,3×, wyżej degraduje się jawnie |

**Punkt 2 jest najważniejszy** — jest jedynym powodem, dla którego motyw DZIEŃ w ogóle istnieje.
Jeśli nie wygrywa z motywem NOC w słońcu, trzeba go poprawić albo usunąć, a nie zostawić „bo jest".

---

## 12. Zmiany w kontraktach

| Miejsce | Zmiana |
|---|---|
| **§12.2 bazowego — motyw** | *„Motyw wyłącznie ciemny"* → **dwa motywy: NOC (domyślny) i DZIEŃ**. Uzasadnienie ciemnego motywu dotyczyło jazdy po ciemku i obowiązuje bez zmian; DZIEŃ dochodzi na słońce |
| **§12.2 bazowego — czcionka** | Doprecyzowane: **JetBrains Mono** dla danych, **Inter** dla tekstu; wymaganie cyfr tabelarycznych bez zmian |
| **§15.1 B bazowego — STYK 1** | Trzy nowe pozycje: **odczyt gęstości ekranu**, **zachowanie przy przycisku „wstecz"**, **czy usługa przeżywa uśpienie radia** — sekcja 13 |
| **Kolor nigdy jedynym sygnałem** | **Bez zmian.** Ta warstwa dodaje kolor, nie zabiera znaczników |
| **Układy sześciu paneli, przeglądu, historii** | **Bez zmian.** Ta warstwa nadaje im język, nie przestawia |
| **Pętla, PID-y, progi, reguły, alarmy, baza** | **Bez żadnych zmian** |

**Czego nie zmieniamy:** pętli gorącej, jej częstotliwości, składu obowiązkowego `0D`, `05`
i `04`, żadnego progu, żadnej reguły, żadnej osi, składu żadnego poziomu odpytywania, schematu
bazy, liczby alarmów.

---

## 13. Trzy niewiadome dopisane do STYK 1

Odsłoniła je instrukcja obsługi radia. Wszystkie trzy sprawdza się **bez Bluetootha**, przy
pierwszej instalacji APK z transportem `Atrapa`.

| # | Pytanie | Dlaczego to ważne |
|---|---|---|
| 1 | **Jaką gęstość radio raportuje Androidowi?** | Rozstrzyga §7 i to, ile mieści się na ekranie |
| 2 | **Co robi przycisk „wstecz"?** | Instrukcja mówi, że **zamyka aplikację**, nie cofa nawigacji. Jeśli tak jest, ubicie aplikacji to **normalna ścieżka, nie awaria** — odzyskiwanie sesji będzie się uruchamiać codziennie, a nie raz na miesiąc |
| 3 | **Czy usługa przeżywa uśpienie radia?** | Radio ma przycisk uśpienia. Jeśli usługa ginie, przejazd urywa się przy każdym postoju z uśpionym ekranem |

**Pytanie 2 jest najważniejsze.** Zmienia status mechanizmu odzyskiwania z zabezpieczenia
awaryjnego na codzienne działanie — a to znaczy, że jego jakość decyduje o tym, czy zbiór
nagrań jest kompletny.

---

## 14. Kolejność realizacji

| Etap | Zakres | Ukończony, gdy |
|---|---|---|
| **W1** | Odczyt gęstości na radiu; `ui/Theme.kt` — oba motywy, tokeny, kroje | Oba motywy przechodzą test kontrastu; gęstość wpisana do §7 |
| **W2** | Skala typograficzna, siatka, pola dotykowe; przełącznik motywu | Przełączenie nie przerywa nagrywania; ograniczenie 1,3× działa |
| **W3** | Pole kalibrowane: warstwy, siatka z granic pasm, brak pasma = brak siatki | Parametr bez pasma nie ma linii siatki |
| **W4** | Ślad, zakazy rysowania, linia skanująca | Test „zero animacji poza jedną"; brak wygładzania krzywej |
| **W5** | Weryfikacja w aucie | Lista z sekcji 11 przejdzie w całości |

Wchodzą **po etapie S4**. `W1` przed resztą jest wiążące — bez tokenów nie ma czym rysować.

**W3 i W4 dotykają tego samego kodu wykresów co etap 8 bazowego.** Jeśli etap 8 jest już
zrobiony, to jest refaktoryzacja rysowania, nie pisanie od zera.

---

## 15. Źródła

Rozszerzenie **nie wprowadza ani jednej stałej diagnostycznej**. Wszystkie progi, pasma i wagi
pochodzą z warstw wcześniejszych; ta warstwa nadaje im wygląd.

| Zasada | Skąd |
|---|---|
| **Konwencja bursztyn / czerwień** | Motoryzacyjna konwencja kontrolek: bursztyn ostrzega, czerwień zatrzymuje |
| **Kontrast ≥ 4,5 : 1 dla tekstu** | WCAG 2.1, kryterium 1.4.3 |
| **Kolor nie jedynym nośnikiem** | WCAG 2.1, kryterium 1.4.1 — zasada już obowiązuje od warstwy diagnostycznej |
| **Ograniczenie ruchu** | `prefers-reduced-motion` / ustawienie systemowe Androida |
| **Pole dotykowe ≥ 48dp** | Material Design — podłoga; **56dp w ruchu to decyzja tego projektu**, nie wytyczna |
| **Licencje krojów** | Inter i JetBrains Mono — SIL Open Font License 1.1, wolno wbudować |
| **Ciemny kokpit** | Filozofia kokpitu Airbusa — przeniesiona w warstwie objaśnień, §2 tamtego dokumentu |

**Jedyne liczby wprowadzone przez tę warstwę są wymiarami, nie progami**: rozmiary czcionek,
odstępy, pola dotykowe, grubość śladu i okres linii skanującej. Żadna nie wchodzi do reguły,
nie pojawia się w werdykcie i niczego nie twierdzi o samochodzie — kategoria „kryterium wyboru"
z `.cursor/rules/30-zrodla.mdc`.
