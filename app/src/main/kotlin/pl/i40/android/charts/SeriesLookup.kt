package pl.i40.android.charts

import pl.i40.android.storage.TrackBlob

/** Odczyt wartości z serii w chwili suwaka — ostatnia próbka o `time <= t`. */
object SeriesLookup {
    fun fullTimeRange(of: TrackBlob): ClosedFloatingPointRange<Float>? {
        val times = of.series.flatMap { it.times }
        val lo = times.minOrNull() ?: return null
        val hi = times.maxOrNull() ?: return null
        if (hi < lo) return null
        if (hi == lo) return lo..(lo + 0.001f)
        return lo..hi
    }

    fun value(at: Float, times: List<Float>, values: List<Float>): Float? {
        if (times.size != values.size || times.isEmpty()) return null
        var lo = 0
        var hi = times.lastIndex
        var answer: Int? = null
        while (lo <= hi) {
            val mid = (lo + hi) / 2
            if (times[mid] <= at) {
                answer = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        val index = answer ?: return null
        return values[index]
    }

    fun value(at: Float, series: TrackBlob.Series): Float? = value(at, series.times, series.values)

    fun clamp(t: Float, to: ClosedFloatingPointRange<Float>): Float = minOf(maxOf(t, to.start), to.endInclusive)

    fun zoomedRange(
        current: ClosedFloatingPointRange<Float>,
        full: ClosedFloatingPointRange<Float>,
        factor: Float,
        anchor: Float
    ): ClosedFloatingPointRange<Float> {
        val span = current.endInclusive - current.start
        if (span <= 0f || factor <= 0f) return current
        val newSpan = maxOf(span / factor, 0.5f)
        val fullSpan = full.endInclusive - full.start
        val clampedSpan = minOf(newSpan, fullSpan)
        val a = clamp(anchor, full)
        var start = a - clampedSpan / 2
        var end = a + clampedSpan / 2
        if (start < full.start) {
            start = full.start
            end = start + clampedSpan
        }
        if (end > full.endInclusive) {
            end = full.endInclusive
            start = end - clampedSpan
        }
        return start..end
    }

    fun pannedRange(
        current: ClosedFloatingPointRange<Float>,
        full: ClosedFloatingPointRange<Float>,
        delta: Float
    ): ClosedFloatingPointRange<Float> {
        val span = current.endInclusive - current.start
        var start = current.start + delta
        var end = current.endInclusive + delta
        if (start < full.start) {
            start = full.start
            end = start + span
        }
        if (end > full.endInclusive) {
            end = full.endInclusive
            start = end - span
        }
        return start..end
    }
}
