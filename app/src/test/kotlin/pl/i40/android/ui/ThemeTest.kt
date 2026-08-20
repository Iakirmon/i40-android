package pl.i40.android.ui

import androidx.compose.ui.graphics.Color
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ThemeTest {
    @Test
    fun obaMotywyPrzechodzaKontrastTekstu45() {
        for (paleta in listOf(I40Motywy.noc, I40Motywy.dzien)) {
            assertTrue(
                KontrastWcag.stosunek(paleta.odczyt, paleta.tloGlebia) >= 4.5,
                "odczyt/tło-głębia ${KontrastWcag.stosunek(paleta.odczyt, paleta.tloGlebia)}"
            )
            assertTrue(
                KontrastWcag.stosunek(paleta.odczyt, paleta.pole) >= 4.5,
                "odczyt/pole ${KontrastWcag.stosunek(paleta.odczyt, paleta.pole)}"
            )
            assertTrue(
                KontrastWcag.stosunek(paleta.przygasle, paleta.tloGlebia) >= 4.5,
                "przygasłe/tło ${KontrastWcag.stosunek(paleta.przygasle, paleta.tloGlebia)}"
            )
        }
    }

    @Test
    fun tokenyNocIDzienZTabeli32i33() {
        assertEquals(Color(0xFF0E1114), I40Motywy.noc.tloGlebia)
        assertEquals(Color(0xFF171B1F), I40Motywy.noc.pole)
        assertEquals(Color(0xFF2A3138), I40Motywy.noc.siatka)
        assertEquals(Color(0xFFE8E4DC), I40Motywy.noc.odczyt)
        assertEquals(Color(0xFF8A9299), I40Motywy.noc.przygasle)
        assertEquals(Color(0xFF7FA8B8), I40Motywy.noc.model)
        assertEquals(Color(0xFFE0A030), I40Motywy.noc.uwaga)
        assertEquals(Color(0xFFD9433A), I40Motywy.noc.usterka)
        assertEquals(Color(0xFFECE7DF), I40Motywy.dzien.tloGlebia)
        assertEquals(Color(0xFFF7F4EF), I40Motywy.dzien.pole)
        assertEquals(Color(0xFFC3BCB0), I40Motywy.dzien.siatka)
        assertEquals(Color(0xFF16181A), I40Motywy.dzien.odczyt)
        assertEquals(Color(0xFF5E6468), I40Motywy.dzien.przygasle)
        assertEquals(Color(0xFF2A5A6B), I40Motywy.dzien.model)
        assertEquals(Color(0xFFA85E00), I40Motywy.dzien.uwaga)
        assertEquals(Color(0xFFB3261E), I40Motywy.dzien.usterka)
    }

    @Test
    fun zadenLiteralKoloruPozaThemeKt() {
        val root = File("src/main/kotlin")
        val naruszenia = mutableListOf<String>()
        root.walkTopDown().filter { it.extension == "kt" }.forEach { plik ->
            if (plik.name == "Theme.kt") return@forEach
            val tekst = plik.readText()
            val re = Regex("""\bColor\s*\(|0xFF[0-9A-Fa-f]{6}""")
            for (m in re.findAll(tekst)) {
                naruszenia += "${plik.relativeTo(root)}:${m.value}"
            }
        }
        assertTrue(naruszenia.isEmpty(), naruszenia.joinToString("\n"))
    }

    @Test
    fun domyslnyMotywToNoc() {
        assertEquals(I40Motywy.noc, I40Motywy.dla(MotywI40.Noc))
        assertEquals(MotywI40.Noc, MotywI40.entries.first())
    }
}
