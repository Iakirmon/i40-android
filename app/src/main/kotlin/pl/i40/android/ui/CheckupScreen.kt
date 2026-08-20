package pl.i40.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pl.i40.android.R
import pl.i40.android.checkup.CheckupOrchestrator
import pl.i40.android.checkup.Raport
import pl.i40.android.checkup.SlownikDtc
import pl.i40.android.checkup.ZrodloRaportu
import pl.i40.android.transport.MockI40Script
import pl.i40.android.transport.MockTransport

@Composable
fun CheckupScreen(wRuchu: Boolean, modifier: Modifier = Modifier) {
    val kolory = LocalI40Kolory.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Na postoju — pełny przegląd na atrapie.") }
    var raport by remember { mutableStateOf<Raport?>(null) }
    val slownik = remember {
        val json = context.resources.openRawResource(R.raw.dtc_dictionary).bufferedReader().use { it.readText() }
        SlownikDtc.zJson(json)
    }

    Column(modifier.fillMaxSize().background(kolory.tlo).padding(16.dp)) {
        BasicText("Przegląd", style = TextStyle(color = kolory.tekst, fontSize = 22.sp))
        BasicText(status, style = TextStyle(color = kolory.tekstDrugi, fontSize = 14.sp))
        val mozna = !wRuchu
        BasicText(
            text = "Uruchom przegląd",
            modifier = Modifier.padding(top = 16.dp).clickable(enabled = mozna) {
                scope.launch {
                    status = "Łączenie…"
                    val transport = MockTransport(MockI40Script.make(), timeScale = 0.0)
                    val wynik = CheckupOrchestrator(slownikDtc = slownik).uruchom(
                        transport = transport,
                        zrodlo = ZrodloRaportu.Atrapa,
                        scope = scope
                    )
                    raport = wynik
                    status = wynik.werdykt.tytul
                }
            }.padding(12.dp).background(if (mozna) kolory.powierzchnia else kolory.powierzchnia),
            style = TextStyle(color = if (mozna) kolory.akcent else kolory.tekstWyciszony, fontSize = 16.sp)
        )
        val r = raport
        if (r != null) {
            BasicText(
                text = r.werdykt.tytul,
                style = TextStyle(color = kolory.werdykt(r.werdykt), fontSize = 28.sp, fontFamily = I40CzcionkaWartosci)
            )
            BasicText(
                text = r.pojazd.vin ?: FormatPomiaru.NIEDOSTEPNE,
                style = TextStyle(color = kolory.tekstDrugi, fontSize = 14.sp)
            )
        }
        if (wRuchu) {
            BasicText(
                "Blokada prędkościowa — przegląd tylko na postoju.",
                style = TextStyle(color = kolory.uwaga, fontSize = 14.sp)
            )
        }
    }
}
