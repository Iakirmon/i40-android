package pl.i40.android.ui

import pl.i40.android.acquisition.OilTempEstimator
import pl.i40.android.obd.PidCatalog
import pl.i40.android.rules.PasmaOdniesienia

/**
 * Formatowanie kafli żywego ekranu. Czwarty kafel to `0107` (poprawka P1), nie paliwo.
 *
 * Pasmo korekty długiej to progi `ltft_lean` / `ltft_rich` z tabeli 10.4 — nie nowy próg.
 */
object FormatKafla {
    const val PID_OLEJ_MODEL = 0x5C

    val KAFLI_DOMYSLNE: List<Int> = listOf(PID_OLEJ_MODEL, 0x05, 0x42, 0x07)

    /** Tabela 10.4: korekta długa poza ±10 % → uwaga. */
    val PASMO_KOREKTY_DLUGIEJ: ClosedFloatingPointRange<Double>
        get() = PasmaOdniesienia.korektaDluga

    fun krotkaEtykieta(pid: Int): String = when (pid) {
        PID_OLEJ_MODEL -> "OLEJ"
        0x05 -> "PŁYN"
        0x42 -> "NAPIĘCIE"
        0x07 -> "KOREKTA D"
        0x0F -> "DOLOT"
        0x0C -> "OBROTY"
        0x04 -> "OBCIĄŻENIE"
        0x0E -> "ZAPŁON"
        0x06 -> "STFT"
        else -> PidCatalog.definition(pid)?.name?.take(12)?.uppercase() ?: "%02X".format(pid)
    }

    fun cyfryPoPrzecinku(pid: Int): Int = when (pid) {
        0x42, 0x06, 0x07, 0x0E, 0x23 -> 1
        0x44 -> 3
        else -> 0
    }

    fun jednostka(pid: Int): String = when (pid) {
        0x05, 0x5C, 0x0F, 0x46 -> "°C"
        0x42 -> "V"
        0x04, 0x06, 0x07, 0x11 -> "%"
        0x0C -> ""
        0x0E -> "°"
        0x0D -> "km/h"
        0x23 -> "bar"
        else -> PidCatalog.definition(pid)?.unit.orEmpty()
    }

    fun wartosc(pid: Int, value: Double?): String = FormatPomiaru.liczba(value, cyfryPoPrzecinku(pid), jednostka(pid))

    fun olejTekst(celsius: Double?): String {
        val number = FormatPomiaru.liczba(celsius, 0, "°C")
        if (number == FormatPomiaru.NIEDOSTEPNE) return number
        return "~$number"
    }

    fun olejPodpis(pewnosc: OilTempEstimator.Pewnosc): String =
        "szacunek · ${pasmoKafla(PID_OLEJ_MODEL)} · ${pewnosc.label}"

    fun podpisZakresu(pid: Int): String = pasmoKafla(pid)

    /** Trzeci wiersz kafla — pasmo z [PasmaOdniesienia], nigdy oś wykresu. */
    fun pasmoKafla(pid: Int): String = when (pid) {
        PID_OLEJ_MODEL -> "≥ ${PasmaOdniesienia.OLEJ_MIN_C.toInt()}"
        0x05 -> {
            val p = PasmaOdniesienia.plyn
            "${p.start.toInt()}–${p.endInclusive.toInt()}"
        }
        0x42 -> {
            val p = PasmaOdniesienia.napieciePraca
            "${formatujKraniec(p.start, 0x42)}–${formatujKraniec(p.endInclusive, 0x42)}"
        }
        0x07 -> {
            val p = PasmaOdniesienia.korektaDluga
            "−${kotlin.math.abs(p.start).toInt()} – +${p.endInclusive.toInt()}"
        }
        else -> FormatPomiaru.NIEDOSTEPNE
    }

    private fun formatujKraniec(value: Double, pid: Int): String {
        val digits = cyfryPoPrzecinku(pid)
        return if (digits == 0) {
            "%.0f".format(java.util.Locale.US, value)
        } else {
            "%.${digits}f".format(java.util.Locale.US, value).replace('.', ',')
        }
    }
}
