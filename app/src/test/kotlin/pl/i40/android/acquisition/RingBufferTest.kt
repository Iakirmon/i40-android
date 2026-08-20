package pl.i40.android.acquisition

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.i40.android.obd.DecodedPid
import pl.i40.android.obd.MultiPidReading

class RingBufferTest {
    @Test
    fun zachowujeKolejnoscPrzedZapelnieniem() {
        val buf = RingBuffer(capacity = 4)
        buf.append(time = 1.0, value = 10.0)
        buf.append(time = 2.0, value = 20.0)
        buf.append(time = 3.0, value = 30.0)
        assertEquals(3, buf.sampleCount)
        assertFalse(buf.isFull)
        assertEquals(listOf(1.0, 2.0, 3.0), buf.samples.map { it.time })
        assertEquals(listOf(10.0, 20.0, 30.0), buf.samples.map { it.value })
        assertEquals(RingSample(3.0, 30.0), buf.latest)
    }

    @Test
    fun nadpisujeNajstarszePoZapelnieniu() {
        val buf = RingBuffer(capacity = 3)
        buf.append(time = 1.0, value = 10.0)
        buf.append(time = 2.0, value = 20.0)
        buf.append(time = 3.0, value = 30.0)
        buf.append(time = 4.0, value = 40.0)
        buf.append(time = 5.0, value = 50.0)
        assertTrue(buf.isFull)
        assertEquals(3, buf.sampleCount)
        assertEquals(listOf(3.0, 4.0, 5.0), buf.samples.map { it.time })
        assertEquals(listOf(30.0, 40.0, 50.0), buf.samples.map { it.value })
        assertEquals(RingSample(5.0, 50.0), buf.latest)
    }

    @Test
    fun pojemnoscJednegoElementu() {
        val buf = RingBuffer(capacity = 1)
        buf.append(time = 1.0, value = 7.0)
        buf.append(time = 2.0, value = 8.0)
        assertEquals(listOf(RingSample(2.0, 8.0)), buf.samples)
    }

    @Test
    fun removeAllZerujeStan() {
        val buf = RingBuffer(capacity = 3)
        buf.append(time = 1.0, value = 1.0)
        buf.removeAll()
        assertTrue(buf.isEmpty)
        assertTrue(buf.samples.isEmpty())
        assertNull(buf.latest)
    }

    @Test
    fun storeRozdzielaSeriePoPid() {
        val store = RingBufferStore(capacity = 3)
        store.append(pid = 0x0C, time = 0.0, value = 800.0)
        store.append(pid = 0x0D, time = 0.0, value = 0.0)
        store.append(pid = 0x0C, time = 0.25, value = 900.0)
        assertEquals(listOf(800.0, 900.0), store.samples(0x0C).map { it.value })
        assertEquals(0.0, store.latest(0x0D)?.value)
        assertTrue(store.samples(0x05).isEmpty())
    }

    @Test
    fun storeAppendZTickaBierzeTylkoNumeric() {
        val store = RingBufferStore(capacity = 8)
        val tick = SampleTick(
            kind = SampleTick.Kind.Hot,
            time = 1.5,
            readings = listOf(
                MultiPidReading(0x0C, listOf(0x0B, 0xB8), DecodedPid.Numeric(750.0)),
                MultiPidReading(0x13, listOf(0x03), DecodedPid.Bytes(listOf(0x03))),
            ),
        )
        store.append(tick)
        assertEquals(750.0, store.latest(0x0C)?.value)
        assertTrue(store.samples(0x13).isEmpty())
    }
}
