package pl.i40.android.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.i40.android.rules.RuleEngine
import pl.i40.android.rules.RuleInput
import pl.i40.android.rules.WagaWniosku

class FormatPaneluStanTest {
    @Test
    fun jedenOdczytToJeszczeNieWiemNigdyWszystkoWNormie() {
        val v = FormatPaneluStan.widok(
            odczyty = mapOf(0x05 to 90.0),
            odczytane = setOf(0x05),
            status0103 = null,
            kody = emptyList(),
            silnikRozgrzany = false,
            olejGotowy = false,
            wnioski = emptyList()
        )
        assertEquals(StanPanelu.JeszczeNieWiem, v.stan)
        assertFalse(v.tytul.contains("normie", ignoreCase = true))
        assertTrue(v.niezmierzone != null)
    }

    @Test
    fun szescOdchylenDajeCzteryWierszeIDalsze() {
        val odczyty = mapOf(
            0x05 to 110.0,
            0x07 to 14.0,
            0x42 to 16.0,
            0x23 to 10.0,
            0x3C to 900.0,
            0x44 to 1.2
        )
        val v = FormatPaneluStan.widok(
            odczyty = odczyty,
            odczytane = odczyty.keys,
            status0103 = 2,
            kody = emptyList(),
            silnikRozgrzany = true,
            olejGotowy = true,
            wnioski = emptyList()
        )
        assertEquals(4, v.odchylenia.size)
        assertTrue(v.dalsze!!.contains("2"))
    }

    @Test
    fun wniosekKorektyDoslownieZReguly() {
        val wnioski = RuleEngine.evaluate(RuleInput(longTermFuelTrim = 14.0))
        val lean = wnioski.first { it.ruleId == "ltft_lean" }
        val v = FormatPaneluStan.widok(
            odczyty = mapOf(0x07 to 14.0),
            odczytane = setOf(0x07),
            status0103 = 2,
            kody = emptyList(),
            silnikRozgrzany = true,
            olejGotowy = true,
            wnioski = wnioski
        )
        assertTrue(v.odchylenia.any { it.zdanie == lean.tytul })
    }

    @Test
    fun kolejnoscUsterkaPrzedUwaga() {
        val wnioski = RuleEngine.evaluate(RuleInput(coolantCelsius = 110.0, longTermFuelTrim = 14.0, rpm = 800.0))
        val v = FormatPaneluStan.widok(
            odczyty = mapOf(0x07 to 14.0, 0x05 to 110.0),
            odczytane = setOf(0x07, 0x05),
            status0103 = 2,
            kody = emptyList(),
            silnikRozgrzany = true,
            olejGotowy = true,
            wnioski = wnioski
        )
        val wagi = v.odchylenia.map { it.waga }
        assertTrue(wagi.contains(WagaWniosku.Usterka))
        assertTrue(wagi.indexOf(WagaWniosku.Usterka) <= wagi.indexOf(WagaWniosku.Uwaga))
    }

    @Test
    fun panelStanNieWywolujeDzwieku() {
        val tekst = java.io.File("src/main/kotlin/pl/i40/android/ui/FormatPaneluStan.kt").readText()
        assertFalse(tekst.contains("pl.i40.android.alerts"))
        assertFalse(tekst.contains("AlertEngine"))
    }
}
