package pl.i40.android.acquisition

import pl.i40.android.elm.ElmSession
import pl.i40.android.obd.MultiPid
import pl.i40.android.obd.MultiPidReading

enum class PidQueryMode {
    Multi,
    Single,
}

/** Sonda i odczyt PID-ów trybu 01 w trybie wielo-PID albo pojedynczym. */
object PidBatchReader {
    val DEFAULT_PROBE_PIDS: List<Int> = listOf(0x0C, 0x0D)

    const val FALLBACK_USER_MESSAGE =
        "Adapter nie obsługuje wielu PID-ów w jednym zapytaniu — odpytywanie pojedyncze (~1,5 Hz)."

    fun responseIndicatesMultiSupport(text: String, probePids: List<Int> = DEFAULT_PROBE_PIDS): Boolean {
        if (probePids.size < 2) return false
        val got = MultiPid.parse(text).map { it.pid }.toSet()
        return got.intersect(probePids.toSet()).size >= 2
    }

    suspend fun probe(session: ElmSession, pids: List<Int> = DEFAULT_PROBE_PIDS): PidQueryMode {
        require(pids.size >= 2) { "Sonda multi-PID wymaga co najmniej dwóch PID-ów" }
        val text = session.send(MultiPid.command(pids))
        return if (responseIndicatesMultiSupport(text, pids)) PidQueryMode.Multi else PidQueryMode.Single
    }

    suspend fun read(session: ElmSession, pids: List<Int>, mode: PidQueryMode): List<MultiPidReading> {
        if (pids.isEmpty()) return emptyList()
        return when (mode) {
            PidQueryMode.Multi -> readMulti(session, pids)
            PidQueryMode.Single -> readSingle(session, pids)
        }
    }

    private suspend fun readMulti(session: ElmSession, pids: List<Int>): List<MultiPidReading> {
        val out = mutableListOf<MultiPidReading>()
        var remaining = pids
        while (remaining.isNotEmpty()) {
            val chunk = remaining.take(MultiPid.MAX_PIDS_PER_QUERY)
            remaining = remaining.drop(chunk.size)
            val map = MultiPid.parseMap(session.send(MultiPid.command(chunk)))
            for (pid in chunk) {
                val reading = map[pid]
                if (reading != null) out.add(reading)
            }
        }
        return out
    }

    private suspend fun readSingle(session: ElmSession, pids: List<Int>): List<MultiPidReading> {
        val out = mutableListOf<MultiPidReading>()
        for (pid in pids) {
            val text = session.send("01%02X".format(pid))
            val reading = MultiPid.parse(text).firstOrNull { it.pid == pid }
            if (reading != null) out.add(reading)
        }
        return out
    }
}
