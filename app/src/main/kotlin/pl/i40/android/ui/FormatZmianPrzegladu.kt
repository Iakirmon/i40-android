package pl.i40.android.ui

import java.util.Calendar
import java.util.Locale
import pl.i40.android.checkup.PorownanieKodow
import pl.i40.android.obd.Dtc
import pl.i40.android.obd.ReadinessMonitor

/**
 * Blok „ZMIANY OD …” — sekcja 9.3 warstwy odniesienia.
 * Pojawienie się kodu to fakt, nie ocena: zero wag i zero słowa „norma”.
 */
object FormatZmianPrzegladu {
    const val BEZ_ZMIAN = "Bez zmian: te same kody, te same monitory, ta sama kontrolka."
    const val KOD_POJAWIL_SIE = "kod pojawił się od poprzedniego przeglądu"
    const val KOD_ZNIKNAL = "kodu już nie ma"
    const val MIL_ZGASZONA_SWIECI = "była zgaszona, teraz świeci"
    const val MIL_SWIECI_ZGASZONA = "świeciła, teraz zgaszona"
    const val MONITOR_STRACIL = "był gotowy, teraz nie"
    const val MONITOR_ODZYSKAL = "nie był gotowy, teraz jest"

    fun blok(porownanie: PorownanieKodow, poprzedniMs: Long): String {
        val naglowek = "ZMIANY OD ${dataDoNaglowka(poprzedniMs)}"
        if (porownanie.bezZmian) return "$naglowek\n$BEZ_ZMIAN"
        val linie = mutableListOf(naglowek)
        for (k in porownanie.kodyNowe) linie.add(wierszKodu("⊕", k, KOD_POJAWIL_SIE))
        for (k in porownanie.kodyZnikniete) linie.add(wierszKodu("⊖", k, KOD_ZNIKNAL))
        for (m in porownanie.monitoryStracone) linie.add(wierszMonitora("⊖", m, MONITOR_STRACIL))
        for (m in porownanie.monitoryOdzyskane) linie.add(wierszMonitora("⊕", m, MONITOR_ODZYSKAL))
        linie.addAll(wierszeMil(porownanie))
        return linie.joinToString("\n")
    }

    fun dataDoNaglowka(ms: Long): String {
        val locale = Locale("pl", "PL")
        val c = Calendar.getInstance(locale)
        c.timeInMillis = ms
        val miesiac = c.getDisplayName(Calendar.MONTH, Calendar.LONG, locale).orEmpty().uppercase(locale)
        return "${c.get(Calendar.DAY_OF_MONTH)} $miesiac"
    }

    private fun wierszKodu(znak: String, dtc: Dtc, opis: String): String =
        "$znak  ${dtc.code}   ${dtc.description}\n     $opis"

    private fun wierszMonitora(znak: String, monitor: ReadinessMonitor, opis: String): String =
        "$znak  Monitor ${monitor.name} — $opis"

    private fun wierszeMil(p: PorownanieKodow): List<String> {
        val bylo = p.milPoprzednio ?: return emptyList()
        val jest = p.milTeraz ?: return emptyList()
        if (bylo == jest) return emptyList()
        val (znak, tekst) = if (!bylo && jest) {
            "⊕" to MIL_ZGASZONA_SWIECI
        } else {
            "⊖" to MIL_SWIECI_ZGASZONA
        }
        return listOf("$znak  Kontrolka MIL — $tekst")
    }
}
