package pl.i40.android.elm

/**
 * Składanie wieloramkowych odpowiedzi ELM (ISO-TP przez adapter):
 * linie `N:HEX` → jeden ciąg hex / bajtów.
 *
 * Wspólne dla trybu 09 (VIN) i odpowiedzi wielo-PID trybu 01.
 * Wiodąca długość całkowita (`014`) jest linią bez dwukropka i wypada sama.
 */
object MultiFrame {
    fun collectFrameHex(from: String): String? {
        val parts = mutableListOf<Pair<Int, String>>()
        for (raw in from.split(Regex("[\\r\\n]+"))) {
            val line = raw.trim()
            val colon = line.indexOf(':')
            if (colon < 0) continue
            val index = line.substring(0, colon).toIntOrNull() ?: continue
            val hexPart = line.substring(colon + 1)
            if (hexPart.isEmpty() || !hexPart.all { it.isHexDigit() }) continue
            parts.add(index to hexPart.uppercase())
        }
        if (parts.isEmpty()) return null
        parts.sortBy { it.first }
        return parts.joinToString("") { it.second }
    }

    fun bytes(fromHex: String): List<UByte> {
        val out = mutableListOf<UByte>()
        var i = 0
        while (i < fromHex.length) {
            val next = minOf(i + 2, fromHex.length)
            if (next <= i) break
            val b = fromHex.substring(i, next).toIntOrNull(16) ?: break
            out.add(b.toUByte())
            i = next
        }
        return out
    }

    fun assembledBytes(from: String): List<UByte>? {
        val hex = collectFrameHex(from) ?: return null
        return bytes(hex)
    }
}

private fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'A'..'F' || this in 'a'..'f'
