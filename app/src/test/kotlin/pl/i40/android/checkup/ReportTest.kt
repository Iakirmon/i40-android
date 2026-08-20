package pl.i40.android.checkup

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.i40.android.obd.Dtc
import pl.i40.android.obd.DtcKind
import pl.i40.android.obd.Readiness
import pl.i40.android.rules.WagaWniosku
import pl.i40.android.rules.Wniosek

class ReportTest {
    private fun przykladowy(
        milOn: Boolean = false,
        ready: Boolean = true,
        stored: List<Dtc> = emptyList(),
        pending: List<Dtc> = emptyList(),
        wnioski: List<Wniosek> = emptyList(),
        napieciePid: Double? = 13.8,
        napieciePin16: Double? = 13.8
    ): Raport {
        val odczyty = mutableListOf(
            MigawkaOdczytu(0x0C, "Obroty", "rpm", 750.0, true, false),
            MigawkaOdczytu(0x05, "Płyn", "°C", 85.0, true, false)
        )
        if (napieciePid != null) {
            odczyty.add(MigawkaOdczytu(0x42, "Napięcie", "V", napieciePid, true, false))
        }
        return Raport(
            startMs = 1_723_046_400_000L,
            koniecMs = 1_723_046_460_000L,
            zrodlo = ZrodloRaportu.Atrapa,
            pojazd = MigawkaPojazdu(
                vin = "KMHLC41DAFU066558",
                producent = "Hyundai Motor Company, Korea",
                rokModelu = 2015,
                fabryka = "Ulsan",
                kalibracja = "GGVF-EE5AFS01600",
                nazwaEcu = "ECM-EngineControl"
            ),
            adapter = MigawkaAdaptera(
                firmware = "ELM327 v1.5",
                opis = null,
                kodProtokolu = "6",
                nazwaProtokolu = "ISO 15765-4 CAN (11 bit, 500 kbit/s)",
                napieciePin16 = napieciePin16
            ),
            obslugiwanePid = listOf(0x05, 0x0C, 0x42),
            gotowosc = Readiness.decode(if (milOn) 0x80 else 0x00, 0x07, 0xE1, if (ready) 0x00 else 0x01),
            kodyZapisane = stored,
            kodyOczekujace = pending,
            kodyTrwale = null,
            odczyty = odczyty,
            wnioski = wnioski
        )
    }

    @Test
    fun werdyktOkGdyNicNieWskazujeInaczej() {
        assertEquals(Werdykt.Ok, przykladowy().werdykt)
        assertEquals("Wszystko OK", Werdykt.Ok.tytul)
    }

    @Test
    fun werdyktUwagaPrzyKodachOczekujacych() {
        val dtc = Dtc("P0171", DtcKind.Generic, "test")
        assertEquals(Werdykt.Uwaga, przykladowy(pending = listOf(dtc)).werdykt)
        assertEquals("Wymaga uwagi", Werdykt.Uwaga.tytul)
    }

    @Test
    fun werdyktUwagaPrzyMonitorachNiegotowych() {
        assertEquals(Werdykt.Uwaga, przykladowy(ready = false).werdykt)
    }

    @Test
    fun werdyktUsterkaPrzyMil() {
        assertEquals(Werdykt.Usterka, przykladowy(milOn = true).werdykt)
        assertEquals("Usterka", Werdykt.Usterka.tytul)
    }

    @Test
    fun werdyktUsterkaPrzyKodachZapisanych() {
        val dtc = Dtc("P0301", DtcKind.Generic, "test")
        assertEquals(Werdykt.Usterka, przykladowy(stored = listOf(dtc)).werdykt)
    }

    @Test
    fun usterkaZWnioskuWygrywaZUwaga() {
        val usterka = Wniosek("overheat", WagaWniosku.Usterka, "Płyn powyżej 105 °C", "Przegrzewanie.")
        val dtc = Dtc("P0171", DtcKind.Generic, "test")
        val raport = przykladowy(pending = listOf(dtc), wnioski = listOf(usterka))
        assertEquals(Werdykt.Usterka, raport.werdykt)
    }

    @Test
    fun uwagaZWnioskuGdyBrakKodow() {
        val uwaga = Wniosek(
            "ltft_lean",
            WagaWniosku.Uwaga,
            "Korekta długoterminowa powyżej +10%",
            "Mieszanka uboga."
        )
        assertEquals(Werdykt.Uwaga, przykladowy(wnioski = listOf(uwaga)).werdykt)
    }

    @Test
    fun napiecieDoRegulZPid42AGdyBrakZPin16() {
        val zPid = przykladowy(napieciePid = 14.1, napieciePin16 = 12.0)
        assertEquals(14.1, zPid.wejscieRegul.voltage)

        val zPin = przykladowy(napieciePid = null, napieciePin16 = 12.2)
        assertEquals(12.2, zPin.wejscieRegul.voltage)
    }

    @Test
    fun odswiezWnioskiZOleju() {
        val zOlejem = przykladowy().copy(
            odczyty = przykladowy().odczyty +
                MigawkaOdczytu(0x5C, "Olej", "°C", 70.0, true, false)
        ).odswiezWnioski()
        assertTrue(zOlejem.wnioski.any { it.ruleId == "oil_cold" })
        assertTrue(zOlejem.werdykt == Werdykt.Ok || zOlejem.werdykt == Werdykt.Uwaga)
    }
}
