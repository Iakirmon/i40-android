package pl.i40.android.ui

import pl.i40.android.acquisition.RingSample
import pl.i40.android.rules.PasmaOdniesienia

/**
 * Teksty panelu Mieszanka. Wykres to suma `0106`+`0107` (pasmo ±20 %), nie korekta długa sama.
 */
object FormatMieszanki {
    const val PID_SONDA_ZA_KAT = 0x15

    fun suma(stft: Double?, ltft: Double?): Double? {
        if (stft == null || ltft == null) return null
        return stft + ltft
    }

    fun sumaProbek(stft: List<RingSample>, ltft: List<RingSample>): List<RingSample> {
        if (stft.isEmpty() || ltft.isEmpty()) return emptyList()
        val times = (stft.map { it.time } + ltft.map { it.time }).toSortedSet()
        val out = mutableListOf<RingSample>()
        for (t in times) {
            val a = ostatniaDo(stft, t) ?: continue
            val b = ostatniaDo(ltft, t) ?: continue
            out.add(RingSample(t, a + b))
        }
        return out
    }

    fun zaKatWartosc(): String = FormatPomiaru.NIEDOSTEPNE

    fun zaKatPowod(): String {
        val wpis = PasmaOdniesienia.wpisyDlaPid(PID_SONDA_ZA_KAT).firstOrNull()
        return wpis?.uzasadnienie ?: "PID bez formuły w katalogu, sekcja 3.2."
    }

    fun normaKrotkiej(): String = FormatPomiaru.NIEDOSTEPNE

    fun normaDlugiej(): String {
        val p = PasmaOdniesienia.korektaDluga
        return "±${p.endInclusive.toInt()}"
    }

    fun normaSumy(): String {
        val p = PasmaOdniesienia.sumaKorekt
        return "±${p.endInclusive.toInt()}"
    }

    fun normaLambdy(): String {
        val v = PasmaOdniesienia.LAMBDA_STECHIOMETRIA
        return "%.3f".format(java.util.Locale.US, v).replace('.', ',')
    }

    fun pozaPasmemWiersz(pozaS: Double?, czasWPetliS: Double?): String {
        val pasmo = normaSumy()
        if (pozaS == null || czasWPetliS == null) {
            return "Poza pasmem $pasmo %:  ${FormatPomiaru.NIEDOSTEPNE}\n(czas w pętli zamkniętej)"
        }
        val poza = formatMmSs(pozaS)
        val mianownik = formatMmSs(czasWPetliS)
        return "Poza pasmem $pasmo %:  $poza z $mianownik\n(czas w pętli zamkniętej)"
    }

    fun linieSumy(): List<Double> {
        val p = PasmaOdniesienia.sumaKorekt
        return listOf(p.start, p.endInclusive)
    }

    private fun ostatniaDo(samples: List<RingSample>, t: Double): Double? {
        var last: Double? = null
        for (s in samples) {
            if (s.time <= t) last = s.value else break
        }
        return last
    }

    private fun formatMmSs(seconds: Double): String {
        val total = kotlin.math.max(0, kotlin.math.round(seconds).toInt())
        return "%d:%02d".format(total / 60, total % 60)
    }
}
