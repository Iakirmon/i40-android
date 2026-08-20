package pl.i40.android.storage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TrackBlobTest {
    @Test
    fun roundTripZachowujeDane() {
        val blob = TrackBlob()
        blob.append(pid = 0x0C, time = 0.0f, value = 800f)
        blob.append(pid = 0x0D, time = 0.0f, value = 0f)
        blob.append(pid = 0x0C, time = 0.25f, value = 925.5f)
        blob.append(pid = 0x05, time = 1.0f, value = 92f)

        val decoded = TrackBlob.decode(blob.encode())
        assertEquals(TrackBlob.CURRENT_VERSION, decoded.version)
        assertEquals(blob, decoded)
        assertEquals(listOf(0x0C, 0x0D, 0x05), decoded.series.map { it.pid })
        assertEquals(listOf(0.0f, 0.25f), decoded.series[0].times)
        assertEquals(listOf(800f, 925.5f), decoded.series[0].values)
        assertEquals(1, decoded.series[2].times.size)
    }

    @Test
    fun serieMogaMiecRozneDlugosci() {
        val blob = TrackBlob()
        for (i in 0 until 10) {
            blob.append(pid = 0x0C, time = i * 0.25f, value = (800 + i).toFloat())
        }
        blob.append(pid = 0x42, time = 0.0f, value = 14.1f)
        blob.append(pid = 0x42, time = 2.5f, value = 13.9f)
        val decoded = TrackBlob.decode(blob.encode())
        assertEquals(10, decoded.series.first { it.pid == 0x0C }.times.size)
        assertEquals(2, decoded.series.first { it.pid == 0x42 }.times.size)
    }

    @Test
    fun pustyBlobRoundTrip() {
        val decoded = TrackBlob.decode(TrackBlob().encode())
        assertEquals(1, decoded.version)
        assertTrue(decoded.series.isEmpty())
    }

    @Test
    fun encodeZaczynaSieOdMagiiI40T() {
        val blob = TrackBlob()
        blob.append(pid = 0x0C, time = 1f, value = 1f)
        val magic = blob.encode().copyOfRange(0, 4).toString(Charsets.US_ASCII)
        assertEquals("I40T", magic)
    }
}
