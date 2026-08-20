package pl.i40.android.storage

data class WynikKasowania(val doUsuniecia: List<String>, val odrzuconoWToku: List<String>) {
    val wymagaVacuum: Boolean get() = doUsuniecia.size > PROG_VACUUM

    companion object {
        const val PROG_VACUUM = 50
    }
}

/**
 * Kasowanie przejazdów — §11.3 warstwy historii.
 * `w_toku` odrzucane; punktów i przeglądów nie ruszamy.
 */
object KasowaniePrzejazdow {
    fun przygotuj(przejazdy: List<Przejazd>, zadane: Collection<String>): WynikKasowania {
        val indeks = przejazdy.associateBy { it.id }
        val doUsuniecia = mutableListOf<String>()
        val wToku = mutableListOf<String>()
        for (id in zadane.distinct()) {
            val p = indeks[id] ?: continue
            if (p.status == StatusPrzejazdu.WToku) {
                wToku.add(id)
            } else {
                doUsuniecia.add(id)
            }
        }
        return WynikKasowania(doUsuniecia, wToku)
    }
}
