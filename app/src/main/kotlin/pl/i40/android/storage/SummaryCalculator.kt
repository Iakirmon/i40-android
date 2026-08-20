package pl.i40.android.storage

import pl.i40.android.obd.FuelSystemStatus

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
    val sredniaHz: Double = 0.0,
    val maxCisnienieSzynyBar: Double? = null,
    val obciazeniePrzyMaxCisnieniu: Double? = null,
    val maxTempKatalizatoraC: Double? = null,
    val czasDo90CSekundy: Double? = null,
    val czasPozaPasmemWPetliZamknietejSekundy: Double? = null,
    val czasWPetliZamknietejSekundy: Double? = null
)

object SummaryCalculator {
    const val RPM_PID = 0x0C
    const val SPEED_PID = 0x0D
    const val COOLANT_PID = 0x05
    const val VOLTAGE_PID = 0x42
    const val FUEL_RATE_PID = 0x5E
    const val RAIL_PID = 0x23
    const val LOAD_ABS_PID = 0x43
    const val CATALYST_PID = 0x3C
    const val STFT_PID = 0x06
    const val LTFT_PID = 0x07
    const val STATUS_PID = 0x03

    /** SAE J1979 PID `0123` jest w kPa; 1 bar = 100 kPa. `41230180` → 38,4 bar. */
    const val KPA_NA_BAR = 100.0

    /** Pierwsza próbka `0105` ≥ 90 °C — §11.1 warstwy diagnostycznej. */
    const val PROG_PLYN_90_C = 90.0

    /** Próg reguły `trim_sum` i pasma sumy korekt — §11.1, nie ±10 % korekty długiej. */
    const val PROG_SUMY_KOREKT = 20.0

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
        val rail = series(from, RAIL_PID)
        val loadAbs = series(from, LOAD_ABS_PID)
        val catalyst = series(from, CATALYST_PID)
        val stft = series(from, STFT_PID)
        val ltft = series(from, LTFT_PID)
        val status = series(from, STATUS_PID)
        val czasyKorekt = czasyKorektWPetliZamknietej(stft, ltft, status)
        val sampleCount = from.series.sumOf { it.values.size }
        val hotSamples = speed?.values?.size ?: rpm?.values?.size ?: 0
        val averageHz = if (durationSeconds > 0 && hotSamples > 0) {
            hotSamples / durationSeconds
        } else {
            0.0
        }
        val maxRailKpa = maxValue(rail?.values)
        val maxRailBar = maxRailKpa?.div(KPA_NA_BAR)
        val czasMaxSzyny = czasMaksimum(rail)
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
            sredniaHz = averageHz,
            maxCisnienieSzynyBar = maxRailBar,
            obciazeniePrzyMaxCisnieniu = czasMaxSzyny?.let { wartoscNajblizszaCzasowo(loadAbs, it) },
            maxTempKatalizatoraC = maxValue(catalyst?.values),
            czasDo90CSekundy = czasPierwszej(coolant, PROG_PLYN_90_C),
            czasPozaPasmemWPetliZamknietejSekundy = czasyKorekt?.first,
            czasWPetliZamknietejSekundy = czasyKorekt?.second
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

    private fun czasMaksimum(s: TrackBlob.Series?): Float? {
        if (s == null || s.values.isEmpty()) return null
        var best = 0
        for (i in 1 until s.values.size) {
            if (s.values[i] > s.values[best]) best = i
        }
        return s.times[best]
    }

    /** Przy remisie zostaje wcześniejsza próbka — indeks nie ma prawa decydować. */
    private fun wartoscNajblizszaCzasowo(s: TrackBlob.Series?, t: Float): Double? {
        if (s == null || s.times.isEmpty()) return null
        var best = 0
        var bestD = kotlin.math.abs(s.times[0] - t)
        for (i in 1 until s.times.size) {
            val d = kotlin.math.abs(s.times[i] - t)
            if (d < bestD) {
                bestD = d
                best = i
            }
        }
        return s.values[best].toDouble()
    }

    private fun czasPierwszej(s: TrackBlob.Series?, prog: Double): Double? {
        if (s == null) return null
        for (i in s.values.indices) {
            if (s.values[i] >= prog) return s.times[i].toDouble()
        }
        return null
    }

    /**
     * Suma odstępów poza ±20 % **tylko** gdy pętla jest zamknięta (`0103` ∈ {2, 16}).
     * Mianownik to czas w pętli zamkniętej, nie czas sesji — §8.4 warstwy kontekstowej.
     */
    private fun czasyKorektWPetliZamknietej(
        stft: TrackBlob.Series?,
        ltft: TrackBlob.Series?,
        status: TrackBlob.Series?
    ): Pair<Double, Double>? {
        if (stft == null || ltft == null) return null
        val times = (stft.times + ltft.times + (status?.times ?: emptyList())).toSortedSet()
        if (times.size < 2) return 0.0 to 0.0
        val ordered = times.toList()
        var poza = 0.0
        var zamknieta = 0.0
        for (i in 0 until ordered.lastIndex) {
            val t0 = ordered[i]
            val t1 = ordered[i + 1]
            val dt = (t1 - t0).toDouble()
            if (dt <= 0) continue
            val a = wartoscWCzasieLubPrzed(stft, t0) ?: continue
            val b = wartoscWCzasieLubPrzed(ltft, t0) ?: continue
            val bajtA = status?.let { wartoscWCzasieLubPrzed(it, t0)?.toInt() }
            if (!FuelSystemStatus.korektyWazne(bajtA)) continue
            zamknieta += dt
            if (kotlin.math.abs(a + b) > PROG_SUMY_KOREKT) poza += dt
        }
        return poza to zamknieta
    }

    private fun wartoscWCzasieLubPrzed(s: TrackBlob.Series, t: Float): Double? {
        var last: Double? = null
        for (i in s.times.indices) {
            if (s.times[i] <= t) last = s.values[i].toDouble() else break
        }
        return last
    }

    private fun maxValue(values: List<Float>?): Double? = values?.maxOrNull()?.toDouble()

    private fun minValue(values: List<Float>?): Double? = values?.minOrNull()?.toDouble()

    private fun average(values: List<Float>?): Double? {
        if (values.isNullOrEmpty()) return null
        return values.sumOf { it.toDouble() } / values.size
    }
}
