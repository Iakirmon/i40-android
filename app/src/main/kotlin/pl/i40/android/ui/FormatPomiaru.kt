package pl.i40.android.ui

/** Formatowanie wartości pomiarowych — nigdy „0” w miejscu braku odczytu. */
object FormatPomiaru {
    const val NIEDOSTEPNE = "—"

    fun liczba(value: Double?, digits: Int = 0, unit: String = ""): String {
        if (value == null) return NIEDOSTEPNE
        val formatted = if (digits == 0) {
            kotlin.math.round(value).toLong().toString()
        } else {
            val factor = pow10(digits)
            val scaled = kotlin.math.round(value * factor) / factor
            val raw = "%.${digits}f".format(java.util.Locale.US, scaled)
            raw.replace('.', ',')
        }
        return if (unit.isEmpty()) formatted else "$formatted $unit"
    }

    private fun pow10(digits: Int): Double {
        var p = 1.0
        repeat(digits) { p *= 10.0 }
        return p
    }
}
