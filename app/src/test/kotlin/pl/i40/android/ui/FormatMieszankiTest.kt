package pl.i40.android.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.i40.android.acquisition.RingSample
import pl.i40.android.rules.PasmaOdniesienia

class FormatMieszankiTest {
    @Test
    fun sumaKorektToSkladnikiANieSamaDluga() {
        assertEquals(3.1, FormatMieszanki.suma(-0.8, 3.9)!!, 1e-9)
        assertEquals(null, FormatMieszanki.suma(null, 3.9))
        assertEquals("±20", FormatMieszanki.normaSumy())
        assertEquals(PasmaOdniesienia.sumaKorekt.start, FormatMieszanki.linieSumy()[0])
        assertEquals(PasmaOdniesienia.sumaKorekt.endInclusive, FormatMieszanki.linieSumy()[1])
    }

    @Test
    fun zaKatToPauzaZPowodemNigdyZero() {
        val text = FormatMieszanki.zaKatWartosc()
        assertEquals(FormatPomiaru.NIEDOSTEPNE, text)
        assertFalse(text.contains("0"))
        assertTrue(FormatMieszanki.zaKatPowod().isNotBlank())
        assertTrue(FormatMieszanki.zaKatPowod().contains("formuły") || FormatMieszanki.zaKatPowod().contains("formuly"))
        assertEquals(FormatPomiaru.NIEDOSTEPNE, FormatMieszanki.normaKrotkiej())
    }

    @Test
    fun sumaProbekPoCzasieGdyGestosciSieRoza() {
        val stft = listOf(RingSample(0.0, 1.0), RingSample(1.0, 2.0), RingSample(2.0, 3.0))
        val ltft = listOf(RingSample(0.0, 10.0), RingSample(2.0, 20.0))
        val suma = FormatMieszanki.sumaProbek(stft, ltft)
        assertEquals(11.0, suma[0].value)
        assertEquals(12.0, suma[1].value)
        assertEquals(23.0, suma[2].value)
    }

    @Test
    fun pozaPasmemWierszDzieliPrzezCzasWPetliZamknietej() {
        val text = FormatMieszanki.pozaPasmemWiersz(30.0, 600.0)
        assertTrue(text.contains("0:30"))
        assertTrue(text.contains("10:00"))
        assertTrue(text.contains("pętli zamkniętej"))
        assertFalse(text.contains("40:00"))
    }
}
