package pl.i40.android.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.i40.android.acquisition.RingSample
import pl.i40.android.rules.PasmaOdniesienia

class FormatGdiTest {
    @Test
    fun osYSzynyTo240BarZPasmaObciazeniowego() {
        assertEquals(0.0..240.0, OsY.zakres(0x23))
        val przyciete = OsY.przytnij(250.0, 0x23)
        assertEquals(240.0, przyciete.wartosc)
        assertTrue(przyciete.przyciete)
        val wOsi = OsY.przytnij(148.0, 0x23)
        assertEquals(148.0, wOsi.wartosc)
        assertFalse(wOsi.przyciete)
    }

    @Test
    fun linieSzynyToTeSameStaleCoGdi1() {
        val linie = FormatGdi.linieSzyny()
        assertEquals(
            listOf(
                PasmaOdniesienia.szynaJalowy.start,
                PasmaOdniesienia.szynaJalowy.endInclusive,
                PasmaOdniesienia.szynaObciazenie.start,
                PasmaOdniesienia.szynaObciazenie.endInclusive
            ),
            linie
        )
        assertEquals(4, linie.size)
        assertEquals(
            PasmaOdniesienia.szynaJalowy.start - PasmaOdniesienia.ODCHYLENIE_JALOWE_BAR,
            PasmaOdniesienia.progGdi1Bar
        )
        assertTrue(FormatGdi.linieObciazenia().isEmpty())
        assertTrue(FormatGdi.liniePrzepustnicy().isEmpty())
    }

    @Test
    fun maxWierszPokazujeObciazenie() {
        val rail = listOf(
            RingSample(0.0, 1000.0),
            RingSample(1.0, 14800.0),
            RingSample(2.0, 2000.0)
        )
        val load = listOf(
            RingSample(0.0, 10.0),
            RingSample(10.0, 99.0),
            RingSample(11.0, 50.0)
        )
        val (bar, obc) = FormatGdi.szczytSesji(rail, load)
        assertEquals(148.0, bar!!, 1e-9)
        assertEquals(10.0, obc)
        val wiersz = FormatGdi.maxWiersz(bar, obc)
        assertTrue(wiersz.contains("148"))
        assertTrue(wiersz.contains("10"))
        assertTrue(wiersz.contains("obciąż") || wiersz.contains("obciaz"))
        assertEquals("Max w sesji: ${FormatPomiaru.NIEDOSTEPNE}", FormatGdi.maxWiersz(null, null))
    }
}
