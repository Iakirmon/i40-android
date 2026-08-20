package pl.i40.android.acquisition

import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.i40.android.elm.ElmSession
import pl.i40.android.obd.DecodedPid
import pl.i40.android.transport.MockScriptEntry
import pl.i40.android.transport.MockTransport

class PidBatchReaderTest {
    @Test
    fun odpowiedzWieloPidOznaczaWsparcie() {
        assertTrue(PidBatchReader.responseIndicatesMultiSupport("410C0E760D00055C\r>"))
    }

    @Test
    fun znakZapytaniaAlboJedenPidToBrakWsparcia() {
        assertFalse(PidBatchReader.responseIndicatesMultiSupport("?\r>"))
        assertFalse(PidBatchReader.responseIndicatesMultiSupport("NO DATA\r>"))
        assertFalse(PidBatchReader.responseIndicatesMultiSupport("410C0E76\r>"))
    }

    @Test
    fun probeWybieraMultiGdyAtrapaSkladaDwaPid() = runTest {
        val (transport, session) = session(
            backgroundScope,
            MockScriptEntry(command = "010C0D", response = "410C0E760D00\r\r>")
        )
        assertEquals(PidQueryMode.Multi, PidBatchReader.probe(session, listOf(0x0C, 0x0D)))
        session.stop()
        transport.close()
    }

    @Test
    fun probeWybieraSingleGdyAtrapaNieRozumieWieloPid() = runTest {
        val (transport, session) = session(backgroundScope)
        assertEquals(PidQueryMode.Single, PidBatchReader.probe(session, listOf(0x0C, 0x0D)))
        session.stop()
        transport.close()
    }

    @Test
    fun readSingleOdpytujeKazdyPidOsobno() = runTest {
        val (transport, session) = session(
            backgroundScope,
            MockScriptEntry(command = "010C", response = "410C0E76\r\r>"),
            MockScriptEntry(command = "010D", response = "410D00\r\r>"),
            MockScriptEntry(command = "0105", response = "41055C\r\r>")
        )
        val readings = PidBatchReader.read(session, listOf(0x0C, 0x0D, 0x05), PidQueryMode.Single)
        assertEquals(listOf(0x0C, 0x0D, 0x05), readings.map { it.pid })
        assertEquals(DecodedPid.Numeric(925.5), readings[0].decoded)
        assertEquals(DecodedPid.Numeric(52.0), readings[2].decoded)
        session.stop()
        transport.close()
    }

    @Test
    fun readMultiJednymZapytaniem() = runTest {
        val (transport, session) = session(
            backgroundScope,
            MockScriptEntry(command = "010C0D05", response = "410C0E760D00055C\r\r>")
        )
        val readings = PidBatchReader.read(session, listOf(0x0C, 0x0D, 0x05), PidQueryMode.Multi)
        assertEquals(listOf(0x0C, 0x0D, 0x05), readings.map { it.pid })
        session.stop()
        transport.close()
    }

    @Test
    fun readSinglePomijaPidBezDanych() = runTest {
        val (transport, session) = session(
            backgroundScope,
            MockScriptEntry(command = "010C", response = "410C0E76\r\r>"),
            MockScriptEntry(command = "015C", response = "NO DATA\r\r>"),
            MockScriptEntry(command = "010D", response = "410D3C\r\r>")
        )
        val readings = PidBatchReader.read(session, listOf(0x0C, 0x5C, 0x0D), PidQueryMode.Single)
        assertEquals(listOf(0x0C, 0x0D), readings.map { it.pid })
        assertEquals(DecodedPid.Numeric(60.0), readings[1].decoded)
        session.stop()
        transport.close()
    }

    private suspend fun session(scope: CoroutineScope, vararg extra: MockScriptEntry): Pair<MockTransport, ElmSession> {
        val script = listOf(MockScriptEntry(command = "ATE0", response = "OK\r\r>")) + extra
        val transport = MockTransport(script)
        transport.open()
        val session = ElmSession(transport, timeout = 2.seconds, maxRetries = 0)
        session.start(scope)
        session.send("ATE0")
        return transport to session
    }
}
