package pl.i40.android.ui

/**
 * Hasło słownika — treść wyłącznie z `docs/slownik.md` / assets.
 * Parametr: cztery rubryki. Pojęcie: trzy.
 */
data class HasloSlownika(
    val id: String,
    val tytul: String,
    val rodzaj: RodzajHasla,
    val stopka: String,
    val rubryki: Map<String, String>
) {
    fun tekstRubryk(): String = rubryki.values.joinToString("\n")
}

enum class RodzajHasla {
    Parametr,
    Pojecie,
}

data class StanNawigacjiSlownika(val stos: List<String> = emptyList()) {
    val aktualne: String? get() = stos.lastOrNull()
    val glebokosc: Int get() = stos.size

    fun otworz(id: String): StanNawigacjiSlownika {
        if (stos.isEmpty()) return copy(stos = listOf(id))
        if (stos.size >= 3) return this
        return copy(stos = stos + id)
    }

    fun wstecz(): StanNawigacjiSlownika {
        if (stos.isEmpty()) return this
        return copy(stos = stos.dropLast(1))
    }

    fun doPoczatku(): StanNawigacjiSlownika = if (stos.isEmpty()) this else copy(stos = listOf(stos.first()))

    val pokazWrocDoPoczatku: Boolean get() = stos.size >= 3
}

/**
 * Parser markdownu słownika — §7–8 warstwy objaśnień.
 * Nie generuje treści; czyta gotowy dokument źródłowy.
 */
object Slownik {
    val RUBRYKI_PARAMETR = listOf(
        "CO TO JEST",
        "PO CO NA TO PATRZEĆ",
        "GDY WYJDZIE POZA PASMO",
        "CZEGO TO NIE MÓWI"
    )
    val RUBRYKI_POJECIE = listOf(
        "CO TO JEST",
        "PO CO CI TO WIEDZIEĆ",
        "CZEGO TO NIE MÓWI"
    )

    /** Liczby dozwolone w prozie — lista zamknięta z nagłówka `docs/slownik.md`. */
    val LICZBY_DOZWOLONE = setOf(
        "2,3",
        "1 013",
        "1013",
        "0,25",
        "2",
        "14,7",
        "1",
        "4",
        "90"
    )

    fun idZTytulu(tytul: String): String {
        val mapa = mapOf(
            'ą' to "a", 'ć' to "c", 'ę' to "e", 'ł' to "l", 'ń' to "n",
            'ó' to "o", 'ś' to "s", 'ź' to "z", 'ż' to "z",
            'Ą' to "a", 'Ć' to "c", 'Ę' to "e", 'Ł' to "l", 'Ń' to "n",
            'Ó' to "o", 'Ś' to "s", 'Ź' to "z", 'Ż' to "z"
        )
        val sb = StringBuilder()
        for (ch in tytul) {
            val z = mapa[ch] ?: ch.lowercaseChar().toString()
            when {
                z.length == 1 && z[0].isLetterOrDigit() -> sb.append(z)
                z == " " || z == "-" || z == "—" -> sb.append('-')
                z == "\"" || z == "„" || z == "”" || z == "(" || z == ")" -> {}
                else -> {}
            }
        }
        return sb.toString().replace(Regex("-+"), "-").trim('-')
    }

    fun parsuj(markdown: String): List<HasloSlownika> {
        val linie = markdown.lines()
        val start = linie.indexOfFirst { it.startsWith("# CZĘŚĆ A") }
        require(start >= 0) { "Brak CZĘŚĆ A w słowniku" }
        val out = mutableListOf<HasloSlownika>()
        var i = start
        var czesc = 'A'
        while (i < linie.size) {
            val linia = linie[i]
            when {
                linia.startsWith("# CZĘŚĆ B") -> czesc = 'B'
                linia.startsWith("# CZĘŚĆ C") -> czesc = 'C'
                linia.startsWith("## ") && !linia.startsWith("### ") -> {
                    val tytul = linia.removePrefix("## ").trim()
                    if (tytul != "Jak pisane są hasła" && tytul != "Liczby dozwolone w treści") {
                        val (haslo, next) = parsujHaslo(linie, i, tytul, czesc)
                        out.add(haslo)
                        i = next
                        continue
                    }
                }
            }
            i += 1
        }
        return out
    }

    fun odsylacze(tekst: String): List<String> {
        val re = Regex("""\[\[([^\]|]+)(?:\|[^\]]+)?\]\]""")
        return re.findAll(tekst).map { idZTytulu(it.groupValues[1].trim()) }.toList()
    }

    /**
     * Proza rubryk bez stopek, bez `kodu` w apostrofach odwrotnych i bez nazw własnych
     * typu ELM327 — do testu liczb z pasm.
     */
    fun prozaRubryk(haslo: HasloSlownika): String {
        var t = haslo.tekstRubryk()
        t = t.replace(Regex("`[^`]+`"), " ")
        t = t.replace(Regex("""\[\[[^\]]+\]\]"""), " ")
        t = t.replace(Regex("""ELM327""", RegexOption.IGNORE_CASE), " ")
        return t
    }

    fun liczbyWProzie(proza: String): List<String> {
        val re = Regex("""\d+(?:[ \u00a0]\d{3})*(?:[,.]\d+)?""")
        return re.findAll(proza).map { it.value.replace('\u00a0', ' ') }.toList()
    }

    private fun parsujHaslo(linie: List<String>, startIdx: Int, tytul: String, czesc: Char): Pair<HasloSlownika, Int> {
        val rubrykiOczekiwane = if (czesc == 'C') RUBRYKI_POJECIE else RUBRYKI_PARAMETR
        val rodzaj = if (czesc == 'C') RodzajHasla.Pojecie else RodzajHasla.Parametr
        var i = startIdx + 1
        val stopkaBuf = StringBuilder()
        while (i < linie.size) {
            val l = linie[i]
            if (l.startsWith("## ") || l.startsWith("# CZĘŚĆ")) break
            if (l.startsWith("**CO TO JEST**")) break
            if (l.startsWith("**") && !jestRubryka(l)) {
                val fragment = l.removePrefix("**").removeSuffix("**").trim()
                if (stopkaBuf.isNotEmpty()) stopkaBuf.append(' ')
                stopkaBuf.append(fragment)
            }
            i += 1
        }
        val rubryki = linkedMapOf<String, String>()
        var biezaca: String? = null
        val buf = StringBuilder()
        fun zakoncz() {
            val klucz = biezaca ?: return
            rubryki[klucz] = buf.toString().trim()
            buf.clear()
        }
        while (i < linie.size) {
            val l = linie[i]
            if (l.startsWith("## ") || l.startsWith("# CZĘŚĆ")) break
            val nag = rubrykiOczekiwane.firstOrNull { l.startsWith("**$it**") }
            if (nag != null) {
                zakoncz()
                biezaca = nag
                val reszta = l.removePrefix("**$nag**").trim()
                if (reszta.isNotEmpty()) buf.append(reszta)
            } else if (biezaca != null) {
                if (l.trim() == "---") {
                    // koniec bloku hasła
                } else if (l.isNotBlank() || buf.isNotEmpty()) {
                    if (buf.isNotEmpty()) buf.append('\n')
                    buf.append(l)
                }
            }
            i += 1
        }
        zakoncz()
        return HasloSlownika(
            id = idZTytulu(tytul),
            tytul = tytul.trim('"').trim('„').trim('”'),
            rodzaj = rodzaj,
            stopka = stopkaBuf.toString().trim(),
            rubryki = rubryki
        ) to i
    }

    private fun jestRubryka(linia: String): Boolean {
        val wszystkie = RUBRYKI_PARAMETR + RUBRYKI_POJECIE
        return wszystkie.any { linia.startsWith("**$it**") }
    }
}
