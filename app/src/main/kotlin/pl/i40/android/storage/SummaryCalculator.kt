package pl.i40.android.storage

/** Podsumowanie sesji — liczone z przebiegu, bez ELM. Pola z §8.6 specu. */
data class PodsumowaniePrzejazdu(
    val czasTrwaniaS: Double = 0.0,
    val dystansKm: Double? = null,
    val maxObroty: Double? = null,
    val srednieObroty: Double? = null,
    val maxPredkoscKmh: Double? = null,
    val maxPlynC: Double? = null,
    val minNapiecie: Double? = null,
    val maxNapiecie: Double? = null,
    val paliwoL: Double? = null,
    val kodyNaStarcie: List<String> = emptyList(),
    val kodyNaKoncu: List<String> = emptyList(),
    val liczbaProbek: Int = 0,
    val sredniaHz: Double = 0.0
)

object SummaryCalculator {
    const val RPM_PID = 0x0C
    const val SPEED_PID = 0x0D
    const val COOLANT_PID = 0x05
    const val VOLTAGE_PID = 0x42
    const val FUEL_RATE_PID = 0x5E

    fun make(
        from: TrackBlob,
        durationSeconds: Double,
        kodyNaStarcie: List<String>,
        kodyNaKoncu: List<String>
    ): PodsumowaniePrzejazdu {
        val rpm = series(from, RPM_PID)
        val speed = series(from, SPEED_PID)
        val coolant = series(from, COOLANT_PID)
        val voltage = series(from, VOLTAGE_PID)
        val fuel = series(from, FUEL_RATE_PID)
        val sampleCount = from.series.sumOf { it.values.size }
        val hotSamples = speed?.values?.size ?: rpm?.values?.size ?: 0
        val averageHz = if (durationSeconds > 0 && hotSamples > 0) {
            hotSamples / durationSeconds
        } else {
            0.0
        }
        return PodsumowaniePrzejazdu(
            czasTrwaniaS = durationSeconds,
            dystansKm = integrateRateToQuantity(speed?.times, speed?.values),
            maxObroty = maxValue(rpm?.values),
            srednieObroty = average(rpm?.values),
            maxPredkoscKmh = maxValue(speed?.values),
            maxPlynC = maxValue(coolant?.values),
            minNapiecie = minValue(voltage?.values),
            maxNapiecie = maxValue(voltage?.values),
            paliwoL = integrateRateToQuantity(fuel?.times, fuel?.values),
            kodyNaStarcie = kodyNaStarcie,
            kodyNaKoncu = kodyNaKoncu,
            liczbaProbek = sampleCount,
            sredniaHz = averageHz
        )
    }

    /** Całkuje wielkość w jednostce/h (km/h → km, l/h → l) regułą trapezów. */
    fun integrateRateToQuantity(times: List<Float>?, values: List<Float>?): Double? {
        if (times == null || values == null || times.size != values.size || times.size < 2) {
            return null
        }
        var sum = 0.0
        for (i in 1 until times.size) {
            val dt = (times[i] - times[i - 1]).toDouble()
            if (dt <= 0) continue
            val avg = (values[i].toDouble() + values[i - 1].toDouble()) / 2
            sum += avg * dt / 3600.0
        }
        return sum
    }

    private fun series(track: TrackBlob, pid: Int): TrackBlob.Series? = track.series.firstOrNull { it.pid == pid }

    private fun maxValue(values: List<Float>?): Double? = values?.maxOrNull()?.toDouble()

    private fun minValue(values: List<Float>?): Double? = values?.minOrNull()?.toDouble()

    private fun average(values: List<Float>?): Double? {
        if (values.isNullOrEmpty()) return null
        return values.sumOf { it.toDouble() } / values.size
    }
}
