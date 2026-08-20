package pl.i40.android.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.i40.android.storage.KryteriumPorzadkow
import pl.i40.android.storage.PodsumowaniePrzejazdu
import pl.i40.android.storage.Przejazd
import pl.i40.android.storage.StatusPrzejazdu
import pl.i40.android.storage.TrackBlob
import pl.i40.android.storage.WynikKasowania

class FormatPorzadkowTest {
    @Test
    fun bezKryteriumPrzyciskNieaktywnyListaPusta() {
        val v = FormatPorzadkow.widok(listOf(przejazd("a", StatusPrzejazdu.Zamkniety, 30.0)), null, 10_000L)
        assertFalse(v.przyciskAktywny)
        assertTrue(v.lista.isEmpty())
        assertEquals("0 przejazdów · 0,0 MB", v.wierszUsuniecia)
        assertTrue(v.wierszChronione.contains("0"))
        assertTrue(v.wierszWToku.contains("0"))
        val tekst = listOf(v.zajete, v.wierszUsuniecia, v.przycisk).joinToString(" ")
        assertFalse(tekst.contains("zalecane", ignoreCase = true))
    }

    @Test
    fun chronionySpelniajacyJestPominietyIPoliczony() {
        val v = FormatPorzadkow.widok(
            listOf(przejazd("a", StatusPrzejazdu.Zamkniety, 60.0, chroniony = true)),
            KryteriumPorzadkow.KrotszeNiz(5),
            10_000L
        )
        assertTrue(v.lista.isEmpty())
        assertFalse(v.przyciskAktywny)
        assertTrue(v.wierszChronione.contains("1"))
        assertTrue(v.wierszChronione.contains("pominięte") || v.wierszChronione.contains("pominięty"))
    }

    @Test
    fun wTokuPominietaIPoliczona() {
        val v = FormatPorzadkow.widok(
            listOf(przejazd("b", StatusPrzejazdu.WToku, 30.0)),
            KryteriumPorzadkow.KrotszeNiz(5),
            10_000L
        )
        assertTrue(v.lista.isEmpty())
        assertTrue(v.wierszWToku.contains("1"))
        assertTrue(v.wierszWToku.contains("pominięta") || v.wierszWToku.contains("pominięte"))
    }

    @Test
    fun sumaBajtowZgadzaSieZSumaWierszy() {
        val a = przejazd("a", StatusPrzejazdu.Zamkniety, 120.0)
        val b = przejazd("b", StatusPrzejazdu.Zamkniety, 180.0)
        val suma = ZajeteMiejsce.bajty(a) + ZajeteMiejsce.bajty(b)
        assertEquals(suma, ZajeteMiejsce.suma(listOf(a, b)))
        assertTrue(suma > 0)
    }

    @Test
    fun vacuumPoWiecejNiz50() {
        assertEquals(50, WynikKasowania.PROG_VACUUM)
        val mag = pl.i40.android.storage.PamiecPrzejazdow()
        val ids = (1..51).map { i ->
            mag.wstaw(przejazd("p$i", StatusPrzejazdu.Zamkniety, 30.0))
            "p$i"
        }
        val wynik = mag.usunWiele(ids)
        assertTrue(wynik.wymagaVacuum)
        assertTrue(mag.wykonanoVacuum)
    }

    @Test
    fun minutyIMiesiaceNieSaZalecane() {
        val etykiety = FormatPorzadkow.MINUTY.map { "$it min" } + FormatPorzadkow.MIESIACE.map { "$it mies." }
        for (e in etykiety) {
            assertFalse(e.contains("zalec"))
        }
        assertEquals(listOf(2, 5, 10, 15), FormatPorzadkow.MINUTY)
        assertEquals(listOf(3, 6, 12), FormatPorzadkow.MIESIACE)
    }

    private fun przejazd(id: String, status: StatusPrzejazdu, czasS: Double, chroniony: Boolean = false): Przejazd {
        val blob = TrackBlob()
        blob.append(0x0D, 0f, 10f)
        return Przejazd(
            id = id,
            poczatekMs = 1_000L,
            koniecMs = 2_000L,
            status = status,
            vin = "VIN",
            notatka = "",
            podsumowanie = PodsumowaniePrzejazdu(czasTrwaniaS = czasS, dystansKm = 1.0),
            przebieg = blob,
            checkpointMs = 2_000L,
            chroniony = chroniony
        )
    }
}
