package pl.i40.android.rules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.i40.android.service.FormatKaflaWykresow
import pl.i40.android.ui.FormatKafla

class PasmaOdniesieniaTest {
    @Test
    fun pasmaNormyZTabeli88() {
        val ids = PasmaOdniesienia.wpisy.filter { it.rodzaj == RodzajPasma.Norma }.map { it.id }.toSet()
        assertEquals(
            setOf(
                "plyn",
                "olej",
                "napiecie_praca",
                "napiecie_zgaszony",
                "korekta_dluga",
                "suma_korekt",
                "szyna_jalowy",
                "szyna_obciazenie",
                "katalizator",
                "lambda"
            ),
            ids
        )
        assertEquals(6, PasmaOdniesienia.wpisy.count { it.rodzaj == RodzajPasma.Brak })
    }

    @Test
    fun progGdi1ToTaSamaReferencjaCoPasmoJalowe() {
        assertEquals(
            PasmaOdniesienia.szynaJalowy.start - PasmaOdniesienia.ODCHYLENIE_JALOWE_BAR,
            PasmaOdniesienia.progGdi1Bar
        )
        assertEquals(27.0, PasmaOdniesienia.progGdi1Bar)
        assertEquals(PasmaOdniesienia.katalizatorPraca.endInclusive, PasmaOdniesienia.progKat2C)
        assertEquals(870.0, PasmaOdniesienia.progKat2C)
    }

    @Test
    fun kompletnoscPasmDlaWyswietlanychPid() {
        val wyswietlane = (
            FormatKafla.KAFLI_DOMYSLNE +
                FormatKaflaWykresow.PIDY_WYKRESOW +
                listOf(0x23, 0x43, 0x11, 0x3C, 0x06, 0x0B, 0x0F, 0x46, 0x15, 0x44, 0x0D, 0x2E)
            ).toSet()
        for (pid in wyswietlane) {
            assertTrue(
                PasmaOdniesienia.wpisyDlaPid(pid).isNotEmpty(),
                "brak wpisu PasmaOdniesienia dla PID ${"%02X".format(pid)}"
            )
        }
        assertNotNull(PasmaOdniesienia.wpisy.firstOrNull { it.id == "suma_korekt" })
    }

    @Test
    fun czteryBrakiNormyMajaUzasadnienie() {
        for (id in listOf("zaplon", "kolektor", "dolot", "otoczenie", "przedmuch")) {
            val wpis = PasmaOdniesienia.wpisy.first { it.id == id }
            assertEquals(RodzajPasma.Brak, wpis.rodzaj)
            assertTrue(wpis.uzasadnienie.isNotBlank())
        }
    }
}
