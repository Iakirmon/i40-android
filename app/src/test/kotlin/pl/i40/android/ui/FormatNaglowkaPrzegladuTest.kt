package pl.i40.android.ui

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.i40.android.checkup.MigawkaAdaptera
import pl.i40.android.checkup.MigawkaOdczytu
import pl.i40.android.checkup.MigawkaPojazdu
import pl.i40.android.checkup.PorownaniePrzegladow
import pl.i40.android.checkup.Raport
import pl.i40.android.checkup.ZapisanyPrzeglad
import pl.i40.android.checkup.ZrodloRaportu
import pl.i40.android.obd.IgnitionType
import pl.i40.android.obd.ReadinessStatus

class FormatNaglowkaPrzegladuTest {
    @Test
    fun wStanieIPozaStanemObokSiebie() {
        val rozgrzany = raport(plynC = 92.0, runtimeS = 840.0)
        val zimny = raport(plynC = 52.0, runtimeS = 120.0)
        val poprzedni = ZapisanyPrzeglad(
            id = "p",
            kiedyMs = 1_721_000_000_000L,
            vin = "VIN",
            stan = PorownaniePrzegladow.STAN_JALOWY_ROZGRZANY,
            raport = rozgrzany
        )
        assertTrue(rozgrzany.jalowyRozgrzany)
        assertFalse(zimny.jalowyRozgrzany)

        val wStanie = FormatNaglowkaPrzegladu.tekst(rozgrzany, poprzedni)
        assertTrue(wStanie.contains("jałowy rozgrzany"))
        assertTrue(wStanie.contains("Porównanie z"))
        assertTrue(wStanie.contains("ten sam stan"))
        assertFalse(wStanie.contains("Porównanie liczbowe niedostępne"))

        val poza = FormatNaglowkaPrzegladu.tekst(zimny, poprzedni)
        assertTrue(poza.contains("silnik nierozgrzany"))
        assertTrue(poza.contains("52"))
        assertTrue(poza.contains("Porównanie liczbowe niedostępne"))
        assertTrue(poza.contains("Kody błędów i monitory porównane mimo to"))
        assertFalse(poza.contains("ten sam stan"))
    }

    private fun raport(plynC: Double, runtimeS: Double): Raport = Raport(
        startMs = 1L,
        koniecMs = 2L,
        zrodlo = ZrodloRaportu.Atrapa,
        pojazd = MigawkaPojazdu(vin = "VIN"),
        adapter = MigawkaAdaptera(),
        obslugiwanePid = listOf(0x05, 0x0C, 0x0D, 0x1F),
        gotowosc = ReadinessStatus(
            milOn = false,
            storedDtcCount = 0,
            ignition = IgnitionType.Spark,
            continuous = emptyList(),
            monitors = emptyList(),
            incomplete = emptyList(),
            ready = true
        ),
        kodyZapisane = emptyList(),
        kodyOczekujace = emptyList(),
        kodyTrwale = null,
        odczyty = listOf(
            MigawkaOdczytu(0x05, "Płyn", "°C", plynC, true, false),
            MigawkaOdczytu(0x0C, "Obroty", "rpm", 712.0, true, false),
            MigawkaOdczytu(0x0D, "Prędkość", "km/h", 0.0, true, false),
            MigawkaOdczytu(0x1F, "Czas", "s", runtimeS, true, false)
        ),
        wnioski = emptyList()
    )
}
