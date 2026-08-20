package pl.i40.android.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.i40.android.acquisition.SampleStream

class StanParametruTest {
    @Test
    fun nullNigdyNieJestPonizej() {
        val s = StanParametru.ocen(0x05, wartosc = null, odczytanoWTejSesji = false, warunkiWaznosciSpelnione = true)
        assertEquals(StanParametru.NieZmierzony, s)
        assertTrue(s != StanParametru.Ponizej)
    }

    @Test
    fun pasmoBrakNieLiczySieDoWNormie() {
        val s = StanParametru.ocen(0x0E, wartosc = 12.0, odczytanoWTejSesji = true, warunkiWaznosciSpelnione = true)
        assertEquals(StanParametru.BezPasma, s)
        val panel = StanPanelu.zloz(listOf(s, StanParametru.WNormie))
        assertEquals(StanPanelu.JeszczeNieWiem, panel)
        assertTrue(panel != StanPanelu.WNormie)
    }

    @Test
    fun jedenOdczytWSesjiToJeszczeNieWiemNigdyWNormie() {
        val stany = listOf(
            StanParametru.ocen(0x05, 90.0, odczytanoWTejSesji = true, warunkiWaznosciSpelnione = true),
            StanParametru.ocen(0x42, null, odczytanoWTejSesji = false, warunkiWaznosciSpelnione = true)
        )
        assertEquals(StanPanelu.JeszczeNieWiem, StanPanelu.zloz(stany))
        assertTrue(StanPanelu.zloz(stany) != StanPanelu.WNormie)
    }

    @Test
    fun odchylenieMaPierwszenstwoNadNieodczytanym() {
        val stany = listOf(
            StanParametru.ocen(0x05, 110.0, odczytanoWTejSesji = true, warunkiWaznosciSpelnione = true),
            StanParametru.ocen(0x42, null, odczytanoWTejSesji = false, warunkiWaznosciSpelnione = true)
        )
        assertEquals(StanPanelu.Odchylenia, StanPanelu.zloz(stany))
    }

    @Test
    fun mapaProwadziDoIstniejacegoCelu() {
        val mapa = mapOf(
            0x0C to CelSkrotu.Podstawowy,
            0x04 to CelSkrotu.Podstawowy,
            0x0E to CelSkrotu.Podstawowy,
            0x06 to CelSkrotu.Mieszanka,
            0x07 to CelSkrotu.Mieszanka,
            0x03 to CelSkrotu.Mieszanka,
            0x44 to CelSkrotu.Mieszanka,
            0x23 to CelSkrotu.WtryskGdi,
            0x43 to CelSkrotu.WtryskGdi,
            0x11 to CelSkrotu.WtryskGdi,
            0x3C to CelSkrotu.Termika,
            0x05 to CelSkrotu.Termika,
            0x5C to CelSkrotu.Termika,
            0x0F to CelSkrotu.Termika,
            0x46 to CelSkrotu.Termika,
            0x0B to CelSkrotu.Powietrze,
            0x33 to CelSkrotu.Powietrze,
            0x4C to CelSkrotu.Powietrze,
            0x49 to CelSkrotu.Powietrze,
            0x42 to CelSkrotu.PrzegladOdczyty
        )
        for ((pid, cel) in mapa) {
            assertEquals(cel, CelSkrotu.dla(pid), "%02X".format(pid))
        }
        assertEquals(CelSkrotu.PrzegladOdczyty, CelSkrotu.dla(0x1F))
    }

    @Test
    fun korektaPrzyPetliOtwartejJestNiewaznaTeraz() {
        for (st in listOf(1, 4, 8, 0)) {
            val wazne = WarunkiWaznosci.spelnione(0x07, st)
            assertFalse(wazne)
            val s = StanParametru.ocen(0x07, 14.0, true, wazne)
            assertEquals(StanParametru.NiewaznyTeraz, s)
            assertTrue(s != StanParametru.Powyzej)
        }
    }

    @Test
    fun korektaBezOdczytu0103JestNiewaznaTeraz() {
        val wazne = WarunkiWaznosci.spelnione(0x07, null)
        assertFalse(wazne)
        val s = StanParametru.ocen(0x07, 14.0, true, wazne)
        assertEquals(StanParametru.NiewaznyTeraz, s)
        assertTrue(s != StanParametru.Powyzej)
        assertTrue(FormatKafla.wartoscKorektyDlugiej(14.0, null).contains("○"))
    }

    @Test
    fun korektaWPetliZamknietejOceniaPasmo() {
        for (st in listOf(2, 16)) {
            val wazne = WarunkiWaznosci.spelnione(0x07, st)
            assertTrue(wazne)
            val s = StanParametru.ocen(0x07, 14.0, true, wazne)
            assertEquals(StanParametru.Powyzej, s)
            assertFalse(FormatKafla.wartoscKorektyDlugiej(14.0, st).contains("○"))
        }
    }

    @Test
    fun kafelIPanelMilczaAlboMowiaRazem() {
        val momentMilczy = 1
        val kafel = FormatKafla.wartoscKorektyDlugiej(14.0, momentMilczy)
        val panel = StanParametru.ocen(0x07, 14.0, true, WarunkiWaznosci.spelnione(0x07, momentMilczy))
        assertTrue(kafel.contains("○"))
        assertEquals(StanParametru.NiewaznyTeraz, panel)
        val momentMowi = 2
        val kafel2 = FormatKafla.wartoscKorektyDlugiej(14.0, momentMowi)
        val panel2 = StanParametru.ocen(0x07, 14.0, true, WarunkiWaznosci.spelnione(0x07, momentMowi))
        assertFalse(kafel2.contains("○"))
        assertEquals(StanParametru.Powyzej, panel2)
    }

    @Test
    fun objasnieniaNieDokladajaZapytanObd() {
        assertEquals(listOf(0x0D, 0x05, 0x04), SampleStream.REQUIRED_HOT_PIDS)
        assertTrue(0x2F !in SampleStream.DEFAULT_SLOW_PIDS)
    }
}
