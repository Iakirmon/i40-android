package pl.i40.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
fun PanelWtryskGdi(
    szynaKpa: List<RingSample>,
    obciazenie: List<RingSample>,
    przepustnica: List<RingSample>,
    modifier: Modifier = Modifier
) {
    val kolory = LocalI40Kolory.current
    val (maxBar, load) = FormatGdi.szczytSesji(szynaKpa, obciazenie)
    Column(modifier.fillMaxWidth().background(kolory.tlo)) {
        RollingChart(
            pid = FormatGdi.PID_SZYNA,
            samples = FormatGdi.probkiBar(szynaKpa),
            tytul = "CIŚNIENIE SZYNY",
            linieOdniesienia = FormatGdi.linieSzyny()
        )
        RollingChart(
            pid = FormatGdi.PID_OBCIAZENIE_ABS,
            samples = obciazenie,
            tytul = "OBCIĄŻENIE ABS.",
            linieOdniesienia = FormatGdi.linieObciazenia()
        )
        RollingChart(
            pid = FormatGdi.PID_PRZEPUSTNICA,
            samples = przepustnica,
            tytul = "PRZEPUSTNICA",
            linieOdniesienia = FormatGdi.liniePrzepustnicy()
        )
        BasicText(
            text = FormatGdi.maxWiersz(maxBar, load),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = TextStyle(color = kolory.tekst, fontSize = 13.sp)
        )
        BasicText(
            text = "norma pod obciążeniem  ${FormatGdi.pasmoObciazeniowe()}",
            modifier = Modifier.padding(horizontal = 8.dp),
            style = TextStyle(color = kolory.tekstWyciszony, fontSize = 12.sp)
        )
        BasicText(
            text = FormatGdi.cisnienieZadane(),
            modifier = Modifier.padding(8.dp),
            style = TextStyle(color = kolory.tekstWyciszony, fontSize = 11.sp)
        )
    }
}
