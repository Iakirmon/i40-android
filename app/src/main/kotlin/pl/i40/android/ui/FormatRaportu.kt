package pl.i40.android.ui

import pl.i40.android.rules.PasmaOdniesienia
import pl.i40.android.storage.PodsumowaniePrzejazdu

data class WierszRaportu(
    val etykieta: String,
    val wartosc: String,
    val norma: String,
    val znacznik: String = "",
    val hasloId: String? = null
)

data class BlokDiagnostyka(
    val cisnienie: WierszRaportu,
    val katalizator: WierszRaportu,
    val plyn90: WierszRaportu,
    val korektyPoza: WierszRaportu
)

/** Nagłówek raportu sesji — §11.2. Pasma z [PasmaOdniesienia], null to `—`. */
object FormatRaportu {
    /** Wirtualny PID wykresu sumy `0106`+`0107` — nie jest poleceniem OBD. */
    const val PID_SUMA_KOREKT = 0xE607

    fun znacznik(value: Double?, pasmo: ClosedFloatingPointRange<Double>?): String {
        if (value == null || pasmo == null) return ""
        if (value > pasmo.endInclusive) return "▲"
        if (value < pasmo.start) return "▼"
        return ""
    }

    fun naglowek(p: PodsumowaniePrzejazdu): List<WierszRaportu> {
        val plynPasmo = PasmaOdniesienia.plyn
        val napieciePasmo = PasmaOdniesienia.napieciePraca
        val napiecieTekst = when {
            p.minNapiecie == null && p.maxNapiecie == null -> FormatPomiaru.NIEDOSTEPNE
            p.minNapiecie != null && p.maxNapiecie != null ->
                "${FormatPomiaru.liczba(p.minNapiecie, 1, "V")} – ${FormatPomiaru.liczba(p.maxNapiecie, 1, "V")}"
            else -> FormatPomiaru.liczba(p.maxNapiecie ?: p.minNapiecie, 1, "V")
        }
        val napiecieDoZnacznika = p.maxNapiecie ?: p.minNapiecie
        return listOf(
            WierszRaportu(
                etykieta = "Max obroty",
                wartosc = FormatPomiaru.liczba(p.maxObroty, 0),
                norma = FormatPomiaru.NIEDOSTEPNE,
                hasloId = "obroty-silnika"
            ),
            WierszRaportu(
                etykieta = "Max płyn",
                wartosc = FormatPomiaru.liczba(p.maxPlynC, 0, "°C"),
                norma = "${plynPasmo.start.toInt()} – ${plynPasmo.endInclusive.toInt()}",
                znacznik = znacznik(p.maxPlynC, plynPasmo),
                hasloId = "temperatura-plynu-chlodzacego"
            ),
            WierszRaportu(
                etykieta = "Max prędkość",
                wartosc = FormatPomiaru.liczba(p.maxPredkoscKmh, 0, "km/h"),
                norma = FormatPomiaru.NIEDOSTEPNE,
                hasloId = "predkosc-pojazdu"
            ),
            WierszRaportu(
                etykieta = "Napięcie",
                wartosc = napiecieTekst,
                norma = "${kraniec(napieciePasmo.start, 1)} – ${kraniec(napieciePasmo.endInclusive, 1)}",
                znacznik = znacznik(napiecieDoZnacznika, napieciePasmo),
                hasloId = "napiecie-sterownika"
            )
        )
    }

    fun diagnostyka(p: PodsumowaniePrzejazdu): BlokDiagnostyka {
        val szyna = PasmaOdniesienia.szynaObciazenie
        val kat = PasmaOdniesienia.katalizatorPraca
        val cisnienieTekst = when {
            p.maxCisnienieSzynyBar == null -> FormatPomiaru.NIEDOSTEPNE
            else -> {
                val bar = FormatPomiaru.liczba(p.maxCisnienieSzynyBar, 0, "bar")
                val obc = p.obciazeniePrzyMaxCisnieniu?.let { FormatPomiaru.liczba(it, 0, "%") }
                    ?: FormatPomiaru.NIEDOSTEPNE
                "$bar  przy $obc obciążenia"
            }
        }
        return BlokDiagnostyka(
            cisnienie = WierszRaportu(
                etykieta = "Max ciśnienie szyny",
                wartosc = cisnienieTekst,
                norma = "${szyna.start.toInt()} – ${szyna.endInclusive.toInt()} bar",
                znacznik = znacznik(p.maxCisnienieSzynyBar, szyna),
                hasloId = "maksymalne-cisnienie-szyny-i-obciazenie-przy-nim"
            ),
            katalizator = WierszRaportu(
                etykieta = "Max temp. katalizatora",
                wartosc = FormatPomiaru.liczba(p.maxTempKatalizatoraC, 0, "°C"),
                norma = "${kat.start.toInt()} – ${kat.endInclusive.toInt()} °C",
                znacznik = znacznik(p.maxTempKatalizatoraC, kat),
                hasloId = "temperatura-katalizatora"
            ),
            plyn90 = WierszRaportu(
                etykieta = "Płyn 90 °C po",
                wartosc = FormatTermika.czasMmSs(p.czasDo90CSekundy),
                norma = FormatPomiaru.NIEDOSTEPNE,
                hasloId = "czas-do-90-c"
            ),
            korektyPoza = WierszRaportu(
                etykieta = "Korekty poza pasmem",
                wartosc = pozaPasmem(
                    p.czasPozaPasmemWPetliZamknietejSekundy,
                    p.czasWPetliZamknietejSekundy
                ),
                norma = "±${PasmaOdniesienia.sumaKorekt.endInclusive.toInt()} %",
                hasloId = "czas-poza-pasmem-w-petli-zamknietej"
            )
        )
    }

    fun wartoscWykresu(pid: Int, value: Double?): String = when (pid) {
        PID_SUMA_KOREKT -> FormatPomiaru.liczba(value, 1, "%")
        0x23 -> FormatPomiaru.liczba(value, 0, "bar")
        else -> FormatKafla.wartosc(pid, value)
    }

    fun linieWykresu(pid: Int): List<Double> = when (pid) {
        0x23 -> FormatGdi.linieSzyny()
        0x3C -> FormatTermika.linieKatalizatora()
        PID_SUMA_KOREKT -> FormatMieszanki.linieSumy()
        0x05 -> FormatTermika.liniePlynu()
        else -> emptyList()
    }

    private fun pozaPasmem(pozaS: Double?, mianownikS: Double?): String {
        if (pozaS == null || mianownikS == null) return FormatPomiaru.NIEDOSTEPNE
        return "${FormatTermika.czasMmSs(pozaS)}  z ${FormatTermika.czasMmSs(mianownikS)}"
    }

    private fun kraniec(value: Double, digits: Int): String =
        "%.${digits}f".format(java.util.Locale.US, value).replace('.', ',')
}
