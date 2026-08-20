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
import pl.i40.android.obd.FuelSystemStatus
import pl.i40.android.rules.PasmaOdniesienia

@Composable
fun PanelMieszanka(
    stft: Double?,
    ltft: Double?,
    lambda: Double?,
    przedmuch: Double?,
    status0103: Int?,
    stftSamples: List<RingSample>,
    ltftSamples: List<RingSample>,
    przedmuchSamples: List<RingSample>,
    statusSamples: List<RingSample>,
    pozaPasmemS: Double?,
    czasWPetliS: Double?,
    modifier: Modifier = Modifier
) {
    val kolory = LocalI40Kolory.current
    val probki = FormatMieszanki.sumaProbek(stftSamples, ltftSamples)
    val domena = OsY.domenaCzasu(probki)
    val cienie = CieniowanieMieszanki.pasma(
        purge = przedmuchSamples,
        status = statusSamples,
        t0 = domena.start,
        t1 = domena.endInclusive
    )
    Column(modifier.fillMaxWidth().background(kolory.tlo)) {
        BasicText(
            text = FuelSystemStatus.tekstWierszaEkranu(status0103),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = TextStyle(color = kolory.tekst, fontSize = 14.sp)
        )
        RollingChart(
            pid = 0x06,
            samples = probki,
            tytul = "KOREKTA RAZEM  norma ${FormatMieszanki.normaSumy()}",
            linieOdniesienia = FormatMieszanki.linieSumy(),
            cienie = cienie
        )
        BasicText(
            text = "▓ przedmuchiwanie   ░ pętla otwarta",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = TextStyle(color = kolory.tekstWyciszony, fontSize = 11.sp)
        )
        Row(Modifier.fillMaxWidth()) {
            PoleMieszanki(
                etykieta = "KRÓTKA",
                wartosc = FormatKafla.wartosc(0x06, stft),
                norma = FormatMieszanki.normaKrotkiej(),
                modifier = Modifier.weight(1f)
            )
            PoleMieszanki(
                etykieta = "PRZEDMUCH.",
                wartosc = FormatKafla.wartosc(0x2E, przedmuch),
                norma = FormatPomiaru.NIEDOSTEPNE,
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
                etykieta = "LAMBDA ZAD.",
                wartosc = FormatKafla.wartosc(0x44, lambda),
                norma = FormatMieszanki.normaLambdy(),
                modifier = Modifier.weight(1f)
            )
        }
        Row(Modifier.fillMaxWidth()) {
            PoleMieszanki(
                etykieta = "ZA KAT.",
                wartosc = FormatMieszanki.zaKatWartosc(),
                norma = FormatPomiaru.NIEDOSTEPNE,
                powod = FormatMieszanki.zaKatPowod(),
                modifier = Modifier.weight(1f)
            )
        }
        BasicText(
            text = FormatMieszanki.pozaPasmemWiersz(pozaPasmemS, czasWPetliS),
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
