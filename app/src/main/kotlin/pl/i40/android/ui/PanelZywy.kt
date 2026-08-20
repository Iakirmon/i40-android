package pl.i40.android.ui

/** Panele ekranu żywego — Stan pierwszy, potem pięć z warstwy diagnostycznej. */
enum class PanelZywy(val etykieta: String) {
    Stan("STAN"),
    Podstawowy("PODSTAWOWY"),
    Mieszanka("MIESZANKA"),
    WtryskGdi("WTRYSK GDI"),
    Termika("TERMIKA"),
    Powietrze("POWIETRZE"),
}

/** Sześć kropek: pierwsza to panel Stan (S2). */
object WskaznikPaneli {
    const val LICZBA_KROPEK = 6

    fun tekst(aktywny: PanelZywy): String {
        val kropki = (0 until LICZBA_KROPEK).joinToString(" ") { i ->
            if (i == aktywny.ordinal) "●" else "○"
        }
        return "$kropki  ${aktywny.etykieta}"
    }
}
