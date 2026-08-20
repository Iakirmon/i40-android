package pl.i40.android.obd

/**
 * Enumeracja PID `0103` — SAE J1979, sekcja 4 warstwy kontekstowej.
 * Bajt A: układ 1; bajt B: układ 2 (ten silnik ma jeden bank).
 *
 * Wartość spoza listy to **nieznany status z surową liczbą**, nigdy zgadywanie najbliższej.
 * `16` to pętla **zamknięta** mimo awarii sondy — nie wrzucać do worka „otwarta".
 */
enum class StanPetliPaliwowej {
    Wylaczony,
    OtwartaZimny,
    Zamknieta,
    OtwartaObciazenie,
    OtwartaAwaria,
    ZamknietaAwariaSondy,
    Nieznany,
}

data class StatusUkladuPaliwowego(val bajtA: Int, val bajtB: Int, val stan: StanPetliPaliwowej, val opis: String)

data class WierszStatusuPetli(val znaczek: String, val tytul: String, val dopisek: String)

object FuelSystemStatus {
    const val PID = 0x03

    fun decode(data: List<Int>): StatusUkladuPaliwowego? {
        if (data.size < 2) return null
        val a = data[0]
        val b = data[1]
        return StatusUkladuPaliwowego(bajtA = a, bajtB = b, stan = stan(a), opis = opis(a))
    }

    fun stan(bajtA: Int): StanPetliPaliwowej = when (bajtA) {
        0 -> StanPetliPaliwowej.Wylaczony
        1 -> StanPetliPaliwowej.OtwartaZimny
        2 -> StanPetliPaliwowej.Zamknieta
        4 -> StanPetliPaliwowej.OtwartaObciazenie
        8 -> StanPetliPaliwowej.OtwartaAwaria
        16 -> StanPetliPaliwowej.ZamknietaAwariaSondy
        else -> StanPetliPaliwowej.Nieznany
    }

    fun opis(bajtA: Int): String = when (bajtA) {
        0 -> "Silnik wyłączony"
        1 -> "Pętla otwarta — niewystarczająca temperatura silnika"
        2 -> "Pętla zamknięta — sprzężenie zwrotne sondy tlenu"
        4 -> "Pętla otwarta — obciążenie silnika albo odcięcie paliwa przy zwalnianiu"
        8 -> "Pętla otwarta — awaria układu"
        16 -> "Pętla zamknięta, ale awaria sprzężenia"
        else -> "Nieznany status ($bajtA)"
    }

    /** Liczbę na kafel tylko po pozytywnym potwierdzeniu pętli zamkniętej. */
    fun korektyWazne(bajtA: Int?): Boolean = bajtA == 2 || bajtA == 16

    /** Wiersz nad wykresem Mieszanka — tabela 8.2, nie długi opis dekodera. */
    fun wierszEkranu(bajtA: Int?): WierszStatusuPetli = when (bajtA) {
        2 -> WierszStatusuPetli("●", "PĘTLA ZAMKNIĘTA", "korekty ważne")
        1 -> WierszStatusuPetli("○", "PĘTLA OTWARTA", "silnik za zimny")
        4 -> WierszStatusuPetli("○", "PĘTLA OTWARTA", "pełne obciążenie lub hamowanie silnikiem")
        8 -> WierszStatusuPetli("✕", "PĘTLA OTWARTA — awaria układu", "")
        16 -> WierszStatusuPetli("⚠", "PĘTLA ZAMKNIĘTA — awaria sondy", "")
        0 -> WierszStatusuPetli("—", "Silnik wyłączony", "")
        null -> WierszStatusuPetli("—", "—", "")
        else -> WierszStatusuPetli("?", "Nieznany status ($bajtA)", "")
    }

    fun tekstWierszaEkranu(bajtA: Int?): String {
        val w = wierszEkranu(bajtA)
        return if (w.dopisek.isEmpty()) "${w.znaczek} ${w.tytul}" else "${w.znaczek} ${w.tytul}          ${w.dopisek}"
    }
}
