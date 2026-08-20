package pl.i40.android.ui

import pl.i40.android.checkup.Raport
import pl.i40.android.obd.PidCatalog
import pl.i40.android.obd.SupportedPids
import pl.i40.android.rules.PasmaOdniesienia
import pl.i40.android.rules.RodzajPasma
import pl.i40.android.rules.WpisPasma

data class WierszPrzegladu(
    val etykieta: String,
    val wartosc: String,
    val norma: String,
    val pid: Int? = null,
    val wyliczony: Boolean = false,
    val powod: String? = null
)

data class KartaGdiPrzegladu(
    val cisnienie: WierszPrzegladu,
    val obciazenie: WierszPrzegladu,
    val obroty: WierszPrzegladu,
    val stopka: String
)

data class KartaKatalizatorPrzegladu(
    val temperatura: WierszPrzegladu,
    val zaplon: String,
    val sonda: WierszPrzegladu,
    val monitorKatalizatora: String,
    val monitorSond: String
)

/**
 * Karty przeglądu — układy §9. Pasma wyłącznie z [PasmaOdniesienia].
 * `012F` nie wchodzi na ekran odczytów (poprawka P1).
 */
object FormatPrzegladu {
    const val PID_PALIWO = 0x2F
    const val PID_SZYNA = 0x23
    const val PID_OBCIAZENIE_ABS = 0x43
    const val PID_OBROTY = 0x0C
    const val PID_KATALIZATOR = 0x3C
    const val PID_KOLEKTOR = 0x0B
    const val PID_ATMOSFERA = 0x33
    const val PID_SONDA_ZA_KAT = 0x15

    const val STOPKA_GDI =
        "Stan pompy widać dopiero pod obciążeniem — zobacz panel WTRYSK GDI podczas jazdy."

    /** Układ 9.2 — kreska z powodem, nie pominięcie wiersza. */
    const val POWOD_SONDY_15 = "PID 15 obsługiwany przez auto, brak formuły w katalogu"

    const val MONITOR_KATALIZATORA = "Katalizator"
    const val MONITOR_SOND = "Sondy tlenu"
    const val MONITOR_GOTOWY = "gotowy"
    const val MONITOR_NIEGOTOWY = "niegotowy"

    fun numeric(raport: Raport, pid: Int): Double? =
        raport.odczyty.firstOrNull { it.pid == pid && it.dostepny }?.wartosc

    fun podcisnienieKpa(raport: Raport): Double? {
        val kolektor = numeric(raport, PID_KOLEKTOR)
        val atmosfera = numeric(raport, PID_ATMOSFERA)
        if (kolektor == null || atmosfera == null) return null
        return atmosfera - kolektor
    }

    fun pasmoSzynyJalowej(): String {
        val p = PasmaOdniesienia.szynaJalowy
        return "${p.start.toInt()} – ${p.endInclusive.toInt()} bar"
    }

    fun pasmoKatalizatora(): String {
        val p = PasmaOdniesienia.katalizatorPraca
        return "${p.start.toInt()} – ${p.endInclusive.toInt()} °C"
    }

    fun kolumnaNormy(pid: Int): String {
        val wpis = PasmaOdniesienia.wpisyDlaPid(pid).firstOrNull { kandydat ->
            kandydat.rodzaj == RodzajPasma.Norma &&
                kandydat.id != "szyna_obciazenie" &&
                kandydat.id != "napiecie_zgaszony"
        }
        return if (wpis == null) FormatPomiaru.NIEDOSTEPNE else formatujPasmo(wpis)
    }

    fun formatujPasmo(wpis: WpisPasma): String {
        if (wpis.rodzaj != RodzajPasma.Norma) return FormatPomiaru.NIEDOSTEPNE
        if (wpis.id == "szyna_jalowy") return pasmoSzynyJalowej()
        if (wpis.id == "katalizator") return pasmoKatalizatora()
        val min = wpis.min
        val max = wpis.max
        val jednostka = wpis.jednostka
        return when {
            min != null && max != null && min == max -> kraniec(min, wpis.pid)
            min != null && max != null -> {
                val zakres = "${kraniec(min, wpis.pid)} – ${kraniec(max, wpis.pid)}"
                if (jednostka.isEmpty()) zakres else "$zakres $jednostka"
            }
            min != null -> {
                val n = kraniec(min, wpis.pid)
                if (jednostka.isEmpty()) "≥ $n" else "≥ $n $jednostka"
            }
            else -> FormatPomiaru.NIEDOSTEPNE
        }
    }

    fun kartaGdi(raport: Raport): KartaGdiPrzegladu {
        val bar = numeric(raport, PID_SZYNA)?.let { PasmaOdniesienia.kpaNaBar(it) }
        return KartaGdiPrzegladu(
            cisnienie = WierszPrzegladu(
                etykieta = "Ciśnienie w szynie",
                wartosc = FormatPomiaru.liczba(bar, 1, "bar"),
                norma = pasmoSzynyJalowej(),
                pid = PID_SZYNA
            ),
            obciazenie = WierszPrzegladu(
                etykieta = "Obciążenie absolutne",
                wartosc = FormatPomiaru.liczba(numeric(raport, PID_OBCIAZENIE_ABS), 0, "%"),
                norma = FormatPomiaru.NIEDOSTEPNE,
                pid = PID_OBCIAZENIE_ABS
            ),
            obroty = WierszPrzegladu(
                etykieta = "Obroty",
                wartosc = FormatPomiaru.liczba(numeric(raport, PID_OBROTY), 0, "obr/min"),
                norma = FormatPomiaru.NIEDOSTEPNE,
                pid = PID_OBROTY
            ),
            stopka = STOPKA_GDI
        )
    }

    fun kartaKatalizator(raport: Raport): KartaKatalizatorPrzegladu = KartaKatalizatorPrzegladu(
        temperatura = WierszPrzegladu(
            etykieta = "Temperatura, bank 1",
            wartosc = FormatPomiaru.liczba(numeric(raport, PID_KATALIZATOR), 0, "°C"),
            norma = pasmoKatalizatora(),
            pid = PID_KATALIZATOR
        ),
        zaplon = FormatPomiaru.liczba(PasmaOdniesienia.KATALIZATOR_ZAPLON_C, 0, "°C"),
        sonda = WierszPrzegladu(
            etykieta = "Sonda za katalizatorem",
            wartosc = FormatPomiaru.NIEDOSTEPNE,
            norma = FormatPomiaru.NIEDOSTEPNE,
            pid = PID_SONDA_ZA_KAT,
            powod = POWOD_SONDY_15
        ),
        monitorKatalizatora = tekstMonitora(raport, MONITOR_KATALIZATORA),
        monitorSond = tekstMonitora(raport, MONITOR_SOND)
    )

    fun grupaPowietrze(raport: Raport): List<WierszPrzegladu> = listOf(
        wierszPid(raport, PID_KOLEKTOR, "Ciśnienie w kolektorze"),
        wierszPid(raport, PID_ATMOSFERA, "Ciśnienie atmosferyczne"),
        WierszPrzegladu(
            etykieta = "Podciśnienie (wyliczone)",
            wartosc = FormatPomiaru.liczba(podcisnienieKpa(raport), 0, "kPa"),
            norma = FormatPomiaru.NIEDOSTEPNE,
            wyliczony = true
        )
    )

    fun wierszeOdczytow(raport: Raport): List<WierszPrzegladu> {
        val maska = raport.obslugiwanePid.toSet()
        val defs = SupportedPids.displayable(maska).filter { it.id != PID_PALIWO }
        val zPid = defs.map { def -> wierszPid(raport, def.id, def.name) }.toMutableList()
        val podcisnienie = grupaPowietrze(raport).last()
        val poAtmosferze = zPid.indexOfFirst { it.pid == PID_ATMOSFERA }
        if (poAtmosferze >= 0) {
            zPid.add(poAtmosferze + 1, podcisnienie)
        } else {
            zPid.add(podcisnienie)
        }
        return zPid
    }

    private fun wierszPid(raport: Raport, pid: Int, etykieta: String): WierszPrzegladu {
        val odczyt = raport.odczyty.firstOrNull { it.pid == pid }
        val wartosc = when {
            odczyt == null || !odczyt.dostepny -> FormatPomiaru.NIEDOSTEPNE
            pid == PID_SZYNA -> FormatPomiaru.liczba(
                odczyt.wartosc?.let { PasmaOdniesienia.kpaNaBar(it) },
                1,
                "bar"
            )
            else -> FormatPomiaru.liczba(
                odczyt.wartosc,
                FormatKafla.cyfryPoPrzecinku(pid),
                jednostka(pid, odczyt.jednostka)
            )
        }
        return WierszPrzegladu(
            etykieta = etykieta,
            wartosc = wartosc,
            norma = kolumnaNormy(pid),
            pid = pid
        )
    }

    private fun jednostka(pid: Int, zOdczytu: String): String {
        val zKafla = FormatKafla.jednostka(pid)
        if (zKafla.isNotEmpty()) return zKafla
        return zOdczytu.ifEmpty { PidCatalog.definition(pid)?.unit.orEmpty() }
    }

    private fun tekstMonitora(raport: Raport, nazwa: String): String {
        val status = raport.gotowosc ?: return FormatPomiaru.NIEDOSTEPNE
        val monitor = (status.continuous + status.monitors).firstOrNull { it.name == nazwa }
            ?: return FormatPomiaru.NIEDOSTEPNE
        return if (monitor.incomplete) MONITOR_NIEGOTOWY else MONITOR_GOTOWY
    }

    private fun kraniec(value: Double, pid: Int?): String {
        val digits = if (pid == null) 0 else FormatKafla.cyfryPoPrzecinku(pid)
        return if (digits == 0) {
            kotlin.math.round(value).toLong().toString()
        } else {
            "%.${digits}f".format(java.util.Locale.US, value).replace('.', ',')
        }
    }
}
