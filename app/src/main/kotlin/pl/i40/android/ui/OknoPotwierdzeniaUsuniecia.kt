package pl.i40.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OknoPotwierdzeniaUsuniecia(
    tekst: PotwierdzenieUsuniecia,
    onAnuluj: () -> Unit,
    onUsun: () -> Unit,
    modifier: Modifier = Modifier
) {
    val kolory = LocalI40Kolory.current
    Column(modifier.fillMaxWidth().background(kolory.powierzchniaPodniesiona).padding(16.dp)) {
        BasicText(tekst.naglowek, style = TextStyle(color = kolory.tekst, fontSize = 18.sp))
        if (tekst.dataGodzina.isNotEmpty()) {
            BasicText(
                tekst.dataGodzina,
                modifier = Modifier.padding(top = 12.dp),
                style = TextStyle(color = kolory.tekst, fontSize = 16.sp)
            )
        }
        BasicText(
            tekst.coGinie,
            modifier = Modifier.padding(top = 8.dp),
            style = TextStyle(color = kolory.tekst, fontSize = 14.sp)
        )
        for (w in tekst.pozycje) {
            BasicText(w, style = TextStyle(color = kolory.tekstDrugi, fontSize = 13.sp))
        }
        if (tekst.dalsze != null) {
            BasicText(tekst.dalsze, style = TextStyle(color = kolory.tekstWyciszony, fontSize = 13.sp))
        }
        BasicText(
            tekst.nieodtwarzalne,
            modifier = Modifier.padding(top = 12.dp),
            style = TextStyle(color = kolory.uwaga, fontSize = 14.sp)
        )
        BasicText(
            tekst.coZostaje,
            modifier = Modifier.padding(top = 12.dp),
            style = TextStyle(color = kolory.tekstDrugi, fontSize = 14.sp)
        )
        BasicText(
            tekst.kartaMiesiaca,
            modifier = Modifier.padding(top = 8.dp),
            style = TextStyle(color = kolory.tekstDrugi, fontSize = 14.sp)
        )
        if (tekst.chroniony != null) {
            BasicText(
                tekst.chroniony,
                modifier = Modifier.padding(top = 8.dp),
                style = TextStyle(color = kolory.uwaga, fontSize = 14.sp)
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 16.dp)) {
            BasicText(
                "Anuluj",
                modifier = Modifier.clickable(onClick = onAnuluj).padding(12.dp),
                style = TextStyle(color = kolory.tekst, fontSize = 16.sp)
            )
            Spacer(Modifier.weight(1f))
            BasicText(
                tekst.przyciskUsun,
                modifier = Modifier.clickable(onClick = onUsun).padding(12.dp),
                style = TextStyle(color = kolory.uwaga, fontSize = 16.sp)
            )
        }
    }
}
