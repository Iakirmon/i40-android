package pl.i40.android.checkup

import pl.i40.android.obd.Dtc
import pl.i40.android.obd.ReadinessStatus
import pl.i40.android.rules.PasmaOdniesienia
import pl.i40.android.rules.RuleEngine
import pl.i40.android.rules.RuleInput
import pl.i40.android.rules.WagaWniosku
import pl.i40.android.rules.Wniosek

/** Źródło danych przeglądu. */
enum class ZrodloRaportu {
    Atrapa,
    Ble,
}

/**
 * Werdykt ekranu głównego — kolor nigdy nie jest jedynym sygnałem.
 * Kolejność z sekcji 8.8: usterka wygrywa z uwagą.
 */
enum class Werdykt(val tytul: String) {
    Ok("Wszystko OK"),
    Uwaga("Wymaga uwagi"),
    Usterka("Usterka"),
}

/** Pojedynczy odczyt PID w migawce raportu. `value == null` gdy odczyt niedostępny. */
data class MigawkaOdczytu(
    val pid: Int,
    val nazwa: String,
    val jednostka: String,
    val wartosc: Double?,
    val dostepny: Boolean,
    val podejrzany: Boolean
)

data class MigawkaPojazdu(
    val vin: String? = null,
    val producent: String? = null,
    val rokModelu: Int? = null,
    val fabryka: String? = null,
    val kalibracja: String? = null,
    val nazwaEcu: String? = null
)

data class MigawkaAdaptera(
    val firmware: String? = null,
    val opis: String? = null,
    val kodProtokolu: String? = null,
    val nazwaProtokolu: String? = null,
    val napieciePin16: Double? = null
)

/**
 * Migawka pełnego przeglądu.
 *
 * Zamrożonej ramki (tryb 02) nie ma — nie-cel z sekcji 4.
 * `kodyTrwale == null` znaczy tryb `0A` nieobsługiwany (`NO DATA`).
 */
data class Raport(
    val startMs: Long,
    val koniecMs: Long?,
    val zrodlo: ZrodloRaportu,
    val pojazd: MigawkaPojazdu,
    val adapter: MigawkaAdaptera,
    val obslugiwanePid: List<Int>,
    val gotowosc: ReadinessStatus?,
    val kodyZapisane: List<Dtc>,
    val kodyOczekujace: List<Dtc>,
    val kodyTrwale: List<Dtc>?,
    val odczyty: List<MigawkaOdczytu>,
    val wnioski: List<Wniosek>
) {
    val werdykt: Werdykt
        get() {
            if (gotowosc?.milOn == true || kodyZapisane.isNotEmpty()) return Werdykt.Usterka
            if (wnioski.any { it.waga == WagaWniosku.Usterka }) return Werdykt.Usterka
            if (kodyOczekujace.isNotEmpty() || gotowosc?.ready == false) return Werdykt.Uwaga
            if (wnioski.any { it.waga == WagaWniosku.Uwaga }) return Werdykt.Uwaga
            return Werdykt.Ok
        }

    /** Wejście do [RuleEngine] z pól migawki. Napięcie: PID `0142`, inaczej pin 16. */
    val wejscieRegul: RuleInput
        get() {
            fun numeric(pid: Int): Double? = odczyty.firstOrNull { it.pid == pid && it.dostepny }?.wartosc
            return RuleInput(
                milOn = gotowosc?.milOn,
                storedCodeCount = kodyZapisane.size,
                pendingCodeCount = kodyOczekujace.size,
                longTermFuelTrim = numeric(0x07),
                shortTermFuelTrim = numeric(0x06),
                coolantCelsius = numeric(0x05),
                runtimeSeconds = numeric(0x1F),
                voltage = numeric(0x42) ?: adapter.napieciePin16,
                rpm = numeric(0x0C),
                monitorsReady = gotowosc?.ready,
                distanceSinceClearKm = numeric(0x31),
                oilCelsius = numeric(0x5C),
                cisnienieSzynyBar = numeric(0x23)?.let { PasmaOdniesienia.kpaNaBar(it) },
                predkoscKmh = numeric(0x0D),
                temperaturaKatalizatoraC = numeric(0x3C),
                statusUkladuPaliwowego = numeric(0x03)?.toInt()
            )
        }

    fun odswiezWnioski(): Raport = copy(wnioski = RuleEngine.evaluate(wejscieRegul))
}
