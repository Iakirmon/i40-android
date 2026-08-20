package pl.i40.android.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.i40.android.storage.PodsumowaniePrzejazdu
import pl.i40.android.storage.Przejazd
import pl.i40.android.storage.StatusPrzejazdu
import pl.i40.android.storage.TrackBlob

class FormatPorownaniaTest {
    @Test
    fun nullDajeKreskeWWartosciIRoznicyNigdzieZero() {
        val a = przejazd("a", dystans = null, czasS = 600.0)
        val b = przejazd("b", dystans = 10.0, czasS = 600.0)
        val v = FormatPorownania.widok(a, b)!!
        val dystans = v.wiersz("Dystans")
        assertEquals(FormatPomiaru.NIEDOSTEPNE, dystans.ten)
        assertEquals(FormatPomiaru.NIEDOSTEPNE, dystans.roznica)
        val tekst = v.bloki.flatMap { it.wiersze }.joinToString(" ") { "${it.ten} ${it.tam} ${it.roznica}" }
        assertFalse(tekst.contains(Regex("(?<![0-9.,+−\\-])0(?![0-9])")))
    }

    @Test
    fun roznyVinBlokujePorownanie() {
        val a = przejazd("a", vin = "AAA")
        val b = przejazd("b", vin = "BBB")
        assertNull(FormatPorownania.widok(a, b))
        assertNull(FormatPorownania.poprzedni(a, listOf(a, b)))
    }

    @Test
    fun kodyNieMajaKolumnyRoznicy() {
        val a = przejazd("a", kodyKoniec = listOf("P0171"))
        val b = przejazd("b", kodyKoniec = listOf("P0300"))
        val v = FormatPorownania.widok(a, b)!!
        val kody = v.bloki.first { it.tytul == "KODY" }
        assertTrue(kody.wiersze.all { it.roznica == null })
    }

    @Test
    fun roznicaBezStrzalekISlowOceniajacych() {
        val a = przejazd("a", dystans = 23.4, czasS = 41 * 60.0, do90 = 400.0)
        val b = przejazd("b", dystans = 21.8, czasS = 39 * 60.0, do90 = 372.0)
        val v = FormatPorownania.widok(a, b)!!
        assertEquals("PRZEJAZD", v.bloki.first().tytul)
        val roznice = v.bloki.flatMap { it.wiersze }.mapNotNull { it.roznica }.joinToString(" ")
        for (z in listOf("▲", "▼", "lepiej", "gorzej", "pogorszenie", "poprawa")) {
            assertFalse(roznice.contains(z), z)
        }
        assertTrue(roznice.contains("+") || roznice.contains("−") || roznice.contains("-"))
    }

    @Test
    fun paliwoZawszeKreska() {
        val a = przejazd("a", paliwo = 12.0)
        val b = przejazd("b", paliwo = 11.0)
        val v = FormatPorownania.widok(a, b)!!
        val paliwo = v.bloki.flatMap { it.wiersze }.firstOrNull { it.etykieta.contains("aliwo", ignoreCase = true) }
        if (paliwo != null) {
            assertEquals(FormatPomiaru.NIEDOSTEPNE, paliwo.ten)
            assertEquals(FormatPomiaru.NIEDOSTEPNE, paliwo.tam)
        }
    }

    @Test
    fun poprzedniToChronologicznieWczesniejszyTenSamVin() {
        val a = przejazd("a", start = 3_000L, vin = "VIN")
        val b = przejazd("b", start = 2_000L, vin = "VIN")
        val c = przejazd("c", start = 1_000L, vin = "VIN")
        assertEquals("b", FormatPorownania.poprzedni(a, listOf(a, b, c))?.id)
    }

    private fun przejazd(
        id: String,
        start: Long = 1_000L,
        vin: String? = "VIN",
        dystans: Double? = 1.0,
        czasS: Double = 60.0,
        do90: Double? = null,
        paliwo: Double? = null,
        kodyKoniec: List<String> = emptyList()
    ) = Przejazd(
        id = id,
        poczatekMs = start,
        koniecMs = start + 1,
        status = StatusPrzejazdu.Zamkniety,
        vin = vin,
        notatka = "",
        podsumowanie = PodsumowaniePrzejazdu(
            czasTrwaniaS = czasS,
            dystansKm = dystans,
            paliwoL = paliwo,
            czasDo90CSekundy = do90,
            kodyNaKoncu = kodyKoniec
        ),
        przebieg = TrackBlob(),
        checkpointMs = start
    )
}
