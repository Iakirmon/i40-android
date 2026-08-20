package pl.i40.android.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.i40.android.acquisition.RingSample

class CieniowanieMieszankiTest {
    @Test
    fun przedmuchPokrywaSieCzasowoZProbkamiPowyzejZera() {
        val purge = listOf(
            RingSample(0.0, 0.0),
            RingSample(2.0, 15.0),
            RingSample(5.0, 0.0),
            RingSample(8.0, 0.0)
        )
        val status = listOf(RingSample(0.0, 2.0))
        val pasma = CieniowanieMieszanki.pasma(purge, status, t0 = 0.0, t1 = 8.0)
        val b = pasma.filter { it.rodzaj == RodzajCienia.Przedmuchiwanie }
        assertEquals(1, b.size)
        assertEquals(2.0, b[0].start, 1e-9)
        assertEquals(5.0, b[0].end, 1e-9)
    }

    @Test
    fun petlaOtwartaTamGdzieStatusNieNalezyDo2Ani16() {
        val status = listOf(
            RingSample(0.0, 2.0),
            RingSample(3.0, 1.0),
            RingSample(7.0, 2.0)
        )
        val pasma = CieniowanieMieszanki.pasma(emptyList(), status, t0 = 0.0, t1 = 10.0)
        val o = pasma.filter { it.rodzaj == RodzajCienia.PetlaOtwarta }
        assertEquals(1, o.size)
        assertEquals(3.0, o[0].start, 1e-9)
        assertEquals(7.0, o[0].end, 1e-9)
    }

    @Test
    fun szesnascieToPetlaZamknietaNieCieniowanaJakoOtwarta() {
        val status = listOf(RingSample(0.0, 16.0), RingSample(5.0, 16.0))
        val pasma = CieniowanieMieszanki.pasma(emptyList(), status, t0 = 0.0, t1 = 5.0)
        assertTrue(pasma.none { it.rodzaj == RodzajCienia.PetlaOtwarta })
    }
}
