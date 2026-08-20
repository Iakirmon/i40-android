package pl.i40.android.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WskaznikPaneliTest {
    @Test
    fun piecKropekMiejsceNaPowietrze() {
        val m = WskaznikPaneli.tekst(PanelZywy.Mieszanka)
        assertEquals(5, m.count { it == '●' || it == '○' })
        assertTrue(m.startsWith("○ ● ○ ○ ○"))
        assertTrue(m.contains("MIESZANKA"))
        val t = WskaznikPaneli.tekst(PanelZywy.Termika)
        assertTrue(t.startsWith("○ ○ ○ ● ○"))
    }
}
