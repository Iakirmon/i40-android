package pl.i40.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import pl.i40.android.checkup.Werdykt

/** Motyw wyłącznie ciemny — garaż / wieczór (sekcja 12.1). Paleta z Theme.swift. */
object I40Kolory {
    val tlo = Color(red = 0.07f, green = 0.08f, blue = 0.09f)
    val powierzchnia = Color(red = 0.12f, green = 0.13f, blue = 0.15f)
    val powierzchniaPodniesiona = Color(red = 0.16f, green = 0.17f, blue = 0.19f)
    val kreska = Color.White.copy(alpha = 0.12f)
    val tekst = Color(red = 0.95f, green = 0.95f, blue = 0.93f)
    val tekstDrugi = Color(red = 0.70f, green = 0.71f, blue = 0.68f)
    val tekstWyciszony = Color(red = 0.48f, green = 0.50f, blue = 0.48f)
    val akcent = Color(red = 0.45f, green = 0.72f, blue = 0.78f)
    val ok = Color(red = 0.35f, green = 0.72f, blue = 0.48f)
    val uwaga = Color(red = 0.92f, green = 0.68f, blue = 0.28f)
    val usterka = Color(red = 0.90f, green = 0.38f, blue = 0.35f)

    fun werdykt(w: Werdykt): Color = when (w) {
        Werdykt.Ok -> ok
        Werdykt.Uwaga -> uwaga
        Werdykt.Usterka -> usterka
    }
}

val LocalI40Kolory = staticCompositionLocalOf { I40Kolory }

@Composable
fun I40Theme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalI40Kolory provides I40Kolory, content = content)
}

val I40CzcionkaWartosci: FontFamily = FontFamily.Monospace
