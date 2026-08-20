package pl.i40.android.ui

import pl.i40.android.charts.DecimatedPoint
import pl.i40.android.charts.Decimator
import pl.i40.android.charts.SeriesLookup
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

    fun wartoscPrzySuwaku(seria: TrackBlob.Series): Float? = SeriesLookup.value(at = czasSuwaka, series = seria)

    fun punktyZdecymowane(seria: TrackBlob.Series): List<DecimatedPoint> =
        Decimator.decimate(seria, buckets = maxOf(szerokoscWykresu, 8), timeRange = okno)

    fun ustawSuwak(fractionX: Float, width: Float) {
        if (width <= 0f) return
        val f = minOf(maxOf(fractionX / width, 0f), 1f)
        val span = okno.endInclusive - okno.start
        czasSuwaka = SeriesLookup.clamp(okno.start + f * span, okno)
    }
}
