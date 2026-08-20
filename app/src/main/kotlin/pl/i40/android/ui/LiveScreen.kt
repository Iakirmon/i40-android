package pl.i40.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.i40.android.service.BlokadaPredkosci
import pl.i40.android.service.FormatKaflaWykresow
import pl.i40.android.service.InterakcjaZywa
import pl.i40.android.service.MigawkaZywego
import pl.i40.android.service.StanPrzejazdu

@Composable
fun LiveScreen(
    migawka: MigawkaZywego,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    punkty: List<pl.i40.android.storage.PunktOdniesienia> = emptyList(),
    onPid: (Int) -> Unit = {},
    onHaslo: (String) -> Unit = {}
) {
    val kolory = LocalI40Kolory.current
    var panel by remember { mutableStateOf(PanelZywy.Stan) }
    Column(Modifier.fillMaxSize().background(kolory.tlo)) {
        Row(Modifier.fillMaxWidth()) {
            for (pid in FormatKafla.KAFLI_DOMYSLNE) {
                Kafel(
                    pid = pid,
                    migawka = migawka,
                    onKlik = { onPid(pid) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        BasicText(
            text = WskaznikPaneli.tekst(panel),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = TextStyle(color = kolory.akcent, fontSize = 12.sp)
        )
        val panelRef = rememberUpdatedState(panel)
        val ruchRef = rememberUpdatedState(migawka.wRuchu)
        val nagrywaRef = rememberUpdatedState(migawka.nagrywa)
        Box(
            Modifier
                .weight(1f)
                .pointerInput(Unit) {
                    var acc = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { acc = 0f },
                        onDragEnd = {
                            val wolno = BlokadaPredkosci.pozwala(
                                InterakcjaZywa.PrzelaczaniePaneli,
                                wRuchu = ruchRef.value,
                                nagrywa = nagrywaRef.value
                            )
                            if (!wolno || kotlin.math.abs(acc) < 80f) return@detectHorizontalDragGestures
                            val all = PanelZywy.entries
                            val i = all.indexOf(panelRef.value)
                            panel = if (acc < 0f) {
                                all[(i + 1) % all.size]
                            } else {
                                all[(i - 1 + all.size) % all.size]
                            }
                        },
                        onHorizontalDrag = { _, dx -> acc += dx }
                    )
                }
        ) {
            Column(Modifier.fillMaxSize()) {
                when (panel) {
                    PanelZywy.Stan -> PanelStan(
                        migawka = migawka,
                        onPid = onPid,
                        modifier = Modifier.fillMaxSize()
                    )
                    PanelZywy.Podstawowy -> {
                        val wierszStanu = if (migawka.jalowyRozgrzany) {
                            "● JAŁOWY ROZGRZANY — punkt odniesienia"
                        } else if (migawka.wRuchu) {
                            "○ w ruchu — punkt odniesienia niedostępny"
                        } else {
                            "○ punkt odniesienia niedostępny"
                        }
                        BasicText(
                            text = wierszStanu,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = TextStyle(color = kolory.tekstDrugi, fontSize = 13.sp)
                        )
                        if (migawka.trybPorownania) {
                            for (pid in FormatKaflaWykresow.PIDY_WYKRESOW) {
                                val teraz = FormatPomiaru.liczba(
                                    migawka.wartosci[pid],
                                    FormatKafla.cyfryPoPrzecinku(pid),
                                    FormatKafla.jednostka(pid)
                                )
                                BasicText(
                                    text = "${FormatKafla.krotkaEtykieta(pid)}  $teraz",
                                    modifier = Modifier
                                        .clickable { onPid(pid) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = TextStyle(color = kolory.tekst, fontSize = 18.sp)
                                )
                                BasicText(
                                    text = FormatOdniesienia.wiersz(pid, punkty),
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    style = TextStyle(color = kolory.tekstDrugi, fontSize = 13.sp)
                                )
                            }
                        } else {
                            for (pid in FormatKaflaWykresow.PIDY_WYKRESOW) {
                                RollingChart(
                                    pid = pid,
                                    samples = migawka.serie[pid].orEmpty(),
                                    onKlik = { onPid(pid) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    PanelZywy.Mieszanka -> PanelMieszanka(
                        stft = migawka.wartosci[0x06],
                        ltft = migawka.wartosci[0x07],
                        lambda = migawka.wartosci[0x44],
                        przedmuch = migawka.wartosci[0x2E],
                        status0103 = migawka.wartosci[0x03]?.toInt(),
                        stftSamples = migawka.serie[0x06].orEmpty(),
                        ltftSamples = migawka.serie[0x07].orEmpty(),
                        przedmuchSamples = migawka.serie[0x2E].orEmpty(),
                        statusSamples = migawka.serie[0x03].orEmpty(),
                        pozaPasmemS = migawka.czasPozaPasmemWPetliZamknietejSekundy,
                        czasWPetliS = migawka.czasWPetliZamknietejSekundy,
                        onPid = onPid,
                        onHaslo = onHaslo,
                        modifier = Modifier.weight(1f)
                    )
                    PanelZywy.WtryskGdi -> PanelWtryskGdi(
                        szynaKpa = migawka.serie[0x23].orEmpty(),
                        obciazenie = migawka.serie[0x43].orEmpty(),
                        przepustnica = migawka.serie[0x11].orEmpty(),
                        jalowy = migawka.jalowyRozgrzany,
                        terazKpa = migawka.wartosci[0x23],
                        punkty = punkty,
                        onPid = onPid,
                        modifier = Modifier.weight(1f)
                    )
                    PanelZywy.Termika -> PanelTermika(
                        katalizator = migawka.serie[0x3C].orEmpty(),
                        plyn = migawka.serie[0x05].orEmpty(),
                        olej = migawka.serie[0x5C].orEmpty(),
                        olejPewnosc = migawka.olejPewnosc,
                        dolot = migawka.wartosci[0x0F],
                        otoczenie = migawka.wartosci[0x46],
                        onPid = onPid,
                        modifier = Modifier.weight(1f)
                    )
                    PanelZywy.Powietrze -> PanelPowietrze(
                        atmosfera = migawka.wartosci[0x33],
                        kolektor = migawka.wartosci[0x0B],
                        zadana = migawka.wartosci[0x4C],
                        rzeczywista = migawka.wartosci[0x11],
                        pedal = migawka.wartosci[0x49],
                        atmosferaSamples = migawka.serie[0x33].orEmpty(),
                        kolektorSamples = migawka.serie[0x0B].orEmpty(),
                        zadanaSamples = migawka.serie[0x4C].orEmpty(),
                        rzeczywistaSamples = migawka.serie[0x11].orEmpty(),
                        pedalSamples = migawka.serie[0x49].orEmpty(),
                        onPid = onPid,
                        onHaslo = onHaslo,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        PasekStanu(migawka = migawka, onStop = onStop)
    }
}

@Composable
private fun Kafel(pid: Int, migawka: MigawkaZywego, onKlik: () -> Unit, modifier: Modifier) {
    val kolory = LocalI40Kolory.current
    val olej = pid == FormatKafla.PID_OLEJ_MODEL
    val wartosc = if (olej) {
        FormatKafla.olejTekst(migawka.olejC)
    } else if (pid == 0x07) {
        FormatKafla.wartoscKorektyDlugiej(migawka.wartosci[pid], migawka.wartosci[0x03]?.toInt())
    } else {
        FormatKafla.wartosc(pid, migawka.wartosci[pid])
    }
    val podpis = if (olej) FormatKafla.olejPodpis(migawka.olejPewnosc) else FormatKafla.podpisZakresu(pid)
    Column(
        modifier
            .clickable(onClick = onKlik)
            .padding(6.dp)
            .background(kolory.powierzchnia)
            .padding(8.dp)
    ) {
        BasicText(
            text = wartosc,
            style = TextStyle(
                color = if (olej) kolory.model else kolory.tekst,
                fontSize = 22.sp,
                fontFamily = I40CzcionkaWartosci,
                fontWeight = FontWeight.Medium
            )
        )
        BasicText(
            text = FormatKafla.krotkaEtykieta(pid) + if (olej) " mod." else "",
            style = TextStyle(color = kolory.tekstDrugi, fontSize = 12.sp)
        )
        BasicText(text = podpis, style = TextStyle(color = kolory.tekstWyciszony, fontSize = 10.sp))
    }
}

@Composable
private fun PasekStanu(migawka: MigawkaZywego, onStop: () -> Unit) {
    val kolory = LocalI40Kolory.current
    val total = migawka.elapsedSeconds.toInt()
    val czas = "%02d:%02d".format(total / 60, total % 60)
    val hz = FormatPomiaru.liczba(migawka.hz, 1, "Hz")
    val stan = when (migawka.stan) {
        StanPrzejazdu.Rozlaczony -> "rozłączony"
        StanPrzejazdu.Czuwanie -> "czeka na silnik"
        StanPrzejazdu.Nagrywa -> "● NAGRYWA"
        StanPrzejazdu.Zamykanie -> "zamykanie"
    }
    val stopOn = migawka.nagrywa &&
        BlokadaPredkosci.pozwala(InterakcjaZywa.Stop, wRuchu = migawka.wRuchu, nagrywa = true)
    Row(
        Modifier.fillMaxWidth().background(kolory.powierzchniaPodniesiona).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            text = "$czas   $hz   ${migawka.queries} zap.   $stan",
            style = TextStyle(color = kolory.tekstDrugi, fontSize = 14.sp, fontFamily = I40CzcionkaWartosci)
        )
        Box(Modifier.weight(1f))
        BasicText(
            text = "Zatrzymaj",
            modifier = Modifier
                .clickable(enabled = stopOn, onClick = onStop)
                .padding(SkalaI40.ODSTEP_CELI_DP.dp)
                .then(
                    Modifier.padding(
                        horizontal = ((SkalaI40.CEL_W_RUCHU_DP - 16) / 2).dp,
                        vertical = ((SkalaI40.CEL_W_RUCHU_DP - 16) / 2).dp
                    )
                ),
            style = TextStyle(color = if (stopOn) kolory.akcent else kolory.tekstWyciszony, fontSize = 16.sp)
        )
    }
}
