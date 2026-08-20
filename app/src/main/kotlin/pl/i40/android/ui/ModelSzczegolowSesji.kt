package pl.i40.android.ui

import pl.i40.android.acquisition.RingSample
import pl.i40.android.charts.DecimatedPoint
import pl.i40.android.charts.Decimator
import pl.i40.android.charts.SeriesLookup
import pl.i40.android.rules.PasmaOdniesienia
import pl.i40.android.storage.PodsumowaniePrzejazdu
import pl.i40.android.storage.TrackBlob

/** Stan szczegółów sesji — wspólny zakres czasu i suwak dla całego stosu wykresów. */
class ModelSzczegolowSesji(
    val podsumowanie: PodsumowaniePrzejazdu,
    val przebieg: TrackBlob,
    val notatka: String = "",
    val startMs: Long = 0L
) {
    private val pelnyZakres: ClosedFloatingPointRange<Float> =
        SeriesLookup.fullTimeRange(przebieg) ?: 0f..1f

    var okno: ClosedFloatingPointRange<Float> = pelnyZakres
    var czasSuwaka: Float = pelnyZakres.start
    var szerokoscWykresu: Int = 320

    val serie: List<TrackBlob.Series> get() = przebieg.series

    /** Stos §11.3: obroty, prędkość, szyna (bar), katalizator, suma korekt, płyn. */
    val stosWykresow: List<TrackBlob.Series>
        get() {
            val byPid = przebieg.series.associateBy { it.pid }
            val out = mutableListOf<TrackBlob.Series>()
            byPid[0x0C]?.let { out.add(it) }
            byPid[0x0D]?.let { out.add(it) }
            byPid[0x23]?.let { s ->
                out.add(
                    TrackBlob.Series(
                        s.pid,
                        s.times,
                        s.values.map { v ->
                            PasmaOdniesienia.kpaNaBar(v.toDouble()).toFloat()
                        }.toMutableList()
                    )
                )
            }
            byPid[0x3C]?.let { out.add(it) }
            seriaSumyKorekt()?.let { out.add(it) }
            byPid[0x05]?.let { out.add(it) }
            return out
        }

    fun wartoscPrzySuwaku(seria: TrackBlob.Series): Float? = SeriesLookup.nearest(at = czasSuwaka, series = seria)

    fun punktyZdecymowane(seria: TrackBlob.Series): List<DecimatedPoint> =
        Decimator.decimate(seria, buckets = maxOf(szerokoscWykresu, 8), timeRange = okno)

    fun ustawSuwak(fractionX: Float, width: Float) {
        if (width <= 0f) return
        val f = minOf(maxOf(fractionX / width, 0f), 1f)
        val span = okno.endInclusive - okno.start
        czasSuwaka = SeriesLookup.clamp(okno.start + f * span, okno)
    }

    private fun seriaSumyKorekt(): TrackBlob.Series? {
        val stft = przebieg.series.firstOrNull { it.pid == 0x06 } ?: return null
        val ltft = przebieg.series.firstOrNull { it.pid == 0x07 } ?: return null
        val a = stft.times.indices.map { i ->
            RingSample(stft.times[i].toDouble(), stft.values[i].toDouble())
        }
        val b = ltft.times.indices.map { i ->
            RingSample(ltft.times[i].toDouble(), ltft.values[i].toDouble())
        }
        val suma = FormatMieszanki.sumaProbek(a, b)
        if (suma.isEmpty()) return null
        return TrackBlob.Series(
            FormatRaportu.PID_SUMA_KOREKT,
            suma.map { it.time.toFloat() }.toMutableList(),
            suma.map { it.value.toFloat() }.toMutableList()
        )
    }
}
