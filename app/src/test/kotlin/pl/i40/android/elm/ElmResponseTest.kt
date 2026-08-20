package pl.i40.android.elm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ElmResponseTest {
    @ParameterizedTest
    @CsvSource(
        "NO DATA, NoData, MarkUnavailableAndContinue",
        "SEARCHING..., Searching, WaitForData",
        "UNABLE TO CONNECT, UnableToConnect, Abort",
        "BUS INIT: ERROR, BusInitError, Abort",
        "CAN ERROR, CanError, Abort",
        "STOPPED, Stopped, Retry",
        "BUFFER FULL, BufferFull, Retry",
        "?, UnknownCommand, LogBug",
    )
    fun kazdyKomunikatZTabeliRozpoznany(line: String, expectedName: String, reactionName: String) {
        val got = ElmResponse.classify(line)
        assertEquals(ElmMessage.valueOf(expectedName), got)
        assertEquals(ElmMessage.Reaction.valueOf(reactionName), got?.reaction)
        assertTrue(ElmResponse.isAdapterMessage(line))
    }

    @Test
    fun rozpoznawanieJestOdporneNaBialaSpacjeIWielkoscLiter() {
        assertEquals(ElmMessage.NoData, ElmResponse.classify("  no data  "))
        assertEquals(ElmMessage.Searching, ElmResponse.classify("searching..."))
        assertEquals(ElmMessage.CanError, ElmResponse.classify("Can Error"))
    }

    @Test
    fun zwyklaOdpowiedzHexNieJestKomunikatem() {
        assertNull(ElmResponse.classify("410C0BB8"))
        assertNull(ElmResponse.classify("OK"))
        assertTrue(!ElmResponse.isAdapterMessage("4100BE3EA813"))
        assertNull(ElmResponse.classify("41NO DATA00"))
    }

    @Test
    fun messagesZWieloliniowejOdpowiedzi() {
        val text = "SEARCHING...\r\n4100BE3EA813\r\nNO DATA\r\n"
        assertEquals(listOf(ElmMessage.Searching, ElmMessage.NoData), ElmResponse.messages(text))
    }
}
