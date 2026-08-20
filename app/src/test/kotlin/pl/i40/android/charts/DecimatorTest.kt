package pl.i40.android.charts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.i40.android.storage.TrackBlob

class DecimatorTest {
    @Test
    fun pustyWejscieDajePustaListe() {
        assertTrue(Decimator.decimate(times = emptyList(), values = emptyList(), buckets = 10).isEmpty())
    }

    @Test
    fun pomijaPusteKoszyki() {
        val times = listOf(0f, 0.1f, 9.9f, 10f)
        val values = listOf(1f, 2f, 3f, 4f)
        val out = Decimator.decimate(times, values, buckets = 10, timeRange = 0f..10f)
        assertTrue(out.isNotEmpty())
        assertTrue(out.size < 20)
        assertTrue(out.first().time == 0f || out.first().time == 0.1f)
    }

    @Test
    fun zachowujeSkokKtorySredniaGubi() {
        val times = MutableList(100) { it.toFloat() }
        val values = MutableList(100) { i -> if (i == 50) 100f else 10f }
        val decimated = Decimator.decimate(times, values, buckets = 10)
        val maxKept = decimated.maxOf { it.value }
        assertEquals(100f, maxKept)

        val slice = values.subList(50, 60)
        val avg = slice.sum() / slice.size
        assertTrue(avg < 30f)
        assertTrue(maxKept > avg)
    }

    @Test
    fun minIMaxWKolejnosciCzasowej() {
        val out = Decimator.decimate(
            times = listOf(0f, 0.2f, 0.8f),
            values = listOf(5f, 50f, 1f),
            buckets = 1,
            timeRange = 0f..1f
        )
        assertEquals(2, out.size)
        assertTrue(out[0].time <= out[1].time)
        assertEquals(setOf(1f, 50f), out.map { it.value }.toSet())
    }

    @Test
    fun jedenPunktWKoszykuToJedenPunkt() {
        val out = Decimator.decimate(listOf(1f), listOf(42f), buckets = 5, timeRange = 0f..10f)
        assertEquals(listOf(DecimatedPoint(1f, 42f)), out)
    }

    @Test
    fun seriaTrackBlob() {
        val blob = TrackBlob()
        for (i in 0 until 20) {
            blob.append(0x0C, i.toFloat(), 800f + i * 10)
        }
        val out = Decimator.decimate(blob.series[0], buckets = 4)
        assertTrue(out.isNotEmpty())
        assertTrue(out.size <= 8)
    }
}
