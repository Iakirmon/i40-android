package pl.i40.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.i40.android.service.BlokadaPredkosci
import pl.i40.android.service.InterakcjaZywa
import pl.i40.android.service.MigawkaZywego
import pl.i40.android.storage.Przejazd

private enum class Zakladka(val tytul: String) {
    Przeglad("Przegląd"),
    Nagrywanie("Nagrywanie"),
    Historia("Historia"),
}

@Composable
fun I40App(
    migawka: MigawkaZywego,
    onStop: () -> Unit,
    przejazdy: List<Przejazd> = emptyList(),
    onUsun: (String) -> Unit = {},
    punkty: List<pl.i40.android.storage.PunktOdniesienia> = emptyList()
) {
    val kolory = LocalI40Kolory.current
    var zakladka by remember { mutableStateOf(Zakladka.Nagrywanie) }
    val nawigacja = BlokadaPredkosci.pozwala(
        InterakcjaZywa.Nawigacja,
        wRuchu = migawka.wRuchu,
        nagrywa = migawka.nagrywa
    )

    I40Theme {
        Column(Modifier.fillMaxSize().background(kolory.tlo)) {
            Row(Modifier.fillMaxWidth().background(kolory.powierzchniaPodniesiona)) {
                for (z in Zakladka.entries) {
                    val aktywna = zakladka == z
                    BasicText(
                        text = z.tytul,
                        modifier = Modifier
                            .clickable(enabled = nawigacja || z == Zakladka.Nagrywanie) { zakladka = z }
                            .padding(16.dp),
                        style = TextStyle(
                            color = when {
                                aktywna -> kolory.akcent
                                nawigacja || z == Zakladka.Nagrywanie -> kolory.tekst
                                else -> kolory.tekstWyciszony
                            },
                            fontSize = 16.sp
                        )
                    )
                }
            }
            when (zakladka) {
                Zakladka.Przeglad -> CheckupScreen(wRuchu = migawka.wRuchu, modifier = Modifier.weight(1f))
                Zakladka.Nagrywanie -> LiveScreen(
                    migawka = migawka,
                    onStop = onStop,
                    punkty = punkty,
                    modifier = Modifier.weight(1f)
                )
                Zakladka.Historia -> HistoryScreen(
                    przejazdy = przejazdy,
                    onUsun = onUsun,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
