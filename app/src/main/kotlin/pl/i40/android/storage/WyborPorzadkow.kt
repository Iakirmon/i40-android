package pl.i40.android.storage

import java.util.Calendar
import java.util.Locale

sealed class KryteriumPorzadkow {
    data class KrotszeNiz(val minuty: Int) : KryteriumPorzadkow()
    data object Przerwane : KryteriumPorzadkow()
    data class StarszeNiz(val miesiace: Int) : KryteriumPorzadkow()
}

data class WynikWyboru(val doUsuniecia: List<String>, val pominietoChronione: Int, val pominietoWToku: Int)

/**
 * Czyste funkcje wyboru panelu Porządki — sekcja 5 warstwy historii.
 * Kryterium to wybór, nie próg: żadna wartość nie jest zalecana.
 */
object WyborPorzadkow {
    fun ktoreDoUsuniecia(przejazdy: List<Przejazd>, kryterium: KryteriumPorzadkow, terazMs: Long): WynikWyboru {
        val doUsuniecia = mutableListOf<String>()
        var chronione = 0
        var wToku = 0
        for (p in przejazdy) {
            if (p.status == StatusPrzejazdu.WToku) {
                wToku += 1
                continue
            }
            if (p.chroniony) {
                if (spelnia(p, kryterium, terazMs)) chronione += 1
                continue
            }
            if (spelnia(p, kryterium, terazMs)) doUsuniecia.add(p.id)
        }
        return WynikWyboru(doUsuniecia, chronione, wToku)
    }

    private fun spelnia(p: Przejazd, kryterium: KryteriumPorzadkow, terazMs: Long): Boolean = when (kryterium) {
        is KryteriumPorzadkow.KrotszeNiz -> p.podsumowanie.czasTrwaniaS < kryterium.minuty * 60.0
        KryteriumPorzadkow.Przerwane -> p.status == StatusPrzejazdu.Odzyskany
        is KryteriumPorzadkow.StarszeNiz -> starszeNiz(p.poczatekMs, kryterium.miesiace, terazMs)
    }

    private fun starszeNiz(poczatekMs: Long, miesiace: Int, terazMs: Long): Boolean {
        val cal = Calendar.getInstance(Locale("pl", "PL"))
        cal.timeInMillis = terazMs
        cal.add(Calendar.MONTH, -miesiace)
        return poczatekMs < cal.timeInMillis
    }
}
