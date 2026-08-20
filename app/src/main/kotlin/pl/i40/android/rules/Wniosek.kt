package pl.i40.android.rules

/** Waga wniosku z tabeli 10.4 — informacja, uwaga albo usterka. */
enum class WagaWniosku {
    Informacja,
    Uwaga,
    Usterka,
}

/** Pojedynczy wniosek: waga, tytuł i zdanie wyjaśniające po polsku. */
data class Wniosek(val ruleId: String, val waga: WagaWniosku, val tytul: String, val szczegol: String)
