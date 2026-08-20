package pl.i40.android.checkup

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.i40.android.obd.Dtc
import pl.i40.android.obd.DtcKind
import pl.i40.android.obd.IgnitionType
import pl.i40.android.obd.ReadinessMonitor
import pl.i40.android.obd.ReadinessStatus
import pl.i40.android.ui.FormatZmianPrzegladu

class PorownaniePrzegladowTest {
    @Test
    fun kodPojawilSie() {
        val poprzedni = raport(kody = emptyList())
        val teraz = raport(kody = listOf(P0171))
        val p = PorownaniePrzegladow.porownaj(teraz, poprzedni)
        assertEquals(listOf("P0171"), p.kodyNowe.map { it.code })
        assertTrue(p.kodyZnikniete.isEmpty())
        val tekst = FormatZmianPrzegladu.blok(p, poprzedni.startMs)
        assertTrue(tekst.contains("P0171"))
        assertTrue(tekst.contains("kod pojawił się od poprzedniego przeglądu"))
        assertFalse(tekst.contains("norma"))
    }

    @Test
    fun kodZniknal() {
        val poprzedni = raport(kody = listOf(P0442))
        val teraz = raport(kody = emptyList())
        val p = PorownaniePrzegladow.porownaj(teraz, poprzedni)
        assertEquals(listOf("P0442"), p.kodyZnikniete.map { it.code })
        val tekst = FormatZmianPrzegladu.blok(p, poprzedni.startMs)
        assertTrue(tekst.contains("P0442"))
        assertTrue(tekst.contains("kodu już nie ma"))
    }

    @Test
    fun monitorStracilGotowosc() {
        val poprzedni = raport(monitory = listOf(ReadinessMonitor(MONITOR_ODPAROWANIA, incomplete = false)))
        val teraz = raport(monitory = listOf(ReadinessMonitor(MONITOR_ODPAROWANIA, incomplete = true)))
        val p = PorownaniePrzegladow.porownaj(teraz, poprzedni)
        assertEquals(1, p.monitoryStracone.size)
        assertEquals(MONITOR_ODPAROWANIA, p.monitoryStracone[0].name)
        val tekst = FormatZmianPrzegladu.blok(p, poprzedni.startMs)
        assertTrue(tekst.contains("był gotowy, teraz nie"))
        assertTrue(tekst.contains(MONITOR_ODPAROWANIA))
    }

    @Test
    fun kontrolkaMilZmienilaStan() {
        val poprzedni = raport(milOn = false)
        val teraz = raport(milOn = true)
        val p = PorownaniePrzegladow.porownaj(teraz, poprzedni)
        assertEquals(false, p.milPoprzednio)
        assertEquals(true, p.milTeraz)
        val tekst = FormatZmianPrzegladu.blok(p, poprzedni.startMs)
        assertTrue(tekst.contains("Kontrolka MIL"))
        assertTrue(tekst.contains("była zgaszona, teraz świeci"))
    }

    @Test
    fun bezZmianPiszeToWprost() {
        val a = raport(kody = listOf(P0171), milOn = false)
        val b = raport(kody = listOf(P0171), milOn = false)
        val p = PorownaniePrzegladow.porownaj(b, a)
        assertTrue(p.bezZmian)
        val tekst = FormatZmianPrzegladu.blok(p, a.startMs)
        assertTrue(tekst.contains("Bez zmian: te same kody, te same monitory, ta sama kontrolka."))
    }

    @Test
    fun porownanieKodowDzialaPozaStanemJalowego() {
        val zimny = raport(
            kody = emptyList(),
            plynC = 52.0,
            predkoscKmh = 0.0,
            rpm = 750.0,
            runtimeS = 120.0
        )
        val rozgrzany = raport(
            kody = listOf(P0171),
            plynC = 92.0,
            predkoscKmh = 0.0,
            rpm = 712.0,
            runtimeS = 840.0
        )
        assertFalse(zimny.jalowyRozgrzany)
        assertTrue(rozgrzany.jalowyRozgrzany)
        val p = PorownaniePrzegladow.porownaj(rozgrzany, zimny)
        assertEquals(listOf("P0171"), p.kodyNowe.map { it.code })
    }

    @Test
    fun dwaVinNieMieszajaSieABrakVinuNieBierzeUdzialu() {
        val mag = PamiecPrzegladow()
        mag.zapisz(zapis("a", "VIN-A", raport(kody = listOf(P0171))))
        mag.zapisz(zapis("b", "VIN-B", raport(kody = listOf(P0442))))
        mag.zapisz(zapis("c", null, raport(kody = listOf(P0171))))
        assertEquals(1, mag.dlaVin("VIN-A").size)
        assertEquals("P0171", mag.dlaVin("VIN-A")[0].raport.kodyZapisane[0].code)
        assertNull(mag.poprzedniDlaVin("VIN-A", pozaId = "a"))
        assertNull(PorownaniePrzegladow.poprzedniPorownywalny(mag, vin = null, pozaId = "c"))
    }

    private fun zapis(id: String, vin: String?, r: Raport) = ZapisanyPrzeglad(
        id = id,
        kiedyMs = r.startMs,
        vin = vin,
        stan = if (r.jalowyRozgrzany) "jalowy_rozgrzany" else null,
        raport = r
    )

    private fun raport(
        kody: List<Dtc> = emptyList(),
        milOn: Boolean = false,
        monitory: List<ReadinessMonitor> = listOf(ReadinessMonitor(MONITOR_ODPAROWANIA, incomplete = false)),
        plynC: Double = 90.0,
        predkoscKmh: Double = 0.0,
        rpm: Double = 750.0,
        runtimeS: Double = 700.0
    ): Raport {
        val incomplete = monitory.filter { it.incomplete }
        return Raport(
            startMs = 1_721_000_000_000L,
            koniecMs = 1_721_000_060_000L,
            zrodlo = ZrodloRaportu.Atrapa,
            pojazd = MigawkaPojazdu(vin = "KMHLC41DAFU066558"),
            adapter = MigawkaAdaptera(),
            obslugiwanePid = listOf(0x05, 0x0C, 0x0D, 0x1F),
            gotowosc = ReadinessStatus(
                milOn = milOn,
                storedDtcCount = kody.size,
                ignition = IgnitionType.Spark,
                continuous = emptyList(),
                monitors = monitory,
                incomplete = incomplete,
                ready = incomplete.isEmpty()
            ),
            kodyZapisane = kody,
            kodyOczekujace = emptyList(),
            kodyTrwale = null,
            odczyty = listOf(
                MigawkaOdczytu(0x05, "Płyn", "°C", plynC, true, false),
                MigawkaOdczytu(0x0C, "Obroty", "rpm", rpm, true, false),
                MigawkaOdczytu(0x0D, "Prędkość", "km/h", predkoscKmh, true, false),
                MigawkaOdczytu(0x1F, "Czas", "s", runtimeS, true, false)
            ),
            wnioski = emptyList()
        )
    }

    companion object {
        val P0171 = Dtc("P0171", DtcKind.Generic, "Mieszanka zbyt uboga, bank 1")
        val P0442 = Dtc("P0442", DtcKind.Generic, "Mała nieszczelność układu odparowania")
        const val MONITOR_ODPAROWANIA = "Układ odparowania paliwa"
    }
}
