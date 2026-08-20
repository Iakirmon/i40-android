package pl.i40.android.ui

import pl.i40.android.obd.FuelSystemStatus
import pl.i40.android.rules.PasmaOdniesienia
import pl.i40.android.rules.RodzajPasma

enum class StanParametru {
    NieZmierzony,
    NiewaznyTeraz,
    BezPasma,
    WNormie,
    Ponizej,
    Powyzej,
    ;

    companion object {
        fun ocen(
            pid: Int,
            wartosc: Double?,
            odczytanoWTejSesji: Boolean,
            warunkiWaznosciSpelnione: Boolean
        ): StanParametru {
            if (!odczytanoWTejSesji) return NieZmierzony
            if (!warunkiWaznosciSpelnione) return NiewaznyTeraz
            val pasmo = PasmaOdniesienia.wpisy.firstOrNull { it.pid == pid && it.rodzaj == RodzajPasma.Norma }
            if (pasmo == null) return BezPasma
            if (wartosc == null) return NieZmierzony
            val min = pasmo.min
            val max = pasmo.max
            if (min != null && wartosc < min) return Ponizej
            if (max != null && wartosc > max) return Powyzej
            return WNormie
        }
    }
}

enum class StanPanelu {
    Odchylenia,
    JeszczeNieWiem,
    WNormie,
    ;

    companion object {
        fun zloz(stany: List<StanParametru>): StanPanelu {
            if (stany.any { it == StanParametru.Ponizej || it == StanParametru.Powyzej }) return Odchylenia
            val niewiedza = stany.any {
                it == StanParametru.NieZmierzony || it == StanParametru.NiewaznyTeraz || it == StanParametru.BezPasma
            }
            if (niewiedza) return JeszczeNieWiem
            return WNormie
        }
    }
}

enum class CelSkrotu {
    Podstawowy,
    Mieszanka,
    WtryskGdi,
    Termika,
    Powietrze,
    PrzegladOdczyty,
    ;

    companion object {
        fun dla(pid: Int): CelSkrotu = when (pid) {
            0x0C, 0x04, 0x0E -> Podstawowy
            0x06, 0x07, 0x03, 0x44 -> Mieszanka
            0x23, 0x43, 0x11 -> WtryskGdi
            0x3C, 0x05, 0x5C, 0x0F, 0x46 -> Termika
            0x0B, 0x33, 0x4C, 0x49 -> Powietrze
            else -> PrzegladOdczyty
        }
    }
}

/** Jedno źródło ważności korekt — kafel i panel Stan czytają stąd. */
object WarunkiWaznosci {
    fun spelnione(pid: Int, status0103: Int?): Boolean {
        if (pid == 0x06 || pid == 0x07) return FuelSystemStatus.korektyWazne(status0103)
        return true
    }
}
