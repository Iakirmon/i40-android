package pl.i40.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.i40.android.acquisition.RingSample

/** Wykres przesuwny 60 s — sztywna oś Y, bieżąca wartość po prawej. */
@Composable
fun RollingChart(
    pid: Int,
    samples: List<RingSample>,
    modifier: Modifier = Modifier,
    dziedzinaCzasu: ClosedFloatingPointRange<Double>? = null,
    tytul: String? = null,
    linieOdniesienia: List<Double> = emptyList(),
    cienie: List<PasmoCienia> = emptyList(),
    samplesDruga: List<RingSample> = emptyList(),
    wartoscNadpisana: String? = null,
    onKlik: (() -> Unit)? = null
) {
    val kolory = LocalI40Kolory.current
    val zakres = OsY.zakres(pid)
    val domena = dziedzinaCzasu ?: OsY.domenaCzasu(samples + samplesDruga)
    val biezaca = samples.lastOrNull()?.value
    val przyciecie = biezaca?.let { OsY.przytnij(it, pid) }

    Column(modifier.fillMaxWidth().height(96.dp).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (onKlik != null) Modifier.clickable(onClick = onKlik) else Modifier),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                text = tytul ?: FormatKafla.krotkaEtykieta(pid),
                style = TextStyle(color = kolory.tekstDrugi, fontSize = 12.sp)
            )
            Box(Modifier.weight(1f))
            BasicText(
                text = wartoscNadpisana ?: FormatKafla.wartosc(pid, biezaca),
                style = TextStyle(
                    color = if (przyciecie?.przyciete == true) kolory.uwaga else kolory.tekst,
                    fontSize = 16.sp,
                    fontFamily = I40CzcionkaWartosci
                )
            )
        }
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val span = (domena.endInclusive - domena.start).toFloat().coerceAtLeast(0.001f)
            val ySpan = (zakres.endInclusive - zakres.start).toFloat().coerceAtLeast(0.001f)
            for (pasmo in cienie) {
                val x0 = ((pasmo.start - domena.start) / span).toFloat().coerceIn(0f, 1f) * w
                val x1 = ((pasmo.end - domena.start) / span).toFloat().coerceIn(0f, 1f) * w
                if (x1 <= x0) continue
                val kolor = when (pasmo.rodzaj) {
                    RodzajCienia.Przedmuchiwanie -> kolory.akcent.copy(alpha = 0.18f)
                    RodzajCienia.PetlaOtwarta -> kolory.tekstWyciszony.copy(alpha = 0.25f)
                }
                drawRect(color = kolor, topLeft = Offset(x0, 0f), size = Size(x1 - x0, h))
            }
            for (linia in linieOdniesienia) {
                val yn = ((linia - zakres.start) / ySpan).toFloat()
                val y = (1f - yn.coerceIn(0f, 1f)) * h
                drawLine(
                    color = kolory.tekstWyciszony,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            if (samples.size >= 2) {
                val path = Path()
                var started = false
                for (sample in samples) {
                    val t = ((sample.time - domena.start) / span).toFloat()
                    val clipped = OsY.przytnij(sample.value, pid)
                    val yn = ((clipped.wartosc - zakres.start) / ySpan).toFloat()
                    val x = t.coerceIn(0f, 1f) * w
                    val y = (1f - yn.coerceIn(0f, 1f)) * h
                    if (!started) {
                        path.moveTo(x, y)
                        started = true
                    } else {
                        path.lineTo(x, y)
                    }
                }
                drawPath(path, color = kolory.akcent, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
            }
            if (samplesDruga.size >= 2) {
                val path = Path()
                var started = false
                for (sample in samplesDruga) {
                    val t = ((sample.time - domena.start) / span).toFloat()
                    val clipped = OsY.przytnij(sample.value, pid)
                    val yn = ((clipped.wartosc - zakres.start) / ySpan).toFloat()
                    val x = t.coerceIn(0f, 1f) * w
                    val y = (1f - yn.coerceIn(0f, 1f)) * h
                    if (!started) {
                        path.moveTo(x, y)
                        started = true
                    } else {
                        path.lineTo(x, y)
                    }
                }
                drawPath(
                    path,
                    color = kolory.tekst,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                    )
                )
            }
            if (przyciecie?.przyciete == true) {
                drawCircle(kolory.uwaga, radius = 4.dp.toPx(), center = Offset(w - 4.dp.toPx(), 4.dp.toPx()))
            }
        }
    }
}
