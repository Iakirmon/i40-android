package pl.i40.android.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.i40.android.acquisition.RingSample

class ChartYAxisTest {
    @Test
    fun sztywneZakresyZTabeli107() {
        assertEquals(0.0..7000.0, OsY.zakres(0x0C))
        assertEquals(0.0..200.0, OsY.zakres(0x0D))
        assertEquals(0.0..100.0, OsY.zakres(0x04))
        assertEquals(-10.0..50.0, OsY.zakres(0x0E))
        assertEquals(-25.0..25.0, OsY.zakres(0x06))
        assertEquals(-25.0..25.0, OsY.zakres(0x07))
        assertEquals(0.0..150.0, OsY.zakres(0x10))
        assertEquals(0.0..130.0, OsY.zakres(0x05))
        assertEquals(0.0..150.0, OsY.zakres(0x5C))
    }

    @Test
    fun clampPrzycinaBezRozciaganiaOsi() {
        val high = OsY.przytnij(9000.0, 0x0C)
        assertEquals(7000.0, high.wartosc)
        assertTrue(high.przyciete)

        val low = OsY.przytnij(-20.0, 0x0E)
        assertEquals(-10.0, low.wartosc)
        assertTrue(low.przyciete)

        val ok = OsY.przytnij(2000.0, 0x0C)
        assertEquals(2000.0, ok.wartosc)
        assertFalse(ok.przyciete)
    }

    @Test
    fun zakresNieZalezyOdProbek() {
        val a = OsY.zakres(0x0C)
        val b = OsY.zakres(0x0C)
        assertEquals(a, b)
        assertEquals(0.0, a.start)
        assertEquals(7000.0, a.endInclusive)
    }

    @Test
    fun domenaCzasuToOstatnie60s() {
        val samples = listOf(RingSample(10.0, 1.0), RingSample(100.0, 2.0))
        val domain = OsY.domenaCzasu(samples)
        assertEquals(40.0, domain.start)
        assertEquals(100.0, domain.endInclusive)
    }

    @Test
    fun etykietaZakresuOsi() {
        assertEquals("0…7000", OsY.etykietaZakresu(0x0C))
    }
}
