package pl.i40.android.ui

/**
 * Skala typografii i siatki — §4–5 warstwy wyglądu.
 * 17sp to podłoga. Skala systemowa ograniczona do 1,3×.
 */
object SkalaI40 {
    const val KAFEL_WARTOSC_SP = 44f
    const val SLAD_WARTOSC_SP = 28f
    const val STAN_ZDANIE_SP = 34f
    const val TEKST_SP = 17f
    const val ETYKIETA_SP = 13f
    const val OS_SP = 12f

    const val JEDNOSTKA_DP = 8
    const val PROMIEN_ROGU_DP = 4
    const val CEL_W_RUCHU_DP = 56
    const val CEL_POSTOJ_DP = 48
    const val ODSTEP_CELI_DP = 8

    /** Maks. mnożnik skali systemowej — powyżej panele się nie mieszczą. */
    const val MAX_SKALA_SYSTEMOWA = 1.3f

    fun ograniczSkale(skalaSystemowa: Float): Float = skalaSystemowa.coerceIn(1f, MAX_SKALA_SYSTEMOWA)

    fun sp(bazowa: Float, skalaSystemowa: Float): Float = bazowa * ograniczSkale(skalaSystemowa)
}

/** Wybór motywu — §3.6. AUTOMATYCZNIE bez sygnału → NOC. */
enum class WyborMotywu {
    Noc,
    Dzien,
    Automatycznie,
}

object RozstrzyganieMotywu {
    /**
     * @param sygnalDzien z radia (podświetlenie); `null` = niedostępny.
     */
    fun motyw(wybor: WyborMotywu, sygnalDzien: Boolean?): MotywI40 = when (wybor) {
        WyborMotywu.Noc -> MotywI40.Noc
        WyborMotywu.Dzien -> MotywI40.Dzien
        WyborMotywu.Automatycznie -> when (sygnalDzien) {
            true -> MotywI40.Dzien
            false -> MotywI40.Noc
            null -> MotywI40.Noc
        }
    }
}
