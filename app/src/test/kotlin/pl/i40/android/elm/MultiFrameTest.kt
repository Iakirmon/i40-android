package pl.i40.android.elm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MultiFrameTest {
    @Test
    fun sklejaLinieNHexPosortowanePoNIpomijaDlugosc() {
        val text = "014\r0:4902014B4D48\r1:4C433431444146\r2:55303636353538\r\r>"
        assertEquals(
            "4902014B4D484C43343144414655303636353538",
            MultiFrame.collectFrameHex(text),
        )
    }

    @Test
    fun zwyklaOdpowiedzJednoramkowaNieJestWieloramkowa() {
        assertNull(MultiFrame.collectFrameHex("4100BE3EA813\r>"))
    }

    @Test
    fun sortujeRamkiNawetGdyPrzychodzaWOdwrotnejKolejnosci() {
        val text = "1:4C433431444146\r0:4902014B4D48\r2:55303636353538"
        assertEquals(
            "4902014B4D484C43343144414655303636353538",
            MultiFrame.collectFrameHex(text),
        )
    }
}
