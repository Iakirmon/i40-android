package pl.i40.android.obd

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FuelSystemStatusTest {
    @Test
    fun szescWartosciZTabeli4() {
        assertEquals(StanPetliPaliwowej.Wylaczony, FuelSystemStatus.stan(0))
        assertEquals(StanPetliPaliwowej.OtwartaZimny, FuelSystemStatus.stan(1))
        assertEquals(StanPetliPaliwowej.Zamknieta, FuelSystemStatus.stan(2))
        assertEquals(StanPetliPaliwowej.OtwartaObciazenie, FuelSystemStatus.stan(4))
        assertEquals(StanPetliPaliwowej.OtwartaAwaria, FuelSystemStatus.stan(8))
        assertEquals(StanPetliPaliwowej.ZamknietaAwariaSondy, FuelSystemStatus.stan(16))
        assertTrue(FuelSystemStatus.opis(2).contains("zamknięta", ignoreCase = true))
        assertTrue(FuelSystemStatus.opis(16).contains("zamknięta", ignoreCase = true))
        assertTrue(FuelSystemStatus.opis(8).contains("awaria", ignoreCase = true))
    }

    @Test
    fun pozaEnumeracjaNieznanyZSurowaLiczba() {
        assertEquals(StanPetliPaliwowej.Nieznany, FuelSystemStatus.stan(3))
        assertTrue(FuelSystemStatus.opis(3).contains("nieznany", ignoreCase = true))
        assertTrue(FuelSystemStatus.opis(3).contains("3"))
        assertEquals(StanPetliPaliwowej.Nieznany, FuelSystemStatus.stan(7))
        assertFalse(FuelSystemStatus.opis(7).contains("zamknięta", ignoreCase = true))
    }

    @Test
    fun szesnascieToPetlaZamknieta() {
        assertTrue(FuelSystemStatus.korektyWazne(2))
        assertTrue(FuelSystemStatus.korektyWazne(16))
        assertFalse(FuelSystemStatus.korektyWazne(1))
        assertFalse(FuelSystemStatus.korektyWazne(4))
        assertFalse(FuelSystemStatus.korektyWazne(8))
        assertFalse(FuelSystemStatus.korektyWazne(0))
        assertFalse(FuelSystemStatus.korektyWazne(null))
        assertFalse(FuelSystemStatus.korektyWazne(3))
    }

    @Test
    fun decodeCzytaBajtA() {
        val s = FuelSystemStatus.decode(listOf(2, 0))!!
        assertEquals(2, s.bajtA)
        assertEquals(0, s.bajtB)
        assertEquals(StanPetliPaliwowej.Zamknieta, s.stan)
    }
}
