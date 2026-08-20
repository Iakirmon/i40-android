package pl.i40.android.charts

import pl.i40.android.acquisition.RingSample
import pl.i40.android.storage.TrackBlob

data class DecimatedPoint(val time: Float, val value: Float)

/**
 * Redukcja serii do szerokości wykresu przez minimum i maksimum w koszyku — nie średnią.
 * Algorytm z sekcji 12.6, port [Decimator.swift].
 */
object Decimator {
    fun decimate(
        times: List<Float>,
        values: List<Float>,
        buckets: Int,
        timeRange: ClosedFloatingPointRange<Float>? = null
    ): List<DecimatedPoint> {
        require(times.size == values.size) { "Decimator: times i values muszą mieć tę samą długość" }
        if (buckets <= 0 || times.isEmpty()) return emptyList()

        val start: Float
        val end: Float
        if (timeRange != null) {
            start = timeRange.start
            end = timeRange.endInclusive
        } else {
            start = times.minOrNull()!!
            end = times.maxOrNull()!!
        }
        if (end <= start) {
            val minV = values.minOrNull()!!
            val maxV = values.maxOrNull()!!
            if (minV == maxV) return listOf(DecimatedPoint(start, minV))
            return listOf(DecimatedPoint(start, minV), DecimatedPoint(start, maxV))
        }

        val width = end - start
        val bucketMin = arrayOfNulls<Float>(buckets)
        val bucketMax = arrayOfNulls<Float>(buckets)
        val bucketMinTime = FloatArray(buckets)
        val bucketMaxTime = FloatArray(buckets)

        for (i in times.indices) {
            val t = times[i]
            if (t < start || t > end) continue
            var index = (buckets * (t - start) / width).toInt()
            if (index >= buckets) index = buckets - 1
            if (index < 0) index = 0
            val v = values[i]
            val curMin = bucketMin[index]
            if (curMin == null || v < curMin) {
                bucketMin[index] = v
                bucketMinTime[index] = t
            }
            val curMax = bucketMax[index]
            if (curMax == null || v > curMax) {
                bucketMax[index] = v
                bucketMaxTime[index] = t
            }
        }

        val out = ArrayList<DecimatedPoint>(buckets * 2)
        for (b in 0 until buckets) {
            val minV = bucketMin[b] ?: continue
            val maxV = bucketMax[b] ?: continue
            val minT = bucketMinTime[b]
            val maxT = bucketMaxTime[b]
            if (minT == maxT && minV == maxV) {
                out.add(DecimatedPoint(minT, minV))
                continue
            }
            if (minT <= maxT) {
                out.add(DecimatedPoint(minT, minV))
                out.add(DecimatedPoint(maxT, maxV))
            } else {
                out.add(DecimatedPoint(maxT, maxV))
                out.add(DecimatedPoint(minT, minV))
            }
        }
        return out
    }

    fun decimate(
        samples: List<RingSample>,
        buckets: Int,
        timeRange: ClosedFloatingPointRange<Float>? = null
    ): List<DecimatedPoint> = decimate(
        times = samples.map { it.time.toFloat() },
        values = samples.map { it.value.toFloat() },
        buckets = buckets,
        timeRange = timeRange
    )

    fun decimate(
        series: TrackBlob.Series,
        buckets: Int,
        timeRange: ClosedFloatingPointRange<Float>? = null
    ): List<DecimatedPoint> = decimate(series.times, series.values, buckets, timeRange)
}
