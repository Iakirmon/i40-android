package pl.i40.android.obd

enum class IgnitionType(val polish: String) {
    Spark("iskrowy"),
    Compression("wysokoprężny"),
}

data class ReadinessMonitor(val name: String, val incomplete: Boolean)

data class ReadinessStatus(
    val milOn: Boolean,
    val storedDtcCount: Int,
    val ignition: IgnitionType,
    val continuous: List<ReadinessMonitor>,
    val monitors: List<ReadinessMonitor>,
    val incomplete: List<ReadinessMonitor>,
    val ready: Boolean,
)

object Readiness {
    private val continuousNames = listOf(
        0 to "Wypadanie zapłonu",
        1 to "Układ paliwowy",
        2 to "Komponenty",
    )

    private val sparkNames = listOf(
        0 to "Katalizator",
        1 to "Katalizator podgrzewany",
        2 to "Układ odparowania paliwa",
        3 to "Powietrze wtórne",
        4 to "Czynnik klimatyzacji",
        5 to "Sondy tlenu",
        6 to "Podgrzewanie sond tlenu",
        7 to "Układ EGR",
    )

    fun decode(a: Int, b: Int, c: Int, d: Int): ReadinessStatus {
        val milOn = a and 0x80 != 0
        val dtcCount = a and 0x7F
        val spark = b and 0x08 == 0

        val continuous = continuousNames.mapNotNull { (bit, name) ->
            val supported = b and (1 shl bit) != 0
            if (!supported) return@mapNotNull null
            val incomplete = b and (1 shl (bit + 4)) != 0
            ReadinessMonitor(name, incomplete)
        }

        val monitors = sparkNames.mapNotNull { (bit, name) ->
            val supported = c and (1 shl bit) != 0
            if (!supported) return@mapNotNull null
            val incomplete = d and (1 shl bit) != 0
            ReadinessMonitor(name, incomplete)
        }

        val incomplete = (continuous + monitors).filter { it.incomplete }
        return ReadinessStatus(
            milOn = milOn,
            storedDtcCount = dtcCount,
            ignition = if (spark) IgnitionType.Spark else IgnitionType.Compression,
            continuous = continuous,
            monitors = monitors,
            incomplete = incomplete,
            ready = incomplete.isEmpty(),
        )
    }

    fun decode(mode01Payload: List<Int>): ReadinessStatus? {
        if (mode01Payload.size < 6 || mode01Payload[0] != 0x41 || mode01Payload[1] != 0x01) {
            return null
        }
        return decode(mode01Payload[2], mode01Payload[3], mode01Payload[4], mode01Payload[5])
    }

    fun decode(hexResponse: String): ReadinessStatus? {
        val upper = hexResponse.uppercase()
        if (upper.contains("NO DATA") || upper.contains("UNABLE TO CONNECT")) return null
        val hex = upper.filter { it.isHexDigit() }
        if (hex.length < 12) return null
        val bytes = hexBytes(hex) ?: return null
        return decode(mode01Payload = bytes)
    }
}
