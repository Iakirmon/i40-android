package pl.i40.android.storage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KasowaniePrzejazdowTest {
    @Test
    fun wTokuOdrzuconyNieKasuje() {
        val mag = PamiecPrzejazdow()
        mag.wstaw(przejazd("nagrywany", StatusPrzejazdu.WToku))
        mag.wstaw(przejazd("gotowy", StatusPrzejazdu.Zamkniety))
        val wynik = mag.usunWiele(listOf("nagrywany"))
        assertTrue(wynik.doUsuniecia.isEmpty())
        assertEquals(listOf("nagrywany"), wynik.odrzuconoWToku)
        assertEquals(2, mag.lista().size)
        assertTrue(mag.czytaj("nagrywany") != null)
    }

    @Test
    fun chronionyDaSieSkasowacPojedynczo() {
        val mag = PamiecPrzejazdow()
        mag.wstaw(przejazd("a", StatusPrzejazdu.Zamkniety, chroniony = true))
        val wynik = mag.usunWiele(listOf("a"))
        assertEquals(listOf("a"), wynik.doUsuniecia)
        assertTrue(mag.czytaj("a") == null)
    }

    @Test
    fun punktyOdniesieniaPrzezywaja() {
        val przejazdy = PamiecPrzejazdow()
        val punkty = PamiecPunktowOdniesienia()
        przejazdy.wstaw(przejazd("sesja", StatusPrzejazdu.Zamkniety))
        punkty.zapisz(
            PunktOdniesienia(
                id = "pkt",
                kiedyMs = 1_500L,
                vin = "VIN",
                stan = "jalowy",
                zrodlo = "okno",
                probek = 20,
                odczyty = mapOf(0x0C to 700.0)
            )
        )
        val przed = punkty.dlaVin("VIN").size
        przejazdy.usunWiele(listOf("sesja"))
        assertEquals(przed, punkty.dlaVin("VIN").size)
        assertEquals(1, przed)
        assertTrue(przejazdy.czytaj("sesja") == null)
    }

    @Test
    fun vacuumPoWiecejNiz50() {
        val mag = PamiecPrzejazdow()
        val ids = (1..51).map { i ->
            val id = "p$i"
            mag.wstaw(przejazd(id, StatusPrzejazdu.Zamkniety))
            id
        }
        val wynik = mag.usunWiele(ids)
        assertEquals(51, wynik.doUsuniecia.size)
        assertTrue(wynik.wymagaVacuum)
        assertTrue(mag.wykonanoVacuum)
    }

    @Test
    fun vacuumNiePo50LubMniej() {
        val mag = PamiecPrzejazdow()
        mag.wstaw(przejazd("a", StatusPrzejazdu.Zamkniety))
        val wynik = mag.usunWiele(listOf("a"))
        assertFalse(wynik.wymagaVacuum)
        assertFalse(mag.wykonanoVacuum)
    }

    @Test
    fun usunNieRuszaTabeliPunktowAniPrzegladow() {
        val mag = PamiecPrzejazdow()
        mag.wstaw(przejazd("a", StatusPrzejazdu.Zamkniety))
        mag.usun("a")
        assertTrue(mag.czytaj("a") == null)
        assertEquals(0, mag.usunietychPunktow)
        assertEquals(0, mag.usunietychPrzegladow)
    }

    private fun przejazd(id: String, status: StatusPrzejazdu, chroniony: Boolean = false) = Przejazd(
        id = id,
        poczatekMs = 1_000L,
        koniecMs = 2_000L,
        status = status,
        vin = "VIN",
        notatka = "",
        podsumowanie = PodsumowaniePrzejazdu(czasTrwaniaS = 120.0, dystansKm = 1.0, liczbaProbek = 10),
        przebieg = TrackBlob(),
        checkpointMs = 2_000L,
        chroniony = chroniony
    )
}
