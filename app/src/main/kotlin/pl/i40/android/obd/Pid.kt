package pl.i40.android.obd

sealed class DecodedPid {
    data class Numeric(val value: Double) : DecodedPid()
    data class Oxygen(val lambda: Double, val voltage: Double) : DecodedPid()
    data class Bytes(val data: List<Int>) : DecodedPid()
    data class Code(val value: Int) : DecodedPid()
}

class PidDefinition(
    val id: Int,
    val name: String,
    val byteCount: Int,
    val unit: String,
    val range: ClosedFloatingPointRange<Double>? = null,
    private val decodeFn: (List<Int>) -> DecodedPid,
) {
    fun decode(data: List<Int>): DecodedPid? {
        if (data.size < byteCount) return null
        return decodeFn(data.take(byteCount))
    }

    fun isSuspect(value: DecodedPid): Boolean {
        val n = (value as? DecodedPid.Numeric)?.value ?: return false
        val r = range ?: return false
        return n !in r
    }

    val command: String
        get() = "01%02X".format(id)
}

object PidDecode {
    fun u8(d: List<Int>) = DecodedPid.Numeric(d[0].toDouble())
    fun temp(d: List<Int>) = DecodedPid.Numeric(d[0].toDouble() - 40)
    fun percent(d: List<Int>) = DecodedPid.Numeric(d[0].toDouble() * 100 / 255)
    fun trim(d: List<Int>) = DecodedPid.Numeric(d[0].toDouble() / 1.28 - 100)
    fun timing(d: List<Int>) = DecodedPid.Numeric(d[0].toDouble() / 2 - 64)
    fun count16(d: List<Int>) = DecodedPid.Numeric((256 * d[0] + d[1]).toDouble())
    fun rpm(d: List<Int>) = DecodedPid.Numeric((256 * d[0] + d[1]).toDouble() / 4)
    fun maf(d: List<Int>) = DecodedPid.Numeric((256 * d[0] + d[1]).toDouble() / 100)
    fun volts(d: List<Int>) = DecodedPid.Numeric((256 * d[0] + d[1]).toDouble() / 1000)
    fun lambda(d: List<Int>) = DecodedPid.Numeric((256 * d[0] + d[1]).toDouble() / 32768)
    fun loadAbs(d: List<Int>) = DecodedPid.Numeric((256 * d[0] + d[1]).toDouble() * 100 / 255)
    fun fuelRail(d: List<Int>) = DecodedPid.Numeric((256 * d[0] + d[1]).toDouble() * 10)
    fun catalyst(d: List<Int>) = DecodedPid.Numeric((256 * d[0] + d[1]).toDouble() / 10 - 40)
    fun injectionTiming(d: List<Int>) = DecodedPid.Numeric(((256 * d[0] + d[1]).toDouble() - 26880) / 128)
    fun fuelRate(d: List<Int>) = DecodedPid.Numeric((256 * d[0] + d[1]).toDouble() / 20)
    fun torquePercent(d: List<Int>) = DecodedPid.Numeric(d[0].toDouble() - 125)
    fun oxygen(d: List<Int>) = DecodedPid.Oxygen(
        lambda = (256 * d[0] + d[1]).toDouble() / 32768,
        voltage = (256 * d[2] + d[3]).toDouble() / 8192
    )
    fun fuelSystem(d: List<Int>) = DecodedPid.Code(d[0])
    fun raw(d: List<Int>) = DecodedPid.Bytes(d)
    fun code(d: List<Int>) = DecodedPid.Code(d[0])
}
