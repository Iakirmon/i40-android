package pl.i40.android.ui

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.i40.android.acquisition.RingSample

class SladWykresuTest {
    @Test
    fun ostrySkokZachowanyBezSpline() {
        val samples = listOf(
            RingSample(0.0, 10.0),
            RingSample(1.0, 10.0),
            RingSample(2.0, 90.0),
            RingSample(3.0, 90.0)
        )
        val punkty = punktySladu(
            samples = samples,
            domena = 0.0..3.0,
            zakres = 0.0..100.0,
            pid = 0x04,
            w = 300f,
            h = 100f,
            span = 3f,
            ySpan = 100f
        )
        assertTrue(punkty.size == 4)
        val dy = kotlin.math.abs(punkty[1].second - punkty[2].second)
        assertTrue(dy > 50f, "skok między próbkami 1 i 2 powinien zostać: dy=$dy")
        assertTrue(punkty[0].second == punkty[1].second)
        assertTrue(punkty[2].second == punkty[3].second)
    }

    @Test
    fun animateTylkoWLiniiSkanujacej() {
        val root = File("src/main/kotlin/pl/i40/android/ui")
        val trafienia = mutableListOf<String>()
        root.walkTopDown().filter { it.extension == "kt" }.forEach { plik ->
            val tekst = plik.readText()
            if (!tekst.contains("animate")) return@forEach
            if (plik.name == "LiniaSkanujaca.kt") return@forEach
            trafienia += plik.name
        }
        assertTrue(trafienia.isEmpty(), "animate poza LiniaSkanujaca: $trafienia")
    }

    @Test
    fun wykresBezWypelnienGradientowICieni() {
        val wykres = File("src/main/kotlin/pl/i40/android/ui/RollingChart.kt").readText()
        assertFalse(wykres.contains("cubicTo") || wykres.contains("quadraticTo"))
        assertFalse(wykres.contains("Brush.linearGradient") || wykres.contains("shadow("))
        assertFalse(wykres.contains("StrokeCap.Round"))
        assertTrue(wykres.contains("StrokeCap.Butt"))
        assertTrue(wykres.contains("lineTo"))
    }

    @Test
    fun ograniczenieRuchuMaTrybStatyczny() {
        val src = File("src/main/kotlin/pl/i40/android/ui/LiniaSkanujaca.kt").readText()
        assertTrue(src.contains("ograniczenieRuchu"))
        assertTrue(src.contains("dashPathEffect"))
        assertTrue(src.contains("2400"))
    }
}
