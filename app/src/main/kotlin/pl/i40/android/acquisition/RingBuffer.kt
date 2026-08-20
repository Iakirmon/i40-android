package pl.i40.android.acquisition

import pl.i40.android.obd.DecodedPid

data class RingSample(val time: Double, val value: Double)

/** Bufor kołowy stałej pojemności — źródło wykresów przesuwnych, niezależny od zapisu sesji. */
class RingBuffer(val capacity: Int = DEFAULT_CAPACITY) {
    companion object {
        const val DEFAULT_CAPACITY = 240
    }

    init {
        require(capacity > 0) { "RingBuffer wymaga pojemności > 0" }
    }

    private val times = DoubleArray(capacity)
    private val values = DoubleArray(capacity)
    private var head = 0
    private var count = 0

    val isEmpty: Boolean get() = count == 0
    val isFull: Boolean get() = count == capacity
    val sampleCount: Int get() = count

    val latest: RingSample?
        get() {
            if (count == 0) return null
            val index = (head - 1 + capacity) % capacity
            return RingSample(times[index], values[index])
        }

    val samples: List<RingSample>
        get() {
            if (count == 0) return emptyList()
            val start = if (isFull) head else 0
            return List(count) { offset ->
                val i = (start + offset) % capacity
                RingSample(times[i], values[i])
            }
        }

    fun append(time: Double, value: Double) {
        times[head] = time
        values[head] = value
        head = (head + 1) % capacity
        if (count < capacity) count += 1
    }

    fun removeAll() {
        head = 0
        count = 0
    }
}

/** Zestaw buforów po PID — kafle i wykresy żywe. */
class RingBufferStore(val capacity: Int = RingBuffer.DEFAULT_CAPACITY) {
    private val buffers = mutableMapOf<Int, RingBuffer>()

    fun append(pid: Int, time: Double, value: Double) {
        val buf = buffers.getOrPut(pid) { RingBuffer(capacity) }
        buf.append(time, value)
    }

    fun append(tick: SampleTick) {
        for (reading in tick.readings) {
            val numeric = reading.decoded as? DecodedPid.Numeric ?: continue
            append(reading.pid, tick.time, numeric.value)
        }
    }

    fun samples(pid: Int): List<RingSample> = buffers[pid]?.samples ?: emptyList()

    fun latest(pid: Int): RingSample? = buffers[pid]?.latest

    fun removeAll() {
        buffers.clear()
    }
}
