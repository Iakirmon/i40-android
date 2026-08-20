package pl.i40.android.rules

/**
 * Zestaw odczytów na wejście reguł — czyste dane, bez I/O.
 *
 * Progi: sekcja 10.4 specu, przeniesione z działającego [RuleEngine.swift].
 */
data class RuleInput(
    val milOn: Boolean? = null,
    val storedCodeCount: Int = 0,
    val pendingCodeCount: Int = 0,
    /** PID `0107`, % */
    val longTermFuelTrim: Double? = null,
    /** PID `0106`, % */
    val shortTermFuelTrim: Double? = null,
    /** PID `0105`, °C */
    val coolantCelsius: Double? = null,
    /** PID `011F`, sekundy pracy silnika */
    val runtimeSeconds: Double? = null,
    /** Napięcie sterownika / instalacji, V (PID `0142` lub pin 16) */
    val voltage: Double? = null,
    /** PID `010C` */
    val rpm: Double? = null,
    /** `false` gdy monitory niegotowe; `null` gdy brak odczytu */
    val monitorsReady: Boolean? = null,
    /** PID `0131`, km */
    val distanceSinceClearKm: Double? = null,
    /** PID `015C`, °C — na tym aucie nieobsługiwany; pole zostaje dla reguły */
    val oilCelsius: Double? = null,
) {
    val runtimeMinutes: Double?
        get() = runtimeSeconds?.div(60.0)

    /** Silnik pracuje przy obrotach > 500. Brak odczytu nie udaje pracy. */
    val engineRunning: Boolean
        get() = rpm != null && rpm > 500.0

    /** Silnik zgaszony przy obrotach < 50. Brak odczytu nie udaje postoju. */
    val engineOff: Boolean
        get() {
            val r = rpm ?: return false
            return r < 50.0
        }
}

/** Odczyty → wnioski. Brakujące wartości pomijają regułę (bez zgadywania). */
object RuleEngine {
    fun evaluate(input: RuleInput): List<Wniosek> {
        val out = mutableListOf<Wniosek>()

        if (input.milOn == true) {
            out.add(
                Wniosek(
                    ruleId = "mil_on",
                    waga = WagaWniosku.Usterka,
                    tytul = "Kontrolka MIL świeci",
                    szczegol = "Sterownik zgłasza potwierdzony problem.",
                ),
            )
        }

        if (input.storedCodeCount > 0) {
            out.add(
                Wniosek(
                    ruleId = "stored_dtc",
                    waga = WagaWniosku.Usterka,
                    tytul = "Kody zapisane obecne",
                    szczegol = "Sterownik zapisał potwierdzone kody błędów.",
                ),
            )
        }

        if (input.pendingCodeCount > 0) {
            out.add(
                Wniosek(
                    ruleId = "pending_dtc",
                    waga = WagaWniosku.Uwaga,
                    tytul = "Kody oczekujące obecne",
                    szczegol = "Wykryte, jeszcze niepotwierdzone.",
                ),
            )
        }

        val ltft = input.longTermFuelTrim
        if (ltft != null) {
            if (ltft > 10.0) {
                out.add(
                    Wniosek(
                        ruleId = "ltft_lean",
                        waga = WagaWniosku.Uwaga,
                        tytul = "Korekta długoterminowa powyżej +10%",
                        szczegol =
                        "Mieszanka uboga: nieszczelność dolotu, słabnący przepływomierz, " +
                            "brudne wtryskiwacze.",
                    ),
                )
            } else if (ltft < -10.0) {
                out.add(
                    Wniosek(
                        ruleId = "ltft_rich",
                        waga = WagaWniosku.Uwaga,
                        tytul = "Korekta długoterminowa poniżej −10%",
                        szczegol =
                        "Mieszanka bogata: przeciekający wtryskiwacz, zbyt wysokie ciśnienie paliwa, " +
                            "zapchany filtr powietrza.",
                    ),
                )
            }
        }

        val stft = input.shortTermFuelTrim
        if (stft != null && ltft != null && kotlin.math.abs(stft + ltft) > 20.0) {
            out.add(
                Wniosek(
                    ruleId = "trim_sum",
                    waga = WagaWniosku.Usterka,
                    tytul = "Poważne odchylenie korekt paliwa",
                    szczegol =
                    "Suma korekty krótkoterminowej i długoterminowej przekracza 20% " +
                        "względem mapy bazowej.",
                ),
            )
        }

        val coolant = input.coolantCelsius
        val minutes = input.runtimeMinutes
        if (coolant != null && minutes != null && coolant < 70.0 && minutes > 10.0) {
            out.add(
                Wniosek(
                    ruleId = "thermostat",
                    waga = WagaWniosku.Uwaga,
                    tytul = "Płyn poniżej 70 °C po 10 min pracy",
                    szczegol = "Możliwy zablokowany termostat.",
                ),
            )
        }

        if (coolant != null && coolant > 105.0) {
            out.add(
                Wniosek(
                    ruleId = "overheat",
                    waga = WagaWniosku.Usterka,
                    tytul = "Płyn powyżej 105 °C",
                    szczegol = "Przegrzewanie.",
                ),
            )
        }

        val voltage = input.voltage
        if (voltage != null && input.engineRunning && voltage < 13.0) {
            out.add(
                Wniosek(
                    ruleId = "alternator_low",
                    waga = WagaWniosku.Uwaga,
                    tytul = "Napięcie poniżej 13,0 V przy obrotach",
                    szczegol = "Alternator nie doładowuje.",
                ),
            )
        }

        if (voltage != null && voltage > 15.0) {
            out.add(
                Wniosek(
                    ruleId = "overcharge",
                    waga = WagaWniosku.Uwaga,
                    tytul = "Napięcie powyżej 15,0 V",
                    szczegol = "Przeładowanie, podejrzenie regulatora.",
                ),
            )
        }

        if (voltage != null && input.engineOff && voltage >= 12.0 && voltage <= 12.4) {
            out.add(
                Wniosek(
                    ruleId = "battery_weak",
                    waga = WagaWniosku.Uwaga,
                    tytul = "Napięcie 12,0–12,4 V przy silniku zgaszonym",
                    szczegol = "Akumulator słabo naładowany.",
                ),
            )
        }

        if (input.monitorsReady == false) {
            out.add(
                Wniosek(
                    ruleId = "monitors_not_ready",
                    waga = WagaWniosku.Uwaga,
                    tytul = "Monitory niegotowe",
                    szczegol = "Auto nie przejdzie badania emisji, potrzebny przejazd.",
                ),
            )
        }

        val km = input.distanceSinceClearKm
        if (km != null && km < 100.0 && input.milOn == false) {
            out.add(
                Wniosek(
                    ruleId = "codes_cleared_recently",
                    waga = WagaWniosku.Informacja,
                    tytul = "Kody skasowano niedawno",
                    szczegol = "Przebieg od skasowania kodów poniżej 100 km, kontrolka zgaszona.",
                ),
            )
        }

        val oil = input.oilCelsius
        if (oil != null && oil < 90.0) {
            out.add(
                Wniosek(
                    ruleId = "oil_cold",
                    waga = WagaWniosku.Informacja,
                    tytul = "Temperatura oleju poniżej 90 °C",
                    szczegol = "Silnik nie jest jeszcze w pełni rozgrzany.",
                ),
            )
        }

        return out
    }
}
