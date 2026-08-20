package pl.i40.android.ui

import pl.i40.android.storage.Przejazd
import pl.i40.android.storage.StatusPrzejazdu

data class TrybZaznaczania(val aktywny: Boolean = false, val zaznaczone: Set<String> = emptySet()) {
    val liczba: Int get() = zaznaczone.size

    fun poPrzytrzymaniu(p: Przejazd): TrybZaznaczania {
        if (p.status == StatusPrzejazdu.WToku) return copy(aktywny = true)
        return copy(aktywny = true, zaznaczone = setOf(p.id))
    }

    fun przelacz(p: Przejazd): TrybZaznaczania {
        if (!aktywny || p.status == StatusPrzejazdu.WToku) return this
        return if (p.id in zaznaczone) copy(zaznaczone = zaznaczone - p.id) else copy(zaznaczone = zaznaczone + p.id)
    }

    fun zaznaczDzien(dnia: List<Przejazd>): TrybZaznaczania {
        val ids = dnia.filter { it.status != StatusPrzejazdu.WToku }.map { it.id }.toSet()
        return copy(aktywny = true, zaznaczone = ids)
    }

    fun zakoncz(): TrybZaznaczania = TrybZaznaczania()

    fun maPoleWyboru(p: Przejazd): Boolean = p.status != StatusPrzejazdu.WToku
}
