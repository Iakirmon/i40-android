package pl.i40.android.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.i40.android.checkup.MigawkaAdaptera
import pl.i40.android.checkup.MigawkaOdczytu
import pl.i40.android.checkup.MigawkaPojazdu
import pl.i40.android.checkup.Raport
import pl.i40.android.checkup.ZrodloRaportu
import pl.i40.android.obd.Readiness
import pl.i40.android.obd.SupportedPids
import pl.i40.android.rules.PasmaOdniesienia
import pl.i40.android.rules.RodzajPasma

class FormatPrzegladuTest {
    @Test
    fun kartyPokazujaKreskeGdyBrakDanych() {
        val gdi = FormatPrzegladu.kartaGdi(pustyRaport())
        assertEquals(FormatPomiaru.NIEDOSTEPNE, gdi.cisnienie.wartosc)
        assertEquals(FormatPomiaru.NIEDOSTEPNE, gdi.obciazenie.wartosc)
        assertEquals(FormatPomiaru.NIEDOSTEPNE, gdi.obroty.wartosc)
        val kat = FormatPrzegladu.kartaKatalizator(pustyRaport())
        assertEquals(FormatPomiaru.NIEDOSTEPNE, kat.temperatura.wartosc)
        assertEquals(FormatPomiaru.NIEDOSTEPNE, kat.sonda.wartosc)
        assertTrue(kat.sonda.powod!!.contains("PID 15"))
        assertTrue(kat.sonda.powod!!.contains("brak formuły"))
    }

    @Test
    fun podcisnienieToRoznicaAtmosferycznegoIKolektora() {
        val raport = raport(
            MigawkaOdczytu(0x0B, "kolektor", "kPa", 34.0, true, false),
            MigawkaOdczytu(0x33, "atm", "kPa", 99.0, true, false)
        )
        assertEquals(65.0, FormatPrzegladu.podcisnienieKpa(raport))
        val powietrze = FormatPrzegladu.grupaPowietrze(raport)
        assertEquals(3, powietrze.size)
        assertTrue(powietrze[2].wyliczony)
        assertEquals(FormatPomiaru.liczba(65.0, 0, "kPa"), powietrze[2].wartosc)
        assertEquals(FormatPomiaru.NIEDOSTEPNE, powietrze[2].norma)

        val bezMap = raport(MigawkaOdczytu(0x33, "atm", "kPa", 99.0, true, false))
        assertEquals(null, FormatPrzegladu.podcisnienieKpa(bezMap))
        assertEquals(FormatPomiaru.NIEDOSTEPNE, FormatPrzegladu.grupaPowietrze(bezMap)[2].wartosc)
    }

    @Test
    fun zakresyKartZPasmaOdniesienia() {
        val jalowy = PasmaOdniesienia.szynaJalowy
        val gdi = FormatPrzegladu.kartaGdi(
            raport(MigawkaOdczytu(0x23, "szyna", "kPa", 3840.0, true, false))
        )
        assertEquals(
            "${jalowy.start.toInt()} – ${jalowy.endInclusive.toInt()} bar",
            gdi.cisnienie.norma
        )
        assertEquals(FormatPomiaru.liczba(PasmaOdniesienia.kpaNaBar(3840.0), 1, "bar"), gdi.cisnienie.wartosc)
        assertEquals(FormatPomiaru.NIEDOSTEPNE, gdi.obciazenie.norma)
        assertEquals(FormatPomiaru.NIEDOSTEPNE, gdi.obroty.norma)
        assertTrue(gdi.stopka.contains("WTRYSK GDI"))
        assertTrue(gdi.stopka.contains("obciążeniem"))

        val praca = PasmaOdniesienia.katalizatorPraca
        val kat = FormatPrzegladu.kartaKatalizator(
            raport(MigawkaOdczytu(0x3C, "kat", "°C", 453.0, true, false))
        )
        assertEquals(
            "${praca.start.toInt()} – ${praca.endInclusive.toInt()} °C",
            kat.temperatura.norma
        )
        assertEquals(
            FormatPomiaru.liczba(PasmaOdniesienia.KATALIZATOR_ZAPLON_C, 0, "°C"),
            kat.zaplon
        )
        assertEquals("gotowy", kat.monitorKatalizatora)
        assertEquals("gotowy", kat.monitorSond)
    }

    @Test
    fun zadenWierszOdczytowNieMaPustejKolumnyNormy() {
        val maska = SupportedPids.bezKontynuacji(
            SupportedPids.merge(
                SupportedPids.pids(fromHex = "4100BE3EA813"),
                SupportedPids.pids(fromHex = "4120A007F011"),
                SupportedPids.pids(fromHex = "4140FED00400")
            )
        )
        val defs = SupportedPids.displayable(maska)
        val odczyty = defs.map { def ->
            MigawkaOdczytu(def.id, def.name, def.unit, 1.0, true, false)
        }
        val raport = pustyRaport().copy(obslugiwanePid = maska.sorted(), odczyty = odczyty)
        val wiersze = FormatPrzegladu.wierszeOdczytow(raport)
        assertTrue(wiersze.isNotEmpty())
        assertFalse(wiersze.any { it.pid == 0x2F })
        for (wiersz in wiersze) {
            assertTrue(wiersz.norma.isNotBlank(), "pusta kolumna normy: ${wiersz.etykieta}")
        }
        val zKatalogu = wiersze.filter { it.pid != null }
        for (wiersz in zKatalogu) {
            val pid = wiersz.pid!!
            val wpisy = PasmaOdniesienia.wpisyDlaPid(pid)
            val maNorme = wpisy.any { it.rodzaj == RodzajPasma.Norma }
            if (!maNorme) {
                assertEquals(FormatPomiaru.NIEDOSTEPNE, wiersz.norma, "PID ${"%02X".format(pid)}")
            } else {
                assertNotEquals(FormatPomiaru.NIEDOSTEPNE, wiersz.norma, "PID ${"%02X".format(pid)}")
            }
        }
        assertTrue(wiersze.any { it.wyliczony })
    }

    private fun pustyRaport(): Raport = raport()

    private fun raport(vararg odczyty: MigawkaOdczytu): Raport = Raport(
        startMs = 1L,
        koniecMs = 2L,
        zrodlo = ZrodloRaportu.Atrapa,
        pojazd = MigawkaPojazdu(),
        adapter = MigawkaAdaptera(),
        obslugiwanePid = odczyty.map { it.pid },
        gotowosc = Readiness.decode(hexResponse = "41010007E100\r>"),
        kodyZapisane = emptyList(),
        kodyOczekujace = emptyList(),
        kodyTrwale = null,
        odczyty = odczyty.toList(),
        wnioski = emptyList()
    )
}
