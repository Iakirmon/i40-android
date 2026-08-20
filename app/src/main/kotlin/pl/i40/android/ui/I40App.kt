package pl.i40.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.i40.android.service.BlokadaPredkosci
import pl.i40.android.service.InterakcjaZywa
import pl.i40.android.service.MigawkaZywego
import pl.i40.android.storage.Przejazd
import pl.i40.android.storage.PunktOdniesienia

private enum class Zakladka(val tytul: String) {
    Przeglad("Przegląd"),
    Nagrywanie("Nagrywanie"),
    Historia("Historia"),
}

@Composable
fun I40App(
    migawka: MigawkaZywego,
    onStop: () -> Unit,
    przejazdy: List<Przejazd> = emptyList(),
    onUsun: (String) -> Unit = {},
    onUsunWiele: (List<String>) -> Unit = {},
    onChroniony: (String, Boolean) -> Unit = { _, _ -> },
    punkty: List<PunktOdniesienia> = emptyList()
) {
    var zakladka by remember { mutableStateOf(Zakladka.Nagrywanie) }
    var nawigacjaSlownika by remember { mutableStateOf(StanNawigacjiSlownika()) }
    var ustawieniaWygladu by remember { mutableStateOf(StanUstawienWygladu()) }
    val motyw = ustawieniaWygladu.motyw
    val nawigacja = BlokadaPredkosci.pozwala(
        InterakcjaZywa.Nawigacja,
        wRuchu = migawka.wRuchu,
        nagrywa = migawka.nagrywa
    )

    fun otworzHaslo(id: String) {
        val otwarte = WejscieSlownika.otworz(id, migawka.wRuchu) ?: return
        nawigacjaSlownika = StanNawigacjiSlownika().otworz(otwarte)
    }

    fun otworzPid(pid: Int) {
        WejscieSlownika.idDlaPid(pid)?.let { otworzHaslo(it) }
    }

    I40Theme(motyw = motyw) {
        val kolory = LocalI40Kolory.current
        Box(Modifier.fillMaxSize().background(kolory.tlo)) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().background(kolory.powierzchniaPodniesiona)) {
                    for (z in Zakladka.entries) {
                        val aktywna = zakladka == z
                        BasicText(
                            text = z.tytul,
                            modifier = Modifier
                                .clickable(enabled = nawigacja || z == Zakladka.Nagrywanie) {
                                    zakladka = z
                                }
                                .padding(16.dp),
                            style = TextStyle(
                                color = when {
                                    aktywna -> kolory.akcent
                                    nawigacja || z == Zakladka.Nagrywanie -> kolory.tekst
                                    else -> kolory.tekstWyciszony
                                },
                                fontSize = 16.sp
                            )
                        )
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (w in WyborMotywu.entries) {
                        val aktywny = ustawieniaWygladu.wyborMotywu == w
                        BasicText(
                            text = when (w) {
                                WyborMotywu.Noc -> "NOC"
                                WyborMotywu.Dzien -> "DZIEŃ"
                                WyborMotywu.Automatycznie -> "AUTO"
                            },
                            modifier = Modifier
                                .clickable {
                                    ustawieniaWygladu = ustawieniaWygladu.zWyboorem(w)
                                }
                                .padding(8.dp),
                            style = TextStyle(
                                color = if (aktywny) kolory.akcent else kolory.tekstWyciszony,
                                fontSize = SkalaI40.ETYKIETA_SP.sp,
                                fontFamily = I40CzcionkaTekstu
                            )
                        )
                    }
                }
                when (zakladka) {
                    Zakladka.Przeglad -> CheckupScreen(
                        wRuchu = migawka.wRuchu,
                        onHaslo = { otworzHaslo(it) },
                        onPid = { otworzPid(it) },
                        modifier = Modifier.weight(1f)
                    )
                    Zakladka.Nagrywanie -> LiveScreen(
                        migawka = migawka,
                        onStop = onStop,
                        punkty = punkty,
                        onPid = { otworzPid(it) },
                        onHaslo = { otworzHaslo(it) },
                        modifier = Modifier.weight(1f)
                    )
                    Zakladka.Historia -> HistoryScreen(
                        przejazdy = przejazdy,
                        onUsun = onUsun,
                        onUsunWiele = onUsunWiele,
                        onChroniony = onChroniony,
                        punkty = punkty,
                        onPid = { otworzPid(it) },
                        onHaslo = { otworzHaslo(it) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            val idHasla = nawigacjaSlownika.aktualne
            val haslo = idHasla?.let { MagazynSlownika.poId(it) }
            if (haslo != null) {
                val pid = FormatBlokuSlownika.pidZeStopki(haslo.stopka)
                    ?: WejscieSlownika.PIDY_WYSWIETLANE.firstOrNull {
                        WejscieSlownika.idDlaPid(it) == haslo.id
                    }
                    ?: if (haslo.id == "temperatura-oleju-model") FormatKafla.PID_OLEJ_MODEL else null
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    ArkuszSlownika(
                        haslo = haslo,
                        nawigacja = nawigacjaSlownika,
                        teraz = terazDlaHasla(haslo.id, pid, migawka),
                        norma = FormatBlokuSlownika.normaDlaPid(pid),
                        poprzednio = FormatBlokuSlownika.poprzednio(pid, punkty),
                        onZamknij = { nawigacjaSlownika = StanNawigacjiSlownika() },
                        onOdsylacz = { cel ->
                            if (WejscieSlownika.moznaOtworzyc(migawka.wRuchu)) {
                                nawigacjaSlownika = nawigacjaSlownika.otworz(cel)
                            }
                        },
                        onWstecz = { nawigacjaSlownika = nawigacjaSlownika.wstecz() },
                        onDoPoczatku = { nawigacjaSlownika = nawigacjaSlownika.doPoczatku() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

private fun terazDlaHasla(id: String, pid: Int?, migawka: MigawkaZywego): String {
    if (id == "temperatura-oleju-model") return FormatKafla.olejTekst(migawka.olejC)
    if (id == "podcisnienie") {
        val a = migawka.wartosci[0x33]
        val k = migawka.wartosci[0x0B]
        if (a == null || k == null) return FormatPomiaru.NIEDOSTEPNE
        return FormatPomiaru.liczba(a - k, 0, "kPa")
    }
    if (id == "suma-korekt") {
        val s = migawka.wartosci[0x06]
        val l = migawka.wartosci[0x07]
        if (s == null || l == null) return FormatPomiaru.NIEDOSTEPNE
        return FormatPomiaru.liczba(s + l, 1, "%")
    }
    if (pid == null) return FormatPomiaru.NIEDOSTEPNE
    if (pid == 0x07) {
        return FormatKafla.wartoscKorektyDlugiej(migawka.wartosci[pid], migawka.wartosci[0x03]?.toInt())
    }
    if (pid == 0x23) {
        val bar = migawka.wartosci[pid]?.let { pl.i40.android.rules.PasmaOdniesienia.kpaNaBar(it) }
        return FormatPomiaru.liczba(bar, 1, "bar")
    }
    return FormatKafla.wartosc(pid, migawka.wartosci[pid])
}
