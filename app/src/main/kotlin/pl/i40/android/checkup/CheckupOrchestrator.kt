package pl.i40.android.checkup

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import pl.i40.android.elm.ElmSession
import pl.i40.android.obd.DecodedPid
import pl.i40.android.obd.DtcDecode
import pl.i40.android.obd.Mode09
import pl.i40.android.obd.PidDefinition
import pl.i40.android.obd.Readiness
import pl.i40.android.obd.SupportedPids
import pl.i40.android.obd.VinInfo
import pl.i40.android.transport.Transport

/**
 * Przegląd na postoju — sekwencja z sekcji 8.8, przepisana z działającego kodu iOS
 * (rozbieżność 6: ATI, AT@1, dwa ATDPN). Timeout 25 s / 2 ponowienia (rozbieżność 5).
 *
 * Zamrożonej ramki (tryb 02) nie odpytuje.
 */
class CheckupOrchestrator(
    private val slownikDtc: Map<String, String>,
    private val terazMs: () -> Long = { System.currentTimeMillis() },
    private val timeout: Duration = TIMEOUT_PRZEGLADU,
    private val maxRetries: Int = PONOWIENIA_PRZEGLADU
) {
    suspend fun uruchom(
        transport: Transport,
        zrodlo: ZrodloRaportu,
        scope: CoroutineScope,
        rozlaczPo: Boolean = true,
        onStatus: (String) -> Unit = {},
        onTrace: (String) -> Unit = {},
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Raport {
        val session = ElmSession(transport, timeout = timeout, maxRetries = maxRetries)
        transport.open()
        session.start(scope)
        try {
            return skanuj(session, zrodlo, onStatus, onTrace, onProgress)
        } finally {
            session.stop()
            if (rozlaczPo) transport.close()
        }
    }

    private suspend fun skanuj(
        session: ElmSession,
        zrodlo: ZrodloRaportu,
        onStatus: (String) -> Unit,
        onTrace: (String) -> Unit,
        onProgress: (Int, Int) -> Unit
    ): Raport {
        suspend fun ask(command: String): String {
            onTrace("→ $command")
            val text = session.send(command)
            onTrace("← ${zwartyZapis(text)}")
            return text
        }

        var pojazd = MigawkaPojazdu()
        var adapter = MigawkaAdaptera()
        val start = terazMs()

        onStatus("Reset adaptera")
        val atz = ask("ATZ")
        adapter = adapter.copy(firmware = liniaZawierajaca("ELM", atz) ?: adapter.firmware)

        onStatus("Wersja firmware")
        val ati = ask("ATI")
        adapter = adapter.copy(firmware = liniaZawierajaca("ELM", ati) ?: adapter.firmware)

        onStatus("Opis adaptera")
        val at1 = ask("AT@1")
        adapter = adapter.copy(
            opis = linieDanych(at1).firstOrNull { linia ->
                val u = linia.uppercase()
                u != "OK" && u != "AT@1"
            }
        )

        val init = listOf(
            "ATE0" to "Echo wyłączone",
            "ATL0" to "Bez linefeedów",
            "ATS0" to "Bez spacji",
            "ATH0" to "Bez nagłówków CAN",
            "ATSP0" to "Automatyczny protokół"
        )
        for ((cmd, etykieta) in init) {
            onStatus(etykieta)
            ask(cmd)
        }

        onStatus("Napięcie pin 16")
        adapter = adapter.copy(napieciePin16 = parsujNapiecie(ask("ATRV")))

        // Pierwsze ATDPN zużywa stan sprzed negocjacji (w nagraniu: A0).
        onStatus("Protokół przed negocjacją")
        ask("ATDPN")

        onStatus("Negocjacja / maska 0100")
        val m100 = ask("0100")
        val supported = mutableSetOf<Int>()
        supported += SupportedPids.pids(fromHex = hexDane(m100))

        onStatus("Protokół po negocjacji")
        val dpn = ask("ATDPN")
        val kod = linieDanych(dpn).firstOrNull { it.matches(Regex("^[A-Z0-9]+$")) }
        adapter = adapter.copy(kodProtokolu = kod, nazwaProtokolu = nazwaProtokolu(kod))

        val maskFollowUps = listOf(0x20 to "0120", 0x40 to "0140", 0x60 to "0160")
        var previousMask = bajtyMaski(m100, 0x00)
        var firstPid = 0x01
        for ((responsePid, command) in maskFollowUps) {
            val prev = previousMask ?: break
            if (!SupportedPids.indicatesNextRange(prev, firstPid)) break
            onStatus("Maska $command")
            val text = ask(command)
            if (text.uppercase().contains("NO DATA")) {
                previousMask = null
                break
            }
            supported += SupportedPids.pids(fromHex = hexDane(text))
            previousMask = bajtyMaski(text, responsePid)
            firstPid = responsePid + 1
        }

        supported -= SupportedPids.bityKontynuacji
        val obslugiwane = supported.sorted()

        onStatus("VIN")
        val vin = Mode09.decodeVin(ask("0902"))
        if (vin != null) {
            val info = VinInfo(vin)
            pojazd = pojazd.copy(
                vin = info.vin,
                producent = info.manufacturer,
                rokModelu = info.modelYear,
                fabryka = info.plant
            )
        }

        onStatus("Kalibracja")
        pojazd = pojazd.copy(kalibracja = Mode09.decodeCalibrationId(ask("0904")))

        onStatus("Nazwa sterownika")
        pojazd = pojazd.copy(nazwaEcu = Mode09.decodeEcuName(ask("090A")))

        onStatus("MIL i monitory")
        val gotowosc = Readiness.decode(ask("0101"))

        onStatus("Kody zapisane")
        val kodyZapisane = DtcDecode.codes(ask("03"), slownikDtc)

        onStatus("Kody oczekujące")
        val kodyOczekujace = DtcDecode.codes(ask("07"), slownikDtc)

        onStatus("Kody trwałe")
        val tekstTrwale = ask("0A")
        val kodyTrwale = if (tekstTrwale.uppercase().contains("NO DATA")) {
            null
        } else {
            DtcDecode.codes(tekstTrwale, slownikDtc)
        }

        val wanted = SupportedPids.displayable(supported)
        onProgress(0, wanted.size)
        onStatus("Odczyt PID-ów")
        val odczyty = mutableListOf<MigawkaOdczytu>()
        for ((index, def) in wanted.withIndex()) {
            onProgress(index + 1, wanted.size)
            onStatus("Odczyt ${def.name}")
            odczyty += dekodujOdczyt(ask(def.command), def)
        }

        return Raport(
            startMs = start,
            koniecMs = terazMs(),
            zrodlo = zrodlo,
            pojazd = pojazd,
            adapter = adapter,
            obslugiwanePid = obslugiwane,
            gotowosc = gotowosc,
            kodyZapisane = kodyZapisane,
            kodyOczekujace = kodyOczekujace,
            kodyTrwale = kodyTrwale,
            odczyty = odczyty,
            wnioski = emptyList()
        ).odswiezWnioski()
    }

    companion object {
        /** Sekcja 10.5 — przegląd, nie atrapa i nie nagrywanie. */
        val TIMEOUT_PRZEGLADU = 25.seconds
        const val PONOWIENIA_PRZEGLADU = 2
    }
}

private fun zwartyZapis(text: String): String = text.replace("\r", "\\r").replace("\n", "\\n")

private fun dekodujOdczyt(text: String, definition: PidDefinition): MigawkaOdczytu {
    val niedostepny = MigawkaOdczytu(
        pid = definition.id,
        nazwa = definition.name,
        jednostka = definition.unit,
        wartosc = null,
        dostepny = false,
        podejrzany = false
    )
    if (text.uppercase().contains("NO DATA") || text.contains("?")) return niedostepny
    val payload = ladunekTrybu01(text, definition.id) ?: return niedostepny
    val decoded = definition.decode(payload) ?: return niedostepny
    val wartosc = when (decoded) {
        is DecodedPid.Numeric -> decoded.value
        is DecodedPid.Oxygen -> decoded.lambda
        is DecodedPid.Bytes -> decoded.data.firstOrNull()?.toDouble()
        is DecodedPid.Code -> decoded.value.toDouble()
    }
    return MigawkaOdczytu(
        pid = definition.id,
        nazwa = definition.name,
        jednostka = definition.unit,
        wartosc = wartosc,
        dostepny = true,
        podejrzany = definition.isSuspect(decoded)
    )
}

private fun ladunekTrybu01(text: String, pid: Int): List<Int>? {
    val bytes = hexNaBajty(hexDane(text)) ?: return null
    if (bytes.size < 3 || bytes[0] != 0x41 || bytes[1] != pid) return null
    return bytes.drop(2)
}

private fun bajtyMaski(text: String, responsePid: Int): List<Int>? {
    val bytes = hexNaBajty(hexDane(text)) ?: return null
    if (bytes.size < 6 || bytes[0] != 0x41 || bytes[1] != responsePid) return null
    return bytes.subList(2, 6)
}

/**
 * Składa linie, które są w całości heksem. Nie wyłuskuje cyfr z `SEARCHING...` —
 * `A` w tym słowie nie jest bajtem maski.
 */
internal fun hexDane(text: String): String = linieDanych(text)
    .map { it.replace(" ", "").uppercase() }
    .filter { it.isNotEmpty() && it.all { ch -> ch.jestHeks() } }
    .joinToString("")

internal fun linieDanych(text: String): List<String> =
    text.split(Regex("[\\r\\n]+")).map { it.trim() }.filter { it.isNotEmpty() }

private fun liniaZawierajaca(igla: String, text: String): String? =
    linieDanych(text).firstOrNull { it.uppercase().contains(igla.uppercase()) }

internal fun parsujNapiecie(text: String): Double? {
    val upper = text.uppercase()
    val vAt = upper.indexOf('V')
    if (vAt < 0) return null
    val before = text.substring(0, vAt).trim()
    val num = StringBuilder()
    for (ch in before.reversed()) {
        if (ch.isDigit() || ch == '.') {
            num.insert(0, ch)
        } else if (num.isNotEmpty()) {
            break
        }
    }
    return num.toString().toDoubleOrNull()
}

internal fun nazwaProtokolu(code: String?): String? {
    val raw = code?.trim()?.uppercase().orEmpty()
    if (raw.isEmpty()) return null
    val automatic = raw.startsWith("A")
    val digit = if (automatic) raw.drop(1) else raw
    val names = mapOf(
        "0" to "AUTOMATIC",
        "1" to "SAE J1850 PWM (41.6 kbit/s)",
        "2" to "SAE J1850 VPW (10.4 kbit/s)",
        "3" to "ISO 9141-2",
        "4" to "ISO 14230-4 KWP (5 baud)",
        "5" to "ISO 14230-4 KWP (fast)",
        "6" to "ISO 15765-4 CAN (11 bit, 500 kbit/s)",
        "7" to "ISO 15765-4 CAN (29 bit, 500 kbit/s)",
        "8" to "ISO 15765-4 CAN (11 bit, 250 kbit/s)",
        "9" to "ISO 15765-4 CAN (29 bit, 250 kbit/s)",
        "A" to "SAE J1939 CAN"
    )
    val base = names[digit] ?: "nieznany ($raw)"
    return if (automatic) "$base (automat)" else base
}

private fun hexNaBajty(hex: String): List<Int>? {
    if (hex.length < 2 || hex.length % 2 != 0) return null
    val out = ArrayList<Int>(hex.length / 2)
    var i = 0
    while (i < hex.length) {
        out.add(hex.substring(i, i + 2).toIntOrNull(16) ?: return null)
        i += 2
    }
    return out
}

private fun Char.jestHeks(): Boolean = this in '0'..'9' || this in 'A'..'F' || this in 'a'..'f'
