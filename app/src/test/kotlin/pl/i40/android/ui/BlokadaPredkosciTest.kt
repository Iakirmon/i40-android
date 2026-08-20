package pl.i40.android.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.i40.android.acquisition.SampleTick
import pl.i40.android.obd.DecodedPid
import pl.i40.android.obd.MultiPidReading
import pl.i40.android.service.BlokadaPredkosci
import pl.i40.android.service.InterakcjaZywa
import pl.i40.android.service.StanZywy

class BlokadaPredkosciTest {
    @Test
    fun predkoscPowyzejZeraToRuch() {
        assertTrue(BlokadaPredkosci.wRuchu(0.1))
        assertTrue(BlokadaPredkosci.wRuchu(50.0))
        assertFalse(BlokadaPredkosci.wRuchu(0.0))
        assertFalse(BlokadaPredkosci.wRuchu(null))
    }

    @Test
    fun wRuchuTylkoStopGdyNagrywa() {
        assertTrue(
            BlokadaPredkosci.pozwala(InterakcjaZywa.Stop, wRuchu = true, nagrywa = true)
        )
        assertFalse(
            BlokadaPredkosci.pozwala(InterakcjaZywa.Nawigacja, wRuchu = true, nagrywa = true)
        )
        assertFalse(
            BlokadaPredkosci.pozwala(InterakcjaZywa.Ustawienia, wRuchu = true, nagrywa = true)
        )
        assertFalse(
            BlokadaPredkosci.pozwala(InterakcjaZywa.ZmianaParametrow, wRuchu = true, nagrywa = true)
        )
        assertFalse(
            BlokadaPredkosci.pozwala(InterakcjaZywa.Przeglad, wRuchu = true, nagrywa = true)
        )
    }

    @Test
    fun naPostojuWszystkoDozwolone() {
        for (rodzaj in InterakcjaZywa.entries) {
            assertTrue(BlokadaPredkosci.pozwala(rodzaj, wRuchu = false, nagrywa = false))
            assertTrue(BlokadaPredkosci.pozwala(rodzaj, wRuchu = false, nagrywa = true))
        }
    }

    @Test
    fun stanZywyCzytaPredkoscZ0d() {
        val stan = StanZywy()
        assertFalse(stan.wRuchu)
        stan.zastosuj(mapOf(0x0D to 12.0), 1.0)
        assertTrue(stan.wRuchu)
        assertTrue(stan.blokujeChrome)
        assertFalse(stan.pozwala(InterakcjaZywa.Stop))
        stan.nagrywa = true
        assertTrue(stan.pozwala(InterakcjaZywa.Stop))
        assertFalse(stan.pozwala(InterakcjaZywa.Nawigacja))
    }

    @Test
    fun olejZModeluIgnorujeObd5c() {
        val stan = StanZywy()
        stan.zastosuj(
            SampleTick(
                kind = SampleTick.Kind.Hot,
                time = 0.0,
                readings = listOf(
                    MultiPidReading(0x05, listOf(0x50), DecodedPid.Numeric(40.0)),
                    MultiPidReading(0x04, listOf(0x33), DecodedPid.Numeric(20.0)),
                    MultiPidReading(0x5C, listOf(0x80), DecodedPid.Numeric(88.0))
                )
            )
        )
        stan.zastosuj(
            SampleTick(
                kind = SampleTick.Kind.Hot,
                time = 1.0,
                readings = listOf(
                    MultiPidReading(0x05, listOf(0x52), DecodedPid.Numeric(42.0)),
                    MultiPidReading(0x04, listOf(0x33), DecodedPid.Numeric(20.0))
                )
            )
        )
        assertTrue(stan.olejC != null)
        assertTrue(stan.olejC != 88.0)
        assertEquals(stan.olejC, stan.wartosciAlarmu()[0x5C])
        assertEquals(stan.olejC, stan.wartosc(0x5C))
    }
}
