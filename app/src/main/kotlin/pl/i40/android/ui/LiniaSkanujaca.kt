package pl.i40.android.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp

/**
 * Jedyna animacja w aplikacji — §6.4 wyglądu.
 * Puste pole kalibrowane: pionowa linia 1dp `siatka`, przebieg 2,4 s.
 * Przy ograniczeniu ruchu: statyczna kreskowana krawędź.
 */
@Composable
fun LiniaSkanujaca(ograniczenieRuchu: Boolean, modifier: Modifier = Modifier) {
    val kolory = LocalI40Kolory.current
    if (ograniczenieRuchu) {
        Canvas(modifier.fillMaxSize()) {
            val x = size.width * 0.5f
            drawLine(
                color = kolory.siatka,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
            )
        }
        return
    }
    val transition = rememberInfiniteTransition(label = "skan")
    val postep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "skanX"
    )
    Canvas(modifier.fillMaxSize()) {
        val x = postep * size.width
        drawLine(
            color = kolory.siatka,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1.dp.toPx()
        )
    }
}
