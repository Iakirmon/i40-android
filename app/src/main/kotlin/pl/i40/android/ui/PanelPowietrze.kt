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
import pl.i40.android.acquisition.RingSample

@Composable
fun PanelPowietrze(
    atmosfera: Double?,
    kolektor: Double?,
    zadana: Double?,
    rzeczywista: Double?,
    pedal: Double?,
    atmosferaSamples: List<RingSample>,
    kolektorSamples: List<RingSample>,
    zadanaSamples: List<RingSample>,
    rzeczywistaSamples: List<RingSample>,
    pedalSamples: List<RingSample>,
    onPid: (Int) -> Unit = {},
    onHaslo: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val kolory = LocalI40Kolory.current
    val vacuum = FormatPowietrza.probkiPodcisnienia(atmosferaSamples, kolektorSamples)
    val biezace = FormatPowietrza.podcisnienieKpa(atmosfera, kolektor)
    val rozjazd = FormatPowietrza.rozjazdPkt(zadana, rzeczywista)
    Column(modifier.fillMaxWidth().background(kolory.tlo)) {
        RollingChart(
            pid = FormatPowietrza.PID_PODCISNIENIE,
            samples = vacuum,
            tytul = FormatPowietrza.podpisWyliczone(),
            wartoscNadpisana = FormatKafla.wartosc(FormatPowietrza.PID_PODCISNIENIE, biezace),
            onKlik = { onHaslo("podcisnienie") }
        )
        BasicText(
            text = "norma  ${FormatPowietrza.norma("podcisnienie")}",
            modifier = Modifier.padding(horizontal = 8.dp),
            style = TextStyle(color = kolory.tekstWyciszony, fontSize = 12.sp)
        )
        RollingChart(
            pid = FormatPowietrza.PID_RZECZYWISTA,
            samples = zadanaSamples,
            samplesDruga = rzeczywistaSamples,
            tytul = "PRZEPUSTNICA        zadana ── rzeczyw. ┈┈",
            onKlik = { onPid(0x11) }
        )
        BasicText(
            text = "zad. ${FormatKafla.wartosc(0x4C, zadana)}  rz. ${FormatKafla.wartosc(0x11, rzeczywista)}" +
                "  rozjazd  ${FormatPowietrza.rozjazdTekst(rozjazd)}",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = TextStyle(color = kolory.tekstDrugi, fontSize = 12.sp)
        )
        BasicText(
            text = "norma rozjazdu  ${FormatPowietrza.norma("rozjazd")}",
            modifier = Modifier.padding(horizontal = 8.dp),
            style = TextStyle(color = kolory.tekstWyciszony, fontSize = 12.sp)
        )
        RollingChart(
            pid = FormatPowietrza.PID_PEDAL,
            samples = pedalSamples,
            tytul = "PEDAŁ",
            onKlik = { onPid(0x49) }
        )
        Row(Modifier.fillMaxWidth().padding(8.dp)) {
            BasicText(
                text = "atmosferyczne  ${FormatKafla.wartosc(0x33, atmosfera)}" +
                    "  ·  kolektor ${FormatKafla.wartosc(0x0B, kolektor)}",
                modifier = Modifier.clickable { onPid(0x33) },
                style = TextStyle(color = kolory.tekstDrugi, fontSize = 12.sp)
            )
        }
        BasicText(
            text = "pedał  ${FormatKafla.wartosc(0x49, pedal)}  norma ${FormatPowietrza.norma("pedal")}",
            modifier = Modifier.padding(horizontal = 8.dp),
            style = TextStyle(color = kolory.tekstWyciszony, fontSize = 12.sp)
        )
    }
}
