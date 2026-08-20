package pl.i40.android.obd

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReadinessTest {
    @Test
    fun i40MilZgaszonaMonitoryGotowe() {
        val s = Readiness.decode(hexResponse = "41010007E100\r>")!!
        assertFalse(s.milOn)
        assertEquals(0, s.storedDtcCount)
        assertEquals(IgnitionType.Spark, s.ignition)
        assertTrue(s.ready)
        assertEquals(
            listOf("Wypadanie zapłonu", "Układ paliwowy", "Komponenty"),
            s.continuous.map { it.name },
        )
        assertEquals(
            listOf("Katalizator", "Sondy tlenu", "Podgrzewanie sond tlenu", "Układ EGR"),
            s.monitors.map { it.name },
        )
        assertFalse(s.monitors.any { it.name == "Powietrze wtórne" })
    }

    @Test
    fun rozrozniaNieobslugiwanyINiegotowy() {
        val s = Readiness.decode(a = 0x82, b = 0x17, c = 0x05, d = 0x04)
        assertTrue(s.milOn)
        assertEquals(2, s.storedDtcCount)
        assertFalse(s.ready)
        assertEquals(
            listOf("Wypadanie zapłonu", "Układ odparowania paliwa"),
            s.incomplete.map { it.name },
        )
    }

    @Test
    fun zlaOdpowiedz() {
        assertNull(Readiness.decode(hexResponse = "NO DATA\r>"))
    }
}
