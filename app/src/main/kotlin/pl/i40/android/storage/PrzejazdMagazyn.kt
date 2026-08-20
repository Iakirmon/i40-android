package pl.i40.android.storage

enum class StatusPrzejazdu(val sql: String) {
    WToku("w_toku"),
    Zamkniety("zamkniety"),
    Odzyskany("odzyskany"),
    ;

    companion object {
        fun fromSql(value: String): StatusPrzejazdu = entries.first { it.sql == value }
    }
}

data class Przejazd(
    val id: String,
    val poczatekMs: Long,
    val koniecMs: Long?,
    val status: StatusPrzejazdu,
    val vin: String?,
    val notatka: String,
    val podsumowanie: PodsumowaniePrzejazdu,
    val przebieg: TrackBlob,
    val checkpointMs: Long
)

interface PrzejazdMagazyn {
    fun wstaw(przejazd: Przejazd)
    fun zapiszPrzebieg(id: String, przebieg: TrackBlob, checkpointMs: Long)
    fun zamknij(
        id: String,
        koniecMs: Long,
        status: StatusPrzejazdu,
        podsumowanie: PodsumowaniePrzejazdu,
        przebieg: TrackBlob
    )
    fun czytaj(id: String): Przejazd?
    fun listaWToku(): List<Przejazd>
    fun lista(): List<Przejazd>
}

/** Magazyn w pamięci — testy JVM bez SQLite. */
class PamiecPrzejazdow : PrzejazdMagazyn {
    private val wiersze = LinkedHashMap<String, Przejazd>()

    override fun wstaw(przejazd: Przejazd) {
        wiersze[przejazd.id] = przejazd
    }

    override fun zapiszPrzebieg(id: String, przebieg: TrackBlob, checkpointMs: Long) {
        val stary = wiersze[id] ?: return
        wiersze[id] = stary.copy(przebieg = przebieg.kopia(), checkpointMs = checkpointMs)
    }

    override fun zamknij(
        id: String,
        koniecMs: Long,
        status: StatusPrzejazdu,
        podsumowanie: PodsumowaniePrzejazdu,
        przebieg: TrackBlob
    ) {
        val stary = wiersze[id] ?: return
        wiersze[id] = stary.copy(
            koniecMs = koniecMs,
            status = status,
            podsumowanie = podsumowanie,
            przebieg = przebieg.kopia(),
            checkpointMs = koniecMs
        )
    }

    override fun czytaj(id: String): Przejazd? = wiersze[id]?.skopiuj()

    override fun listaWToku(): List<Przejazd> =
        wiersze.values.filter { it.status == StatusPrzejazdu.WToku }.map { it.skopiuj() }

    override fun lista(): List<Przejazd> = wiersze.values.map { it.skopiuj() }

    private fun Przejazd.skopiuj() = copy(przebieg = przebieg.kopia())
}
