package pl.i40.android.obd

/** Katalog definicji PID-ów trybu 01 ze specyfikacji (SAE J1979). */
object PidCatalog {
    val all: List<PidDefinition> = listOf(
        wpis(0x01, "Stan monitorów", 4, "", decode = PidDecode::raw),
        wpis(0x03, "Status układu paliwowego", 2, "", decode = PidDecode::fuelSystem),
        wpis(0x04, "Obliczone obciążenie silnika", 1, "%", 0.0..100.0, PidDecode::percent),
        wpis(0x05, "Temperatura płynu chłodzącego", 1, "°C", -40.0..150.0, PidDecode::temp),
        wpis(0x06, "Korekta krótkoterminowa, bank 1", 1, "%", -100.0..100.0, PidDecode::trim),
        wpis(0x07, "Korekta długoterminowa, bank 1", 1, "%", -100.0..100.0, PidDecode::trim),
        wpis(0x0B, "Ciśnienie w kolektorze dolotowym", 1, "kPa", 0.0..255.0, PidDecode::u8),
        wpis(0x0C, "Obroty silnika", 2, "obr/min", 0.0..8000.0, PidDecode::rpm),
        wpis(0x0D, "Prędkość pojazdu", 1, "km/h", 0.0..250.0, PidDecode::u8),
        wpis(0x0E, "Wyprzedzenie zapłonu", 1, "° przed GMP", -64.0..64.0, PidDecode::timing),
        wpis(0x0F, "Temperatura powietrza dolotowego", 1, "°C", -40.0..100.0, PidDecode::temp),
        wpis(0x10, "Przepływ masowy powietrza", 2, "g/s", 0.0..100.0, PidDecode::maf),
        wpis(0x11, "Pozycja przepustnicy", 1, "%", 0.0..100.0, PidDecode::percent),
        wpis(0x13, "Które sondy tlenu są zamontowane", 1, "", decode = PidDecode::raw),
        wpis(0x1C, "Norma OBD, do której auto się stosuje", 1, "", decode = PidDecode::code),
        wpis(0x1F, "Czas pracy od uruchomienia", 2, "s", 0.0..65535.0, PidDecode::count16),
        wpis(0x21, "Przebieg z zapaloną kontrolką", 2, "km", 0.0..65535.0, PidDecode::count16),
        wpis(0x23, "Ciśnienie w szynie wysokiego ciśnienia", 2, "kPa", 0.0..655350.0, PidDecode::fuelRail),
        wpis(0x24, "Sonda tlenu 1", 4, "— / V", decode = PidDecode::oxygen),
        wpis(0x25, "Sonda tlenu 2", 4, "— / V", decode = PidDecode::oxygen),
        wpis(0x26, "Sonda tlenu 3", 4, "— / V", decode = PidDecode::oxygen),
        wpis(0x27, "Sonda tlenu 4", 4, "— / V", decode = PidDecode::oxygen),
        wpis(0x28, "Sonda tlenu 5", 4, "— / V", decode = PidDecode::oxygen),
        wpis(0x29, "Sonda tlenu 6", 4, "— / V", decode = PidDecode::oxygen),
        wpis(0x2A, "Sonda tlenu 7", 4, "— / V", decode = PidDecode::oxygen),
        wpis(0x2B, "Sonda tlenu 8", 4, "— / V", decode = PidDecode::oxygen),
        wpis(0x2C, "Zadana wartość EGR", 1, "%", 0.0..100.0, PidDecode::percent),
        wpis(0x2D, "Uchyb EGR", 1, "%", -100.0..100.0, PidDecode::trim),
        wpis(0x2E, "Zadane przedmuchiwanie zbiornika węglowego", 1, "%", 0.0..100.0, PidDecode::percent),
        wpis(0x2F, "Poziom paliwa w zbiorniku", 1, "%", 0.0..100.0, PidDecode::percent),
        wpis(0x30, "Rozgrzania od skasowania kodów", 1, "", 0.0..255.0, PidDecode::u8),
        wpis(0x31, "Przebieg od skasowania kodów", 2, "km", 0.0..65535.0, PidDecode::count16),
        wpis(0x33, "Ciśnienie atmosferyczne", 1, "kPa", 50.0..110.0, PidDecode::u8),
        wpis(0x3C, "Temperatura katalizatora 1", 2, "°C", -40.0..6513.5, PidDecode::catalyst),
        wpis(0x3D, "Temperatura katalizatora 2", 2, "°C", -40.0..6513.5, PidDecode::catalyst),
        wpis(0x3E, "Temperatura katalizatora 3", 2, "°C", -40.0..6513.5, PidDecode::catalyst),
        wpis(0x3F, "Temperatura katalizatora 4", 2, "°C", -40.0..6513.5, PidDecode::catalyst),
        wpis(0x41, "Gotowość monitorów w bieżącym cyklu", 4, "", decode = PidDecode::raw),
        wpis(0x42, "Napięcie sterownika", 2, "V", 0.0..20.0, PidDecode::volts),
        wpis(0x43, "Obciążenie absolutne", 2, "%", 0.0..100.0, PidDecode::loadAbs),
        wpis(0x44, "Zadany współczynnik lambda", 2, "", 0.0..2.0, PidDecode::lambda),
        wpis(0x45, "Względna pozycja przepustnicy", 1, "%", 0.0..100.0, PidDecode::percent),
        wpis(0x46, "Temperatura otoczenia", 1, "°C", -40.0..60.0, PidDecode::temp),
        wpis(0x47, "Bezwzględna pozycja przepustnicy B", 1, "%", 0.0..100.0, PidDecode::percent),
        wpis(0x48, "Bezwzględna pozycja przepustnicy C", 1, "%", 0.0..100.0, PidDecode::percent),
        wpis(0x49, "Pozycja pedału przyspieszenia D", 1, "%", 0.0..100.0, PidDecode::percent),
        wpis(0x4A, "Pozycja pedału przyspieszenia E", 1, "%", 0.0..100.0, PidDecode::percent),
        wpis(0x4B, "Pozycja pedału przyspieszenia F", 1, "%", 0.0..100.0, PidDecode::percent),
        wpis(0x4C, "Pozycja zadana przepustnicy", 1, "%", 0.0..100.0, PidDecode::percent),
        wpis(0x4D, "Czas pracy z zapaloną kontrolką", 2, "min", 0.0..65535.0, PidDecode::count16),
        wpis(0x4E, "Czas od skasowania kodów", 2, "min", 0.0..65535.0, PidDecode::count16),
        wpis(0x51, "Rodzaj paliwa", 1, "", decode = PidDecode::code),
        wpis(0x5A, "Względna pozycja pedału", 1, "%", 0.0..100.0, PidDecode::percent),
        wpis(0x5C, "Temperatura oleju silnikowego", 1, "°C", -40.0..160.0, PidDecode::temp),
        wpis(0x5D, "Kąt wtrysku paliwa", 2, "°", -210.0..301.992, PidDecode::injectionTiming),
        wpis(0x5E, "Chwilowe zużycie paliwa", 2, "l/h", 0.0..3276.75, PidDecode::fuelRate),
        wpis(0x61, "Moment żądany przez kierowcę", 1, "%", -125.0..130.0, PidDecode::torquePercent),
        wpis(0x62, "Moment faktycznie wydany", 1, "%", -125.0..130.0, PidDecode::torquePercent),
        wpis(0x63, "Moment odniesienia silnika", 2, "N·m", 0.0..65535.0, PidDecode::count16)
    )

    private val byId: Map<Int, PidDefinition> = all.associateBy { it.id }

    fun definition(forId: Int): PidDefinition? = byId[forId]
}

private fun wpis(
    id: Int,
    name: String,
    byteCount: Int,
    unit: String,
    range: ClosedFloatingPointRange<Double>? = null,
    decode: (List<Int>) -> DecodedPid
): PidDefinition = PidDefinition(
    id = id,
    name = name,
    byteCount = byteCount,
    unit = unit,
    range = range,
    decodeFn = decode
)
