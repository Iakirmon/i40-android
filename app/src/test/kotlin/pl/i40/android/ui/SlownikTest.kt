package pl.i40.android.ui

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import pl.i40.android.rules.PasmaOdniesienia

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SlownikTest {
    private lateinit var zrodlo: String
    private lateinit var hasla: List<HasloSlownika>

    @BeforeAll
    fun wczytaj() {
        zrodlo = File("docs/slownik.md").takeIf { it.exists() }?.readText()
            ?: File("../docs/slownik.md").readText()
        hasla = Slownik.parsuj(zrodlo)
    }

    @Test
    fun siedemdziesiatHasel() {
        assertEquals(70, hasla.size)
        assertEquals(27, hasla.count { it.rodzaj == RodzajHasla.Parametr && it.stopka.contains("PID") })
        assertEquals(33, hasla.count { it.rodzaj == RodzajHasla.Pojecie })
        assertEquals(10, hasla.count { it.rodzaj == RodzajHasla.Parametr && !it.stopka.contains("PID") })
    }

    @Test
    fun zadnejPustejRubryki() {
        for (h in hasla) {
            val oczekiwane = if (h.rodzaj == RodzajHasla.Pojecie) {
                Slownik.RUBRYKI_POJECIE
            } else {
                Slownik.RUBRYKI_PARAMETR
            }
            assertEquals(oczekiwane.toSet(), h.rubryki.keys, h.tytul)
            for ((k, v) in h.rubryki) {
                assertTrue(v.isNotBlank(), "${h.tytul} / $k")
            }
        }
    }

    @Test
    fun zgodnoscZZrodlemCoDoZdania() {
        val asset = File("app/src/main/assets/slownik.md").takeIf { it.exists() }?.readText()
            ?: File("src/main/assets/slownik.md").readText()
        assertEquals(zrodlo, asset)
        val ponownie = Slownik.parsuj(asset)
        assertEquals(hasla.map { it.id to it.rubryki }, ponownie.map { it.id to it.rubryki })
    }

    @Test
    fun odsylaczeProwadzaDoIstniejacych() {
        val ids = hasla.map { it.id }.toSet()
        for (h in hasla) {
            for (cel in Slownik.odsylacze(h.tekstRubryk())) {
                assertTrue(cel in ids, "${h.tytul} -> $cel")
            }
        }
    }

    @Test
    fun zadnaLiczbaZPasmWProziePozaListaZamknieta() {
        val progi = mutableSetOf<String>()
        for (w in PasmaOdniesienia.wpisy) {
            w.min?.let { progi.addAll(wariantyLiczby(it)) }
            w.max?.let { progi.addAll(wariantyLiczby(it)) }
        }
        progi.addAll(
            listOf(
                PasmaOdniesienia.OLEJ_MIN_C,
                PasmaOdniesienia.NAPIECIE_ZGASZONY_MIN_V,
                PasmaOdniesienia.KATALIZATOR_ZAPLON_C,
                PasmaOdniesienia.LAMBDA_STECHIOMETRIA,
                PasmaOdniesienia.OBROTY_PRACA_MIN,
                PasmaOdniesienia.CZAS_ROZGRZANY_S,
                PasmaOdniesienia.ODCHYLENIE_JALOWE_BAR,
                PasmaOdniesienia.progGdi1Bar,
                PasmaOdniesienia.progKat2C
            ).flatMap { wariantyLiczby(it) }
        )
        for (h in hasla) {
            val proza = Slownik.prozaRubryk(h)
            for (liczba in Slownik.liczbyWProzie(proza)) {
                val znormalizowana = liczba.replace(" ", "").replace('.', ',')
                val dozwolona = Slownik.LICZBY_DOZWOLONE.any { d ->
                    liczba == d || znormalizowana == d.replace(" ", "") || liczba.startsWith(d)
                }
                if (dozwolona) continue
                assertFalse(
                    liczba in progi || znormalizowana in progi,
                    "${h.tytul}: niedozwolona liczba z pasma „$liczba”"
                )
            }
        }
    }

    @Test
    fun nawigacjaPoTrzechPokazujeWrocDoPoczatku() {
        var stan = StanNawigacjiSlownika()
        stan = stan.otworz("a").otworz("b").otworz("c")
        assertTrue(stan.pokazWrocDoPoczatku)
        assertEquals(3, stan.glebokosc)
        val zablokowany = stan.otworz("d")
        assertEquals(listOf("a", "b", "c"), zablokowany.stos)
        assertEquals(listOf("a"), stan.doPoczatku().stos)
    }

    private fun wariantyLiczby(v: Double): Set<String> {
        val i = v.toInt()
        val out = mutableSetOf(v.toString(), i.toString())
        if (v == i.toDouble()) out.add(i.toString())
        val zPrzecinkiem = "%.1f".format(java.util.Locale.US, v).replace('.', ',')
        out.add(zPrzecinkiem)
        out.add(zPrzecinkiem.replace(",0", ""))
        return out
    }
}
