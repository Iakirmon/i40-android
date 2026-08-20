package pl.i40.android.rules

enum class RodzajPasma {
    Norma,
    Fizyczny,
    Brak,
}

/**
 * Wpis z tabeli 8.8 warstwy diagnostycznej. Pasmo pochodzi z reguły albo ze źródła —
 * nigdy nie powstaje na potrzeby wyświetlania.
 */
data class WpisPasma(
    val id: String,
    val pid: Int?,
    val rodzaj: RodzajPasma,
    val min: Double?,
    val max: Double?,
    val jednostka: String,
    val uzasadnienie: String
)

/**
 * Jedno miejsce wszystkich progów i pasm. Reguły, kafle, linie na wykresach i raport
 * czytają stąd, nie z własnych kopii.
 */
object PasmaOdniesienia {
    /** Reguły `thermostat` (< 70) i `overheat` (> 105), tabela 10.4 bazowego. */
    val plyn = 70.0..105.0

    /** Reguła `oil_cold`, tabela 10.4 bazowego. */
    const val OLEJ_MIN_C = 90.0

    /** Reguły `alternator_low` i `overcharge`. */
    val napieciePraca = 13.0..15.0

    /** Reguła `battery_weak`: norma przy zgaszonym to > 12,4 V. */
    const val NAPIECIE_ZGASZONY_MIN_V = 12.4

    /** Reguły `ltft_lean` / `ltft_rich`. */
    val korektaDluga = -10.0..10.0

    /** Reguła `trim_sum`. */
    val sumaKorekt = -20.0..20.0

    /** Sekcja 4.1 — ciśnienie zadane na jałowym, GDI. */
    val szynaJalowy = 34.0..55.0

    /** Sekcja 4.1 — ciśnienie pod obciążeniem, GDI. */
    val szynaObciazenie = 138.0..241.0

    /**
     * Dopuszczalne odchylenie rzeczywistego od zadanego na jałowym, sekcja 4.1 (100 psi).
     */
    const val ODCHYLENIE_JALOWE_BAR = 7.0

    /**
     * Próg `GDI-1`: 34 bar (dolna granica zadanego na jałowym) − 7 bar (odchylenie) = 27 bar.
     */
    val progGdi1Bar: Double get() = szynaJalowy.start - ODCHYLENIE_JALOWE_BAR

    /** Sekcja 4.2 — normalny zakres pracy katalizatora. */
    val katalizatorPraca = 650.0..870.0

    /** Sekcja 4.2 — minimum skuteczne dla konwersji; próg `KAT-1`. */
    const val KATALIZATOR_ZAPLON_C = 300.0

    /** Górna granica normalnego zakresu; próg `KAT-2`. */
    val progKat2C: Double get() = katalizatorPraca.endInclusive

    /** Definicja mieszanki stechiometrycznej. */
    const val LAMBDA_STECHIOMETRIA = 1.0

    /** Próg „rozgrzany” z modelu oleju, §8.5 bazowego. Nie [OilTempEstimator.silnikRozgrzany]. */
    const val CZAS_ROZGRZANY_S = 600.0

    /** SAE J1979 PID `0123` w kPa; `41230180` → 38,4 bar. */
    const val KPA_NA_BAR = 100.0

    fun kpaNaBar(kpa: Double): Double = kpa / KPA_NA_BAR

    /**
     * `silnikRozgrzany` ≡ płyn `0105` ≥ 70 °C ∧ czas pracy `011F` ≥ 600 s.
     * Brak odczytu nie udaje rozgrzania.
     */
    fun silnikRozgrzany(plynC: Double?, runtimeS: Double?): Boolean {
        if (plynC == null || runtimeS == null) return false
        return plynC >= plyn.start && runtimeS >= CZAS_ROZGRZANY_S
    }

    val wpisy: List<WpisPasma> = listOf(
        WpisPasma("plyn", 0x05, RodzajPasma.Norma, plyn.start, plyn.endInclusive, "°C", "thermostat / overheat"),
        WpisPasma("olej", 0x5C, RodzajPasma.Norma, OLEJ_MIN_C, null, "°C", "oil_cold"),
        WpisPasma(
            "napiecie_praca",
            0x42,
            RodzajPasma.Norma,
            napieciePraca.start,
            napieciePraca.endInclusive,
            "V",
            "alternator_low / overcharge"
        ),
        WpisPasma(
            "napiecie_zgaszony",
            0x42,
            RodzajPasma.Norma,
            NAPIECIE_ZGASZONY_MIN_V,
            null,
            "V",
            "battery_weak"
        ),
        WpisPasma(
            "korekta_dluga",
            0x07,
            RodzajPasma.Norma,
            korektaDluga.start,
            korektaDluga.endInclusive,
            "%",
            "ltft_lean / ltft_rich"
        ),
        WpisPasma(
            "suma_korekt",
            null,
            RodzajPasma.Norma,
            sumaKorekt.start,
            sumaKorekt.endInclusive,
            "%",
            "trim_sum"
        ),
        WpisPasma(
            "szyna_jalowy",
            0x23,
            RodzajPasma.Norma,
            szynaJalowy.start,
            szynaJalowy.endInclusive,
            "bar",
            "sekcja 4.1"
        ),
        WpisPasma(
            "szyna_obciazenie",
            0x23,
            RodzajPasma.Norma,
            szynaObciazenie.start,
            szynaObciazenie.endInclusive,
            "bar",
            "sekcja 4.1"
        ),
        WpisPasma(
            "katalizator",
            0x3C,
            RodzajPasma.Norma,
            katalizatorPraca.start,
            katalizatorPraca.endInclusive,
            "°C",
            "sekcja 4.2, zapłon ${KATALIZATOR_ZAPLON_C.toInt()}"
        ),
        WpisPasma("lambda", 0x44, RodzajPasma.Norma, LAMBDA_STECHIOMETRIA, LAMBDA_STECHIOMETRIA, "", "stechiometria"),
        WpisPasma("stft", 0x06, RodzajPasma.Fizyczny, -100.0, 100.0, "%", "katalog; osobnej normy brak"),
        WpisPasma("obroty", 0x0C, RodzajPasma.Fizyczny, 0.0, 8000.0, "obr/min", "katalog"),
        WpisPasma("predkosc", 0x0D, RodzajPasma.Fizyczny, 0.0, 250.0, "km/h", "katalog"),
        WpisPasma("obciazenie_04", 0x04, RodzajPasma.Fizyczny, 0.0, 100.0, "%", "katalog"),
        WpisPasma("obciazenie_43", 0x43, RodzajPasma.Fizyczny, 0.0, 100.0, "%", "katalog"),
        WpisPasma("przepustnica", 0x11, RodzajPasma.Fizyczny, 0.0, 100.0, "%", "katalog"),
        WpisPasma(
            "zaplon",
            0x0E,
            RodzajPasma.Brak,
            null,
            null,
            "°",
            "Mapa zapłonu jest własnością sterownika, zależy od obciążenia, obrotów i paliwa."
        ),
        WpisPasma(
            "kolektor",
            0x0B,
            RodzajPasma.Brak,
            null,
            null,
            "kPa",
            "Zależy od obciążenia i wysokości n.p.m."
        ),
        WpisPasma("dolot", 0x0F, RodzajPasma.Brak, null, null, "°C", "Zależy od temperatury otoczenia."),
        WpisPasma("otoczenie", 0x46, RodzajPasma.Brak, null, null, "°C", "Z definicji bez normy."),
        WpisPasma("sonda_za_kat", 0x15, RodzajPasma.Brak, null, null, "", "PID bez formuły w katalogu, sekcja 3.2."),
        WpisPasma(
            "przedmuch",
            0x2E,
            RodzajPasma.Brak,
            null,
            null,
            "%",
            "Zadane przedmuchiwanie; tłumaczy skok korekty, osobnej normy brak."
        ),
        WpisPasma(
            "podcisnienie",
            null,
            RodzajPasma.Brak,
            null,
            null,
            "kPa",
            "Zależy od obciążenia i wysokości n.p.m.; brak źródła na wartość jałową."
        ),
        WpisPasma(
            "rozjazd",
            null,
            RodzajPasma.Brak,
            null,
            null,
            "pkt",
            "Brak źródła na dopuszczalną różnicę."
        ),
        WpisPasma("przepustnica_zadana", 0x4C, RodzajPasma.Fizyczny, 0.0, 100.0, "%", "katalog"),
        WpisPasma("pedal", 0x49, RodzajPasma.Fizyczny, 0.0, 100.0, "%", "katalog"),
        WpisPasma("atmosfera", 0x33, RodzajPasma.Fizyczny, 50.0, 110.0, "kPa", "katalog")
    )

    fun wpisyDlaPid(pid: Int): List<WpisPasma> = wpisy.filter { it.pid == pid }
}
