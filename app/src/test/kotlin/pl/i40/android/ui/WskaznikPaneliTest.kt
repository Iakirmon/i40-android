package pl.i40.android.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WskaznikPaneliTest {
    @Test
    fun szescKropekStanPierwszy() {
        val s = WskaznikPaneli.tekst(PanelZywy.Stan)
        assertEquals(6, s.count { it == '●' || it == '○' })
        assertTrue(s.startsWith("● ○ ○ ○ ○ ○"))
        assertTrue(s.contains("STAN"))
        val m = WskaznikPaneli.tekst(PanelZywy.Mieszanka)
        assertTrue(m.startsWith("○ ○ ● ○ ○ ○"))
        val p = WskaznikPaneli.tekst(PanelZywy.Powietrze)
        assertTrue(p.startsWith("○ ○ ○ ○ ○ ●"))
        assertTrue(p.contains("POWIETRZE"))
    }
}
