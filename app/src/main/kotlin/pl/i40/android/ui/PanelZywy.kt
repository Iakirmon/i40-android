package pl.i40.android.ui

/** Panele ekranu żywego — kolejność z warstwy diagnostycznej, Stan dojdzie w S1. */
enum class PanelZywy(val etykieta: String) {
    Podstawowy("PODSTAWOWY"),
    Mieszanka("MIESZANKA"),
    WtryskGdi("WTRYSK GDI"),
    Termika("TERMIKA"),
}
