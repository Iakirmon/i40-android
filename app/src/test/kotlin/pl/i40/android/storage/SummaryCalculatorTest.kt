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
}
