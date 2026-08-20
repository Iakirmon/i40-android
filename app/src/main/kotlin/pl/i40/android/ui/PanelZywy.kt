package pl.i40.android.ui

/** Panele ekranu żywego — kolejność z warstwy diagnostycznej, Stan dojdzie w S1. */
enum class PanelZywy(val etykieta: String) {
    Podstawowy("PODSTAWOWY"),
    Mieszanka("MIESZANKA"),
    WtryskGdi("WTRYSK GDI"),
    Termika("TERMIKA"),
}

/** Pięć kropek: piąta to miejsce na panel Powietrze (K4). */
object WskaznikPaneli {
    const val LICZBA_KROPEK = 5

    fun tekst(aktywny: PanelZywy): String {
        val kropki = (0 until LICZBA_KROPEK).joinToString(" ") { i ->
            if (i == aktywny.ordinal) "●" else "○"
        }
        return "$kropki  ${aktywny.etykieta}"
    }
}
