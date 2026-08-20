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
import pl.i40.android.storage.Przejazd

@Composable
fun PorownanieScreen(ten: Przejazd, kandydaci: List<Przejazd>, onWstecz: () -> Unit, modifier: Modifier = Modifier) {
    val kolory = LocalI40Kolory.current
    val start = remember(ten.id, kandydaci) { FormatPorownania.poprzedni(ten, kandydaci) }
    var tam by remember(ten.id) { mutableStateOf(start) }
    val widok = tam?.let { FormatPorownania.widok(ten, it) }
    Column(modifier.fillMaxSize().background(kolory.tlo).padding(12.dp).verticalScroll(rememberScrollState())) {
        BasicText(
            "← Porównanie",
            modifier = Modifier.clickable(onClick = onWstecz).padding(8.dp),
            style = TextStyle(color = kolory.akcent, fontSize = 16.sp)
        )
        if (widok == null || tam == null) {
            BasicText("Porównanie niedostępne.", style = TextStyle(color = kolory.tekstWyciszony, fontSize = 14.sp))
        } else {
            Row(Modifier.fillMaxWidth()) {
                BasicText(" ", modifier = Modifier.weight(1.2f), style = TextStyle(fontSize = 12.sp))
                BasicText(
                    widok.dataTen + if (widok.przerwanyTen) "\nprzerwany" else "",
                    modifier = Modifier.weight(1f),
                    style = TextStyle(color = kolory.tekst, fontSize = 12.sp)
                )
                BasicText(
                    widok.dataTam + if (widok.przerwanyTam) "\nprzerwany" else "",
                    modifier = Modifier.weight(1f).clickable {
                        val vin = ten.vin
                        val pula = kandydaci.filter { it.vin == vin && it.id != ten.id }
                        if (pula.isNotEmpty()) {
                            val i = pula.indexOfFirst { it.id == tam?.id }
                            tam = pula[(i + 1).mod(pula.size)]
                        }
                    },
                    style = TextStyle(color = kolory.akcent, fontSize = 12.sp)
                )
                BasicText(
                    "różnica",
                    modifier = Modifier.weight(0.8f),
                    style = TextStyle(color = kolory.tekstDrugi, fontSize = 12.sp)
                )
            }
            BasicText("[ zmień ]", style = TextStyle(color = kolory.tekstWyciszony, fontSize = 11.sp))
            for (blok in widok.bloki) {
                BasicText(
                    blok.tytul,
                    modifier = Modifier.padding(top = 12.dp),
                    style = TextStyle(color = kolory.akcent, fontSize = 13.sp)
                )
                for (w in blok.wiersze) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        BasicText(
                            w.etykieta,
                            modifier = Modifier.weight(1.2f),
                            style = TextStyle(color = kolory.tekstDrugi, fontSize = 13.sp)
                        )
                        BasicText(
                            w.ten,
                            modifier = Modifier.weight(1f),
                            style = TextStyle(color = kolory.tekst, fontSize = 13.sp, fontFamily = I40CzcionkaWartosci)
                        )
                        BasicText(
                            w.tam,
                            modifier = Modifier.weight(1f),
                            style = TextStyle(color = kolory.tekst, fontSize = 13.sp, fontFamily = I40CzcionkaWartosci)
                        )
                        BasicText(
                            w.roznica ?: "",
                            modifier = Modifier.weight(0.8f),
                            style = TextStyle(color = kolory.tekst, fontSize = 13.sp, fontFamily = I40CzcionkaWartosci)
                        )
                    }
                }
            }
        }
    }
}
