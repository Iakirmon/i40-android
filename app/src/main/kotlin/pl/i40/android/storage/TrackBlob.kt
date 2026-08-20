package pl.i40.android.storage

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets

/**
 * Kolumnowy przebieg sesji — format z §8.6 specu, big-endian.
 * Nie jest to plist z iOS: radio zapisuje magię `I40T`.
 */
class TrackBlob(val version: Int = CURRENT_VERSION, private val seriesInternal: MutableList<Series> = mutableListOf()) {
    data class Series(val pid: Int, val times: MutableList<Float>, val values: MutableList<Float>)

    val series: List<Series> get() = seriesInternal

    fun append(pid: Int, time: Float, value: Float) {
        val existing = seriesInternal.firstOrNull { it.pid == pid }
        if (existing != null) {
            existing.times.add(time)
            existing.values.add(value)
        } else {
            seriesInternal.add(Series(pid, mutableListOf(time), mutableListOf(value)))
        }
    }

    fun kopia(): TrackBlob = TrackBlob(
        version = version,
        seriesInternal = seriesInternal.map {
            Series(it.pid, it.times.toMutableList(), it.values.toMutableList())
        }.toMutableList()
    )

    fun encode(): ByteArray {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).use { data ->
            data.write(MAGIC)
            data.writeInt(version)
            data.writeInt(seriesInternal.size)
            for (s in seriesInternal) {
                data.writeByte(s.pid)
                data.writeInt(s.times.size)
                for (t in s.times) data.writeFloat(t)
                for (v in s.values) data.writeFloat(v)
            }
        }
        return out.toByteArray()
    }

    companion object {
        const val CURRENT_VERSION = 1
        private val MAGIC = "I40T".toByteArray(StandardCharsets.US_ASCII)

        fun decode(from: ByteArray): TrackBlob {
            DataInputStream(ByteArrayInputStream(from)).use { data ->
                val magic = ByteArray(4)
                data.readFully(magic)
                require(magic.contentEquals(MAGIC)) { "TrackBlob: zła magia" }
                val version = data.readInt()
                require(version == CURRENT_VERSION) { "TrackBlob: nieznana wersja $version" }
                val count = data.readInt()
                val series = mutableListOf<Series>()
                repeat(count) {
                    val pid = data.readUnsignedByte()
                    val n = data.readInt()
                    val times = MutableList(n) { data.readFloat() }
                    val values = MutableList(n) { data.readFloat() }
                    series.add(Series(pid, times, values))
                }
                return TrackBlob(version, series)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is TrackBlob) return false
        return version == other.version && seriesInternal == other.seriesInternal
    }

    override fun hashCode(): Int = 31 * version + seriesInternal.hashCode()
}
