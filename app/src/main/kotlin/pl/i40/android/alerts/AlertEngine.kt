package pl.i40.android.alerts

import pl.i40.android.rules.PasmaOdniesienia

enum class AlertSeverity { Urgent, Warning, Info }

enum class AlertKind(val severity: AlertSeverity, val message: String) {
    CoolantHot(AlertSeverity.Urgent, "Temperatura płynu powyżej 105 °C"),
    LowVoltage(AlertSeverity.Warning, "Niskie napięcie przy pracującym silniku"),
    NewDtc(AlertSeverity.Warning, "Nowy kod błędu w trakcie sesji"),
    ColdOilHighRpm(AlertSeverity.Info, "Wysokie obroty przy zimnym oleju"),
    CatalystHot(AlertSeverity.Warning, "Temperatura katalizatora powyżej normalnego zakresu"),
}

data class AlertEvent(val kind: AlertKind) {
    val severity: AlertSeverity get() = kind.severity
    val message: String get() = kind.message
}

data class AlertSnapshot(
    val coolantC: Double? = null,
    val oilC: Double? = null,
    val voltageV: Double? = null,
    val rpm: Double? = null,
    val dtcsAtStart: Set<String> = emptySet(),
    val dtcsNow: Set<String> = emptySet(),
    val temperaturaKatalizatoraC: Double? = null
) {
    companion object {
        fun from(values: Map<Int, Double>, dtcsAtStart: Set<String> = emptySet(), dtcsNow: Set<String> = emptySet()) =
            AlertSnapshot(
                coolantC = values[0x05],
                oilC = values[0x5C],
                voltageV = values[0x42],
                rpm = values[0x0C],
                dtcsAtStart = dtcsAtStart,
                dtcsNow = dtcsNow,
                temperaturaKatalizatoraC = values[0x3C]
            )
    }
}

object AlertRules {
    val COOLANT_LIMIT_C: Double get() = PasmaOdniesienia.plyn.endInclusive
    val VOLTAGE_FLOOR_V: Double get() = PasmaOdniesienia.napieciePraca.start
    const val VOLTAGE_RPM_GATE = 500.0
    val OIL_COLD_LIMIT_C: Double get() = PasmaOdniesienia.OLEJ_MIN_C
    const val OIL_COLD_RPM_GATE = 4000.0

    fun evaluate(snapshot: AlertSnapshot): List<AlertEvent> {
        val out = mutableListOf<AlertEvent>()
        val coolant = snapshot.coolantC
        if (coolant != null && coolant > COOLANT_LIMIT_C) out.add(AlertEvent(AlertKind.CoolantHot))
        val voltage = snapshot.voltageV
        val rpm = snapshot.rpm
        if (voltage != null && rpm != null && voltage < VOLTAGE_FLOOR_V && rpm > VOLTAGE_RPM_GATE) {
            out.add(AlertEvent(AlertKind.LowVoltage))
        }
        if (snapshot.dtcsNow.subtract(snapshot.dtcsAtStart).isNotEmpty()) {
            out.add(AlertEvent(AlertKind.NewDtc))
        }
        val oil = snapshot.oilC
        if (oil != null && rpm != null && oil < OIL_COLD_LIMIT_C && rpm > OIL_COLD_RPM_GATE) {
            out.add(AlertEvent(AlertKind.ColdOilHighRpm))
        }
        val kat = snapshot.temperaturaKatalizatoraC
        if (kat != null && kat > PasmaOdniesienia.progKat2C) {
            out.add(AlertEvent(AlertKind.CatalystHot))
        }
        return out
    }
}

class AlertCooldown {
    companion object {
        const val DEFAULT_INTERVAL_S = 60.0
        const val URGENT_REPEAT_S = 10.0
    }

    private val lastFiredAt = mutableMapOf<AlertKind, Double>()

    fun filter(events: List<AlertEvent>, at: Double): List<AlertEvent> {
        val allowed = mutableListOf<AlertEvent>()
        for (event in events) {
            val interval = if (event.severity == AlertSeverity.Urgent) URGENT_REPEAT_S else DEFAULT_INTERVAL_S
            val previous = lastFiredAt[event.kind]
            if (previous != null && at - previous < interval) continue
            lastFiredAt[event.kind] = at
            allowed.add(event)
        }
        return allowed
    }

    fun reset() {
        lastFiredAt.clear()
    }
}

class AlertEngine {
    private val cooldown = AlertCooldown()

    fun evaluate(snapshot: AlertSnapshot, at: Double): List<AlertEvent> =
        cooldown.filter(AlertRules.evaluate(snapshot), at)

    fun resetCooldown() {
        cooldown.reset()
    }
}
