package pl.i40.android.transport

/**
 * Skrypt atrapy z żywego zapisu Hyundai i40 2.0 GDI (vLinker MC-IOS, 2026-08-08).
 *
 * Źródło: Diagnostyka → „Kopiuj zapis z auta” (linie `→` / `←`).
 * `015C` = NO DATA (ECU nie odpowiada; maska też nie zgłasza).
 */
object MockI40Script {
    fun make(): List<MockScriptEntry> {
        val out = mutableListOf<MockScriptEntry>()

        fun echo(cmd: String, response: String) {
            out.add(MockScriptEntry(command = cmd, response = response, recordedWithEcho = true))
        }

        fun plain(cmd: String, response: String) {
            out.add(MockScriptEntry(command = cmd, response = response, recordedWithEcho = false))
        }

        fun chunks(cmd: String, pieces: List<String>, recorded: Boolean = false) {
            out.add(
                MockScriptEntry(
                    command = cmd,
                    chunks = pieces.map { it.toByteArray(Charsets.UTF_8) },
                    recordedWithEcho = recorded,
                ),
            )
        }

        echo("ATZ", "ATZ\r\r\rELM327 v2.2\r\r>")
        echo("ATI", "ATI\rELM327 v2.2\r\r>")
        echo("AT@1", "AT@1\rOBDII to RS232 Interpreter\r\r>")
        echo("ATE0", "ATE0\rOK\r\r>")
        plain("ATL0", "OK\r\r>")
        plain("ATS0", "OK\r\r>")
        plain("ATH0", "OK\r\r>")
        plain("ATSP0", "OK\r\r>")
        plain("ATRV", "14.2V\r\r>")

        plain("ATDPN", "A0\r\r>")
        chunks("0100", listOf("SEARCHING...\r", "4100BE3EA813\r", "\r>"))
        plain("ATDPN", "A6\r\r>")

        plain("0120", "4120A007F011\r\r>")
        plain("0140", "4140FED00400\r\r>")

        plain("0902", "014\r0:4902014B4D48\r1:4C433431444146\r2:55303636353538\r\r>")
        plain("0904", "013\r0:490401474756\r1:462D4545354146\r2:53303136303000\r\r>")
        plain("090A", "017\r0:490A0145434D\r1:002D456E67696E\r2:65436F6E74726F\r3:6C000000000000\r\r>")

        plain("0101", "41010007E100\r\r>")
        plain("03", "4300\r\r>")
        plain("07", "4700\r\r>")
        plain("0A", "NO DATA\r\r>")

        // Drugie 0101 — odczyt PID-ów (katalog); ta sama odpowiedź.
        plain("0101", "41010007E100\r\r>")
        plain("0103", "41030200\r\r>")
        plain("0104", "41043C\r\r>")
        plain("0105", "41055C\r\r>")
        plain("0106", "41067F\r\r>")
        plain("0107", "410785\r\r>")
        plain("010B", "410B22\r\r>")
        plain("010C", "410C0E76\r\r>")
        plain("010D", "410D00\r\r>")
        plain("010E", "410E87\r\r>")
        plain("010F", "410F3F\r\r>")
        plain("0111", "411127\r\r>")
        plain("0113", "411303\r\r>")
        plain("011C", "411C06\r\r>")
        plain("011F", "411F0096\r\r>")
        plain("0121", "41210000\r\r>")
        plain("0123", "41230180\r\r>")
        plain("012E", "412E00\r\r>")
        plain("012F", "412F00\r\r>")
        plain("0130", "4130FF\r\r>")
        plain("0131", "4131FFFF\r\r>")
        plain("0133", "413363\r\r>")
        plain("013C", "413C1344\r\r>")
        plain("0141", "41410007E1E1\r\r>")
        plain("0142", "41423795\r\r>")
        plain("0143", "41430039\r\r>")
        plain("0144", "41448000\r\r>")
        plain("0145", "41450F\r\r>")
        plain("0146", "41463D\r\r>")
        plain("0147", "414726\r\r>")
        plain("0149", "414925\r\r>")
        plain("014A", "414A25\r\r>")
        plain("014C", "414C14\r\r>")
        // Pętla średnia — te same bajty co pojedyncze odpowiedzi z zapisu, jeden prefiks 41.
        plain("01233C0B114344", "412301803C13440B221127430039448000\r\r>")
        // Sonda — maska nie zgłasza; ECU odpowiada NO DATA.
        plain("015C", "NO DATA\r\r>")

        return out
    }
}
