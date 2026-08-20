package pl.i40.android.acquisition

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.i40.android.elm.ElmSession
import pl.i40.android.obd.MultiPid
import pl.i40.android.transport.MockScriptEntry
import pl.i40.android.transport.MockTransport

class TestSampleClock : SampleClock {
    private var t = 0.0
    val pacingSleeps = mutableListOf<Double>()

    override fun seconds(): Double = t

    override fun advance(duration: Duration) {
        t += duration.toDouble(DurationUnit.SECONDS)
    }

    override suspend fun sleep(forDuration: Duration) {
        val s = forDuration.toDouble(DurationUnit.SECONDS)
        pacingSleeps.add(s)
        t += s
    }
}

class SampleStreamTest {
    private val hotResponse = "410D00055C043C0C0E760E87067F\r\r>"

    @Test
    fun skladaGoracaSzostkeZWymaganychIGniazd() {
        val hot = SampleStream.composeHotPids(chartSlots = listOf(0x0C, 0x0E, 0x06))
        assertEquals(6, hot.size)
        assertTrue(hot.containsAll(listOf(0x0D, 0x05, 0x04, 0x0C, 0x0E, 0x06)))
        assertEquals(listOf(0x0D, 0x05, 0x04), hot.take(3))
    }

    @Test
    fun trzyGniazdaNieWypychajaPredkosciPlynuAniObciazenia() {
        val hot = SampleStream.composeHotPids(chartSlots = listOf(0x11, 0x23, 0x2E))
        assertEquals(6, hot.size)
        assertTrue(hot.contains(0x0D))
        assertTrue(hot.contains(0x05))
        assertTrue(hot.contains(0x04))
    }

    @Test
    fun domyslnaRotacjaZimnaMaPiecPidBez2f5c5e10() {
        val cold = SampleStream.DEFAULT_COLD_PIDS
        assertEquals(listOf(0x46, 0x1F, 0x42, 0x0F, 0x07), cold)
        assertTrue(0x2F !in cold)
        assertTrue(0x5C !in cold)
        assertTrue(0x5E !in cold)
        assertTrue(0x10 !in cold)
    }

    @Test
    fun przyCyklu50msI4HzNieKolejkujeICelujeWInterwal() = runTest {
        val clock = TestSampleClock()
        val (transport, session) = makeSession(backgroundScope)
        val stream = SampleStream(
            session,
            SampleStream.Configuration(
                rate = SampleRate.Balanced,
                queryMode = PidQueryMode.Multi,
                clock = clock,
                simulatedCycleWork = 50.milliseconds,
                maxHotCycles = 2
            )
        )
        val hotTicks = stream.events().toList().count { it.kind == SampleTick.Kind.Hot }
        assertEquals(2, hotTicks)
        val pacing = clock.pacingSleeps.filter { it >= 0.15 }
        assertTrue(pacing.isNotEmpty())
        assertTrue(pacing.all { it <= 0.26 })
        assertTrue(stream.totalQueries >= 2)
        session.stop()
        transport.close()
    }

    @Test
    fun przyCyklu400msNieDokladaSnuPacingowego() = runTest {
        val clock = TestSampleClock()
        val (transport, session) = makeSession(backgroundScope)
        val stream = SampleStream(
            session,
            SampleStream.Configuration(
                rate = SampleRate.Balanced,
                queryMode = PidQueryMode.Multi,
                clock = clock,
                simulatedCycleWork = 400.milliseconds,
                maxHotCycles = 2
            )
        )
        val hotTicks = stream.events().toList().count { it.kind == SampleTick.Kind.Hot }
        assertEquals(2, hotTicks)
        assertTrue(clock.pacingSleeps.all { it < 0.05 })
        session.stop()
        transport.close()
    }

    @Test
    fun zimnaRotacjaNaFazieNMod10Rowna5() = runTest {
        val clock = TestSampleClock()
        val extra = MockScriptEntry(command = "0142", response = "41423795\r\r>")
        val (transport, session) = makeSession(backgroundScope, extra)
        val stream = SampleStream(
            session,
            SampleStream.Configuration(
                coldPids = listOf(0x42),
                rate = SampleRate.Detailed,
                queryMode = PidQueryMode.Multi,
                clock = clock,
                simulatedCycleWork = Duration.ZERO,
                maxHotCycles = 10
            )
        )
        val ticks = stream.events().toList()
        val cold = ticks.filter { it.kind == SampleTick.Kind.Cold }
        assertEquals(1, cold.size)
        assertTrue(cold[0].readings.any { it.pid == 0x42 })
        assertEquals(10, ticks.count { it.kind == SampleTick.Kind.Hot })
        session.stop()
        transport.close()
    }

    @Test
    fun zimnaRotacjaNieWchodziPrzedCykl5() = runTest {
        val clock = TestSampleClock()
        val extra = MockScriptEntry(command = "0142", response = "41423795\r\r>")
        val (transport, session) = makeSession(backgroundScope, extra)
        val stream = SampleStream(
            session,
            SampleStream.Configuration(
                coldPids = listOf(0x42),
                rate = SampleRate.Detailed,
                queryMode = PidQueryMode.Multi,
                clock = clock,
                maxHotCycles = 4
            )
        )
        val ticks = stream.events().toList()
        assertEquals(0, ticks.count { it.kind == SampleTick.Kind.Cold })
        session.stop()
        transport.close()
    }

    @Test
    fun seriaPustychOdczytowZwalniaIZatrzymuje() = runTest {
        val clock = TestSampleClock()
        val hotCmd = MultiPid.command(SampleStream.composeHotPids(SampleStream.DEFAULT_CHART_SLOTS))
        val transport = MockTransport(
            listOf(
                MockScriptEntry(command = "ATE0", response = "OK\r\r>"),
                MockScriptEntry(command = hotCmd, response = "NO DATA\r\r>")
            )
        )
        transport.open()
        val session = ElmSession(transport, timeout = 2.seconds, maxRetries = 0)
        session.start(backgroundScope)
        session.send("ATE0")

        val stream = SampleStream(
            session,
            SampleStream.Configuration(
                rate = SampleRate.Balanced,
                queryMode = PidQueryMode.Multi,
                clock = clock,
                coldPids = emptyList()
            )
        )
        val thrown = runCatching { stream.events().toList() }.exceptionOrNull()
        assertTrue(thrown is SampleStreamError.TooManyEmptyReads)
        val pacing = clock.pacingSleeps.filter { it >= 0.3 }
        assertTrue(pacing.isNotEmpty())
        session.stop()
        transport.close()
    }

    private suspend fun makeSession(
        scope: CoroutineScope,
        extra: MockScriptEntry? = null
    ): Pair<MockTransport, ElmSession> {
        val hotCmd = MultiPid.command(SampleStream.composeHotPids(SampleStream.DEFAULT_CHART_SLOTS))
        val script = mutableListOf(
            MockScriptEntry(command = "ATE0", response = "OK\r\r>"),
            MockScriptEntry(command = hotCmd, response = hotResponse)
        )
        if (extra != null) script.add(extra)
        val transport = MockTransport(script)
        transport.open()
        val session = ElmSession(transport, timeout = 2.seconds, maxRetries = 0)
        session.start(scope)
        session.send("ATE0")
        return transport to session
    }
}
