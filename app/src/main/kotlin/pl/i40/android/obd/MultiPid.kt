package pl.i40.android.obd

import pl.i40.android.elm.MultiFrame

data class MultiPidReading(val pid: Int, val data: List<Int>, val decoded: DecodedPid)

/**
 * Budowa zapytania o wiele PID-ów trybu 01 i rozbiór odpowiedzi.
 * Długości danych wyłącznie z [PidCatalog] — bez zgadywania.
 */
object MultiPid {
    const val MAX_PIDS_PER_QUERY = 6

    fun command(pids: List<Int>): String {
        require(pids.isNotEmpty()) { "MultiPid.command wymaga co najmniej jednego PID" }
        require(pids.size <= MAX_PIDS_PER_QUERY) { "ELM327: max $MAX_PIDS_PER_QUERY PID-ów w zapytaniu" }
        return buildString {
            append("01")
            for (pid in pids) append("%02X".format(pid))
        }
    }

    fun parse(response: String): List<MultiPidReading> {
        if (hasFatalElmError(response)) return emptyList()
        val bytes = responseBytes(response) ?: return emptyList()
        if (bytes.isEmpty() || bytes[0] != 0x41) return emptyList()

        var index = 1
        val out = mutableListOf<MultiPidReading>()
        while (index < bytes.size) {
            val pid = bytes[index]
            index += 1
            val definition = PidCatalog.definition(pid) ?: break
            val need = definition.byteCount
            if (index + need > bytes.size) break
            val data = bytes.subList(index, index + need)
            index += need
            val decoded = definition.decode(data) ?: break
            out.add(MultiPidReading(pid, data, decoded))
        }
        return out
    }

    fun parseMap(response: String): Map<Int, MultiPidReading> = parse(response).associateBy { it.pid }

    private fun responseBytes(text: String): List<Int>? {
        val assembled = MultiFrame.assembledBytes(text)
        if (assembled != null && assembled.isNotEmpty()) {
            return assembled.map { it.toInt() }
        }
        val hex = hexDataLines(text)
        if (hex.isEmpty()) return null
        return MultiFrame.bytes(hex).map { it.toInt() }
    }

    private fun hexDataLines(text: String): String {
        val lines = text.split(Regex("[\\r\\n]+"))
            .map { it.trim().replace(" ", "").uppercase() }
            .filter { it.isNotEmpty() && it.all { c -> c.isHexDigit() } }
        return lines.firstOrNull { it.startsWith("41") } ?: lines.joinToString("")
    }

    private fun hasFatalElmError(text: String): Boolean {
        val upper = text.uppercase()
        val tokens = listOf(
            "UNABLE TO CONNECT",
            "BUS INIT: ERROR",
            "CAN ERROR",
            "STOPPED",
            "BUFFER FULL"
        )
        for (token in tokens) {
            if (upper.contains(token)) return true
        }
        val lines = upper.split(Regex("[\\r\\n]+")).map { it.trim() }
        return lines.any { it == "?" || it == "NO DATA" }
    }
}
