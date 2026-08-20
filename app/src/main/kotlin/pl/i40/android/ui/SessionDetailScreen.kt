package pl.i40.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.i40.android.acquisition.RingSample
import pl.i40.android.storage.Przejazd

@Composable
fun SessionDetailScreen(przejazd: Przejazd, onWstecz: () -> Unit, modifier: Modifier = Modifier) {
    val kolory = LocalI40Kolory.current
    val model = remember(przejazd.id) {
        ModelSzczegolowSesji(przejazd.podsumowanie, przejazd.przebieg, przejazd.notatka, przejazd.startMs())
    }
    val tick = remember { mutableStateOf(0) }
    fun odswiez() {
        tick.value += 1
    }
    tick.value
    Column(modifier.fillMaxSize().background(kolory.tlo).padding(12.dp).verticalScroll(rememberScrollState())) {
        BasicText(
            "Wstecz",
            modifier = Modifier.clickable(onClick = onWstecz).padding(8.dp),
            style = TextStyle(color = kolory.akcent, fontSize = 16.sp)
        )
        val s = model.podsumowanie
        BasicText(
            "Czas ${FormatPomiaru.liczba(s.czasTrwaniaS, 0, "s")} · dystans ${
                s.dystansKm?.let { FormatPomiaru.liczba(it, 1, "km") } ?: FormatPomiaru.NIEDOSTEPNE
            }",
            style = TextStyle(color = kolory.tekst, fontSize = 16.sp)
        )
        val suwakTekst = FormatPomiaru.liczba(model.czasSuwaka.toDouble(), 1, "s")
        BasicText("Suwak $suwakTekst", style = TextStyle(color = kolory.tekstDrugi, fontSize = 13.sp))
        for (seria in model.serie) {
            val scrub = model.wartoscPrzySuwaku(seria)?.toDouble()
            val samples = model.punktyZdecymowane(seria).map { RingSample(it.time.toDouble(), it.value.toDouble()) }
            Column(
                Modifier
                    .fillMaxWidth()
                    .onSizeChanged { model.szerokoscWykresu = it.width }
                    .pointerInput(seria.pid, model.okno) {
                        detectDragGestures { change, _ ->
                            model.ustawSuwak(change.position.x, size.width.toFloat())
                            odswiez()
                        }
                    }
            ) {
                RollingChart(
                    pid = seria.pid,
                    samples = samples,
                    modifier = Modifier.height(110.dp),
                    dziedzinaCzasu = model.okno.start.toDouble()..model.okno.endInclusive.toDouble()
                )
                BasicText(
                    FormatKafla.wartosc(seria.pid, scrub),
                    style = TextStyle(color = kolory.akcent, fontFamily = I40CzcionkaWartosci, fontSize = 14.sp)
                )
            }
        }
    }
}

private fun Przejazd.startMs(): Long = poczatekMs
