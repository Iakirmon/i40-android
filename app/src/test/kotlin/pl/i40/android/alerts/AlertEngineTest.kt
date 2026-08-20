package pl.i40.android.alerts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AlertEngineTest {
    @Test
    fun plynPowyzej105() {
        val events = AlertRules.evaluate(AlertSnapshot(coolantC = 105.1))
        assertEquals(listOf(AlertKind.CoolantHot), events.map { it.kind })
        assertEquals(AlertSeverity.Urgent, events[0].severity)
    }

    @Test
    fun plynDokladnie105BezAlarmu() {
        assertTrue(AlertRules.evaluate(AlertSnapshot(coolantC = 105.0)).isEmpty())
    }

    @Test
    fun niskieNapieciePrzyObrotach() {
        val events = AlertRules.evaluate(AlertSnapshot(voltageV = 12.9, rpm = 501.0))
        assertEquals(listOf(AlertKind.LowVoltage), events.map { it.kind })
    }

    @Test
    fun niskieNapiecieNaPostojuBezAlarmu() {
        assertTrue(AlertRules.evaluate(AlertSnapshot(voltageV = 12.0, rpm = 0.0)).isEmpty())
        assertTrue(AlertRules.evaluate(AlertSnapshot(voltageV = 12.0, rpm = 500.0)).isEmpty())
    }

    @Test
    fun nowyKodBleduWTrakcieSesji() {
        val events = AlertRules.evaluate(
            AlertSnapshot(dtcsAtStart = emptySet(), dtcsNow = setOf("P0301"))
        )
        assertEquals(listOf(AlertKind.NewDtc), events.map { it.kind })
    }

    @Test
    fun tenSamKodNaStarcieBezAlarmu() {
        assertTrue(
            AlertRules.evaluate(
                AlertSnapshot(dtcsAtStart = setOf("P0301"), dtcsNow = setOf("P0301"))
            ).isEmpty()
        )
    }

    @Test
    fun zimnyOlejPrzyWysokichObrotach() {
        val events = AlertRules.evaluate(AlertSnapshot(oilC = 89.9, rpm = 4001.0))
        assertEquals(listOf(AlertKind.ColdOilHighRpm), events.map { it.kind })
        assertEquals(AlertSeverity.Info, events[0].severity)
    }

    @Test
    fun kilkaRegulNaraz() {
        val events = AlertRules.evaluate(
            AlertSnapshot(
                coolantC = 110.0,
                oilC = 80.0,
                voltageV = 12.5,
                rpm = 4500.0,
                dtcsAtStart = emptySet(),
                dtcsNow = setOf("P0171")
            )
        )
        assertEquals(
            setOf(AlertKind.CoolantHot, AlertKind.LowVoltage, AlertKind.NewDtc, AlertKind.ColdOilHighRpm),
            events.map { it.kind }.toSet()
        )
    }

    @Test
    fun karencja60sDlaJednorazowych() {
        val engine = AlertEngine()
        val snap = AlertSnapshot(voltageV = 12.5, rpm = 800.0)
        assertEquals(listOf(AlertKind.LowVoltage), engine.evaluate(snap, 0.0).map { it.kind })
        assertTrue(engine.evaluate(snap, 30.0).isEmpty())
        assertTrue(engine.evaluate(snap, 59.9).isEmpty())
        assertEquals(listOf(AlertKind.LowVoltage), engine.evaluate(snap, 60.0).map { it.kind })
    }

    @Test
    fun pilnyPowtarzaCo10s() {
        val engine = AlertEngine()
        val snap = AlertSnapshot(coolantC = 110.0)
        assertEquals(listOf(AlertKind.CoolantHot), engine.evaluate(snap, 0.0).map { it.kind })
        assertTrue(engine.evaluate(snap, 9.0).isEmpty())
        assertEquals(listOf(AlertKind.CoolantHot), engine.evaluate(snap, 10.0).map { it.kind })
        assertEquals(listOf(AlertKind.CoolantHot), engine.evaluate(snap, 20.0).map { it.kind })
    }

    @Test
    fun kat2Powyzej870() {
        val events = AlertRules.evaluate(AlertSnapshot(temperaturaKatalizatoraC = 880.0))
        assertEquals(listOf(AlertKind.CatalystHot), events.map { it.kind })
        assertEquals(AlertSeverity.Warning, events[0].severity)
        assertTrue(AlertRules.evaluate(AlertSnapshot(temperaturaKatalizatoraC = 870.0)).isEmpty())
        assertTrue(AlertRules.evaluate(AlertSnapshot()).isEmpty())
    }

    @Test
    fun kat2Karencja60s() {
        val engine = AlertEngine()
        val snap = AlertSnapshot(temperaturaKatalizatoraC = 880.0)
        assertEquals(listOf(AlertKind.CatalystHot), engine.evaluate(snap, 0.0).map { it.kind })
        assertTrue(engine.evaluate(snap, 59.9).isEmpty())
        assertEquals(listOf(AlertKind.CatalystHot), engine.evaluate(snap, 60.0).map { it.kind })
    }

    @Test
    fun kat2NieMaWagiUsterka() {
        assertEquals(AlertSeverity.Warning, AlertKind.CatalystHot.severity)
    }
}
