package pl.i40.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import pl.i40.android.acquisition.OilTempEstimator
import pl.i40.android.acquisition.RingSample
import pl.i40.android.storage.SummaryCalculator

@Composable
fun PanelTermika(
    katalizator: List<RingSample>,
    plyn: List<RingSample>,
    olej: List<RingSample>,
    olejPewnosc: OilTempEstimator.Pewnosc,
    dolot: Double?,
    otoczenie: Double?,
    onPid: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val kolory = LocalI40Kolory.current
    val plyn90 = FormatTermika.czasDo(plyn, SummaryCalculator.PROG_PLYN_90_C)
    val olej90 = FormatTermika.czasDo(olej, SummaryCalculator.PROG_PLYN_90_C)
    Column(modifier.fillMaxWidth().background(kolory.tlo)) {
        RollingChart(
            pid = 0x3C,
            samples = katalizator,
            tytul = "KATALIZATOR",
            linieOdniesienia = FormatTermika.linieKatalizatora(),
            onKlik = { onPid(0x3C) }
        )
        RollingChart(
            pid = 0x05,
            samples = plyn,
            tytul = "PŁYN",
            linieOdniesienia = FormatTermika.liniePlynu(),
            onKlik = { onPid(0x05) }
        )
        RollingChart(
            pid = 0x5C,
            samples = olej,
            tytul = "OLEJ (model · ${olejPewnosc.label})",
            linieOdniesienia = FormatTermika.linieOleju(),
            onKlik = { onPid(0x5C) }
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            BasicText(
                text = "DOLOT ${FormatKafla.wartosc(0x0F, dolot)}  norma ${FormatTermika.normaDolotu()}",
                modifier = Modifier.weight(1f).clickable { onPid(0x0F) },
                style = TextStyle(color = kolory.tekstDrugi, fontSize = 12.sp)
            )
            BasicText(
                text = "OTOCZ. ${FormatKafla.wartosc(0x46, otoczenie)}  norma ${FormatTermika.normaOtoczenia()}",
                modifier = Modifier.weight(1f).clickable { onPid(0x46) },
                style = TextStyle(color = kolory.tekstDrugi, fontSize = 12.sp)
            )
        }
        BasicText(
            text = FormatTermika.wierszCzasow(plyn90, olej90),
            modifier = Modifier.padding(8.dp),
            style = TextStyle(color = kolory.tekst, fontSize = 13.sp)
        )
    }
}
