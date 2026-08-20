package pl.i40.android.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TripStateMachineTest {
    @Test
    fun startZObrotowNieZZaplonu() {
        val m = TripStateMachine()
        assertEquals(StanPrzejazdu.Rozlaczony, m.stan)
        m.on(ZdarzeniePrzejazdu.Polaczono)
        assertEquals(StanPrzejazdu.Czuwanie, m.stan)
        val wynik = m.on(ZdarzeniePrzejazdu.Obroty(rpm = 800.0, terazMs = 0))
        assertEquals(StanPrzejazdu.Nagrywa, wynik.stan)
        assertEquals(listOf(AkcjaPrzejazdu.StartSesji), wynik.akcje)
    }

    @Test
    fun zeroObrotowPrzez30sZamyka() {
        val m = TripStateMachine()
        m.on(ZdarzeniePrzejazdu.Polaczono)
        m.on(ZdarzeniePrzejazdu.Obroty(800.0, 0))
        m.on(ZdarzeniePrzejazdu.Obroty(0.0, 1_000))
        val zaWczesnie = m.on(ZdarzeniePrzejazdu.Obroty(0.0, 20_000))
        assertEquals(StanPrzejazdu.Nagrywa, zaWczesnie.stan)
        val poPostoju = m.on(ZdarzeniePrzejazdu.Obroty(0.0, 31_000))
        assertEquals(StanPrzejazdu.Zamykanie, poPostoju.stan)
        assertEquals(listOf(AkcjaPrzejazdu.ZamknijSesje), poPostoju.akcje)
    }

    @Test
    fun rozlaczenieWTrakcieNagrywaniaZapisuje() {
        val m = TripStateMachine()
        m.on(ZdarzeniePrzejazdu.Polaczono)
        m.on(ZdarzeniePrzejazdu.Obroty(900.0, 0))
        val wynik = m.on(ZdarzeniePrzejazdu.Rozlaczono)
        assertEquals(StanPrzejazdu.Zamykanie, wynik.stan)
        assertEquals(listOf(AkcjaPrzejazdu.ZamknijSesje), wynik.akcje)
    }

    @Test
    fun zadaniePrzegladuPrzechodziPrzezZamykanie() {
        val m = TripStateMachine()
        m.on(ZdarzeniePrzejazdu.Polaczono)
        m.on(ZdarzeniePrzejazdu.Obroty(700.0, 0))
        m.on(ZdarzeniePrzejazdu.ZadaniePrzegladu)
        assertEquals(StanPrzejazdu.Zamykanie, m.stan)
        val po = m.on(ZdarzeniePrzejazdu.ZamkniecieUkonczone)
        assertEquals(StanPrzejazdu.Czuwanie, po.stan)
        assertEquals(listOf(AkcjaPrzejazdu.UruchomPrzeglad), po.akcje)
    }

    @Test
    fun spadekRuntimeOtwieraNowyPrzejazd() {
        val m = TripStateMachine()
        m.on(ZdarzeniePrzejazdu.Polaczono)
        m.on(ZdarzeniePrzejazdu.Obroty(800.0, 0))
        m.on(ZdarzeniePrzejazdu.Runtime(100.0))
        val spadek = m.on(ZdarzeniePrzejazdu.Runtime(2.0))
        assertEquals(StanPrzejazdu.Zamykanie, spadek.stan)
        assertEquals(listOf(AkcjaPrzejazdu.ZamknijIOtworzNowa), spadek.akcje)
        val po = m.on(ZdarzeniePrzejazdu.ZamkniecieUkonczone)
        assertEquals(StanPrzejazdu.Nagrywa, po.stan)
        assertEquals(listOf(AkcjaPrzejazdu.StartSesji), po.akcje)
    }

    @Test
    fun brakMiejscaZamykaZTymCoZebrano() {
        val m = TripStateMachine()
        m.on(ZdarzeniePrzejazdu.Polaczono)
        m.on(ZdarzeniePrzejazdu.Obroty(800.0, 0))
        val wynik = m.on(ZdarzeniePrzejazdu.BrakMiejsca)
        assertEquals(StanPrzejazdu.Zamykanie, wynik.stan)
        assertEquals(listOf(AkcjaPrzejazdu.ZamknijSesje), wynik.akcje)
    }

    @Test
    fun na200000CyklachZeroKolizjiTrzechZapytan() {
        var trzy = 0
        var max = 0
        var queries = 0
        var poziomC = 0
        for (n in 1..200_000) {
            val q = PetlaFaz.liczbaZapytan(n)
            queries += q
            if (q > max) max = q
            if (q >= 3) trzy += 1
            if (PetlaFaz.wolnyC(n)) poziomC += 1
        }
        assertEquals(0, trzy)
        assertEquals(2, max)
        assertEquals(10_000, poziomC)
        val naSekundePrzy4Hz = queries * 4.0 / 200_000.0
        assertTrue(naSekundePrzy4Hz <= 25.0)
        assertEquals(5.62, naSekundePrzy4Hz, 0.01)
        assertTrue(PetlaFaz.kody03(150))
        assertTrue(!PetlaFaz.sredniB(150))
        assertTrue(!PetlaFaz.wolnyC(150))
        assertTrue(PetlaFaz.sredniB(5))
        assertTrue(PetlaFaz.wolnyC(13))
        assertTrue(!PetlaFaz.szybkiA(13))
        assertTrue(!PetlaFaz.sredniB(13))
        assertTrue(!PetlaFaz.kody03(5))
    }

    @Test
    fun czteryPoziomyNa20000CyklachZeroTrzechZapytan() {
        var trzy = 0
        var szybkich = 0
        var queries = 0
        for (n in 1..20_000) {
            val q = PetlaFaz.liczbaZapytan(n)
            queries += q
            if (q >= 3) trzy += 1
            if (PetlaFaz.szybkiA(n)) szybkich += 1
        }
        assertEquals(0, trzy)
        assertEquals(5_000, szybkich)
        val naSekundePrzy4Hz = queries * 4.0 / 20_000.0
        assertTrue(naSekundePrzy4Hz <= 25.0)
        assertTrue(PetlaFaz.szybkiA(4))
        assertTrue(!PetlaFaz.szybkiA(5))
        assertTrue(!PetlaFaz.szybkiA(150))
    }

    @Test
    fun poziomCDokladnieCoDwudziestyCykl() {
        for (n in 1..40) {
            assertEquals(n % 20 == 13, PetlaFaz.wolnyC(n), "n=$n")
        }
    }

    @Test
    fun petlaSredniaDokladnieCoCzwartyCykl() {
        for (n in 1..40) {
            assertEquals(n % 4 == 0, PetlaFaz.szybkiA(n), "n=$n")
        }
    }
}
