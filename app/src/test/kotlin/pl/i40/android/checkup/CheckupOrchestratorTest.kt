package pl.i40.android.checkup

import java.io.File
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.i40.android.transport.MockI40Script
import pl.i40.android.transport.MockTransport
import pl.i40.android.transport.Transport

class CheckupOrchestratorTest {
    private val slownik: Map<String, String> = File("src/main/res/raw/dtc_dictionary.json").readText()
        .let { json ->
            val wpis = Regex("\"(P[0-9A-Z]+)\":\\s*\"([^\"]*)\"")
            wpis.findAll(json).associate { it.groupValues[1] to it.groupValues[2] }
        }

    private fun orkiestrator() = CheckupOrchestrator(
        slownikDtc = slownik,
        terazMs = { 1_000L },
        timeout = 5.seconds,
        maxRetries = 0
    )

    @Test
    fun timeoutPrzegladuTo25sIDwaPonowienia() {
        assertEquals(25.seconds, CheckupOrchestrator.TIMEOUT_PRZEGLADU)
        assertEquals(2, CheckupOrchestrator.PONOWIENIA_PRZEGLADU)
    }

    @Test
    fun pelnaSesjaOdPolaczeniaDoWerdyktuNaAtrapie() = runTest {
        val transport = MockTransport(script = MockI40Script.make(), timeScale = 0.0)
        val raport = orkiestrator().uruchom(
            transport = transport,
            zrodlo = ZrodloRaportu.Atrapa,
            scope = backgroundScope
        )

        assertEquals(ZrodloRaportu.Atrapa, raport.zrodlo)
        assertEquals("KMHLC41DAFU066558", raport.pojazd.vin)
        assertEquals(2015, raport.pojazd.rokModelu)
        assertEquals("Ulsan", raport.pojazd.fabryka)
        assertEquals("GGVF-EE5AFS01600", raport.pojazd.kalibracja)
        assertEquals("ECM-EngineControl", raport.pojazd.nazwaEcu)

        assertTrue(raport.adapter.firmware?.contains("ELM327") == true)
        assertEquals("A6", raport.adapter.kodProtokolu)
        assertEquals(14.2, raport.adapter.napieciePin16)

        assertEquals(false, raport.gotowosc?.milOn)
        assertEquals(true, raport.gotowosc?.ready)
        assertTrue(raport.kodyZapisane.isEmpty())
        assertTrue(raport.kodyOczekujace.isEmpty())
        assertNull(raport.kodyTrwale)

        val rpm = raport.odczyty.first { it.pid == 0x0C }
        assertTrue(rpm.dostepny)
        assertEquals(925.5, rpm.wartosc)

        assertNull(raport.odczyty.firstOrNull { it.pid == 0x5C })
        assertTrue(0x0C in raport.obslugiwanePid)
        assertFalse(0x5C in raport.obslugiwanePid)
        assertEquals(Werdykt.Ok, raport.werdykt)

        assertEquals(1, transport.closeCount)
        assertFalse(transport.isOpen)
    }

    @Test
    fun poPrzegladzieMoznaZostawicTransportOtwarty() = runTest {
        val transport = MockTransport(script = MockI40Script.make(), timeScale = 0.0)
        orkiestrator().uruchom(
            transport = transport,
            zrodlo = ZrodloRaportu.Atrapa,
            scope = backgroundScope,
            rozlaczPo = false
        )
        assertEquals(0, transport.closeCount)
        assertTrue(transport.isOpen)

        orkiestrator().uruchom(
            transport = transport,
            zrodlo = ZrodloRaportu.Atrapa,
            scope = backgroundScope,
            rozlaczPo = false
        )
        assertEquals(0, transport.closeCount)
        assertTrue(transport.isOpen)
    }

    @Test
    fun nieWysyla0160Gdy0140BezKontynuacji() = runTest {
        val inner = MockTransport(script = MockI40Script.make(), timeScale = 0.0)
        val transport = RecordingTransport(inner)
        orkiestrator().uruchom(
            transport = transport,
            zrodlo = ZrodloRaportu.Atrapa,
            scope = backgroundScope
        )
        assertTrue("0120" in transport.polecenia)
        assertTrue("0140" in transport.polecenia)
        assertFalse("0160" in transport.polecenia)
    }
}

private class RecordingTransport(private val inner: Transport) : Transport {
    val polecenia = mutableListOf<String>()

    override suspend fun open() = inner.open()

    override suspend fun close() = inner.close()

    override suspend fun write(bytes: ByteArray) {
        polecenia.add(bytes.toString(Charsets.UTF_8).trim())
        inner.write(bytes)
    }

    override val chunks: Flow<ByteArray>
        get() = inner.chunks

    override val disconnects: Flow<Unit>
        get() = inner.disconnects
}
