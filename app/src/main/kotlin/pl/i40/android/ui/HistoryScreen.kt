package pl.i40.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Siatka miesięczna powstaje w etapie 8. */
@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    val kolory = LocalI40Kolory.current
    Column(modifier.fillMaxSize().background(kolory.tlo).padding(16.dp)) {
        BasicText("Historia", style = TextStyle(color = kolory.tekst, fontSize = 22.sp))
        BasicText("Brak przejazdów", style = TextStyle(color = kolory.tekstDrugi, fontSize = 16.sp))
    }
}
