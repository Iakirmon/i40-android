package pl.i40.android.ui

/**
 * Mapowanie wejść UI → hasła słownika oraz blokada w ruchu — §7.1 / §8.1 warstwy objaśnień.
 *
 * Przy `010D` > 0 słownik się nie otwiera. Przełączanie paneli pozostaje dozwolone.
 */
object WejscieSlownika {
    /**
     * 32 PID-y z §8.2 — przecięcie katalogu i maski, bez `012F` (poprawka P1).
     * Model oleju `5C` jest osobno (wyliczany), nie w tej liście.
     */
    val PIDY_WYSWIETLANE: List<Int> = listOf(
        0x01, 0x03, 0x04, 0x05, 0x06, 0x07, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x11,
        0x13, 0x1C, 0x1F, 0x21, 0x23, 0x2E, 0x30, 0x31, 0x33, 0x3C,
        0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x49, 0x4A, 0x4C
    )

    private val PID_NA_ID: Map<Int, String> = mapOf(
        0x01 to "stan-monitorow",
        0x03 to "status-ukladu-paliwowego",
        0x04 to "obliczone-obciazenie-silnika",
        0x05 to "temperatura-plynu-chlodzacego",
        0x06 to "korekta-krotkoterminowa",
        0x07 to "korekta-dlugoterminowa",
        0x0B to "cisnienie-w-kolektorze-dolotowym",
        0x0C to "obroty-silnika",
        0x0D to "predkosc-pojazdu",
        0x0E to "wyprzedzenie-zaplonu",
        0x0F to "temperatura-powietrza-dolotowego",
        0x11 to "pozycje-przepustnicy",
        0x13 to "zamontowane-sondy-tlenu",
        0x15 to "sonda-lambda",
        0x1C to "norma-obd",
        0x1F to "czas-pracy-od-uruchomienia",
        0x21 to "przebieg-z-zapalona-kontrolka",
        0x23 to "cisnienie-w-szynie-wysokiego-cisnienia",
        0x2E to "zadane-przedmuchiwanie-zbiornika",
        0x30 to "rozgrzania-od-skasowania-kodow",
        0x31 to "przebieg-od-skasowania-kodow",
        0x33 to "cisnienie-atmosferyczne",
        0x3C to "temperatura-katalizatora",
        0x41 to "stan-monitorow",
        0x42 to "napiecie-sterownika",
        0x43 to "obciazenie-absolutne",
        0x44 to "zadany-wspolczynnik-lambda",
        0x45 to "pozycje-przepustnicy",
        0x46 to "temperatura-otoczenia",
        0x47 to "pozycje-przepustnicy",
        0x49 to "pozycja-pedalu",
        0x4A to "pozycja-pedalu",
        0x4C to "pozycje-przepustnicy",
        FormatKafla.PID_OLEJ_MODEL to "temperatura-oleju-model",
        FormatPowietrza.PID_PODCISNIENIE to "podcisnienie",
        FormatRaportu.PID_SUMA_KOREKT to "suma-korekt"
    )

    private val WYLICZONE_NA_ID: Map<String, String> = mapOf(
        "podcisnienie" to "podcisnienie",
        "suma-korekt" to "suma-korekt",
        "rozjazd-przepustnicy" to "rozjazd-przepustnicy",
        "dystans" to "dystans",
        "srednia-predkosc" to "srednia-predkosc",
        "czas-do-90-c" to "czas-do-90-c",
        "czas-poza-pasmem-w-petli-zamknietej" to "czas-poza-pasmem-w-petli-zamknietej",
        "mediana-korekty-dlugiej" to "mediana-korekty-dlugiej",
        "maksymalne-cisnienie-szyny-i-obciazenie-przy-nim" to
            "maksymalne-cisnienie-szyny-i-obciazenie-przy-nim"
    )

    fun moznaOtworzyc(wRuchu: Boolean): Boolean = !wRuchu

    fun otworz(id: String, wRuchu: Boolean): String? = if (moznaOtworzyc(wRuchu)) id else null

    fun idDlaPid(pid: Int): String? = PID_NA_ID[pid]

    fun idDlaWyliczonego(klucz: String): String? = WYLICZONE_NA_ID[klucz]

    fun idDlaWiersza(wiersz: WierszPrzegladu): String? {
        if (wiersz.wyliczony) return idDlaWyliczonego("podcisnienie")
        val pid = wiersz.pid ?: return null
        return idDlaPid(pid)
    }
}
