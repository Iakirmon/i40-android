package pl.i40.android.obd

enum class DtcKind { Generic, Manufacturer, Other }

data class Dtc(val code: String, val kind: DtcKind, val description: String)

object DtcDecode {
    private val families = listOf("P", "C", "B", "U")

    fun code(hi: Int, lo: Int): String? {
        if (hi == 0 && lo == 0) return null
        val family = families[(hi shr 6) and 0x03]
        val d1 = (hi shr 4) and 0x03
        val d2 = hi and 0x0F
        val d3 = (lo shr 4) and 0x0F
        val d4 = lo and 0x0F
        return "%s%X%X%X%X".format(family, d1, d2, d3, d4)
    }

    fun kind(of: String): DtcKind {
        if (of.length != 5) return DtcKind.Other
        val chars = of.uppercase()
        if (chars[0] != 'P') return DtcKind.Other
        return when (chars[1]) {
            '0', '2' -> DtcKind.Generic
            '1' -> DtcKind.Manufacturer
            '3' -> if (chars[2] in '0'..'3') DtcKind.Manufacturer else DtcKind.Other
            else -> DtcKind.Other
        }
    }

    fun describe(code: String, dictionary: Map<String, String>): Dtc {
        val upper = code.uppercase()
        val k = kind(upper)
        val text = dictionary[upper]
        if (text != null) {
            return Dtc(upper, DtcKind.Generic, text)
        }
        return when (k) {
            DtcKind.Manufacturer ->
                Dtc(upper, DtcKind.Manufacturer, "kod producencki Hyundai, opis nieznany")
            DtcKind.Generic ->
                Dtc(upper, DtcKind.Generic, "Kod generyczny — brak wpisu w słowniku")
            DtcKind.Other ->
                Dtc(upper, DtcKind.Other, "Brak opisu w słowniku")
        }
    }

    fun codes(fromModePayload: List<Int>, dictionary: Map<String, String>): List<Dtc> {
        val first = fromModePayload.firstOrNull() ?: return emptyList()
        if (first != 0x43 && first != 0x47 && first != 0x4A) return emptyList()
        val out = mutableListOf<Dtc>()
        var i = 1
        while (i + 1 < fromModePayload.size) {
            val raw = code(fromModePayload[i], fromModePayload[i + 1])
            if (raw != null) out.add(describe(raw, dictionary))
            i += 2
        }
        return out
    }

    fun codes(fromHexResponse: String, dictionary: Map<String, String>): List<Dtc> {
        val upper = fromHexResponse.uppercase()
        if (upper.contains("NO DATA")) return emptyList()
        val hex = upper.filter { it.isHexDigit() }
        if (hex.length < 2 || hex.length % 2 != 0) return emptyList()
        val bytes = hexBytes(hex) ?: return emptyList()
        return codes(fromModePayload = bytes, dictionary = dictionary)
    }
}
