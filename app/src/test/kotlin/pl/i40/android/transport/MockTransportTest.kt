package pl.i40.android.transport

import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MockTransportTest {
    @Test
    fun atrapaOddajeZapisaneOdpowiedzi() = runTest {
        val transport = MockTransport(
            script = listOf(
                MockScriptEntry(command = "ATI", response = "ELM327 v2.2\r\r>", recordedWithEcho = false),
                MockScriptEntry(command = "ATRV", response = "13.8V\r\r>", recordedWithEcho = false),
            ),
        )

        transport.open()
        // Po open echo jest włączone — jak po ATZ na prawdziwym adapterze.
        transport.write("ATE0\r".toByteArray(Charsets.UTF_8))
        zbierzDoZachety(transport.chunks)

        val ati = async { zbierzDoZachety(transport.chunks) }
        transport.write("ATI\r".toByteArray(Charsets.UTF_8))
        assertEquals("ELM327 v2.2\r\r>", ati.await())

        val atrv = async { zbierzDoZachety(transport.chunks) }
        transport.write("ATRV\r".toByteArray(Charsets.UTF_8))
        assertEquals("13.8V\r\r>", atrv.await())

        transport.close()
    }

    @Test
    fun echoWlaczoneDoklejaPrefiksDoWpisuNagranegoBezEcha() = runTest {
        val transport = MockTransport(
            script = listOf(
                MockScriptEntry(command = "ATRV", response = "13.8V\r\r>", recordedWithEcho = false),
            ),
        )
        transport.open()

        val odpowiedz = async { zbierzDoZachety(transport.chunks) }
        transport.write("ATRV\r".toByteArray(Charsets.UTF_8))
        assertEquals("ATRV\r13.8V\r\r>", odpowiedz.await())
    }

    @Test
    fun echoWylaczoneObcinaNagraneEcho() = runTest {
        val transport = MockTransport(
            script = listOf(
                MockScriptEntry(
                    command = "ATI",
                    response = "ATI\rELM327 v2.2\r\r>",
                    recordedWithEcho = true,
                ),
            ),
        )
        transport.open()
        transport.write("ATE0\r".toByteArray(Charsets.UTF_8))
        zbierzDoZachety(transport.chunks)

        val odpowiedz = async { zbierzDoZachety(transport.chunks) }
        transport.write("ATI\r".toByteArray(Charsets.UTF_8))
        assertEquals("ELM327 v2.2\r\r>", odpowiedz.await())
    }

    @Test
    fun kolejneWystapieniaTegoSamegoPoleceniaDajaKolejneWpisy() = runTest {
        val transport = MockTransport(
            script = listOf(
                MockScriptEntry(command = "ATDPN", response = "A0\r\r>"),
                MockScriptEntry(command = "ATDPN", response = "A6\r\r>"),
            ),
        )
        transport.open()
        transport.write("ATE0\r".toByteArray(Charsets.UTF_8))
        zbierzDoZachety(transport.chunks)

        val pierwsze = async { zbierzDoZachety(transport.chunks) }
        transport.write("ATDPN\r".toByteArray(Charsets.UTF_8))
        assertEquals("A0\r\r>", pierwsze.await())

        val drugie = async { zbierzDoZachety(transport.chunks) }
        transport.write("ATDPN\r".toByteArray(Charsets.UTF_8))
        assertEquals("A6\r\r>", drugie.await())
    }

    @Test
    fun nieznanePolecenieDostajeZnakZapytania() = runTest {
        val transport = MockTransport(script = emptyList())
        transport.open()
        transport.write("ATE0\r".toByteArray(Charsets.UTF_8))
        zbierzDoZachety(transport.chunks)

        val odpowiedz = async { zbierzDoZachety(transport.chunks) }
        transport.write("01FF\r".toByteArray(Charsets.UTF_8))
        assertEquals("?\r\r>", odpowiedz.await())
    }

    @Test
    fun porownaniePoleceniaIgnorujeWielkoscLiterISpacje() = runTest {
        val transport = MockTransport(
            script = listOf(
                MockScriptEntry(command = "010C", response = "410C0E76\r\r>"),
            ),
        )
        transport.open()
        transport.write("ATE0\r".toByteArray(Charsets.UTF_8))
        zbierzDoZachety(transport.chunks)

        val odpowiedz = async { zbierzDoZachety(transport.chunks) }
        transport.write("01 0c\r".toByteArray(Charsets.UTF_8))
        assertEquals("410C0E76\r\r>", odpowiedz.await())
    }

    @Test
    fun dropBudziDisconnects() = runTest {
        val transport = MockTransport(script = emptyList())
        transport.open()
        val rozlaczenie = async { transport.disconnects.first() }
        transport.drop()
        withTimeout(1_000) { rozlaczenie.await() }
    }

    @Test
    fun closeNieBudziDisconnects() = runTest {
        val transport = MockTransport(script = emptyList())
        transport.open()
        val zebrane = mutableListOf<Unit>()
        backgroundScope.launch {
            transport.disconnects.collect { zebrane.add(it) }
        }
        transport.close()
        testScheduler.runCurrent()
        assertTrue(zebrane.isEmpty())
    }

    @Test
    fun zapisZAuta_0100PocieteNaTrzyKawalki() = runTest {
        val transport = MockTransport(script = MockI40Script.make())
        transport.open()
        transport.write("ATE0\r".toByteArray(Charsets.UTF_8))
        zbierzDoZachety(transport.chunks)

        val kawalki = async {
            withTimeout(1_000) {
                transport.chunks.take(3).toList().map { it.toString(Charsets.UTF_8) }
            }
        }
        transport.write("0100\r".toByteArray(Charsets.UTF_8))
        assertEquals(
            listOf("SEARCHING...\r", "4100BE3EA813\r", "\r>"),
            kawalki.await(),
        )
    }

    @Test
    fun zapisZAuta_015C_i_0A_toNoData() = runTest {
        val transport = MockTransport(script = MockI40Script.make())
        transport.open()
        transport.write("ATE0\r".toByteArray(Charsets.UTF_8))
        zbierzDoZachety(transport.chunks)

        val olej = async { zbierzDoZachety(transport.chunks) }
        transport.write("015C\r".toByteArray(Charsets.UTF_8))
        assertEquals("NO DATA\r\r>", olej.await())

        val trwale = async { zbierzDoZachety(transport.chunks) }
        transport.write("0A\r".toByteArray(Charsets.UTF_8))
        assertEquals("NO DATA\r\r>", trwale.await())
    }

    @Test
    fun zapisZAuta_dwaWpis0101() = runTest {
        val transport = MockTransport(script = MockI40Script.make())
        transport.open()
        transport.write("ATE0\r".toByteArray(Charsets.UTF_8))
        zbierzDoZachety(transport.chunks)

        val pierwsze = async { zbierzDoZachety(transport.chunks) }
        transport.write("0101\r".toByteArray(Charsets.UTF_8))
        assertEquals("41010007E100\r\r>", pierwsze.await())

        val drugie = async { zbierzDoZachety(transport.chunks) }
        transport.write("0101\r".toByteArray(Charsets.UTF_8))
        assertEquals("41010007E100\r\r>", drugie.await())
    }

    @Test
    fun manifestNieMaSkanowaniaAniLokalizacji() {
        val xml = File("src/main/AndroidManifest.xml").readText()
        assertFalse(xml.contains("BLUETOOTH_SCAN"))
        assertFalse(xml.contains("ACCESS_FINE_LOCATION"))
        assertFalse(xml.contains("ACCESS_COARSE_LOCATION"))
        assertTrue(xml.contains("BLUETOOTH_CONNECT"))
    }

    @Test
    fun slownikDtcMaDokladnie41WpisowZeSpecu() {
        val json = File("src/main/res/raw/dtc_dictionary.json").readText()
        val kody = Regex("\"(P[0-9A-Z]+)\"").findAll(json).map { it.groupValues[1] }.toSet()
        assertEquals(41, kody.size)
        assertEquals(OCZEKIWANE_DTC, kody)
    }
}

private val OCZEKIWANE_DTC = setOf(
    "P0100", "P0101", "P0102", "P0103", "P0104",
    "P0110", "P0111", "P0112", "P0113",
    "P0115", "P0116", "P0117", "P0118",
    "P0120", "P0121", "P0122", "P0123",
    "P0130", "P0131", "P0132", "P0133", "P0134", "P0135",
    "P0171", "P0172",
    "P0300", "P0301", "P0302", "P0303", "P0304",
    "P0325", "P0335", "P0340",
    "P0401", "P0420", "P0442", "P0455",
    "P0500", "P0505", "P0562", "P0563",
)

private suspend fun zbierzDoZachety(kawalki: Flow<ByteArray>): String {
    val bufor = ByteArrayOutputStream()
    kawalki.first { kawalek ->
        bufor.write(kawalek)
        bufor.toByteArray().contains('>'.code.toByte())
    }
    return bufor.toString(Charsets.UTF_8.name())
}
