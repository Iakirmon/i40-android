package pl.i40.android.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import pl.i40.android.acquisition.RingSample

class FormatPowietrzaTest {
    @Test
    fun podcisnienieNullGdyBrakKtoregosSkladnika() {
        assertEquals(65.0, FormatPowietrza.podcisnienieKpa(99.0, 34.0)!!, 1e-9)
        assertNull(FormatPowietrza.podcisnienieKpa(null, 34.0))
        assertNull(FormatPowietrza.podcisnienieKpa(99.0, null))
        assertNull(FormatPowietrza.podcisnienieKpa(null, null))
    }

    @Test
    fun podcisnienieNieZaklada101kPa() {
        val atm = emptyList<RingSample>()
        val map = listOf(RingSample(1.0, 34.0))
        assertEquals(emptyList<RingSample>(), FormatPowietrza.probkiPodcisnienia(atm, map))
    }

    @Test
    fun rozjazdWPunktachProcentowych() {
        assertEquals(0.0, FormatPowietrza.rozjazdPkt(18.0, 18.0)!!, 1e-9)
        assertEquals(5.0, FormatPowietrza.rozjazdPkt(20.0, 15.0)!!, 1e-9)
        assertNull(FormatPowietrza.rozjazdPkt(null, 15.0))
        assertNull(FormatPowietrza.rozjazdPkt(20.0, null))
    }
}
