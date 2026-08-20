package pl.i40.android.service

/** Blokada prędkościowa — czysta logika, sekcja 12.4. */
object BlokadaPredkosci {
    const val PID_PREDKOSC = 0x0D

    /** `010D` > 0 → pojazd w ruchu. Brak odczytu nie udaje jazdy. */
    fun wRuchu(predkoscKmh: Double?): Boolean = predkoscKmh != null && predkoscKmh > 0.0

    fun pozwala(rodzaj: InterakcjaZywa, wRuchu: Boolean, nagrywa: Boolean): Boolean {
        if (!wRuchu) return true
        if (rodzaj == InterakcjaZywa.PrzelaczaniePaneli) return true
        return rodzaj == InterakcjaZywa.Stop && nagrywa
    }
}

enum class InterakcjaZywa {
    Stop,
    Nawigacja,
    Ustawienia,
    ZmianaParametrow,
    Przeglad,

    /**
     * Szerokie przeciągnięcie bez celu do trafienia — nie zmienia nagrywania
     * i jest mniej rozpraszające niż czytanie ośmiu liczb naraz; po to panele powstały.
     */
    PrzelaczaniePaneli,
}
