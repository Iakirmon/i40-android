package pl.i40.android.acquisition

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import pl.i40.android.elm.ElmSession
import pl.i40.android.obd.MultiPid
import pl.i40.android.obd.MultiPidReading

enum class SampleRate {
    Economy,
    Balanced,
    Detailed,
    ;

    val targetInterval: Duration?
        get() = when (this) {
            Economy -> 500.milliseconds
            Balanced -> 250.milliseconds
            Detailed -> null
        }
}

interface SampleClock {
    fun seconds(): Double
    fun advance(duration: Duration)
    suspend fun sleep(forDuration: Duration)
}

class ContinuousSampleClock : SampleClock {
    private val startNs = System.nanoTime()

    override fun seconds(): Double = (System.nanoTime() - startNs) / 1_000_000_000.0

    override fun advance(duration: Duration) = Unit

    override suspend fun sleep(forDuration: Duration) {
        if (forDuration <= Duration.ZERO) return
        delay(forDuration)
    }
}

data class SampleTick(val kind: Kind, val time: Double, val readings: List<MultiPidReading>) {
    enum class Kind { Hot, Fast, Medium, Slow }
}

sealed class SampleStreamError(message: String) : Exception(message) {
    data object TooManyEmptyReads : SampleStreamError("Powtarzające się NO DATA — zatrzymano odczyt.")
}

/**
 * Pętla gorąca plus poziomy A/B/C. Bez timera: następne zapytanie po odpowiedzi.
 * Zegar jest wstrzykiwany — testy tempa nie czekają w czasie rzeczywistym.
 */
class SampleStream(private val session: ElmSession, private val config: Configuration = Configuration()) {
    class Configuration(
        val chartSlots: List<Int> = DEFAULT_CHART_SLOTS,
        val fastPids: List<Int> = DEFAULT_FAST_PIDS,
        val mediumPids: List<Int> = DEFAULT_MEDIUM_PIDS,
        val slowPids: List<Int> = DEFAULT_SLOW_PIDS,
        val rate: SampleRate = SampleRate.Balanced,
        val queryMode: PidQueryMode? = null,
        val clock: SampleClock = ContinuousSampleClock(),
        val simulatedCycleWork: Duration = Duration.ZERO,
        val maxHotCycles: Int? = null
    )

    companion object {
        val DEFAULT_CHART_SLOTS: List<Int> = listOf(0x0C, 0x0E, 0x06)
        val REQUIRED_HOT_PIDS: List<Int> = listOf(0x0D, 0x05, 0x04)

        /** Szybki A — `n % 4 == 0`. */
        val DEFAULT_FAST_PIDS: List<Int> = listOf(0x23, 0x0B, 0x11, 0x4C, 0x49, 0x43)

        /** Średni B — `n % 10 == 5`. PID `0103` to status układu paliwowego, nie tryb 03. */
        val DEFAULT_MEDIUM_PIDS: List<Int> = listOf(0x3C, 0x44, 0x2E, 0x03, 0x07, 0x42)

        /** Wolny C — `n % 20 == 13`. Dwa miejsca wolne: `012F` wypadł (P1). */
        val DEFAULT_SLOW_PIDS: List<Int> = listOf(0x1F, 0x46, 0x0F, 0x33)
        const val FAST_EVERY_N = 4
        const val FAST_PHASE = 0
        const val MEDIUM_EVERY_N = 10
        const val MEDIUM_PHASE = 5
        const val SLOW_EVERY_N = 20
        const val SLOW_PHASE = 13
        val MIN_COMMAND_GAP: Duration = 20.milliseconds
        const val MAX_QUERIES_PER_SECOND = 25
        const val EMPTY_READS_BEFORE_STOP = 10

        fun composeHotPids(chartSlots: List<Int>): List<Int> {
            val hot = mutableListOf<Int>()
            fun appendUnique(pid: Int) {
                if (hot.size >= MultiPid.MAX_PIDS_PER_QUERY || pid in hot) return
                hot.add(pid)
            }
            for (pid in REQUIRED_HOT_PIDS) appendUnique(pid)
            for (pid in chartSlots.take(3)) appendUnique(pid)
            for (pid in DEFAULT_CHART_SLOTS) appendUnique(pid)
            return hot
        }
    }

    var queryMode: PidQueryMode? = config.queryMode
        private set
    var totalQueries: Int = 0
        private set
    var hotCycles: Int = 0
        private set
    var measuredHotHz: Double = 0.0
        private set

    private var consecutiveEmpty = 0
    private var paceMultiplier = 1.0
    private var lastCommandEnd = 0.0
    private val queryTimestamps = mutableListOf<Double>()
    private val hotCycleStarts = mutableListOf<Double>()

    fun events(): Flow<SampleTick> = flow { runLoop() }

    private suspend fun FlowCollector<SampleTick>.runLoop() {
        val mode = queryMode ?: PidBatchReader.probe(session).also {
            queryMode = it
            totalQueries += 1
        }
        val hotPids = composeHotPids(config.chartSlots)

        while (currentCoroutineContext().isActive) {
            val cycleStart = config.clock.seconds()
            recordHotCycleStart(cycleStart)

            respectGapsAndCeiling()
            val hotReadings = PidBatchReader.read(session, hotPids, mode)
            noteQueries(queryCount(hotPids, mode))
            lastCommandEnd = config.clock.seconds()
            hotCycles += 1

            registerReadResult(hotReadings)
            emit(SampleTick(SampleTick.Kind.Hot, lastCommandEnd, hotReadings))

            if (config.simulatedCycleWork > Duration.ZERO) {
                config.clock.advance(config.simulatedCycleWork)
            }

            if (hotCycles % FAST_EVERY_N == FAST_PHASE && config.fastPids.isNotEmpty()) {
                respectGapsAndCeiling()
                val fastReadings = PidBatchReader.read(session, config.fastPids, mode)
                noteQueries(queryCount(config.fastPids, mode))
                lastCommandEnd = config.clock.seconds()
                registerReadResult(fastReadings)
                emit(SampleTick(SampleTick.Kind.Fast, lastCommandEnd, fastReadings))
            }

            if (hotCycles % MEDIUM_EVERY_N == MEDIUM_PHASE && config.mediumPids.isNotEmpty()) {
                respectGapsAndCeiling()
                val mediumReadings = PidBatchReader.read(session, config.mediumPids, mode)
                noteQueries(queryCount(config.mediumPids, mode))
                lastCommandEnd = config.clock.seconds()
                registerReadResult(mediumReadings)
                emit(SampleTick(SampleTick.Kind.Medium, lastCommandEnd, mediumReadings))
            }

            if (hotCycles % SLOW_EVERY_N == SLOW_PHASE && config.slowPids.isNotEmpty()) {
                respectGapsAndCeiling()
                val slowReadings = PidBatchReader.read(session, config.slowPids, mode)
                noteQueries(queryCount(config.slowPids, mode))
                lastCommandEnd = config.clock.seconds()
                registerReadResult(slowReadings)
                emit(SampleTick(SampleTick.Kind.Slow, lastCommandEnd, slowReadings))
            }

            val max = config.maxHotCycles
            if (max != null && hotCycles >= max) return

            pace(cycleStart)
        }
    }

    private fun queryCount(pids: List<Int>, mode: PidQueryMode): Int = when (mode) {
        PidQueryMode.Multi -> (pids.size + MultiPid.MAX_PIDS_PER_QUERY - 1) / MultiPid.MAX_PIDS_PER_QUERY
        PidQueryMode.Single -> pids.size
    }

    private fun noteQueries(count: Int) {
        totalQueries += count
        val now = config.clock.seconds()
        repeat(count) { queryTimestamps.add(now) }
        val cutoff = now - 1.0
        queryTimestamps.removeAll { it < cutoff }
    }

    private fun recordHotCycleStart(t: Double) {
        hotCycleStarts.add(t)
        val cutoff = t - 2.0
        hotCycleStarts.removeAll { it < cutoff }
        if (hotCycleStarts.size >= 2) {
            val first = hotCycleStarts.first()
            val dt = t - first
            if (dt > 0) measuredHotHz = (hotCycleStarts.size - 1) / dt
        }
    }

    private fun registerReadResult(readings: List<MultiPidReading>) {
        if (readings.isEmpty()) {
            consecutiveEmpty += 1
            paceMultiplier = minOf(paceMultiplier * 1.5, 8.0)
            if (consecutiveEmpty >= EMPTY_READS_BEFORE_STOP) {
                throw SampleStreamError.TooManyEmptyReads
            }
        } else {
            consecutiveEmpty = 0
            paceMultiplier = maxOf(1.0, paceMultiplier * 0.9)
            if (paceMultiplier < 1.05) paceMultiplier = 1.0
        }
    }

    private suspend fun respectGapsAndCeiling() {
        val now = config.clock.seconds()
        val sinceLast = now - lastCommandEnd
        val minGap = MIN_COMMAND_GAP.toDouble(DurationUnit.SECONDS)
        if (lastCommandEnd > 0 && sinceLast < minGap) {
            config.clock.sleep((minGap - sinceLast).toDuration(DurationUnit.SECONDS))
        }

        val t = config.clock.seconds()
        val recent = queryTimestamps.count { it >= t - 1.0 }
        if (recent >= MAX_QUERIES_PER_SECOND) {
            val oldest = queryTimestamps.filter { it >= t - 1.0 }.minOrNull() ?: t
            val wait = maxOf(0.0, 1.0 - (t - oldest))
            if (wait > 0) config.clock.sleep(wait.toDuration(DurationUnit.SECONDS))
        }
    }

    private suspend fun pace(cycleStart: Double) {
        val target = config.rate.targetInterval
        if (target == null) {
            config.clock.sleep(MIN_COMMAND_GAP)
            return
        }
        val scaled = target.toDouble(DurationUnit.SECONDS) * paceMultiplier
        val elapsed = config.clock.seconds() - cycleStart
        val remaining = scaled - elapsed
        if (remaining > 0) {
            config.clock.sleep(remaining.toDuration(DurationUnit.SECONDS))
        }
    }
}
