# i40-android — słownik

**Dokument źródłowy.** Treść haseł przenosi się do aplikacji **co do zdania**. Nie jest to
materiał do przepisania własnymi słowami ani do uzupełnienia z pamięci — obowiązuje
`.cursor/rules/30-zrodla.mdc` w pełnym brzmieniu.

Projekt: `docs/spec/2026-08-15-i40-android-objasnienia-design.md`, sekcja 8.

---

## Jak pisane są hasła

**Cztery rubryki, zawsze te same, zawsze w tej kolejności:**

| Rubryka | Co zawiera |
|---|---|
| **CO TO JEST** | Czym jest ta wielkość. Bez żargonu, bez odsyłania do innych haseł w pierwszym zdaniu |
| **PO CO NA TO PATRZEĆ** | Dlaczego akurat ta liczba coś wnosi |
| **GDY WYJDZIE POZA PASMO** | Co znaczy odchylenie. `—` z powodem, gdy parametr nie ma pasma |
| **CZEGO TO NIE MÓWI** | Granica wnioskowania. **Rubryka obowiązkowa** |

Poziom języka: **dwunastolatek zainteresowany samochodami**. Krótkie zdania. Porównania do
rzeczy znanych. Zero słów, których nie da się wyjaśnić w tym samym akapicie.

Zapis w podwójnych nawiasach kwadratowych oznacza **odsyłacz** do innego hasła.

## Liczby dozwolone w treści

Zasada z §7.3 specu: **żaden tekst hasła nie zawiera liczby, która jest progiem**. Progi czyta
się z `PasmaOdniesienia` i pokazuje w bloku górnym.

Wyjątek dotyczy liczb, które są **częścią wyjaśnienia fizyki** albo **nazwą identyfikatora**,
a nie progiem. Lista zamknięta:

```
2,3 bara      typowe ciśnienie w oponie — punkt odniesienia dla ciśnienia szyny
1 013 hPa     ciśnienie atmosferyczne na poziomie morza
0,25 s        okres pętli gorącej przy nastawie Zrównoważona
2 s           okres pętli gorącej ×8 — użyte w wyjaśnieniu poziomów odpytywania
14,7 : 1      stosunek powietrza do benzyny przy lambda = 1
1 : 4         liczba cylindrów w tym silniku
90 °C         część nazwy pola `czasDo90CSekundy` i hasła „Czas do 90 °C" — nazwa
              identyfikatora, nie twierdzenie o progu
```

Liczba spoza tej listy w treści hasła to błąd, który wyłapuje test z §12 specu.

**Test musi odsiać trzy rodzaje fałszywych trafień** — sprawdzono to na gotowej treści
i wszystkie trzy wystąpiły:

| Wygląda jak próg | Czym jest naprawdę |
|---|---|
| `0105`, `0123`, `0142` | **numery PID-ów** — `105` w środku to nie sto pięć stopni |
| `ELM327` | **nazwa adaptera** — `27` to nie próg |
| `co 0,25 s`, `co 1 s`, `co 2,5 s`, `co 5 s` | **stopka techniczna**, częstotliwość odpytywania |

Test porównuje liczby **w prozie rubryk**, po odrzuceniu stopek, tekstu w apostrofach
odwrotnych i nazw własnych.

---

# CZĘŚĆ A — PARAMETRY MIERZONE

Dwadzieścia osiem haseł na trzydzieści trzy PID-y.

---

## Obroty silnika

**PID `010C` · poziom gorący · co 0,25 s**

**CO TO JEST**
Jak szybko kręci się wał silnika, w obrotach na minutę. Przy pracy na luzie około siedmiuset,
w spokojnej jeździe dwa tysiące, na czerwonym polu obrotomierza sześć i pół tysiąca.

**PO CO NA TO PATRZEĆ**
To najbardziej podstawowa informacja o tym, co silnik robi. Prawie każdy inny parametr trzeba
czytać razem z obrotami — ta sama korekta paliwa przy siedmiuset obrotach i przy czterech
tysiącach znaczy coś zupełnie innego.

**GDY WYJDZIE POZA PASMO**
— Obroty nie mają pasma, bo nie ma „prawidłowej" wartości. Zależy wyłącznie od tego, czego
w danej chwili od silnika chcesz.

**CZEGO TO NIE MÓWI**
Wysokie obroty same w sobie nie są niczym złym — silnik jest na nie zbudowany. Szkodzą dopiero
w połączeniu z zimnym olejem, i dokładnie na to patrzy jeden z alarmów. Zobacz
[[temperatura oleju (model)]].

---

## Prędkość pojazdu

**PID `010D` · poziom gorący · co 0,25 s**

**CO TO JEST**
Prędkość, jaką widzi sterownik — liczona z obrotów kół, nie z GPS-u.

**PO CO NA TO PATRZEĆ**
Z niej aplikacja liczy przejechany [[dystans]]. Steruje też
[[blokada prędkościowa|blokada prędkościowa]]: gdy pokaże cokolwiek powyżej zera, część ekranów
się blokuje, żeby nie kusiły w trakcie jazdy.

**GDY WYJDZIE POZA PASMO**
— Prędkość nie ma pasma z tego samego powodu co obroty.

**CZEGO TO NIE MÓWI**
Może się nieznacznie różnić od prędkościomierza na desce. Fabryczny wskaźnik jest z założenia
lekko zawyżony, a ten odczyt jest surowy. Nie znaczy to, że któryś jest zepsuty.

---

## Obliczone obciążenie silnika

**PID `0104` · poziom gorący · co 0,25 s**

**CO TO JEST**
Jak mocno silnik się stara — w procentach tego, co mógłby dać przy tych obrotach. Zero znaczy,
że tylko podtrzymuje sam siebie. Sto — że daje wszystko, co ma.

**PO CO NA TO PATRZEĆ**
To ciekawsze niż obroty. Dwa tysiące obrotów zjeżdżając z górki to obciążenie kilku procent.
Te same dwa tysiące pod górę z obciążoną przyczepą to dziewięćdziesiąt. Silnik pracuje wtedy
zupełnie inaczej, a obrotomierz pokazuje to samo.

Ta wartość jest też **wejściem modelu temperatury oleju** — bez niej model przestaje działać,
dlatego nie wolno jej usunąć z pętli gorącej.

**GDY WYJDZIE POZA PASMO**
— Obciążenie nie ma pasma. Wysokie jest normalne, gdy właśnie tego chcesz.

**CZEGO TO NIE MÓWI**
To nie jest moc w koniach mechanicznych. To udział w tym, co silnik mógłby dać **przy tych
konkretnych obrotach** — sto procent przy tysiącu obrotów to znacznie mniej mocy niż sto
procent przy pięciu tysiącach.

---

## Obciążenie absolutne

**PID `0143` · poziom szybki A · co 1 s**

**CO TO JEST**
Druga miara obciążenia, liczona inaczej: ile powietrza wpadło do cylindra w porównaniu z tym,
ile zmieściłoby się przy pełnym wypełnieniu.

**PO CO NA TO PATRZEĆ**
Trzyma się bliżej fizyki niż [[obliczone obciążenie silnika]] i dlatego lepiej nadaje się do
porównywania różnych sytuacji między sobą. Aplikacja zapisuje, **przy jakim obciążeniu
absolutnym padło najwyższe [[ciśnienie w szynie wysokiego ciśnienia|ciśnienie na szynie]]** —
bo pompa oceniana jest właśnie pod obciążeniem.

**GDY WYJDZIE POZA PASMO**
— Bez pasma, jak każde obciążenie.

**CZEGO TO NIE MÓWI**
Może przekroczyć sto procent w silnikach z doładowaniem. Ten silnik jest wolnossący, więc tu
się to nie zdarzy — ale jeśli kiedyś zobaczysz taką wartość, nie jest to błąd odczytu.

---

## Temperatura płynu chłodzącego

**PID `0105` · poziom gorący · co 0,25 s**

**CO TO JEST**
Temperatura cieczy krążącej w silniku i chłodnicy. To ona stoi za wskaźnikiem temperatury
na desce rozdzielczej — tyle że deska pokazuje ją bardzo zgrubnie, a tu masz konkret.

**PO CO NA TO PATRZEĆ**
Od niej zależy prawie wszystko inne. Zimny silnik pracuje na [[pętla zamknięta i otwarta|pętli
otwartej]], leje więcej paliwa i szybciej się zużywa. Ten odczyt jest też **wejściem
[[temperatura oleju (model)|modelu temperatury oleju]]** i podstawą jednego z pięciu
[[alarm i karencja|alarmów]].

**GDY WYJDZIE POZA PASMO**
Powyżej górnej granicy silnik się przegrzewa i **trzeba zjechać**. Nie „dojechać jeszcze
kawałek" — przegrzany silnik potrafi się zniszczyć w kilka minut: wygina się głowica, puszcza
uszczelka.
Utrzymujący się odczyt poniżej dolnej granicy przy rozgrzanym silniku zwykle znaczy termostat,
który został otwarty na stałe.

**CZEGO TO NIE MÓWI**
Ciepły płyn **nie znaczy ciepły olej**. Płyn nagrzewa się mniej więcej dwa razy szybciej. To
jest powód, dla którego wskaźnik na desce pokazuje „gotowe" długo, zanim silnik naprawdę jest
gotowy.

---

## Temperatura powietrza dolotowego

**PID `010F` · poziom wolny C · co 5 s**

**CO TO JEST**
Temperatura powietrza wchodzącego do silnika, mierzona już za filtrem.

**PO CO NA TO PATRZEĆ**
Chłodne powietrze jest gęstsze, więc niesie więcej tlenu — sterownik dolewa wtedy więcej
paliwa. Ta liczba tłumaczy część drobnych zmian w [[korekta krótkoterminowa|korektach paliwa]],
które inaczej wyglądałyby na przypadkowe.

**GDY WYJDZIE POZA PASMO**
— Bez pasma. Zależy od pogody i od tego, jak długo auto stało.

**CZEGO TO NIE MÓWI**
Po dłuższym postoju z gorącym silnikiem pokaże znacznie więcej niż jest na dworze, bo czujnik
nagrzał się od komory. To normalne i mija po kilku minutach jazdy.

---

## Temperatura otoczenia

**PID `0146` · poziom wolny C · co 5 s**

**CO TO JEST**
Temperatura powietrza na zewnątrz auta, z czujnika umieszczonego z dala od gorących części.

**PO CO NA TO PATRZEĆ**
Jest **wejściem [[temperatura oleju (model)|modelu temperatury oleju]]** — w mróz olej
nagrzewa się wyraźnie dłużej i model musi o tym wiedzieć. Tłumaczy też, dlaczego ten sam
przejazd w styczniu i w lipcu wygląda inaczej.

**GDY WYJDZIE POZA PASMO**
— Bez pasma. To pogoda, nie stan auta.

**CZEGO TO NIE MÓWI**
Nie musi zgadzać się co do stopnia z tym, co pokazuje deska rozdzielcza — fabryczny wskaźnik
zwykle wygładza odczyt i wolniej reaguje.

---

## Temperatura katalizatora

**PID `013C` · poziom średni B · co 2,5 s**

**CO TO JEST**
Temperatura wewnątrz [[katalizator|katalizatora]] — dopalacza spalin w rurze wydechowej.

**PO CO NA TO PATRZEĆ**
Katalizator ma wąskie okno, w którym w ogóle pracuje. Za zimny nic nie robi, za gorący zaczyna
się rozpadać w środku. To jedyny sposób, żeby zobaczyć, w którym miejscu tego okna właśnie
jesteś.

**GDY WYJDZIE POZA PASMO**
Poniżej dolnej granicy przy rozgrzanym silniku: katalizator nie osiąga temperatury pracy.
Zwykle po samych krótkich trasach — wtedy jest to normalne. Utrzymujące się przy długiej
jeździe wskazuje na zużycie.
Powyżej górnej granicy zwykle znaczy, że do katalizatora trafia **niespalona benzyna** i dopala
się dopiero tam. Ten warunek ma własny [[alarm i karencja|alarm]].

**CZEGO TO NIE MÓWI**
To odczyt sprzed katalizatora, nie z jego środka. Nie mówi też, **jak skutecznie** katalizator
oczyszcza spaliny — to potrafiłby powiedzieć dopiero tryb 06, którego ta aplikacja nie używa.

---

## Wyprzedzenie zapłonu

**PID `010E` · poziom gorący · co 0,25 s**

**CO TO JEST**
O ile wcześniej — mierzone w stopniach obrotu wału — strzela iskra, zanim tłok dojdzie na samą
górę. Zobacz [[GMP i wyprzedzenie zapłonu]].

**PO CO NA TO PATRZEĆ**
Sterownik przesuwa iskrę bez przerwy: wcześniej dla mocy, później dla bezpieczeństwa. **Gdy
wyczuje spalanie stukowe, natychmiast ją cofa** — i wtedy ta liczba spada. Nagły spadek przy
mocnym gazie jest sygnałem, którego nie widać w żaden inny sposób.

**GDY WYJDZIE POZA PASMO**
— Bez pasma. Prawidłowa wartość zależy od obrotów, obciążenia, temperatury i paliwa naraz.

**CZEGO TO NIE MÓWI**
Pojedynczy spadek nie znaczy usterki — sterownik cofa iskrę także profilaktycznie, na przykład
w upał albo po zatankowaniu gorszej benzyny.

---

## Korekta krótkoterminowa

**PID `0106` · poziom gorący · co 0,25 s**

**CO TO JEST**
Poprawka dawki paliwa **w tej chwili**. Sterownik wącha spaliny [[sonda lambda|sondą lambda]]
i na bieżąco dolewa albo ujmuje. Plus znaczy „dolewam więcej, niż mówi przepis".

**PO CO NA TO PATRZEĆ**
Skacze w górę i w dół cały czas i **tak ma być** — to znak, że regulacja żyje. Krzywa, która
zamarła w jednym miejscu, jest bardziej podejrzana niż taka, która skacze.

**GDY WYJDZIE POZA PASMO**
— Sama krótka korekta nie ma pasma, bo jej wahania są normalne. Pasmo ma dopiero
[[suma korekt]].

**CZEGO TO NIE MÓWI**
Poza [[pętla zamknięta i otwarta|pętlą zamkniętą]] ta liczba **nic nie znaczy** — jest wtedy
zamrożona z ostatniej chwili przed przejściem w tryb otwarty. Dlatego panel Mieszanka pisze
stan pętli nad wykresem, a nie pod nim.

---

## Korekta długoterminowa

**PID `0107` · poziom średni B · co 2,5 s · czwarty kafel górnego paska**

**CO TO JEST**
Poprawka dawki paliwa, której sterownik **nauczył się na stałe**. Zapamiętuje ją i stosuje od
razu przy następnym uruchomieniu, zanim [[sonda lambda]] zdąży się rozgrzać.

**PO CO NA TO PATRZEĆ**
To jest ta ważna. Zmienia się powoli, przez tygodnie, więc **pokazuje, że coś się psuje, zanim
to poczujesz** i zanim zapali się kontrolka. Aplikacja liczy jej medianę z każdego przejazdu
właśnie po to, żeby dało się oglądać trend z miesiąca na miesiąc.

Dlatego stoi też **na górnym pasku, widoczna z każdego panelu**. Trzy pozostałe kafle mówią,
czy coś dzieje się teraz — ten jeden mówi, czy coś dzieje się od tygodni. Zajął miejsce po
poziomie paliwa, który na tym aucie nic nie pokazuje: [[obsługiwany bez danych]].

**Na kaflu zobaczysz `— ○`, gdy silnik pracuje w [[pętla zamknięta i otwarta|pętli otwartej]]** —
przy rozgrzewaniu, przy pełnym gazie i przy hamowaniu silnikiem. Wtedy ta liczba jest zamrożona
i opisuje chwilę sprzed przejścia w tryb otwarty, więc pokazanie jej wprowadzałoby w błąd.
Zobacz [[znaczniki ▲ ▼ ~ ⌀ ○|znaczniki]].

**GDY WYJDZIE POZA PASMO**
Powyżej górnej granicy: silnik stale dolewa więcej benzyny, niż przewiduje przepis. Mieszanka
jest uboga — najczęściej dlatego, że skądś zasysa dodatkowe powietrze, którego nikt nie zmierzył.
Poniżej dolnej: stale ujmuje. Mieszanka jest bogata.

**CZEGO TO NIE MÓWI**
**Nie wskazuje, co konkretnie jest zepsute.** Pęknięty wężyk podciśnienia, zmęczona uszczelka
kolektora, brudne wtryskiwacze i zużyta sonda dają ten sam odczyt. To lista podejrzanych,
nie wyrok.

---

## Zadany współczynnik lambda

**PID `0144` · poziom średni B · co 2,5 s**

**CO TO JEST**
Jaką mieszankę sterownik **chce** mieć w tej chwili. Jeden oznacza dokładnie tyle powietrza,
ile potrzeba, żeby spalić całą benzynę — w praktyce około 14,7 : 1.

**PO CO NA TO PATRZEĆ**
Pokazuje zamiar, a nie skutek. Gdy sterownik zjeżdża poniżej jedynki, celowo wzbogaca mieszankę
— przy pełnym gazie robi to, żeby chłodzić komorę spalania. Korekty odczytane w takiej chwili
znaczą co innego.

**GDY WYJDZIE POZA PASMO**
— Bez pasma. Wartość inna niż jeden bywa zupełnie prawidłowa, zależnie od tego, co się dzieje.

**CZEGO TO NIE MÓWI**
To **żądanie, nie pomiar**. Nie mówi, czy silnik faktycznie taką mieszankę dostał — o tym
mówią dopiero [[korekta krótkoterminowa|korekty]].

---

## Status układu paliwowego

**PID `0103` · poziom średni B · co 2,5 s**

**CO TO JEST**
Informacja, w jakim trybie sterownik akurat dozuje paliwo. Zobacz
[[pętla zamknięta i otwarta]].

**PO CO NA TO PATRZEĆ**
To **najważniejsza informacja na panelu Mieszanka** i dlatego stoi nad wykresem, a nie pod nim.
Rozstrzyga, czy na korekty w ogóle warto patrzeć.

**GDY WYJDZIE POZA PASMO**
To nie liczba, tylko stan. Dwa z nich są niepokojące: **pętla otwarta z powodu awarii układu**
oraz **pętla zamknięta mimo awarii sondy**. Oba znaczą, że sterownik wie o problemie
z regulacją paliwa.

**CZEGO TO NIE MÓWI**
Pętla otwarta sama w sobie nie jest usterką. Na zimnym silniku i przy pełnym gazie jest
całkowicie normalna — tak ma być.

---

## Ciśnienie w szynie wysokiego ciśnienia

**PID `0123` · poziom szybki A · co 1 s**

**CO TO JEST**
Jak mocno ściśnięta jest benzyna tuż przed wtryskiem. W tym silniku paliwo wtryskiwane jest
**prosto do cylindra** — zobacz [[wtrysk bezpośredni GDI]] — więc musi przebić ciśnienie
sprężonego już powietrza. Dla porównania: w oponie masz 2,3 bara.

**PO CO NA TO PATRZEĆ**
To najlepszy pojedynczy wskaźnik kondycji pompy wysokiego ciśnienia. Sprawna pompa **nadąża za
żądaniem sterownika także pod obciążeniem**; zmęczona zaczyna zostawać w tyle właśnie wtedy,
gdy wciskasz gaz — a więc wtedy, kiedy najmniej to widać na co dzień.

**GDY WYJDZIE POZA PASMO**
Pasmo jest podwójne: inne na biegu jałowym, inne pod obciążeniem, i aplikacja pokazuje oba.
Odczyt poniżej dolnej granicy na rozgrzanym silniku stojącym na luzie to sygnał, że pompa nie
buduje ciśnienia tak, jak powinna.

**CZEGO TO NIE MÓWI**
Progi pochodzą z **materiałów branżowych o silnikach GDI, nie z dokumentacji Hyundaia**.
Dlatego aplikacja opisuje odchylenie jako `uwaga`, nigdy jako `usterka` — zobacz
[[waga wniosku]].

---

## Ciśnienie w kolektorze dolotowym

**PID `010B` · poziom szybki A · co 1 s**

**CO TO JEST**
Ciśnienie powietrza tuż przed wejściem do cylindrów. Im mocniej silnik zasysa przy przymkniętej
przepustnicy, tym niższe.

**PO CO NA TO PATRZEĆ**
Razem z [[ciśnienie atmosferyczne|ciśnieniem atmosferycznym]] daje
[[podciśnienie]] — a to jest najprostszy sposób, żeby zobaczyć, jak mocno silnik „ciągnie"
przez przymkniętą przepustnicę.

**GDY WYJDZIE POZA PASMO**
— Bez pasma. Prawidłowa wartość zależy wprost od tego, jak otwarta jest przepustnica.

**CZEGO TO NIE MÓWI**
Ten silnik nie ma przepływomierza — sterownik liczy masę powietrza właśnie z tego ciśnienia.
Nie jest to jednak pomiar ilości powietrza, tylko dana wejściowa do jej wyliczenia.

---

## Ciśnienie atmosferyczne

**PID `0133` · poziom wolny C · co 5 s**

**CO TO JEST**
Ciśnienie powietrza na zewnątrz. Na poziomie morza około 1 013 hPa; im wyżej w góry, tym mniej.

**PO CO NA TO PATRZEĆ**
Jest punktem odniesienia dla [[podciśnienie|podciśnienia]]. Bez niego samo
[[ciśnienie w kolektorze dolotowym|ciśnienie w kolektorze]] nie mówi, jak mocno silnik zasysa —
bo zależy też od tego, ile powietrza jest dookoła.

**GDY WYJDZIE POZA PASMO**
— Bez pasma. To pogoda i wysokość nad poziomem morza.

**CZEGO TO NIE MÓWI**
Nie jest to czuły barometr. Odczyt ma rozdzielczość jednego kilopaskala, więc nie nadaje się
do przewidywania pogody.

---

## Zadane przedmuchiwanie zbiornika

**PID `012E` · poziom średni B · co 2,5 s**

**CO TO JEST**
Jak mocno otwarty jest zawór, który przepuszcza opary benzyny z pojemnika z węglem aktywnym
do silnika. Zobacz [[przedmuchiwanie zbiornika]].

**PO CO NA TO PATRZEĆ**
To jedna z najczęstszych przyczyn nagłego skoku [[korekta krótkoterminowa|korekty paliwa]] bez
żadnej usterki. Aplikacja **cieniuje wykres korekt w miejscach, gdzie przedmuchiwanie było
aktywne** — dzięki temu widać górkę i od razu wiadomo, że ma wytłumaczenie.

**GDY WYJDZIE POZA PASMO**
— Bez pasma. Sterownik otwiera ten zawór wtedy, kiedy uzna za stosowne.

**CZEGO TO NIE MÓWI**
To wartość **zadana**, czyli ile sterownik kazał. Nie ma odczytu mówiącego, ile faktycznie
przeszło oparów.

---

## Napięcie sterownika

**PID `0142` · poziom średni B · co 2,5 s**

**CO TO JEST**
Napięcie, które sterownik widzi na swoim zasilaniu. Praktycznie to samo, co na akumulatorze.

**PO CO NA TO PATRZEĆ**
Gdy silnik pracuje, alternator powinien **ładować** akumulator — dlatego zdrowa wartość jest
wyraźnie wyższa niż napięcie samego akumulatora. To jeden z pięciu
[[alarm i karencja|alarmów]].

**GDY WYJDZIE POZA PASMO**
Poniżej dolnej granicy przy pracującym silniku: alternator nie ładuje i auto jedzie
z akumulatora. Zgaśnie wszystko naraz, tylko nie wiadomo kiedy.
Powyżej górnej: regulator ładuje za mocno, co skraca życie akumulatora i potrafi uszkodzić
elektronikę.

**CZEGO TO NIE MÓWI**
Na wyłączonym silniku niskie napięcie jest **normalne** i niczego nie oznacza — dlatego alarm
odzywa się dopiero powyżej pewnych obrotów. Nie mówi też nic o pojemności akumulatora; do tego
potrzebny jest test obciążeniowy.

---

## Pozycje przepustnicy

**PID-y `0111` `0145` `0147` `014C` · `0111` i `014C` poziom szybki A · co 1 s**

**CO TO JEST**
Jak bardzo otwarta jest klapa wpuszczająca powietrze do silnika. Cztery odczyty pokazują to
z różnych stron:

| Odczyt | Co znaczy |
|---|---|
| `0111` **pozycja przepustnicy** | Ile faktycznie jest otwarte |
| `014C` **pozycja zadana** | Ile sterownik kazał otworzyć |
| `0145` **pozycja względna** | To samo, ale liczone od pozycji zamkniętej |
| `0147` **pozycja bezwzględna B** | Odczyt z drugiego, zapasowego czujnika |

**PO CO NA TO PATRZEĆ**
W tym aucie przepustnica **nie jest połączona linką z pedałem** — otwiera ją silniczek na
polecenie sterownika. Dlatego ma sens porównywanie *ile kazał* z *ile jest*: zobacz
[[rozjazd przepustnicy]].

**GDY WYJDZIE POZA PASMO**
— Bez pasma. Każda wartość od zera do stu jest prawidłowa, zależnie od tego, jak jedziesz.

**CZEGO TO NIE MÓWI**
Otwarcie przepustnicy nie jest tym samym co wciśnięcie [[pozycja pedału|pedału]]. Sterownik
regularnie otwiera ją inaczej, niż prosisz — i zwykle ma ku temu powód.

---

## Pozycja pedału

**PID-y `0149` `014A` · `0149` poziom szybki A · co 1 s**

**CO TO JEST**
Jak mocno wciskasz pedał gazu. Dwa odczyty, bo pedał ma **dwa niezależne czujniki** — jeśli
zaczną mówić co innego, sterownik wie, że jeden z nich kłamie.

**PO CO NA TO PATRZEĆ**
To jedyna wielkość na całym ekranie, która pokazuje **Twoją decyzję**, a nie reakcję auta.
Wszystko inne jest odpowiedzią na to.

**GDY WYJDZIE POZA PASMO**
— Bez pasma.

**CZEGO TO NIE MÓWI**
Wciśnięcie pedału do połowy nie znaczy połowy mocy. Zależność jest celowo nieliniowa, żeby
auto dało się precyzyjnie prowadzić przy małych prędkościach.

---

## Czas pracy od uruchomienia

**PID `011F` · poziom wolny C · co 5 s**

**CO TO JEST**
Ile sekund silnik pracuje od ostatniego odpalenia. Zeruje się przy każdym uruchomieniu.

**PO CO NA TO PATRZEĆ**
Robi w aplikacji trzy rzeczy. Jest częścią warunku „silnik rozgrzany na dobre". Jest wejściem
[[temperatura oleju (model)|modelu oleju]]. I po jego **spadku** aplikacja rozpoznaje, że
silnik zgasł i odpalił od nowa — model resetuje się wtedy do temperatury płynu.

**GDY WYJDZIE POZA PASMO**
— Bez pasma. Rośnie i tyle.

**CZEGO TO NIE MÓWI**
Nie jest to czas trwania przejazdu ani czas jazdy — silnik pracujący na postoju też go
naliczy. W kolumnie „poprzednio" ma z tego powodu zawsze kreskę: porównywanie go nie ma sensu.

---

## Przebieg z zapaloną kontrolką

**PID `0121` · odczyt przy przeglądzie**

**CO TO JEST**
Ile kilometrów auto przejechało od chwili, gdy zapaliła się [[kontrolka MIL]].

**PO CO NA TO PATRZEĆ**
Odróżnia „zapaliła się przed chwilą" od „świeci od dwóch tysięcy kilometrów i ktoś się
przyzwyczaił". To dwie zupełnie różne sytuacje przy tym samym kodzie błędu.

**GDY WYJDZIE POZA PASMO**
— Bez pasma. Każda wartość powyżej zera znaczy tyle, że kontrolka świeci albo świeciła.

**CZEGO TO NIE MÓWI**
Zero nie znaczy, że nigdy się nie paliła — znaczy, że **teraz nie świeci**. Historia mogła
zostać skasowana, o czym powie [[przebieg od skasowania kodów]].

---

## Rozgrzania od skasowania kodów

**PID `0130` · odczyt przy przeglądzie**

**CO TO JEST**
Ile razy silnik przeszedł pełny cykl rozgrzania od ostatniego wyczyszczenia pamięci błędów.

**PO CO NA TO PATRZEĆ**
[[monitory gotowości|Monitory gotowości]] odbudowują się właśnie przez takie cykle. Ta liczba
mówi, jak daleko zaszedł ten proces — i pośrednio, ile jeszcze trzeba pojeździć przed badaniem
technicznym.

**GDY WYJDZIE POZA PASMO**
— Bez pasma.

**CZEGO TO NIE MÓWI**
Wysoka wartość nie gwarantuje, że monitory są gotowe. Część z nich wymaga **określonych
warunków jazdy**, a nie samego rozgrzania. Stan monitorów czytaj z ich własnego odczytu.

---

## Przebieg od skasowania kodów

**PID `0131` · odczyt przy przeglądzie**

**CO TO JEST**
Ile kilometrów auto przejechało od ostatniego wyczyszczenia pamięci błędów.

**PO CO NA TO PATRZEĆ**
Mała wartość przy zgaszonej kontrolce znaczy, że **ktoś niedawno skasował kody**. Przy zakupie
używanego auta to jedna z najważniejszych liczb, jakie da się odczytać — czysta pamięć błędów
po dwudziestu kilometrach nie znaczy, że problemu nie było.

To jedna z siedemnastu reguł przeglądu.

**GDY WYJDZIE POZA PASMO**
Poniżej progu przy zgaszonej kontrolce: pamięć skasowano zbyt niedawno, żeby brak błędów coś
znaczył.

**CZEGO TO NIE MÓWI**
Nie mówi, **kto i po co** kasował. Mechanik po naprawie kasuje kody rutynowo i jest to zupełnie
w porządku.

---

## Stan monitorów

**PID-y `0101` `0141` · odczyt przy przeglądzie**

**CO TO JEST**
Dwa odczyty mówiące, które [[monitory gotowości]] zdążyły się wykonać — każdy z innej strony:
`0101` liczy od ostatniego skasowania kodów, `0141` tylko w bieżącym cyklu jazdy.

**PO CO NA TO PATRZEĆ**
**Auto z niegotowymi monitorami nie przejdzie badania emisji**, nawet jeśli nic mu nie jest.
To najczęstszy powód oblania badania po niedawnej naprawie.

**GDY WYJDZIE POZA PASMO**
To nie liczba, tylko zestaw stanów. Niegotowe monitory to jedna z siedemnastu reguł przeglądu,
z wagą `uwaga`.

**CZEGO TO NIE MÓWI**
„Gotowy" znaczy **wykonany**, a nie **zdany**. Monitor może być gotowy i jednocześnie wykryć
usterkę — wtedy będzie też [[kod błędu DTC|kod błędu]].

---

## Zamontowane sondy tlenu

**PID `0113` · odczyt przy przeglądzie**

**CO TO JEST**
Informacja o tym, ile [[sonda lambda|sond lambda]] ma ten silnik i w których miejscach układu
wydechowego siedzą.

**PO CO NA TO PATRZEĆ**
Mówi, których odczytów sond w ogóle ma sens szukać. Bez tego aplikacja pytałaby o sondy, których
nie ma, i dostawała pustkę.

**GDY WYJDZIE POZA PASMO**
— To opis budowy silnika, nie wielkość mierzona.

**CZEGO TO NIE MÓWI**
Nie mówi, czy sondy działają. Tylko że są.

---

## Norma OBD

**PID `011C` · odczyt przy przeglądzie**

**CO TO JEST**
Kod mówiący, według której odmiany przepisów zbudowano sterownik tego auta — europejskiej,
amerykańskiej czy innej.

**PO CO NA TO PATRZEĆ**
Wyjaśnia, dlaczego niektóre odczyty zachowują się inaczej, niż opisuje ogólna dokumentacja
OBD-II. Różne odmiany przepisów wymagają różnych monitorów.

**GDY WYJDZIE POZA PASMO**
— To kod, nie liczba.

**CZEGO TO NIE MÓWI**
Nie mówi nic o stanie auta. To metryczka, nie pomiar.

---

# CZĘŚĆ B — WARTOŚCI WYLICZANE

Dziesięć haseł. **Żadna z tych wartości nie jest odczytem** — każda powstaje z działania na
odczytach i każda musi to o sobie powiedzieć.

---

## Temperatura oleju (model)

**Model termiczny · wejścia: `0105` `0104` `0146` `011F` · odświeżany co 0,25 s**

**CO TO JEST**
**To nie jest pomiar.** Ten silnik nie ma czujnika temperatury oleju — sprawdziliśmy to,
pytając sterownik o listę tego, co potrafi, i takiego odczytu tam nie ma.

Aplikacja **liczy** tę wartość: bierze temperaturę płynu, obciążenie silnika i temperaturę
na dworze, i symuluje, jak olej się nagrzewa. Wynik jest zawsze oznaczony tyldą `~`.

**PO CO NA TO PATRZEĆ**
Bo olej to jedyna rzecz, przy której człowiek naprawdę podejmuje decyzję: wcisnąć gaz czy
jeszcze poczekać. Wskaźnik na desce pokazuje płyn, a płyn nagrzewa się mniej więcej dwa razy
szybciej — więc deska mówi „gotowe" długo przed czasem.

**GDY WYJDZIE POZA PASMO**
Poniżej progu przy wysokich obrotach to jeden z pięciu [[alarm i karencja|alarmów]] — i jedyny,
który mówi o **Twoim** postępowaniu, nie o stanie auta. Zimny olej jest gęsty i nie zdąży
wejść wszędzie, gdzie powinien.

**CZEGO TO NIE MÓWI**
To model, więc **może się mylić**. Dlatego zawsze towarzyszy mu ocena pewności — zobacz
[[pewność modelu]]. Nie mówi też **nic o poziomie ani o stanie oleju**: czy jest go dość i czy
nadaje się do dalszej jazdy, tego OBD-II nie potrafi sprawdzić w żaden sposób.

---

## Podciśnienie

**Wyliczane: `0133` − `010B` · odświeżane co 1 s**

**CO TO JEST**
O ile mniejsze jest ciśnienie w kolektorze dolotowym niż na zewnątrz auta. Silnik zasysa
powietrze przez przymkniętą przepustnicę jak przez słomkę — im mocniej ciągnie, tym większa ta
różnica.

**PO CO NA TO PATRZEĆ**
To najprostsza miara tego, jak bardzo silnik „się dusi" na przymkniętej przepustnicy.
Utrzymujący się niski poziom przy zamkniętej przepustnicy sugeruje, że powietrze wchodzi gdzieś
poza nią.

**GDY WYJDZIE POZA PASMO**
— Bez pasma. Prawidłowa wartość zależy wprost od otwarcia przepustnicy i obrotów.

**CZEGO TO NIE MÓWI**
Jest to **różnica dwóch odczytów, nie pomiar**. Oba mają rozdzielczość jednego kilopaskala,
więc drobne wahania mogą pochodzić z zaokrągleń, a nie z silnika.

---

## Suma korekt

**Wyliczana: `0106` + `0107` · odświeżana co 0,25 s**

**CO TO JEST**
[[korekta krótkoterminowa|Krótka]] i [[korekta długoterminowa|długa]] korekta paliwa dodane do
siebie. Łączna poprawka względem przepisu.

**PO CO NA TO PATRZEĆ**
Same korekty potrafią mylić: długa może być wysoka, a krótka akurat ją równoważyć w drugą
stronę. **Dopiero suma mówi, ile naprawdę paliwa idzie ponad przepis** — i dlatego to ona ma
pasmo, a nie krótka korekta.

**GDY WYJDZIE POZA PASMO**
Trwałe wyjście poza pasmo znaczy, że silnik konsekwentnie dostaje inną dawkę, niż przewiduje
przepis. Aplikacja liczy, **ile czasu w [[pętla zamknięta i otwarta|pętli zamkniętej]]** suma
była poza pasmem — pojedynczy skok nie znaczy nic, utrzymujący się stan już tak.

**CZEGO TO NIE MÓWI**
Liczona poza pętlą zamkniętą jest bez wartości. Cieniowanie pod wykresem pokazuje, które
fragmenty krzywej należy pominąć.

---

## Rozjazd przepustnicy

**Wyliczany: `014C` − `0111` · odświeżany co 1 s**

**CO TO JEST**
Różnica między tym, ile sterownik kazał otworzyć przepustnicę, a tym, ile faktycznie jest
otwarte.

**PO CO NA TO PATRZEĆ**
Przepustnica nie jest połączona linką z pedałem — otwiera ją silniczek. Normalnie obie krzywe
leżą jedna na drugiej. Gdy zaczynają się rozjeżdżać, klapa się brudzi albo zacina.

**GDY WYJDZIE POZA PASMO**
— Bez pasma. Nie mamy źródła mówiącego, jaki rozjazd jest jeszcze normalny, więc aplikacja
**nie stawia tu granicy** — pokazuje liczbę i wykres, a ocenę zostawia oczom.

**CZEGO TO NIE MÓWI**
Chwilowa różnica przy gwałtownym ruchu pedałem jest normalna — klapa ma bezwładność
i potrzebuje ułamka sekundy. Liczy się tylko rozjazd utrzymujący się w spokojnej jeździe.

---

## Dystans

**Wyliczany: całkowanie `010D` metodą trapezów**

**CO TO JEST**
Ile kilometrów przejechano w danym przejeździe. Aplikacja **nie ma dostępu do drogomierza** —
sumuje drogę z odczytów prędkości, cztery razy na sekundę.

**PO CO NA TO PATRZEĆ**
Pozwala porównywać przejazdy między sobą i jest podstawą wszystkich zestawień w Historii.

**GDY WYJDZIE POZA PASMO**
— Bez pasma.

**CZEGO TO NIE MÓWI**
**Nie jest to stan drogomierza i nigdy się z nim nie zgodzi.** Liczy tylko przejazdy, które były
nagrywane, i to z pewnym błędem sumowania. Każda jazda bez włączonej aplikacji w tej liczbie
nie istnieje.

---

## Średnia prędkość

**Wyliczana: [[dystans]] ÷ czas trwania przejazdu**

**CO TO JEST**
Przeciętna prędkość na całym przejeździe, razem z postojami na światłach.

**PO CO NA TO PATRZEĆ**
Jest najszybszym sposobem sprawdzenia, **czy dwa przejazdy są w ogóle porównywalne**. Trasa
z trzydziestką średniej i trasa z sześćdziesiątką to zupełnie inne warunki dla silnika, nawet
przy tym samym dystansie.

**GDY WYJDZIE POZA PASMO**
— Bez pasma.

**CZEGO TO NIE MÓWI**
Nie jest to prędkość, z którą się jechało — postoje ją zaniżają. Dwie trasy o tej samej średniej
mogą wyglądać zupełnie inaczej.

---

## Czas do 90 °C

**Wyliczany z serii `0105` w przejeździe**

**CO TO JEST**
Ile trwało, zanim płyn chłodzący osiągnął dziewięćdziesiąt stopni. **Puste, gdy nigdy nie
osiągnął** — i to właśnie pusta wartość jest tu najciekawsza.

**PO CO NA TO PATRZEĆ**
Karta miesiąca zlicza przejazdy, w których ta wartość jest pusta, i podpisuje to „bez
rozgrzania". Odpowiada na pytanie, którego inaczej nie da się zadać: *ile razy w tym miesiącu
odpaliłem silnik i zgasiłem, zanim się rozgrzał.*

**Krótkie przejazdy zużywają silnik najbardziej i jest to jedyna rzecz na całej liście, na którą
kierowca ma bezpośredni wpływ.**

**GDY WYJDZIE POZA PASMO**
— Bez pasma. Rosnący czas z miesiąca na miesiąc **może** wskazywać na termostat, ale równie
dobrze na chłodniejszą pogodę. Aplikacja pokazuje obie liczby obok siebie i nie orzeka.

**CZEGO TO NIE MÓWI**
Wartość pusta nie znaczy usterki — znaczy krótki przejazd. Rozróżnienie jest istotne: to
wskaźnik sposobu użytkowania, nie stanu auta.

---

## Czas poza pasmem w pętli zamkniętej

**Wyliczany z serii `0106`, `0107` i `0103`**

**CO TO JEST**
Ile czasu w danym przejeździe [[suma korekt]] była poza swoim pasmem — liczone **wyłącznie
wtedy, gdy pętla była zamknięta**, i odnoszone do czasu spędzonego w pętli zamkniętej.

**PO CO NA TO PATRZEĆ**
Pojedynczy skok korekty nic nie znaczy. Ta liczba mówi, **jak długo** stan się utrzymywał —
a to jest różnica między chwilowym zaburzeniem a rzeczywistym problemem.

**GDY WYJDZIE POZA PASMO**
— Sam licznik nie ma pasma; opisuje, jak często przekraczane było pasmo czego innego.

**CZEGO TO NIE MÓWI**
Zero nie znaczy, że wszystko gra — przejazd mógł być tak krótki, że pętla nigdy się nie
zamknęła. Zawsze czytaj razem z czasem spędzonym w pętli zamkniętej.

---

## Mediana korekty długiej

**Wyliczana z serii `0107` w przejeździe**

**CO TO JEST**
Wartość środkowa [[korekta długoterminowa|korekty długoterminowej]] w całym przejeździe.
**Mediana, nie średnia** — jedna nietypowa chwila nie przesuwa jej wcale.

**PO CO NA TO PATRZEĆ**
Pojedynczy odczyt korekty nic nie mówi. Dopiero jedna liczba na przejazd, oglądana z miesiąca
na miesiąc, pokazuje powolny dryf — a właśnie tak zachowuje się większość problemów z układem
paliwowym.

**GDY WYJDZIE POZA PASMO**
Pasmo ma sama [[korekta długoterminowa]]; mediana jest jej podsumowaniem i czyta się ją tak
samo.

**CZEGO TO NIE MÓWI**
Puste, gdy w przejeździe nie było odczytów tej korekty. Nie mówi też nic o rozrzucie — dwa
przejazdy o tej samej medianie mogą wyglądać zupełnie inaczej.

---

## Maksymalne ciśnienie szyny i obciążenie przy nim

**Wyliczane z serii `0123` i `0143`, dopasowywane po czasie**

**CO TO JEST**
Najwyższe [[ciśnienie w szynie wysokiego ciśnienia|ciśnienie na szynie]] w całym przejeździe
oraz **obciążenie silnika w tej samej chwili**.

**PO CO NA TO PATRZEĆ**
Samo maksimum nic nie mówi. Sto pięćdziesiąt barów przy dziewięćdziesięciu procentach
obciążenia to zupełnie co innego niż sto pięćdziesiąt przy trzydziestu — w pierwszym przypadku
pompa była proszona o wszystko i tyle dała, w drugim nikt jej nie obciążył.

**GDY WYJDZIE POZA PASMO**
Czytaj wobec pasma „pod obciążeniem" z hasła ciśnienia szyny — ale tylko wtedy, gdy obciążenie
przy maksimum było naprawdę wysokie.

**CZEGO TO NIE MÓWI**
Jeśli w całym przejeździe nie było mocnego przyspieszania, maksimum będzie niskie i **nie
znaczy to nic o pompie**. Spokojna jazda po mieście nie jest testem pompy wysokiego ciśnienia.

---

# CZĘŚĆ C — POJĘCIA

Trzydzieści dwa hasła. **Pojęcia mają trzy rubryki, nie cztery** — rubryka o wyjściu poza pasmo
nie ma tu zastosowania, bo pojęcie nie jest wielkością mierzoną.

| Rubryka | Co zawiera |
|---|---|
| **CO TO JEST** | Wyjaśnienie pojęcia |
| **PO CO CI TO WIEDZIEĆ** | Co się dzięki temu rozumie w aplikacji |
| **CZEGO TO NIE MÓWI** | Granica. **Rubryka obowiązkowa** |

---

## OBD-II

**CO TO JEST**
Zestaw przepisów mówiących, że **każde auto musi umieć opowiedzieć o sobie** przez jedno,
znormalizowane gniazdo. W Europie obowiązuje dla benzyn od 2001 roku, dla diesli od 2004.

Gniazdo jest w zasięgu ręki kierowcy — w tym aucie pod kierownicą. To samo, do którego mechanik
podpina swój komputer.

**PO CO CI TO WIEDZIEĆ**
Dzięki temu ta aplikacja w ogóle może istnieć. Nie musi znać Hyundaia — wystarczy, że zna
przepis, którego Hyundai musiał się trzymać.

**CZEGO TO NIE MÓWI**
Przepis obejmuje **wyłącznie rzeczy związane ze spalinami**. Dlatego jest temperatura płynu,
a nie ma poziomu oleju, stanu klocków ani zawieszenia — zobacz [[czego ten egzemplarz nie ma]].

---

## PID

**CO TO JEST**
Numer parametru — jak numer pytania w formularzu. Wysyłasz `010C` i sterownik odpowiada, ile ma
obrotów. `0105` to temperatura płynu, `0142` to napięcie.

Pierwsze dwie cyfry to [[tryb zapytania]], kolejne dwie — numer parametru w tym trybie.

**PO CO CI TO WIEDZIEĆ**
Każda liczba w tej aplikacji ma swój PID i jest on wypisany w stopce hasła. Dzięki temu da się
sprawdzić w dowolnym źródle, o co dokładnie aplikacja zapytała.

**CZEGO TO NIE MÓWI**
Istnienie PID-u w przepisie **nie znaczy, że to auto go obsługuje**. O tym mówi
[[maska PID-ów]].

---

## Tryb zapytania

**CO TO JEST**
Rodzaj pytania zadawanego sterownikowi. Aplikacja używa czterech:

| Tryb | O co pyta |
|---|---|
| **01** | Bieżące wartości — obroty, temperatury, ciśnienia |
| **03** | Zapisane [[kod błędu DTC\|kody błędów]] |
| **07** | Kody oczekujące, jeszcze niepotwierdzone |
| **09** | Dane pojazdu, w tym [[VIN]] |

**PO CO CI TO WIEDZIEĆ**
Wyjaśnia zapis `0103`: to **tryb 01, parametr 03**, czyli bieżąca wartość statusu układu
paliwowego. Samo `03` bez prefiksu znaczy co innego — tryb odczytu kodów.

**CZEGO TO NIE MÓWI**
Przepis ma więcej trybów. Tryb 02 zwraca zdjęcie warunków z chwili zapisania błędu, tryb 06 —
wyniki testów pokładowych. **Ta aplikacja ich nie używa.** Nie używa też trybu 04, który kasuje
kody — zobacz [[monitory gotowości]], żeby zrozumieć, dlaczego to dobrze.

---

## Adapter ELM327

**CO TO JEST**
Pudełko wielkości pudełka zapałek, wtykane w gniazdo diagnostyczne. Tłumaczy między językiem
magistrali samochodowej a zwykłym tekstem, który da się wysłać przez Bluetooth.

Rozmowa wygląda dosłownie tak: aplikacja wysyła `010C`, adapter odpowiada `410C1AF8`.

**PO CO CI TO WIEDZIEĆ**
To najsłabsze ogniwo całego układu. Potrafi się zawiesić, zgubić połączenie albo odpowiedzieć
z opóźnieniem kilkunastu sekund przy pierwszym łączeniu — a aplikacja musi to znosić bez
zgadywania danych.

**CZEGO TO NIE MÓWI**
Adapter **niczego nie mierzy**. Jest tłumaczem. Wszystkie liczby pochodzą ze sterownika
samochodu.

---

## Maska PID-ów

**CO TO JEST**
Lista tego, co ten konkretny egzemplarz potrafi. Zanim aplikacja o cokolwiek zapyta, zadaje
pytanie wstępne: *„które parametry obsługujesz?"* — a sterownik odpowiada listą.

Ten samochód obsługuje **trzydzieści siedem** parametrów.

**PO CO CI TO WIEDZIEĆ**
Przepis mówi, co **może** istnieć. Maska mówi, co **istnieje w tym aucie**. Lista parametrów
w aplikacji powstaje z przecięcia jednego z drugim — dlatego nie zobaczysz tu pól, które zawsze
byłyby puste.

**CZEGO TO NIE MÓWI**
Obsługiwany nie znaczy sensowny. Poziom paliwa jest w masce, a mimo to zwraca zero niezależnie
od tego, ile jest w baku — zobacz [[obsługiwany bez danych]].

---

## Obsługiwany bez danych

**CO TO JEST**
Sytuacja, w której [[maska PID-ów|maska]] mówi „ten parametr obsługuję", a odczyt i tak nie
przynosi nic sensownego.

Na tym samochodzie zdarza się to **dokładnie raz**: przy poziomie paliwa w zbiorniku. Bit
w masce jest ustawiony, sterownik odpowiada bez błędu — i za każdym razem podaje zero,
niezależnie od tego, czy bak jest pełny, czy pusty.

**PO CO CI TO WIEDZIEĆ**
Bo to jedyny znany przypadek, w którym samochód **mówi o sobie nieprawdę**, i pokazuje, gdzie
leży granica zaufania do maski.

Maska jest deklaracją sterownika, nie obietnicą danych. Aplikacja traktuje ją jako podpowiedź,
o co warto zapytać — nie jako dowód, że coś przyjdzie. Dlatego poziomu paliwa **nie ma nigdzie
na ekranie**: ani kafla, ani wiersza w odczytach, ani pozycji w punkcie odniesienia. Miejsce po
nim w listwie kafli zajęła [[korekta długoterminowa]].

**CZEGO TO NIE MÓWI**
Nie wiadomo **dlaczego** tak jest — czy czujnik w baku jest uszkodzony, czy nigdy nie był
podpięty do magistrali diagnostycznej, czy Hyundai świadomie zostawił ten parametr pusty.
Aplikacja stwierdza fakt i nie zgaduje przyczyny.

Nie znaczy to też, że wskaźnik paliwa na desce rozdzielczej nie działa — deska bierze odczyt
własną drogą, nie przez [[OBD-II]]. **Brakuje go w diagnostyce, nie w samochodzie.**

To osobna kategoria od [[czego ten egzemplarz nie ma]]: tam parametrów **nie ma w masce
w ogóle**. Tutaj jest, i to jest cała różnica.

---

## Sterownik silnika

**CO TO JEST**
Komputer zarządzający pracą silnika. Sto razy na sekundę decyduje, ile wtrysnąć paliwa, kiedy
zapalić iskrę i jak otworzyć przepustnicę.

**PO CO CI TO WIEDZIEĆ**
**Wszystko, co pokazuje ta aplikacja, to jego punkt widzenia.** Nie mierzymy niczego sami —
pytamy sterownik, co widzi.

**CZEGO TO NIE MÓWI**
Sterownik widzi tylko to, do czego ma czujniki. Zepsuty czujnik daje zły odczyt, a sterownik
nie zawsze o tym wie — wtedy aplikacja pokaże tę samą nieprawdę.

---

## Kod błędu DTC

**CO TO JEST**
Pięcioznakowy kod zapisywany, gdy sterownik wykryje coś nie tak — na przykład `P0171`. Pierwsza
litera mówi, jakiego układu dotyczy: `P` silnik i skrzynia, `C` podwozie, `B` nadwozie,
`U` sieć pokładowa.

**PO CO CI TO WIEDZIEĆ**
To najkonkretniejsza informacja, jaką sterownik potrafi dać. Nowy kod w trakcie jazdy ma własny
[[alarm i karencja\|alarm]] i własny wiersz na panelu Stan.

**CZEGO TO NIE MÓWI**
Kod wskazuje **objaw, nie przyczynę**. `P0171` znaczy „mieszanka uboga", a nie „wymień sondę".
Aplikacja ma słownik czterdziestu jeden kodów; kod spoza niego pokazuje surowo, z jawnym
napisem, że opisu nie zna. **Zgadnięty opis wygląda identycznie jak prawdziwy** — i to jest cały
problem z wymyślaniem.

---

## Kody: zapisany, oczekujący, trwały

**CO TO JEST**
Ten sam błąd może być w trzech stanach:

| Rodzaj | Co znaczy |
|---|---|
| **Oczekujący** | Wykryty raz, jeszcze niepotwierdzony. Kontrolka zwykle nie świeci |
| **Zapisany** | Potwierdzony przy kolejnym wystąpieniu. [[kontrolka MIL\|Kontrolka]] się pali |
| **Trwały** | Zapisany i **niekasowalny** — znika, gdy sterownik sam potwierdzi naprawę |

**PO CO CI TO WIEDZIEĆ**
Kod oczekujący to wczesne ostrzeżenie: coś zdarzyło się raz. Może zniknąć samo, może się
potwierdzić. Zobaczenie go **zanim zapali się kontrolka** to jedna z rzeczy, dla których warto
mieć tę aplikację.

**CZEGO TO NIE MÓWI**
Ten egzemplarz **nie odpowiada na pytanie o kody trwałe** — aplikacja pokazuje wtedy kreskę,
a nie pustą listę. Pusta lista znaczyłaby „sprawdzono, nie ma", a to nieprawda.

---

## Kontrolka MIL

**CO TO JEST**
Żółta lampka silnika na desce rozdzielczej. Zapala się, gdy sterownik potwierdzi usterkę mającą
wpływ na spaliny.

**PO CO CI TO WIEDZIEĆ**
Aplikacja czyta jej stan wprost ze sterownika, razem z tym, **ile kilometrów przejechano od jej
zapalenia**. To odróżnia „zapaliła się przed chwilą" od „świeci od dwóch tysięcy kilometrów".

**CZEGO TO NIE MÓWI**
Zgaszona kontrolka nie znaczy, że wszystko gra. Może być kod oczekujący, mogą być niegotowe
[[monitory gotowości\|monitory]], a ktoś mógł niedawno skasować pamięć — o czym powie
[[przebieg od skasowania kodów]].

---

## Monitory gotowości

**CO TO JEST**
Sterownik sam sobie robi testy: sprawdza katalizator, sondy, szczelność układu paliwowego,
wypadanie zapłonu. Każdy taki test to **monitor**. Żeby się wykonał, auto musi przejechać
określony dystans w określonych warunkach.

**PO CO CI TO WIEDZIEĆ**
**Auto z niegotowymi monitorami nie przejdzie badania emisji**, choćby było zupełnie zdrowe.
Kasowanie kodów zeruje monitory — dlatego skasowanie błędu tuż przed badaniem jest najprostszym
sposobem, żeby je oblać. To także powód, dla którego ta aplikacja **nie kasuje kodów**.

**CZEGO TO NIE MÓWI**
„Gotowy" znaczy **wykonany**, nie **zdany**. Monitor może się wykonać i wykryć usterkę.

---

## Pętla zamknięta i otwarta

**CO TO JEST**
Dwa tryby dozowania paliwa.

**Pętla zamknięta:** sterownik wącha spaliny [[sonda lambda\|sondą lambda]] i na bieżąco
poprawia dawkę. Sprawdza skutek i reaguje — stąd „zamknięta", bo informacja wraca do początku.

**Pętla otwarta:** sterownik ignoruje sondę i leje według gotowej mapy. Robi tak, gdy silnik
jest zimny (sonda jeszcze nie działa) albo przy pełnym gazie (potrzeba mocy, nie oszczędności).

**PO CO CI TO WIEDZIEĆ**
**[[korekta krótkoterminowa\|Korekty paliwa]] mają sens wyłącznie w pętli zamkniętej.**
W otwartej są zamrożone z ostatniej chwili przed przełączeniem. Dlatego panel Mieszanka pisze
stan pętli **nad** wykresem i cieniuje fragmenty krzywej, których nie należy czytać.

**CZEGO TO NIE MÓWI**
Pętla otwarta nie jest usterką. Na zimnym silniku i przy pełnym gazie ma tak być. Niepokojące
są tylko dwa stany: otwarta z powodu awarii i zamknięta mimo awarii sondy.

---

## Sonda lambda

**CO TO JEST**
Czujnik w rurze wydechowej mierzący, ile tlenu **zostało** w spalinach. Za dużo tlenu znaczy,
że benzyny było za mało.

W tym aucie są dwie: jedna przed [[katalizator\|katalizatorem]], druga za nim.

**PO CO CI TO WIEDZIEĆ**
Sonda przed katalizatorem steruje dozowaniem paliwa — to od niej pochodzą korekty. Sonda za
katalizatorem sprawdza, czy katalizator jeszcze pracuje.

**CZEGO TO NIE MÓWI**
Sonda działa dopiero po rozgrzaniu do kilkuset stopni. Do tego czasu silnik jest w
[[pętla zamknięta i otwarta\|pętli otwartej]] i korekty nie znaczą nic.

---

## Bank

**CO TO JEST**
Grupa cylindrów obsługiwana przez jedną sondę i jeden zestaw korekt. W silnikach w kształcie
litery V są dwa banki, po jednym na każdą stronę.

**PO CO CI TO WIEDZIEĆ**
Ten silnik jest rzędowy, czterocylindrowy — ma **jeden bank**. Dlatego wszędzie w aplikacji
widzisz „bank 1" i nigdy „bank 2".

**CZEGO TO NIE MÓWI**
Jeden bank znaczy, że korekty są wspólne dla wszystkich czterech cylindrów. **Nie da się z nich
wyczytać, który cylinder sprawia problem** — do tego potrzebny byłby tryb 06, którego ta
aplikacja nie używa.

---

## Katalizator

**CO TO JEST**
Dopalacz w rurze wydechowej. W środku ma ceramiczny plaster miodu pokryty metalami
szlachetnymi, na których dopalają się trucizny.

**PO CO CI TO WIEDZIEĆ**
Pracuje w wąskim oknie temperatur. Za zimny — nie robi nic. Za gorący — rozpada się w środku.
Dlatego aplikacja pokazuje jego temperaturę z zaznaczonymi granicami tego okna.

**Auto jeżdżące wyłącznie na krótkich trasach truje najbardziej**, bo katalizator nigdy nie
dochodzi do temperatury pracy.

**CZEGO TO NIE MÓWI**
Temperatura nie mówi, **jak skutecznie** katalizator oczyszcza. Zużyty potrafi być gorący
i bezużyteczny naraz.

---

## Wtrysk bezpośredni GDI

**CO TO JEST**
Sposób podawania paliwa, w którym benzyna wtryskiwana jest **prosto do cylindra**, a nie do
kanału dolotowego przed zaworem.

Daje więcej mocy przy mniejszym spalaniu, ale wymaga ciśnienia kilkadziesiąt razy większego niż
wtrysk zwykły — bo paliwo musi się przepchać do sprężonego już powietrza.

**PO CO CI TO WIEDZIEĆ**
Stąd bierze się cały panel Wtrysk GDI i całe zainteresowanie
[[ciśnienie w szynie wysokiego ciśnienia\|ciśnieniem na szynie]]. W zwykłym silniku ten parametr
w ogóle by nie istniał.

**CZEGO TO NIE MÓWI**
Silniki GDI mają znaną przypadłość: **benzyna nie omywa zaworów dolotowych**, więc osadza się na
nich nagar. OBD-II **nie potrafi tego zmierzyć**. Jedyny pośredni ślad to powolny dryf
[[korekta długoterminowa\|korekty długiej]] przez miesiące — i dlatego karta miesiąca w ogóle
istnieje.

---

## Przedmuchiwanie zbiornika

**CO TO JEST**
Benzyna w baku paruje. Opary nie mogą uciec do atmosfery, więc łapie je pojemnik z węglem
aktywnym. Gdy się zapełni, sterownik otwiera zawór i **przepuszcza opary do silnika**, żeby je
spalić.

**PO CO CI TO WIEDZIEĆ**
W chwili przedmuchiwania do silnika trafia paliwo, którego nikt nie wtrysnął — więc
[[korekta krótkoterminowa\|korekta]] gwałtownie skacze. **To nie jest usterka.**

Aplikacja cieniuje wykres korekt w takich miejscach: widzisz górkę i od razu wiesz, że ma
wytłumaczenie.

**CZEGO TO NIE MÓWI**
Aplikacja zna tylko wartość **zadaną** — ile sterownik kazał otworzyć. Nie ma odczytu mówiącego,
ile oparów faktycznie przeszło.

---

## GMP i wyprzedzenie zapłonu

**CO TO JEST**
GMP to **górny martwy punkt** — moment, w którym tłok jest najwyżej.

Iskra musi strzelić **trochę wcześniej**, bo benzyna potrzebuje chwili, żeby się rozpalić. To
„trochę wcześniej" mierzy się w stopniach obrotu wału i nazywa wyprzedzeniem zapłonu.

**PO CO CI TO WIEDZIEĆ**
Sterownik przesuwa iskrę bez przerwy — wcześniej dla mocy, później dla bezpieczeństwa. Gdy
wykryje spalanie stukowe, **natychmiast ją cofa**, i wtedy wartość na wykresie spada. Tego
zjawiska nie widać w żaden inny sposób.

**CZEGO TO NIE MÓWI**
Nie ma jednej prawidłowej wartości. Zależy naraz od obrotów, obciążenia, temperatury i jakości
benzyny.

---

## Norma

**CO TO JEST**
Pasmo, w którym wartość **powinna** się mieścić. Stoi pod każdym parametrem w całej aplikacji.

Gdzie normy nie ma, stoi kreska — i **jest to informacja, nie przeoczenie**. Znaczy: nie ma
źródła mówiącego, ile powinno być.

**PO CO CI TO WIEDZIEĆ**
Nie musisz wiedzieć, co znaczy korekta plus trzy przecinek dziewięć procent. Wystarczy
sprawdzić, czy jesteś w środku pasma.

**CZEGO TO NIE MÓWI**
Norma mówi, **ile powinno być** — według reguły albo źródła. Nie mówi, ile było u Ciebie kiedyś;
do tego służy [[„poprzednio"]]. Zatarcie tej różnicy jest w tej aplikacji zakazane.

---

## „Poprzednio"

**CO TO JEST**
Wartość zmierzona **na tym samym aucie przy poprzednim przeglądzie**, w tych samych warunkach:
silnik rozgrzany, auto stoi, na luzie.

**PO CO CI TO WIEDZIEĆ**
Kilkanaście parametrów ma w kolumnie normy kreskę, bo nie ma źródła mówiącego, ile powinno być.
Ale **zmiana wobec poprzedniego pomiaru mówi bardzo dużo** — nawet gdy nie wiadomo, jaka wartość
jest prawidłowa.

**CZEGO TO NIE MÓWI**
**To nie jest [[norma]].** Aplikacja nigdy nie opisze wartości z historii jako normy — gdyby to
zrobiła, uznałbyś, że tak ma być w tym modelu. Jeśli auto miało problem już wtedy, „poprzednio"
pokazuje wartość problemową jako punkt odniesienia.

---

## Model kontra pomiar

**CO TO JEST**
**Pomiar** to liczba odczytana z czujnika. **Model** to liczba wyliczona z innych liczb, bo
czujnika nie ma.

W tej aplikacji modelem jest [[temperatura oleju (model)\|temperatura oleju]]. Oznaczana jest
zawsze tyldą `~`.

**PO CO CI TO WIEDZIEĆ**
Model może się mylić, pomiar zwykle nie. Podanie modelu jako pomiaru byłoby najgorszym możliwym
kłamstwem tej aplikacji — dotyczyłoby wielkości, przy której podejmujesz decyzję o wciśnięciu
gazu na zimnym silniku.

**CZEGO TO NIE MÓWI**
Oznaczenie tyldą nie znaczy, że wartość jest zła. Znaczy, że **wiadomo, skąd pochodzi**.

---

## Pewność modelu

**CO TO JEST**
Ocena tego, na ile [[temperatura oleju (model)\|model oleju]] zdążył się ustabilizować. Trzy
poziomy: niska, średnia, dobra — zależnie od tego, jak długo silnik pracuje bez przerwy.

**PO CO CI TO WIEDZIEĆ**
Model potrzebuje czasu. Tuż po uruchomieniu zgaduje, po kwadransie jazdy jest już naprawdę
blisko. Bez tej oceny obie sytuacje wyglądałyby tak samo.

**CZEGO TO NIE MÓWI**
„Dobra" nie znaczy „zmierzona". To nadal [[model kontra pomiar\|model]].

---

## Kreska zamiast zera

**CO TO JEST**
Gdy odczyt się nie uda, aplikacja pokazuje `—`. **Nigdy zero.**

**PO CO CI TO WIEDZIEĆ**
Zero wygląda jak pomiar. Zero obrotów, zero barów, zero stopni — wszystko to są wartości, które
mogą naprawdę wystąpić, więc podstawione zero jest **nierozróżnialne od prawdy**.

Kreska mówi wprost: *nie wiem*. Zasada obowiązuje w całej aplikacji, w regułach i w alarmach —
brakująca wartość **pomija regułę**, zamiast uruchamiać ją na wymyślonym zerze.

**CZEGO TO NIE MÓWI**
Kreska nie znaczy usterki. Zwykle znaczy, że akurat ten parametr nie zdążył się odczytać, albo
że to auto go nie obsługuje.

---

## Znaczniki ▲ ▼ ~ ⌀ ○

**CO TO JEST**
Jeden zestaw oznaczeń, ten sam we wszystkich widokach:

| Znak | Znaczenie |
|---|---|
| brak | Wartość w paśmie |
| `▲` | Powyżej pasma |
| `▼` | Poniżej pasma |
| `~` | [[model kontra pomiar\|Model]], nie pomiar |
| `— ⌀` | Odczyt nieudany, powód obok |
| `— ○` | Odczyt się udał, ale wartość w tym stanie nic nie znaczy |

**PO CO CI TO WIEDZIEĆ**
Ten sam znak zawsze znaczy to samo — nie trzeba uczyć się każdego ekranu osobno.

Dwa ostatnie łatwo pomylić, a mówią co innego. `⌀` znaczy **nie wiem** — nic nie przyszło,
zerwane połączenie albo sterownik nie odpowiedział. `○` znaczy **wiem, ale to teraz nic nie
mówi** — liczba przyszła, tylko warunki, w których cokolwiek znaczy, nie są spełnione.

Dziś zobaczysz `○` w jednym miejscu: na kaflu [[korekta długoterminowa|korekty długiej]], gdy
silnik pracuje w [[pętla zamknięta i otwarta|pętli otwartej]]. Wtedy korekta jest zamrożona
i opisuje chwilę sprzed przejścia w ten tryb, a nie to, co dzieje się teraz.

**CZEGO TO NIE MÓWI**
**Znaczniki nie są kolorem i kolor ich nie zastępuje.** W jaskrawym słońcu na ekranie w desce
kolory bledną, a strzałka zostaje widoczna. Dlatego kolor nigdy nie jest jedynym nośnikiem
informacji.

---

## Waga wniosku

**CO TO JEST**
Każdy wniosek przeglądu ma jedną z trzech wag:

| Waga | Co znaczy |
|---|---|
| **usterka** | Sterownik zgłasza potwierdzony problem |
| **uwaga** | Coś odstaje i warto się przyjrzeć |
| **informacja** | Warto wiedzieć, nie trzeba nic robić |

**PO CO CI TO WIEDZIEĆ**
Waga zależy też od **tego, jak pewne jest źródło progu**. Reguły oparte na materiałach
branżowych, a nie na dokumentacji Hyundaia, mają zawsze `uwaga` — nigdy `usterka`, choćby
odchylenie było duże.

**CZEGO TO NIE MÓWI**
`usterka` nie znaczy „nie jedź". Znaczy „sterownik ma potwierdzony problem". O tym, czy da się
jechać, aplikacja nie orzeka.

---

## Alarm i karencja

**CO TO JEST**
Pięć warunków odzywa się **dźwiękiem**, bo w trakcie jazdy patrzysz na drogę. Po odezwaniu się
alarm milknie na jakiś czas — to **karencja**.

Karencja liczona jest **osobno dla każdego rodzaju**, żeby jeden alarm nie zagłuszył innego.

**PO CO CI TO WIEDZIEĆ**
Alarmów jest pięć i **taka ma pozostać ich liczba**. Alarm, który odzywa się często, przestaje
być alarmem — zaczynasz go ignorować, a potem ignorujesz też ten jeden ważny.

**CZEGO TO NIE MÓWI**
Cisza nie znaczy, że wszystko gra. Znaczy, że żaden z pięciu warunków nie zachodzi. Nieudany
odczyt **nigdy nie odpala alarmu** — aplikacja pomija wtedy regułę, zamiast zgadywać.

---

## Poziomy odpytywania

**CO TO JEST**
[[adapter ELM327\|Adapter]] przepuszcza ograniczoną liczbę pytań na sekundę, więc nie da się
pytać o wszystko równie często. Parametry podzielono na cztery poziomy — **według tego, jak
szybko się zmieniają**:

| Poziom | Jak często | Przykład |
|---|---|---|
| Gorący | co 0,25 s | obroty, prędkość |
| Szybki | co 1 s | ciśnienie na szynie |
| Średni | co 2,5 s | korekta długa, katalizator |
| Wolny | co 5 s | temperatura otoczenia |

Do tego kody błędów, sprawdzane najrzadziej.

**PO CO CI TO WIEDZIEĆ**
Wyjaśnia, dlaczego niektóre wykresy są gładkie, a inne schodkowe — i dlaczego zaraz po
uruchomieniu część parametrów jest jeszcze „nie zmierzona".

**CZEGO TO NIE MÓWI**
Rzadziej odpytywany nie znaczy mniej ważny. Temperatura otoczenia po prostu nie zmienia się
cztery razy na sekundę.

---

## Nastawa Zrównoważona

**CO TO JEST**
Domyślne tempo pracy: **cztery pytania na sekundę** w pętli gorącej.

**PO CO CI TO WIEDZIEĆ**
Sprawdzono, że przy tej nastawie wszystkie [[poziomy odpytywania\|poziomy]] mieszczą się
z zapasem pod limitem adaptera, a wykresy są czytelne. Podkręcenie tempa niczego nie poprawia —
adapter zaczyna gubić odpowiedzi, a wtedy **przybywa kresek zamiast danych**.

**CZEGO TO NIE MÓWI**
Cztery razy na sekundę to dużo jak na obserwację, ale mało jak na złapanie bardzo krótkich
zjawisk. Pojedyncze wypadnięcie zapłonu trwa krócej i tą drogą się go nie zobaczy.

---

## Przejazd i sesja odzyskana

**CO TO JEST**
**Przejazd** to jedno nagranie: od uruchomienia silnika do zatrzymania. Zapisuje się sam.

**Sesja odzyskana** to przejazd, który nie został domknięty normalnie — bo system ubił aplikację
albo odcięto zasilanie. Aplikacja zapisuje punkty kontrolne co pół minuty, więc większość danych
zostaje.

**PO CO CI TO WIEDZIEĆ**
Sesja odzyskana jest wszędzie oznaczona jako **przerwana**. Brakuje jej ostatnich sekund
i odczytu kodów na końcu — pokazywanie jej jak każdej innej byłoby kłamstwem tego samego rodzaju
co zero zamiast [[kreska zamiast zera\|kreski]].

**CZEGO TO NIE MÓWI**
Przerwana sesja nie znaczy usterki auta. Zwykle znaczy, że system operacyjny radia zwolnił
pamięć.

---

## Decymacja

**CO TO JEST**
Dwudziestominutowy przejazd to około pięciu tysięcy punktów na każdy wykres. Ekran ma może
tysiąc pikseli szerokości, więc punkty trzeba zredukować.

Aplikacja robi to przez **minimum i maksimum w każdym koszyku**, nigdy przez średnią.

**PO CO CI TO WIEDZIEĆ**
Uśrednianie wygładziłoby dokładnie te ostre skoki, dla których się na wykres patrzy. Krótkie
szarpnięcie korekty zniknęłoby bez śladu.

**CZEGO TO NIE MÓWI**
Wykres pokazuje skrajności w każdym przedziale czasu, nie każdą próbkę z osobna. Przy dużym
oddaleniu widać, **że** coś się wydarzyło, ale nie ile dokładnie trwało.

---

## VIN

**CO TO JEST**
Siedemnastoznakowy numer nadwozia, jedyny w swoim rodzaju dla każdego auta. Aplikacja czyta go
wprost ze sterownika.

**PO CO CI TO WIEDZIEĆ**
Chroni przed **pomieszaniem historii dwóch samochodów**. Gdy adapter trafi do innego auta,
aplikacja zauważy zmianę i zapyta, zanim dopisze cokolwiek do historii.

Z VIN-u odczytywane są tylko trzy rzeczy pewne: producent, rok modelowy i zakład produkcyjny.

**CZEGO TO NIE MÓWI**
Znaków opisujących nadwozie i silnik **aplikacja nie tłumaczy**, bo Hyundai nie publikuje tego
kodowania. Zgadnięcie byłoby podaniem nieprawdy jako faktu.

---

## Blokada prędkościowa

**CO TO JEST**
Gdy [[prędkość pojazdu]] pokaże cokolwiek powyżej zera, część aplikacji przestaje reagować:
ustawienia, zmiana parametrów, uruchomienie przeglądu i słownik. Zostaje zatrzymanie nagrywania
i przełączanie paneli.

**PO CO CI TO WIEDZIEĆ**
Ekran w desce rozdzielczej jest na wyciągnięcie ręki i kusi. Blokada zdejmuje pokusę, zamiast
liczyć na samodyscyplinę.

**Nagrywanie idzie niezależnie od tego, co jest na ekranie** — patrzenie nigdy nie jest
konieczne, żeby mieć dane.

**CZEGO TO NIE MÓWI**
Blokada zwalnia się natychmiast po zatrzymaniu. Nie ma opóźnienia ani potwierdzania — jest
zabezpieczeniem przed rozproszeniem, nie systemem nadzoru.

---

## Czego ten egzemplarz nie ma

**CO TO JEST**
Trzy parametry z przepisu OBD-II, których ten samochód **nie obsługuje** — sprawdzone
w [[maska PID-ów\|masce]], nie założone:

| Parametr | Konsekwencja w aplikacji |
|---|---|
| **Temperatura oleju** | Jest [[temperatura oleju (model)\|modelem]], nie pomiarem |
| **Chwilowe zużycie paliwa** | Zużycia paliwa **nie ma w ogóle** — żadnego pola, żadnej wartości |
| **Przepływomierz powietrza** | Silnik liczy masę powietrza z ciśnienia. Fizycznie nie ma tego czujnika |

**PO CO CI TO WIEDZIEĆ**
Żeby nie szukać rzeczy, których nie ma — i żeby wiedzieć, że ich brak jest **stwierdzony**,
a nie przeoczony.

**CZEGO TO NIE MÓWI**
Poza tymi trzema OBD-II nie daje wielu rzeczy, których nikt nie obiecywał: **poziomu oleju**,
stanu klocków, ciśnienia w oponach, stanu zawieszenia. Przepis obejmuje wyłącznie to, co wiąże
się ze spalinami.

Nie obejmuje też czwartej rzeczy, której w aplikacji nie zobaczysz — **poziomu paliwa**. Ta
jest w masce, więc formalnie nie należy tutaj, tylko do [[obsługiwany bez danych]]. Skutek dla
Ciebie jest ten sam, powód zupełnie inny.
