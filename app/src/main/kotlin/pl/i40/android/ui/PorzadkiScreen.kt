package pl.i40.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.i40.android.storage.KryteriumPorzadkow
import pl.i40.android.storage.Przejazd

@Composable
fun PorzadkiScreen(
    przejazdy: List<Przejazd>,
    onWstecz: () -> Unit,
    onUsun: (List<String>) -> Unit,
    terazMs: Long = System.currentTimeMillis(),
    modifier: Modifier = Modifier
) {
    val kolory = LocalI40Kolory.current
    var kryterium by remember { mutableStateOf<KryteriumPorzadkow?>(null) }
    var minuty by remember { mutableStateOf(FormatPorzadkow.MINUTY.first()) }
    var miesiace by remember { mutableStateOf(FormatPorzadkow.MIESIACE.first()) }
    val wybrane = when (kryterium) {
        is KryteriumPorzadkow.KrotszeNiz -> KryteriumPorzadkow.KrotszeNiz(minuty)
        KryteriumPorzadkow.Przerwane -> KryteriumPorzadkow.Przerwane
        is KryteriumPorzadkow.StarszeNiz -> KryteriumPorzadkow.StarszeNiz(miesiace)
        null -> null
    }
    val widok = FormatPorzadkow.widok(przejazdy, wybrane, terazMs)
    var doPotwierdzenia by remember { mutableStateOf<List<Przejazd>?>(null) }
    val cal = remember { SiatkaMiesiaca.kalendarzPolski() }
    val wiele = doPotwierdzenia
    if (wiele != null) {
        OknoPotwierdzeniaUsuniecia(
            tekst = FormatPotwierdzenia.wielokrotne(
                wiele,
                0,
                FormatPotwierdzenia.rozmiarBajtow(wiele),
                cal
            ),
            onAnuluj = { doPotwierdzenia = null },
            onUsun = {
                onUsun(wiele.map { it.id })
                doPotwierdzenia = null
            },
            modifier = modifier
        )
        return
    }
    Column(modifier.fillMaxSize().background(kolory.tlo).padding(12.dp).verticalScroll(rememberScrollState())) {
        BasicText(
            "← Porządki",
            modifier = Modifier.clickable(onClick = onWstecz).padding(8.dp),
            style = TextStyle(color = kolory.akcent, fontSize = 16.sp)
        )
        BasicText("Zajęte miejsce", style = TextStyle(color = kolory.tekstDrugi, fontSize = 13.sp))
        BasicText(widok.zajete, style = TextStyle(color = kolory.tekst, fontSize = 16.sp))
        BasicText(
            "WYBIERZ JEDNO KRYTERIUM",
            modifier = Modifier.padding(top = 16.dp),
            style = TextStyle(color = kolory.akcent, fontSize = 13.sp)
        )
        WierszKryterium("Krótsze niż", kryterium is KryteriumPorzadkow.KrotszeNiz) {
            kryterium = KryteriumPorzadkow.KrotszeNiz(minuty)
        }
        Row(Modifier.fillMaxWidth()) {
            for (m in FormatPorzadkow.MINUTY) {
                val on = kryterium is KryteriumPorzadkow.KrotszeNiz && minuty == m
                BasicText(
                    "$m min",
                    modifier = Modifier.clickable {
                        minuty = m
                        kryterium = KryteriumPorzadkow.KrotszeNiz(m)
                    }.padding(8.dp),
                    style = TextStyle(color = if (on) kolory.akcent else kolory.tekst, fontSize = 14.sp)
                )
            }
        }
        WierszKryterium("Sesje przerwane", kryterium is KryteriumPorzadkow.Przerwane) {
            kryterium = KryteriumPorzadkow.Przerwane
        }
        WierszKryterium("Starsze niż", kryterium is KryteriumPorzadkow.StarszeNiz) {
            kryterium = KryteriumPorzadkow.StarszeNiz(miesiace)
        }
        Row(Modifier.fillMaxWidth()) {
            for (m in FormatPorzadkow.MIESIACE) {
                val on = kryterium is KryteriumPorzadkow.StarszeNiz && miesiace == m
                BasicText(
                    "$m mies.",
                    modifier = Modifier.clickable {
                        miesiace = m
                        kryterium = KryteriumPorzadkow.StarszeNiz(m)
                    }.padding(8.dp),
                    style = TextStyle(color = if (on) kolory.akcent else kolory.tekst, fontSize = 14.sp)
                )
            }
        }
        BasicText(
            "DO USUNIĘCIA",
            modifier = Modifier.padding(top = 16.dp),
            style = TextStyle(color = kolory.akcent, fontSize = 13.sp)
        )
        BasicText(widok.wierszUsuniecia, style = TextStyle(color = kolory.tekst, fontSize = 14.sp))
        BasicText(widok.wierszChronione, style = TextStyle(color = kolory.tekstDrugi, fontSize = 14.sp))
        BasicText(widok.wierszWToku, style = TextStyle(color = kolory.tekstDrugi, fontSize = 14.sp))
        for (w in widok.lista) {
            BasicText(w, style = TextStyle(color = kolory.tekst, fontSize = 13.sp))
        }
        BasicText(
            widok.przycisk,
            modifier = Modifier
                .clickable(enabled = widok.przyciskAktywny) {
                    val wybraneIds = widok.ids.toSet()
                    doPotwierdzenia = przejazdy.filter { it.id in wybraneIds }
                }
                .padding(16.dp),
            style = TextStyle(
                color = if (widok.przyciskAktywny) kolory.uwaga else kolory.tekstWyciszony,
                fontSize = 16.sp
            )
        )
    }
}

@Composable
private fun WierszKryterium(etykieta: String, wybrane: Boolean, onClick: () -> Unit) {
    val kolory = LocalI40Kolory.current
    val znak = if (wybrane) "●" else "○"
    BasicText(
        "$znak  $etykieta",
        modifier = Modifier.clickable(onClick = onClick).padding(vertical = 6.dp),
        style = TextStyle(color = kolory.tekst, fontSize = 15.sp)
    )
}
