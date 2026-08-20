package pl.i40.android.obd

import pl.i40.android.elm.MultiFrame

/** Odczyt trybu 09 (VIN, kalibracja, nazwa ECU) oraz pewne pola z VIN-u. */
object Mode09 {
    fun collectFrameHex(from: String): String? = MultiFrame.collectFrameHex(from)

    fun ascii(from: List<Int>, startingAt: Int): String {
        if (startingAt >= from.size) return ""
        val s = buildString {
            for (b in from.drop(startingAt)) {
                if (b == 0) continue
                if (b in 0x20..0x7E) append(b.toChar())
            }
        }
        return s.trim()
    }

    fun decodeVin(from: String): String? {
        val bytes = payload(from, infoType = 0x02) ?: return null
        val vin = ascii(bytes, startingAt = 3)
        if (vin.isEmpty()) return null
        return if (vin.length >= 17) vin.take(17) else vin
    }

    fun decodeCalibrationId(from: String): String? {
        val bytes = payload(from, infoType = 0x04) ?: return null
        val id = ascii(bytes, startingAt = 3)
        return id.ifEmpty { null }
    }

    fun decodeEcuName(from: String): String? {
        val bytes = payload(from, infoType = 0x0A) ?: return null
        val name = ascii(bytes, startingAt = 3)
        return name.ifEmpty { null }
    }

    private fun payload(from: String, infoType: Int): List<Int>? {
        if (hasFatalElmError(from)) return null
        val bytes = MultiFrame.assembledBytes(from)?.map { it.toInt() } ?: return null
        if (bytes.size < 3 || bytes[0] != 0x49 || bytes[1] != infoType) return null
        return bytes
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

data class VinInfo(val vin: String, val manufacturer: String?, val modelYear: Int?, val plant: String?) {
    constructor(vin: String) : this(
        vin = vin.uppercase(),
        manufacturer = manufacturerOf(vin.uppercase()),
        modelYear = if (vin.length >= 10) modelYear(vin.uppercase()[9]) else null,
        plant = if (vin.length >= 11) plant(vin.uppercase()[10]) else null
    )

    companion object {
        fun modelYear(from: Char): Int? {
            val letters = "ABCDEFGHJKLMNPRSTVWXY"
            val idx = letters.indexOf(from)
            if (idx >= 0) return 2010 + idx
            if (from in '1'..'9') return 2000 + (from - '0')
            return null
        }

        fun plant(from: Char): String? = if (from == 'U') "Ulsan" else null

        private fun manufacturerOf(vin: String): String? {
            if (vin.length < 3) return null
            return if (vin.take(3) == "KMH") "Hyundai Motor Company, Korea" else null
        }
    }
}
