package pl.i40.android.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.i40.android.acquisition.SampleStream
import pl.i40.android.storage.PamiecPunktowOdniesienia
import pl.i40.android.storage.PunktOdniesienia
import pl.i40.android.storage.SummaryCalculator

class ZbieraczPunktowTest {
    @Test
    fun oknoKrotszeNizObiegWolnegoNieTworzyPunktu() {
        val z = ZbieraczPunktow(terazMs = { 1_000L }, nowyId = { "p1" })
        repeat(SampleStream.SLOW_EVERY_N - 1) {
            assertNull(z.naCyklGoracy(true, "VIN1", mapOf(0x0C to 750.0)))
        }
        assertNull(z.naCyklGoracy(false, "VIN1", mapOf(0x0C to 750.0)))
    }

    @Test
    fun medianaNaOknieZWyskokiemRozniSieOdSredniej() {
        val z = ZbieraczPunktow(terazMs = { 2_000L }, nowyId = { "p2" })
        val wartosci = MutableList(SampleStream.SLOW_EVERY_N) { 750.0 }
        wartosci[5] = 4000.0
        repeat(SampleStream.SLOW_EVERY_N) { i ->
            z.naCyklGoracy(true, "VIN1", mapOf(0x0C to wartosci[i]))
        }
        val punkt = z.naCyklGoracy(false, "VIN1", mapOf(0x0C to 750.0))
        assertNotNull(punkt)
        val mediana = punkt!!.odczyty[0x0C]!!
        val srednia = wartosci.average()
        assertEquals(750.0, mediana, 1e-9)
        assertTrue(kotlin.math.abs(mediana - srednia) > 10.0)
        assertEquals(SampleStream.SLOW_EVERY_N, punkt.probek)
        assertEquals(ZbieraczPunktow.STAN_JALOWY_ROZGRZANY, punkt.stan)
        assertEquals(ZbieraczPunktow.ZRODLO_PRZEJAZD, punkt.zrodlo)
    }

    @Test
    fun brakVinuNieTworzyPunktu() {
        val z = ZbieraczPunktow(terazMs = { 3_000L }, nowyId = { "p3" })
        repeat(SampleStream.SLOW_EVERY_N) {
            z.naCyklGoracy(true, null, mapOf(0x0C to 750.0))
        }
        assertNull(z.naCyklGoracy(false, null, mapOf(0x0C to 750.0)))
    }

    @Test
    fun zakonczSesjeZapisujePunktPoPelnychObiegu() {
        val z = ZbieraczPunktow(terazMs = { 4_000L }, nowyId = { "p4" })
        repeat(SampleStream.SLOW_EVERY_N) {
            z.naCyklGoracy(true, "VIN1", mapOf(0x0C to 750.0, 0x1F to 700.0))
        }
        val punkt = z.zakonczSesje("VIN1")
        assertNotNull(punkt)
        assertEquals(750.0, punkt!!.odczyty[0x0C])
        assertEquals(700.0, punkt.odczyty[0x1F])
    }

    @Test
    fun dwaVinNieMieszajaSieWMagazynie() {
        val mag = PamiecPunktowOdniesienia()
        mag.zapisz(punkt("a", "VIN-A", 10.0))
        mag.zapisz(punkt("b", "VIN-B", 99.0))
        val a = mag.dlaVin("VIN-A")
        assertEquals(1, a.size)
        assertEquals(10.0, a[0].odczyty[0x0C])
        assertTrue(mag.dlaVin("VIN-B").none { it.vin == "VIN-A" })
    }

    private fun punkt(id: String, vin: String, rpm: Double) = PunktOdniesienia(
        id = id,
        kiedyMs = 1L,
        vin = vin,
        stan = ZbieraczPunktow.STAN_JALOWY_ROZGRZANY,
        zrodlo = ZbieraczPunktow.ZRODLO_PRZEJAZD,
        probek = 20,
        odczyty = mapOf(0x0C to rpm)
    )
}

class OdniesienieNieDokladaZapytanTest {
    @Test
    fun skladyPetliIdentyczneZeStanemAktualnym() {
        assertEquals(listOf(0x0D, 0x05, 0x04), SampleStream.REQUIRED_HOT_PIDS)
        assertEquals(listOf(0x23, 0x0B, 0x11, 0x4C, 0x49, 0x43), SampleStream.DEFAULT_FAST_PIDS)
        assertEquals(listOf(0x3C, 0x44, 0x2E, 0x03, 0x07, 0x42), SampleStream.DEFAULT_MEDIUM_PIDS)
        assertEquals(listOf(0x1F, 0x46, 0x0F, 0x33), SampleStream.DEFAULT_SLOW_PIDS)
        assertEquals(6, SampleStream.composeHotPids(SampleStream.DEFAULT_CHART_SLOTS).size)
        assertTrue(0x2F !in SampleStream.DEFAULT_SLOW_PIDS)
        var queries = 0
        for (n in 1..200_000) queries += PetlaFaz.liczbaZapytan(n)
        val naSekundePrzy4Hz = queries * 4.0 / 200_000.0
        assertEquals(5.62, naSekundePrzy4Hz, 0.01)
    }

    @Test
    fun medianaZbieraczaToMedianaPodsumowania() {
        val wyskok = listOf(3f, 4f, 4f, 5f, 100f)
        assertEquals(4.0, SummaryCalculator.mediana(wyskok))
    }
}
