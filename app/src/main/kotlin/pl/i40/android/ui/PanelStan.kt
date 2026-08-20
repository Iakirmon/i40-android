package pl.i40.android.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.i40.android.acquisition.OilTempEstimator
import pl.i40.android.rules.PasmaOdniesienia
import pl.i40.android.rules.RuleEngine
import pl.i40.android.rules.RuleInput
import pl.i40.android.service.MigawkaZywego

@Composable
fun PanelStan(migawka: MigawkaZywego, modifier: Modifier = Modifier) {
    val kolory = LocalI40Kolory.current
    val odczytane = migawka.wartosci.keys + if (migawka.olejC != null) setOf(0x5C) else emptySet()
    val odczyty = migawka.wartosci.mapValues { (pid, v) ->
        if (pid == 0x23) PasmaOdniesienia.kpaNaBar(v) else v
    } + (0x5C to migawka.olejC)
    val plyn = migawka.wartosci[0x05]
    val runtime = migawka.wartosci[0x1F]
    val wnioski = RuleEngine.evaluate(
        RuleInput(
            longTermFuelTrim = migawka.wartosci[0x07],
            shortTermFuelTrim = migawka.wartosci[0x06],
            coolantCelsius = plyn,
            runtimeSeconds = runtime,
            voltage = migawka.wartosci[0x42],
            rpm = migawka.wartosci[0x0C],
            oilCelsius = migawka.olejC,
            cisnienieSzynyBar = migawka.wartosci[0x23]?.let { PasmaOdniesienia.kpaNaBar(it) },
            temperaturaKatalizatoraC = migawka.wartosci[0x3C],
            predkoscKmh = migawka.predkoscKmh,
            statusUkladuPaliwowego = migawka.wartosci[0x03]?.toInt()
        )
    )
    val widok = FormatPaneluStan.widok(
        odczyty = odczyty,
        odczytane = odczytane,
        status0103 = migawka.wartosci[0x03]?.toInt(),
        kody = emptyList(),
        silnikRozgrzany = PasmaOdniesienia.silnikRozgrzany(plyn, runtime),
        olejGotowy = migawka.olejPewnosc != OilTempEstimator.Pewnosc.Niska &&
            (migawka.olejC ?: 0.0) >= PasmaOdniesienia.OLEJ_MIN_C,
        wnioski = wnioski
    )
    Column(modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        for (kod in widok.kody) {
            BasicText("✕ Nowy kod błędu: $kod", style = TextStyle(color = kolory.uwaga, fontSize = 16.sp))
        }
        if (widok.tytul.isNotEmpty()) {
            Spacer(Modifier.weight(1f))
            BasicText(
                widok.tytul,
                style = TextStyle(color = kolory.tekst, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            )
            if (widok.kontekst.isNotEmpty()) {
                BasicText(
                    widok.kontekst,
                    modifier = Modifier.padding(top = 8.dp),
                    style = TextStyle(color = kolory.tekstDrugi, fontSize = 14.sp)
                )
            }
            Spacer(Modifier.weight(1f))
        }
        for (w in widok.odchylenia) {
            BasicText(
                "${w.znacznik} ${w.zdanie}",
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                style = TextStyle(color = kolory.tekst, fontSize = 16.sp)
            )
            BasicText(
                w.wartoscPasmo,
                modifier = Modifier.fillMaxWidth(),
                style = TextStyle(color = kolory.tekstDrugi, fontSize = 13.sp)
            )
            BasicText(
                w.skrot,
                modifier = Modifier.fillMaxWidth(),
                style = TextStyle(color = kolory.akcent, fontSize = 13.sp)
            )
        }
        if (widok.dalsze != null) {
            BasicText(widok.dalsze, style = TextStyle(color = kolory.tekstWyciszony, fontSize = 13.sp))
        }
        if (widok.niezmierzone != null) {
            BasicText(
                "Jeszcze nie zmierzone",
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                style = TextStyle(color = kolory.tekstDrugi, fontSize = 14.sp)
            )
            BasicText(
                widok.niezmierzone,
                modifier = Modifier.fillMaxWidth(),
                style = TextStyle(color = kolory.tekstWyciszony, fontSize = 13.sp)
            )
        }
    }
}
