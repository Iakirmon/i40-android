package pl.i40.android.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SkalaI40Test {
    @Test
    fun skalaSystemowa2xOgraniczonaDo13() {
        assertEquals(1.3f, SkalaI40.ograniczSkale(2.0f))
        assertEquals(1.3f, SkalaI40.ograniczSkale(1.5f))
        assertEquals(1.0f, SkalaI40.ograniczSkale(1.0f))
        assertEquals(1.2f, SkalaI40.ograniczSkale(1.2f))
    }

    @Test
    fun skalaTypograficznaZTabeli42() {
        assertEquals(44f, SkalaI40.KAFEL_WARTOSC_SP)
        assertEquals(28f, SkalaI40.SLAD_WARTOSC_SP)
        assertEquals(34f, SkalaI40.STAN_ZDANIE_SP)
        assertEquals(17f, SkalaI40.TEKST_SP)
        assertEquals(13f, SkalaI40.ETYKIETA_SP)
        assertEquals(12f, SkalaI40.OS_SP)
    }

    @Test
    fun celeWRuchuMaja56dp() {
        assertEquals(56, SkalaI40.CEL_W_RUCHU_DP)
        assertEquals(48, SkalaI40.CEL_POSTOJ_DP)
        assertTrue(SkalaI40.CEL_W_RUCHU_DP >= 56)
    }

    @Test
    fun automatBezSygnaluWybieraNoc() {
        assertEquals(MotywI40.Noc, RozstrzyganieMotywu.motyw(WyborMotywu.Automatycznie, null))
        assertEquals(MotywI40.Dzien, RozstrzyganieMotywu.motyw(WyborMotywu.Automatycznie, true))
        assertEquals(MotywI40.Noc, RozstrzyganieMotywu.motyw(WyborMotywu.Automatycznie, false))
        assertEquals(MotywI40.Noc, RozstrzyganieMotywu.motyw(WyborMotywu.Noc, true))
        assertEquals(MotywI40.Dzien, RozstrzyganieMotywu.motyw(WyborMotywu.Dzien, false))
    }

    @Test
    fun przelaczenieMotywuNieTykaFlagiNagrywania() {
        var nagrywa = true
        var ustawienia = StanUstawienWygladu(WyborMotywu.Noc)
        ustawienia = ustawienia.zWyboorem(WyborMotywu.Dzien)
        assertEquals(MotywI40.Dzien, ustawienia.motyw)
        assertTrue(nagrywa)
        ustawienia = ustawienia.zWyboorem(WyborMotywu.Automatycznie)
        assertEquals(MotywI40.Noc, ustawienia.motyw)
        assertTrue(nagrywa)
    }
}
