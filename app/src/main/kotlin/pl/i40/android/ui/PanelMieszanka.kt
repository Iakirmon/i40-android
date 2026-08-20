package pl.i40.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.i40.android.acquisition.RingSample
import pl.i40.android.rules.PasmaOdniesienia

@Composable
fun PanelMieszanka(
    stft: Double?,
    ltft: Double?,
    lambda: Double?,
    stftSamples: List<RingSample>,
    ltftSamples: List<RingSample>,
    pozaPasmemS: Double?,
    sesjaS: Double,
    modifier: Modifier = Modifier
) {
    val kolory = LocalI40Kolory.current
    val probki = FormatMieszanki.sumaProbek(stftSamples, ltftSamples)
    Column(modifier.fillMaxWidth().background(kolory.tlo)) {
        RollingChart(
            pid = 0x06,
            samples = probki,
            tytul = "KOREKTA RAZEM  norma ${FormatMieszanki.normaSumy()}",
            linieOdniesienia = FormatMieszanki.linieSumy()
        )
        Row(Modifier.fillMaxWidth()) {
            PoleMieszanki(
                etykieta = "KRÓTKA",
                wartosc = FormatKafla.wartosc(0x06, stft),
                norma = FormatMieszanki.normaKrotkiej(),
                modifier = Modifier.weight(1f)
            )
            PoleMieszanki(
                etykieta = "LAMBDA ZAD.",
                wartosc = FormatKafla.wartosc(0x44, lambda),
                norma = FormatMieszanki.normaLambdy(),
                modifier = Modifier.weight(1f)
            )
        }
        Row(Modifier.fillMaxWidth()) {
            PoleMieszanki(
                etykieta = "DŁUGA",
                wartosc = FormatKafla.wartosc(0x07, ltft),
                norma = "±${PasmaOdniesienia.korektaDluga.endInclusive.toInt()}",
                modifier = Modifier.weight(1f)
            )
            PoleMieszanki(
                etykieta = "ZA KAT.",
                wartosc = FormatMieszanki.zaKatWartosc(),
                norma = FormatPomiaru.NIEDOSTEPNE,
                powod = FormatMieszanki.zaKatPowod(),
                modifier = Modifier.weight(1f)
            )
        }
        BasicText(
            text = FormatMieszanki.pozaPasmemWiersz(pozaPasmemS, sesjaS),
            modifier = Modifier.padding(8.dp),
            style = TextStyle(color = kolory.tekstDrugi, fontSize = 12.sp)
        )
    }
}

@Composable
private fun PoleMieszanki(
    etykieta: String,
    wartosc: String,
    norma: String,
    modifier: Modifier = Modifier,
    powod: String? = null
) {
    val kolory = LocalI40Kolory.current
    Column(modifier.padding(8.dp)) {
        BasicText(text = "$etykieta    $wartosc", style = TextStyle(color = kolory.tekst, fontSize = 14.sp))
        BasicText(
            text = "norma  $norma",
            style = TextStyle(color = kolory.tekstWyciszony, fontSize = 12.sp)
        )
        if (powod != null) {
            BasicText(text = powod, style = TextStyle(color = kolory.tekstWyciszony, fontSize = 10.sp))
        }
    }
}
