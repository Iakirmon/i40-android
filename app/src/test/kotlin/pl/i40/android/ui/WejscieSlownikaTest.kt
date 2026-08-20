package pl.i40.android.ui

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import pl.i40.android.checkup.MigawkaAdaptera
import pl.i40.android.checkup.MigawkaOdczytu
import pl.i40.android.checkup.MigawkaPojazdu
import pl.i40.android.checkup.Raport
import pl.i40.android.checkup.ZrodloRaportu
import pl.i40.android.obd.SupportedPids
import pl.i40.android.service.FormatKaflaWykresow

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WejscieSlownikaTest {
    @BeforeAll
    fun wczytaj() {
        MagazynSlownika.wczytaj(File("src/main/assets/slownik.md").readText())
    }

    @Test
    fun kontraktHaselDlaWszystkichWyswietlanychPid() {
        for (pid in WejscieSlownika.PIDY_WYSWIETLANE) {
            val id = WejscieSlownika.idDlaPid(pid)
            assertNotNull(id, "brak mapowania hasła dla PID ${"%02X".format(pid)}")
            assertNotNull(MagazynSlownika.poId(id!!), "brak hasła $id dla PID ${"%02X".format(pid)}")
        }
        assertNull(WejscieSlownika.idDlaPid(0x2F), "012F nie jest wyświetlany (P1)")
    }

    @Test
    fun kazdyKafelOtwieraHaslo() {
        for (pid in FormatKafla.KAFLI_DOMYSLNE) {
            val id = WejscieSlownika.idDlaPid(pid)
            assertNotNull(id, "kafel ${"%02X".format(pid)}")
            assertNotNull(MagazynSlownika.poId(id!!))
        }
    }

    @Test
    fun kazdyWierszOdczytowOtwieraHaslo() {
        val maska = SupportedPids.bezKontynuacji(
            SupportedPids.merge(
                SupportedPids.pids(fromHex = "4100BE3EA813"),
                SupportedPids.pids(fromHex = "4120A007F011"),
                SupportedPids.pids(fromHex = "4140FED00400")
            )
        )
        val defs = SupportedPids.displayable(maska)
        val odczyty = defs.map { MigawkaOdczytu(it.id, it.name, it.unit, 1.0, true, false) }
        val raport = Raport(
            startMs = 0L,
            koniecMs = 1L,
            zrodlo = ZrodloRaportu.Atrapa,
            pojazd = MigawkaPojazdu(),
            adapter = MigawkaAdaptera(),
            obslugiwanePid = maska.sorted(),
            gotowosc = null,
            kodyZapisane = emptyList(),
            kodyOczekujace = emptyList(),
            kodyTrwale = null,
            odczyty = odczyty,
            wnioski = emptyList()
        )
        val wiersze = FormatPrzegladu.wierszeOdczytow(raport)
        assertEquals(33, wiersze.size)
        for (w in wiersze) {
            val id = WejscieSlownika.idDlaWiersza(w)
            assertNotNull(id, "wiersz bez hasła: ${w.etykieta}")
            assertNotNull(MagazynSlownika.poId(id!!), "brak hasła $id dla ${w.etykieta}")
        }
    }

    @Test
    fun podpisyWykresowOtwierajaHaslo() {
        val pidy = FormatKaflaWykresow.PIDY_WYKRESOW +
            listOf(0x06, 0x07, 0x44, 0x23, 0x43, 0x11, 0x3C, 0x05, 0x5C, 0x2E, 0x33, 0x0B, 0x4C, 0x49)
        for (pid in pidy) {
            val id = WejscieSlownika.idDlaPid(pid)
            assertNotNull(id, "wykres ${"%02X".format(pid)}")
            assertNotNull(MagazynSlownika.poId(id!!))
        }
    }

    @Test
    fun przyPredkosciWiekszejOdZeraSlownikSieNieOtwiera() {
        assertTrue(WejscieSlownika.moznaOtworzyc(wRuchu = false))
        assertFalse(WejscieSlownika.moznaOtworzyc(wRuchu = true))
        assertNull(WejscieSlownika.otworz(id = "obroty-silnika", wRuchu = true))
        assertEquals("obroty-silnika", WejscieSlownika.otworz(id = "obroty-silnika", wRuchu = false))
    }

    @Test
    fun pidyBliskoznaczneDzielaHaslo() {
        assertEquals("pozycje-przepustnicy", WejscieSlownika.idDlaPid(0x11))
        assertEquals("pozycje-przepustnicy", WejscieSlownika.idDlaPid(0x45))
        assertEquals("pozycje-przepustnicy", WejscieSlownika.idDlaPid(0x47))
        assertEquals("pozycje-przepustnicy", WejscieSlownika.idDlaPid(0x4C))
        assertEquals("pozycja-pedalu", WejscieSlownika.idDlaPid(0x49))
        assertEquals("pozycja-pedalu", WejscieSlownika.idDlaPid(0x4A))
        assertEquals("stan-monitorow", WejscieSlownika.idDlaPid(0x01))
        assertEquals("stan-monitorow", WejscieSlownika.idDlaPid(0x41))
    }
}
