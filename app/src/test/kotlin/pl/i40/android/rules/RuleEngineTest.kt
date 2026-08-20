package pl.i40.android.rules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RuleEngineTest {
    private fun ids(input: RuleInput) = RuleEngine.evaluate(input).map { it.ruleId }

    private fun insight(input: RuleInput, ruleId: String) = RuleEngine.evaluate(input).first { it.ruleId == ruleId }

    @Test
    fun milSwieci() {
        val i = insight(RuleInput(milOn = true), "mil_on")
        assertEquals(WagaWniosku.Usterka, i.waga)
        assertTrue(i.szczegol.contains("potwierdzony"))
    }

    @Test
    fun kodyZapisane() {
        assertEquals(WagaWniosku.Usterka, insight(RuleInput(storedCodeCount = 1), "stored_dtc").waga)
    }

    @Test
    fun kodyOczekujace() {
        val i = insight(RuleInput(pendingCodeCount = 2), "pending_dtc")
        assertEquals(WagaWniosku.Uwaga, i.waga)
        assertTrue(i.szczegol.contains("niepotwierdzone"))
    }

    @Test
    fun ltftUboga() {
        val i = insight(RuleInput(longTermFuelTrim = 12.0), "ltft_lean")
        assertEquals(WagaWniosku.Uwaga, i.waga)
        assertTrue(i.szczegol.contains("uboga"))
        assertFalse(ids(RuleInput(longTermFuelTrim = 10.0)).contains("ltft_lean"))
    }

    @Test
    fun ltftBogata() {
        val i = insight(RuleInput(longTermFuelTrim = -11.0), "ltft_rich")
        assertEquals(WagaWniosku.Uwaga, i.waga)
        assertTrue(i.szczegol.contains("bogata"))
    }

    @Test
    fun sumaKorekt() {
        val i = insight(RuleInput(longTermFuelTrim = 12.0, shortTermFuelTrim = 10.0), "trim_sum")
        assertEquals(WagaWniosku.Usterka, i.waga)
        assertTrue(i.szczegol.contains("20%"))
    }

    @Test
    fun termostat() {
        val i = insight(RuleInput(coolantCelsius = 65.0, runtimeSeconds = 11 * 60.0), "thermostat")
        assertEquals(WagaWniosku.Uwaga, i.waga)
        assertTrue(i.szczegol.contains("termostat"))
        assertFalse(ids(RuleInput(coolantCelsius = 65.0, runtimeSeconds = 5 * 60.0)).contains("thermostat"))
    }

    @Test
    fun przegrzewanie() {
        assertEquals(WagaWniosku.Usterka, insight(RuleInput(coolantCelsius = 106.0), "overheat").waga)
    }

    @Test
    fun alternator() {
        val i = insight(RuleInput(voltage = 12.5, rpm = 800.0), "alternator_low")
        assertEquals(WagaWniosku.Uwaga, i.waga)
        assertTrue(i.szczegol.contains("Alternator"))
        assertFalse(ids(RuleInput(voltage = 12.5, rpm = 0.0)).contains("alternator_low"))
    }

    @Test
    fun przeladowanie() {
        val i = insight(RuleInput(voltage = 15.2), "overcharge")
        assertEquals(WagaWniosku.Uwaga, i.waga)
        assertTrue(i.szczegol.contains("regulatora"))
    }

    @Test
    fun slabAkumulator() {
        val i = insight(RuleInput(voltage = 12.2, rpm = 0.0), "battery_weak")
        assertEquals(WagaWniosku.Uwaga, i.waga)
        assertFalse(ids(RuleInput(voltage = 12.2, rpm = 800.0)).contains("battery_weak"))
    }

    @Test
    fun monitoryNiegotowe() {
        val i = insight(RuleInput(monitorsReady = false), "monitors_not_ready")
        assertEquals(WagaWniosku.Uwaga, i.waga)
        assertTrue(i.szczegol.contains("emisji"))
        assertFalse(ids(RuleInput(monitorsReady = true)).contains("monitors_not_ready"))
    }

    @Test
    fun kodySkasowanoNiedawno() {
        val i = insight(RuleInput(milOn = false, distanceSinceClearKm = 40.0), "codes_cleared_recently")
        assertEquals(WagaWniosku.Informacja, i.waga)
        assertFalse(ids(RuleInput(milOn = true, distanceSinceClearKm = 40.0)).contains("codes_cleared_recently"))
        assertFalse(ids(RuleInput(milOn = false, distanceSinceClearKm = 100.0)).contains("codes_cleared_recently"))
    }

    @Test
    fun olejZimny() {
        val i = insight(RuleInput(oilCelsius = 75.0), "oil_cold")
        assertEquals(WagaWniosku.Informacja, i.waga)
        assertTrue(i.szczegol.contains("rozgrzany"))
        assertFalse(ids(RuleInput(oilCelsius = 90.0)).contains("oil_cold"))
    }

    @Test
    fun brakujaceWartosciNieZgaduja() {
        assertTrue(RuleEngine.evaluate(RuleInput()).isEmpty())
    }
}
