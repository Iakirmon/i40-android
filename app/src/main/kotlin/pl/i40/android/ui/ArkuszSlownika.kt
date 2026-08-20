package pl.i40.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import pl.i40.android.rules.PasmaOdniesienia
import pl.i40.android.rules.RodzajPasma
import pl.i40.android.storage.PunktOdniesienia

/**
 * Arkusz słownika z dołu — §7.1. Nie okno modalne; zsuwalny palcem.
 */
@Composable
fun ArkuszSlownika(
    haslo: HasloSlownika,
    nawigacja: StanNawigacjiSlownika,
    teraz: String,
    norma: String,
    poprzednio: String,
    onZamknij: () -> Unit,
    onOdsylacz: (String) -> Unit,
    onWstecz: () -> Unit,
    onDoPoczatku: () -> Unit,
    modifier: Modifier = Modifier
) {
    val kolory = LocalI40Kolory.current
    var offsetY by remember { mutableFloatStateOf(0f) }
    Column(
        modifier
            .fillMaxWidth()
            .offset { IntOffset(0, offsetY.roundToInt().coerceAtLeast(0)) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (offsetY > 120f) onZamknij()
                        offsetY = 0f
                    },
                    onVerticalDrag = { _, dy -> if (dy > 0) offsetY += dy }
                )
            }
            .background(kolory.powierzchniaPodniesiona)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        BasicText(
            "✕",
            modifier = Modifier.clickable(onClick = onZamknij).padding(8.dp),
            style = TextStyle(color = kolory.tekst, fontSize = 18.sp)
        )
        BasicText(
            haslo.tytul.uppercase(),
            style = TextStyle(color = kolory.tekst, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        )
        if (haslo.rodzaj == RodzajHasla.Parametr) {
            WierszBloku("Teraz", teraz)
            WierszBloku("Norma", norma)
            WierszBloku("Poprzednio", poprzednio)
        }
        for ((nazwa, tekst) in haslo.rubryki) {
            BasicText(
                nazwa,
                modifier = Modifier.padding(top = 12.dp),
                style = TextStyle(color = kolory.akcent, fontSize = 13.sp)
            )
            TekstZOdsylaczami(tekst, onOdsylacz)
        }
        if (haslo.stopka.isNotEmpty()) {
            BasicText(
                haslo.stopka,
                modifier = Modifier.padding(top = 16.dp),
                style = TextStyle(color = kolory.tekstWyciszony, fontSize = 12.sp)
            )
        }
        if (nawigacja.pokazWrocDoPoczatku) {
            BasicText(
                "wróć do początku",
                modifier = Modifier.clickable(onClick = onDoPoczatku).padding(8.dp),
                style = TextStyle(color = kolory.akcent, fontSize = 14.sp)
            )
        } else if (nawigacja.glebokosc > 1) {
            BasicText(
                "wstecz",
                modifier = Modifier.clickable(onClick = onWstecz).padding(8.dp),
                style = TextStyle(color = kolory.akcent, fontSize = 14.sp)
            )
        }
    }
}

@Composable
private fun WierszBloku(etykieta: String, wartosc: String) {
    val kolory = LocalI40Kolory.current
    BasicText(
        "$etykieta    $wartosc",
        style = TextStyle(color = kolory.tekst, fontSize = 14.sp, fontFamily = I40CzcionkaWartosci)
    )
}

@Composable
private fun TekstZOdsylaczami(tekst: String, onOdsylacz: (String) -> Unit) {
    val kolory = LocalI40Kolory.current
    val re = Regex("""\[\[([^\]|]+)(?:\|([^\]]+))?\]\]""")
    var last = 0
    val matches = re.findAll(tekst).toList()
    if (matches.isEmpty()) {
        BasicText(tekst, style = TextStyle(color = kolory.tekstDrugi, fontSize = 14.sp))
        return
    }
    Column {
        for (m in matches) {
            if (m.range.first > last) {
                BasicText(
                    tekst.substring(last, m.range.first),
                    style = TextStyle(color = kolory.tekstDrugi, fontSize = 14.sp)
                )
            }
            val celTytul = m.groupValues[1].trim()
            val etykieta = m.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() } ?: celTytul
            val id = Slownik.idZTytulu(celTytul)
            BasicText(
                etykieta,
                modifier = Modifier.clickable { onOdsylacz(id) },
                style = TextStyle(color = kolory.akcent, fontSize = 14.sp)
            )
            last = m.range.last + 1
        }
        if (last < tekst.length) {
            BasicText(
                tekst.substring(last),
                style = TextStyle(color = kolory.tekstDrugi, fontSize = 14.sp)
            )
        }
    }
}

object FormatBlokuSlownika {
    fun normaDlaPid(pid: Int?): String {
        if (pid == null) return FormatPomiaru.NIEDOSTEPNE
        val pasmo = PasmaOdniesienia.wpisy.firstOrNull {
            it.pid == pid && it.rodzaj == RodzajPasma.Norma
        } ?: return FormatPomiaru.NIEDOSTEPNE
        return when {
            pasmo.min != null && pasmo.max != null && pasmo.min == -pasmo.max ->
                "±${pasmo.max.toInt()} ${pasmo.jednostka}".trim()
            pasmo.min != null && pasmo.max != null ->
                "${pasmo.min.toInt()}–${pasmo.max.toInt()} ${pasmo.jednostka}".trim()
            pasmo.min != null -> "≥ ${pasmo.min.toInt()} ${pasmo.jednostka}".trim()
            else -> FormatPomiaru.NIEDOSTEPNE
        }
    }

    fun poprzednio(pid: Int?, punkty: List<PunktOdniesienia>): String {
        if (pid == null || punkty.isEmpty()) return FormatPomiaru.NIEDOSTEPNE
        return FormatOdniesienia.wiersz(pid, punkty).ifBlank { FormatPomiaru.NIEDOSTEPNE }
    }

    fun pidZeStopki(stopka: String): Int? {
        val m = Regex("""PID `0*([0-9A-Fa-f]+)`""").find(stopka) ?: return null
        return m.groupValues[1].toIntOrNull(16)
    }
}

object MagazynSlownika {
    private var cache: List<HasloSlownika>? = null

    fun wczytaj(markdown: String): List<HasloSlownika> {
        cache = Slownik.parsuj(markdown)
        return cache!!
    }

    fun hasla(): List<HasloSlownika> = cache.orEmpty()

    fun poId(id: String): HasloSlownika? = hasla().firstOrNull { it.id == id }
}
