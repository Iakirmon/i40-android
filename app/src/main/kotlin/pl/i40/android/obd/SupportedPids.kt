package pl.i40.android.obd

/** Parsowanie masek `0100` / `0120` / `0140` / `0160` → zbiór numerów PID-ów. */
object SupportedPids {
    data class Range(val command: Int, val responsePid: Int, val firstPid: Int)

    val ranges: List<Range> = listOf(
        Range(0x00, 0x00, 0x01),
        Range(0x20, 0x20, 0x21),
        Range(0x40, 0x40, 0x41),
        Range(0x60, 0x60, 0x61),
    )

    /** Bity `20 40 60 80` to znaczniki następnej maski, nie PID-y pomiarowe. */
    val bityKontynuacji: Set<Int> = setOf(0x20, 0x40, 0x60, 0x80)

    fun pids(fromMask: List<Int>, firstPid: Int): Set<Int> {
        if (fromMask.size < 4) return emptySet()
        val out = mutableSetOf<Int>()
        for (i in 0 until 4) {
            val byte = fromMask[i]
            for (bit in 0 until 8) {
                if (byte and (0x80 shr bit) != 0) {
                    out.add(firstPid + i * 8 + bit)
                }
            }
        }
        return out
    }

    fun indicatesNextRange(bytes: List<Int>, firstPid: Int): Boolean {
        if (bytes.size < 4) return false
        return bytes[3] and 0x01 != 0
    }

    fun pids(fromMode01Payload: List<Int>): Set<Int> {
        if (fromMode01Payload.size < 6 || fromMode01Payload[0] != 0x41) return emptySet()
        val responsePid = fromMode01Payload[1]
        val range = ranges.firstOrNull { it.responsePid == responsePid } ?: return emptySet()
        return pids(fromMask = fromMode01Payload.subList(2, 6), firstPid = range.firstPid)
    }

    fun pids(fromHex: String): Set<Int> {
        val cleaned = fromHex.uppercase().filter { it.isHexDigit() }
        if (cleaned.length < 12 || cleaned.length % 2 != 0) return emptySet()
        val bytes = hexBytes(cleaned) ?: return emptySet()
        return pids(fromMode01Payload = bytes)
    }

    fun merge(vararg sets: Set<Int>): Set<Int> = sets.fold(emptySet()) { acc, s -> acc + s }

    fun bezKontynuacji(supported: Set<Int>): Set<Int> = supported - bityKontynuacji

    fun displayable(supported: Set<Int>): List<PidDefinition> = PidCatalog.all.filter { it.id in supported }

    fun maskCommandsToQuery(following: List<Pair<Int, List<Int>>>): List<Int> {
        val commands = mutableListOf(0x00)
        for (index in 0 until ranges.size - 1) {
            val range = ranges[index]
            val response = following.firstOrNull { it.first == range.responsePid } ?: break
            if (indicatesNextRange(response.second, range.firstPid)) {
                commands.add(ranges[index + 1].command)
            } else {
                break
            }
        }
        return commands
    }
}

internal fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'A'..'F' || this in 'a'..'f'

internal fun hexBytes(cleaned: String): List<Int>? {
    val bytes = mutableListOf<Int>()
    var i = 0
    while (i < cleaned.length) {
        val next = i + 2
        if (next > cleaned.length) return null
        bytes.add(cleaned.substring(i, next).toIntOrNull(16) ?: return null)
        i = next
    }
    return bytes
}
