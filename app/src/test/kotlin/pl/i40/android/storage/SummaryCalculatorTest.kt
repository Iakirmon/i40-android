package pl.i40.android.storage

import kotlin.math.abs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SummaryCalculatorTest {
    @Test
    fun dystansZPredkosciTrapezami() {
        val distance = SummaryCalculator.integrateRateToQuantity(
            times = listOf(0f, 1f, 2f),
            values = listOf(36f, 36f, 36f)
        )
        assertTrue(distance != null && abs(distance - 0.02) < 1e-9)
    }

    @Test
    fun paliwoLNullGdyBrakSerii5e() {
        val track = TrackBlob()
        track.append(pid = 0x0D, time = 0f, value = 36f)
        track.append(pid = 0x0D, time = 1f, value = 36f)
        val summary = SummaryCalculator.make(
            from = track,
            durationSeconds = 1.0,
            kodyNaStarcie = emptyList(),
            kodyNaKoncu = emptyList()
        )
        assertNull(summary.paliwoL)
        assertTrue(summary.dystansKm != null && abs(summary.dystansKm!! - 0.01) < 1e-9)
    }

    @Test
    fun podsumowanieZTrackBlobGdyJest5e() {
        val track = TrackBlob()
        track.append(pid = 0x0C, time = 0f, value = 800f)
        track.append(pid = 0x0C, time = 1f, value = 1200f)
        track.append(pid = 0x0D, time = 0f, value = 36f)
        track.append(pid = 0x0D, time = 1f, value = 36f)
        track.append(pid = 0x05, time = 0f, value = 90f)
        track.append(pid = 0x42, time = 0f, value = 13.5f)
        track.append(pid = 0x42, time = 1f, value = 14.1f)
        track.append(pid = 0x5E, time = 0f, value = 2f)
        track.append(pid = 0x5E, time = 1f, value = 2f)
        val summary = SummaryCalculator.make(
            from = track,
            durationSeconds = 1.0,
            kodyNaStarcie = emptyList(),
            kodyNaKoncu = listOf("P0301")
        )
        assertEquals(1200.0, summary.maxObroty)
        assertEquals(1000.0, summary.srednieObroty)
        assertEquals(36.0, summary.maxPredkoscKmh)
        assertEquals(90.0, summary.maxPlynC)
        assertEquals(13.5, summary.minNapiecie)
        assertTrue(summary.maxNapiecie != null && abs(summary.maxNapiecie!! - 14.1) < 0.001)
        assertEquals(listOf("P0301"), summary.kodyNaKoncu)
        assertTrue(summary.paliwoL != null && abs(summary.paliwoL!! - 2.0 / 3600.0) < 1e-9)
        assertEquals(2.0, summary.sredniaHz)
    }

    @Test
    fun bezDwochPunktowBrakCalki() {
        assertNull(SummaryCalculator.integrateRateToQuantity(listOf(0f), listOf(50f)))
    }

    @Test
    fun maxCisnienieSzynyBarZSerii23WKpa() {
        val track = TrackBlob()
        track.append(pid = 0x23, time = 0f, value = 3840f)
        track.append(pid = 0x23, time = 1f, value = 14800f)
        val summary = make(track)
        assertTrue(summary.maxCisnienieSzynyBar != null && abs(summary.maxCisnienieSzynyBar!! - 148.0) < 1e-9)
    }

    @Test
    fun maxCisnienieSzynyBarNullGdyBrakSerii23() {
        val track = TrackBlob()
        track.append(pid = 0x0D, time = 0f, value = 0f)
        assertNull(make(track).maxCisnienieSzynyBar)
        assertNull(make(track).obciazeniePrzyMaxCisnieniu)
    }

    @Test
    fun obciazeniePrzyMaxCisnieniuPoCzasieNiePoIndeksie() {
        val track = TrackBlob()
        track.append(pid = 0x23, time = 0f, value = 1000f)
        track.append(pid = 0x23, time = 1f, value = 5000f)
        track.append(pid = 0x23, time = 2f, value = 2000f)
        track.append(pid = 0x43, time = 0f, value = 10f)
        track.append(pid = 0x43, time = 10f, value = 99f)
        track.append(pid = 0x43, time = 11f, value = 50f)
        val summary = make(track)
        assertTrue(summary.maxCisnienieSzynyBar != null && abs(summary.maxCisnienieSzynyBar!! - 50.0) < 1e-9)
        assertEquals(10.0, summary.obciazeniePrzyMaxCisnieniu)
    }

    @Test
    fun maxTempKatalizatoraCZserii3c() {
        val track = TrackBlob()
        track.append(pid = 0x3C, time = 0f, value = 400f)
        track.append(pid = 0x3C, time = 1f, value = 712f)
        assertEquals(712.0, make(track).maxTempKatalizatoraC)
        assertNull(make(TrackBlob()).maxTempKatalizatoraC)
    }

    @Test
    fun czasDo90CPierwszaProbkaPlynu() {
        val track = TrackBlob()
        track.append(pid = 0x05, time = 0f, value = 40f)
        track.append(pid = 0x05, time = 12.5f, value = 91f)
        track.append(pid = 0x05, time = 20f, value = 95f)
        assertEquals(12.5, make(track).czasDo90CSekundy)
        val zimny = TrackBlob()
        zimny.append(pid = 0x05, time = 0f, value = 40f)
        zimny.append(pid = 0x05, time = 30f, value = 80f)
        assertNull(make(zimny).czasDo90CSekundy)
        assertNull(make(TrackBlob()).czasDo90CSekundy)
    }

    @Test
    fun czasPozaPasmemKorektToSumaOdstepowNieLiczbaProbek() {
        val track = TrackBlob()
        track.append(pid = 0x06, time = 0f, value = 0f)
        track.append(pid = 0x07, time = 0f, value = 0f)
        track.append(pid = 0x03, time = 0f, value = 2f)
        track.append(pid = 0x06, time = 1f, value = 15f)
        track.append(pid = 0x07, time = 1f, value = 10f)
        track.append(pid = 0x03, time = 1f, value = 2f)
        track.append(pid = 0x06, time = 10f, value = 15f)
        track.append(pid = 0x07, time = 10f, value = 10f)
        track.append(pid = 0x03, time = 10f, value = 2f)
        assertEquals(9.0, make(track).czasPozaPasmemWPetliZamknietejSekundy)
        assertEquals(10.0, make(track).czasWPetliZamknietejSekundy)
        val wPasmie = TrackBlob()
        wPasmie.append(pid = 0x06, time = 0f, value = 1f)
        wPasmie.append(pid = 0x07, time = 0f, value = 1f)
        wPasmie.append(pid = 0x03, time = 0f, value = 2f)
        wPasmie.append(pid = 0x06, time = 5f, value = 2f)
        wPasmie.append(pid = 0x07, time = 5f, value = 2f)
        wPasmie.append(pid = 0x03, time = 5f, value = 2f)
        assertEquals(0.0, make(wPasmie).czasPozaPasmemWPetliZamknietejSekundy)
        assertEquals(5.0, make(wPasmie).czasWPetliZamknietejSekundy)
        assertNull(make(TrackBlob()).czasPozaPasmemWPetliZamknietejSekundy)
        assertNull(make(TrackBlob()).czasWPetliZamknietejSekundy)
    }

    @Test
    fun licznikLiczyWylacznieWPetliZamknietejLicznikIMianownik() {
        val track = TrackBlob()
        track.append(pid = 0x06, time = 0f, value = 15f)
        track.append(pid = 0x07, time = 0f, value = 10f)
        track.append(pid = 0x03, time = 0f, value = 2f)
        track.append(pid = 0x06, time = 5f, value = 15f)
        track.append(pid = 0x07, time = 5f, value = 10f)
        track.append(pid = 0x03, time = 5f, value = 1f)
        track.append(pid = 0x06, time = 10f, value = 1f)
        track.append(pid = 0x07, time = 10f, value = 1f)
        track.append(pid = 0x03, time = 10f, value = 2f)
        track.append(pid = 0x06, time = 15f, value = 1f)
        track.append(pid = 0x07, time = 15f, value = 1f)
        track.append(pid = 0x03, time = 15f, value = 16f)
        val s = make(track)
        assertEquals(5.0, s.czasPozaPasmemWPetliZamknietejSekundy)
        assertEquals(10.0, s.czasWPetliZamknietejSekundy)
    }

    private fun make(track: TrackBlob) = SummaryCalculator.make(
        from = track,
        durationSeconds = 10.0,
        kodyNaStarcie = emptyList(),
        kodyNaKoncu = emptyList()
    )
}
