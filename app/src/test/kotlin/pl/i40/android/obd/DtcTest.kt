package pl.i40.android.obd

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DtcTest {
    private val dict: Map<String, String> = File("src/main/res/raw/dtc_dictionary.json").readText()
        .let { json ->
            val wpis = Regex("\"(P[0-9A-Z]+)\":\\s*\"([^\"]*)\"")
            wpis.findAll(json).associate { it.groupValues[1] to it.groupValues[2] }
        }

    @Test
    fun bajtyNaKod() {
        assertEquals("P0171", DtcDecode.code(0x01, 0x71))
        assertEquals(null, DtcDecode.code(0x00, 0x00))
        assertEquals("P0300", DtcDecode.code(0x03, 0x00))
        assertEquals("U0100", DtcDecode.code(0xC1, 0x00))
    }

    @Test
    fun rozroznienieProducenckichISlownika() {
        val p0171 = DtcDecode.describe("P0171", dict)
        assertEquals(DtcKind.Generic, p0171.kind)
        assertEquals("Mieszanka zbyt uboga, bank 1", p0171.description)

        val p1 = DtcDecode.describe("P1128", dict)
        assertEquals(DtcKind.Manufacturer, p1.kind)
        assertEquals("kod producencki Hyundai, opis nieznany", p1.description)

        val missing = DtcDecode.describe("P2187", dict)
        assertTrue(missing.description.contains("brak wpisu"))
    }

    @Test
    fun tryby03() {
        val one = DtcDecode.codes(fromHexResponse = "430171\r>", dictionary = dict)
        assertEquals(listOf("P0171"), one.map { it.code })
        assertTrue(DtcDecode.codes(fromHexResponse = "NO DATA\r>", dictionary = dict).isEmpty())
    }
}
