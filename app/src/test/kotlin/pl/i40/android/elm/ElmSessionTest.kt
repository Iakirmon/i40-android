package pl.i40.android.elm

import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.i40.android.transport.MockScriptEntry
import pl.i40.android.transport.MockTransport
import pl.i40.android.transport.MockTransportError
import pl.i40.android.transport.Transport

class ElmSessionTest {
    @Test
    fun ramkowanieSkladaPocietaOdpowiedz() = runTest {
        val transport = MockTransport(
            script = listOf(
                MockScriptEntry(command = "ATE0", response = "OK\r\r>"),
                MockScriptEntry(
                    command = "010C",
                    chunks = listOf(
                        "41".toByteArray(),
                        "0C0B".toByteArray(),
                        "B8\r".toByteArray(),
                        ">".toByteArray(),
                    ),
                ),
            ),
        )
        transport.open()
        val session = ElmSession(transport, timeout = 2.seconds)
        session.start(backgroundScope)

        session.send("ATE0")
        val text = session.send("010C")
        val compact = text.filter { it != '\n' && it != '\r' }
        assertEquals("410C0BB8", compact)

        session.stop()
        transport.close()
    }

    @Test
    fun ramkowanieRozdzielaDwieOdpowiedziWJednymKawalku() = runTest {
        val transport = MockTransport(
            script = listOf(
                MockScriptEntry(command = "ATE0", response = "OK\r\r>"),
                MockScriptEntry(
                    command = "010C",
                    chunks = listOf("410C0BB8\r>410C0C1A\r>".toByteArray()),
                ),
                MockScriptEntry(command = "010D", response = "410D00\r>"),
            ),
        )
        transport.open()
        val session = ElmSession(transport, timeout = 2.seconds)
        session.start(backgroundScope)

        session.send("ATE0")
        val first = session.send("010C")
        assertTrue(first.contains("410C0BB8"))
        assertTrue(!first.contains("410C0C1A"))

        val second = session.send("010D")
        assertTrue(second.contains("410D00"))

        session.stop()
        transport.close()
    }

    @Test
    fun szeregowanieDziesieciuWspolbieznychPolecen() = runTest {
        val probe = ProbeTransport()
        probe.open()
        val session = ElmSession(transport = probe, timeout = 2.seconds, maxRetries = 0)
        session.start(backgroundScope)

        val wyniki = (1..10).map { i ->
            async { session.send("CMD$i") }
        }.awaitAll()

        assertTrue(wyniki.all { it.contains("OK") })
        assertEquals((1..10).map { "CMD$it\r" }, probe.writeLog)
        assertEquals(1, probe.maxInFlightWrites)

        session.stop()
        probe.close()
    }

    @Test
    fun timeoutIPonowienie() = runTest {
        val probe = ProbeTransport(responseDelay = 0.milliseconds)
        probe.silenceNextResponses = 1
        probe.open()
        val session = ElmSession(
            transport = probe,
            timeout = 80.milliseconds,
            maxRetries = 2,
        )
        session.start(backgroundScope)

        val text = session.send("ATI")
        assertTrue(text.contains("OK"))
        assertEquals(2, probe.writeLog.count { it == "ATI\r" })

        session.stop()
        probe.close()
    }

    @Test
    fun stoppedPowodujePonowienie() = runTest {
        val transport = MockTransport(
            script = listOf(
                MockScriptEntry(command = "ATE0", response = "OK\r\r>"),
                MockScriptEntry(command = "0100", response = "STOPPED\r>"),
                MockScriptEntry(command = "0100", response = "4100BE3EA813\r>"),
            ),
        )
        transport.open()
        val session = ElmSession(transport, timeout = 2.seconds, maxRetries = 2)
        session.start(backgroundScope)

        session.send("ATE0")
        val text = session.send("0100")
        assertTrue(text.contains("4100BE3EA813"))

        session.stop()
        transport.close()
    }
}

/**
 * Transport testowy: rejestruje zapisy i mierzy równoległość.
 */
private class ProbeTransport(private val responseDelay: kotlin.time.Duration = 5.milliseconds) : Transport {
    private val writeLogMut = mutableListOf<String>()
    val writeLog: List<String>
        get() = writeLogMut.toList()

    var maxInFlightWrites = 0
        private set
    private val inFlightWrites = AtomicInteger(0)

    var silenceNextResponses = 0

    private var openFlag = false
    private val chunksChannel = Channel<ByteArray>(Channel.UNLIMITED)
    private val disconnectsChannel = Channel<Unit>(Channel.UNLIMITED)

    override val chunks: Flow<ByteArray> = chunksChannel.receiveAsFlow()
    override val disconnects: Flow<Unit> = disconnectsChannel.receiveAsFlow()

    override suspend fun open() {
        openFlag = true
        writeLogMut.clear()
        maxInFlightWrites = 0
        inFlightWrites.set(0)
    }

    override suspend fun close() {
        openFlag = false
    }

    override suspend fun write(bytes: ByteArray) {
        if (!openFlag) throw MockTransportError.NotOpen
        val inFlight = inFlightWrites.incrementAndGet()
        maxInFlightWrites = maxOf(maxInFlightWrites, inFlight)
        writeLogMut.add(bytes.toString(Charsets.UTF_8))
        val silent = synchronized(this) {
            if (silenceNextResponses > 0) {
                silenceNextResponses -= 1
                true
            } else {
                false
            }
        }
        try {
            if (responseDelay > kotlin.time.Duration.ZERO) delay(responseDelay)
            if (!silent) {
                chunksChannel.trySend("OK\r\r>".toByteArray())
            }
        } finally {
            inFlightWrites.decrementAndGet()
        }
    }
}
