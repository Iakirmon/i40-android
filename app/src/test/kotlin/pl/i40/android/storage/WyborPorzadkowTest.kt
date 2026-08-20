package pl.i40.android.storage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.i40.android.acquisition.SampleStream

class MigracjaSchematuTest {
    @Test
    fun zWersji1WykonujeWszystkieKrokiNieTylkoPierwszy() {
        val kroki = MigracjaSchematu.kroki(1)
        assertEquals(listOf("punkt_odniesienia", "przeglad", "chroniony"), kroki)
        assertTrue(kroki.contains("chroniony"))
        assertEquals(3, kroki.size)
    }

    @Test
    fun zWersji2NieTworzyPunktuPonownie() {
        val kroki = MigracjaSchematu.kroki(2)
        assertFalse(kroki.contains("punkt_odniesienia"))
        assertEquals(listOf("przeglad", "chroniony"), kroki)
    }

    @Test
    fun zWersji3TylkoChroniony() {
        assertEquals(listOf("chroniony"), MigracjaSchematu.kroki(3))
    }

    @Test
    fun historiaNieDokladaZapytanObd() {
        assertEquals(listOf(0x0D, 0x05, 0x04), SampleStream.REQUIRED_HOT_PIDS)
        assertEquals(listOf(0x1F, 0x46, 0x0F, 0x33), SampleStream.DEFAULT_SLOW_PIDS)
        assertTrue(0x2F !in SampleStream.DEFAULT_SLOW_PIDS)
    }
}

class WyborPorzadkowTest {
    @Test
    fun chronionySpelniajacyKryteriumJestPomijanyILiczone() {
        val krotki = przejazd("a", StatusPrzejazdu.Zamkniety, 60.0, chroniony = true)
        val wynik = WyborPorzadkow.ktoreDoUsuniecia(
            listOf(krotki),
            KryteriumPorzadkow.KrotszeNiz(5),
            terazMs = 10_000L
        )
        assertTrue(wynik.doUsuniecia.isEmpty())
        assertEquals(1, wynik.pominietoChronione)
        assertEquals(0, wynik.pominietoWToku)
    }

    @Test
    fun wTokuJestPomijanyILiczone() {
        val wToku = przejazd("b", StatusPrzejazdu.WToku, 30.0)
        val wynik = WyborPorzadkow.ktoreDoUsuniecia(
            listOf(wToku),
            KryteriumPorzadkow.KrotszeNiz(5),
            terazMs = 10_000L
        )
        assertTrue(wynik.doUsuniecia.isEmpty())
        assertEquals(1, wynik.pominietoWToku)
        assertEquals(0, wynik.pominietoChronione)
    }

    @Test
    fun pustyZbiorDajeZeraNieWyjatek() {
        val wynik = WyborPorzadkow.ktoreDoUsuniecia(
            emptyList(),
            KryteriumPorzadkow.Przerwane,
            terazMs = 1L
        )
        assertTrue(wynik.doUsuniecia.isEmpty())
        assertEquals(0, wynik.pominietoChronione)
        assertEquals(0, wynik.pominietoWToku)
    }

    @Test
    fun krotszeNizWybieraSpelniajace() {
        val krotki = przejazd("k", StatusPrzejazdu.Zamkniety, 120.0)
        val dlugi = przejazd("d", StatusPrzejazdu.Zamkniety, 600.0)
        val wynik = WyborPorzadkow.ktoreDoUsuniecia(
            listOf(krotki, dlugi),
            KryteriumPorzadkow.KrotszeNiz(5),
            terazMs = 1L
        )
        assertEquals(listOf("k"), wynik.doUsuniecia)
    }

    private fun przejazd(id: String, status: StatusPrzejazdu, czasS: Double, chroniony: Boolean = false) = Przejazd(
        id = id,
        poczatekMs = 1_000L,
        koniecMs = 2_000L,
        status = status,
        vin = "VIN",
        notatka = "",
        podsumowanie = PodsumowaniePrzejazdu(czasTrwaniaS = czasS),
        przebieg = TrackBlob(),
        checkpointMs = 2_000L,
        chroniony = chroniony
    )
}
