package pl.i40.android.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt
import pl.i40.android.storage.Przejazd
import pl.i40.android.storage.PunktOdniesienia
import pl.i40.android.storage.StatusPrzejazdu

@Composable
fun HistoryScreen(
    przejazdy: List<Przejazd>,
    onUsun: (String) -> Unit,
    onUsunWiele: (List<String>) -> Unit = {},
    onChroniony: (String, Boolean) -> Unit = { _, _ -> },
    punkty: List<PunktOdniesienia> = emptyList(),
    onPid: (Int) -> Unit = {},
    onHaslo: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val kolory = LocalI40Kolory.current
    val cal = remember { SiatkaMiesiaca.kalendarzPolski() }
    var miesiac by remember { mutableLongStateOf(SiatkaMiesiaca.poczatekMiesiaca(System.currentTimeMillis(), cal)) }
    var dzien by remember { mutableStateOf<Long?>(null) }
    var wybrany by remember { mutableStateOf<Przejazd?>(null) }
    var doPotwierdzenia by remember { mutableStateOf<Przejazd?>(null) }
    var doPotwierdzeniaWiele by remember { mutableStateOf<List<Przejazd>?>(null) }
    var tryb by remember { mutableStateOf(TrybZaznaczania()) }
    var porzadki by remember { mutableStateOf(false) }
    var porownanie by remember { mutableStateOf<Przejazd?>(null) }
    var odPoczatku by remember { mutableStateOf(false) }
    var filtr by remember { mutableStateOf(FiltrHistorii()) }

    val potwierdzany = doPotwierdzenia
    if (potwierdzany != null) {
        OknoPotwierdzeniaUsuniecia(
            tekst = FormatPotwierdzenia.pojedynczy(
                potwierdzany,
                liczbaPunktowPrzejazdu(potwierdzany, punkty),
                FormatPotwierdzenia.rozmiarBajtow(potwierdzany),
                cal
            ),
            onAnuluj = { doPotwierdzenia = null },
            onUsun = {
                onUsun(potwierdzany.id)
                doPotwierdzenia = null
                wybrany = null
            },
            modifier = modifier
        )
        return
    }

    val wiele = doPotwierdzeniaWiele
    if (wiele != null) {
        OknoPotwierdzeniaUsuniecia(
            tekst = FormatPotwierdzenia.wielokrotne(
                wiele,
                wiele.sumOf { liczbaPunktowPrzejazdu(it, punkty) },
                FormatPotwierdzenia.rozmiarBajtow(wiele),
                cal
            ),
            onAnuluj = { doPotwierdzeniaWiele = null },
            onUsun = {
                onUsunWiele(wiele.map { it.id })
                doPotwierdzeniaWiele = null
                tryb = tryb.zakoncz()
            },
            modifier = modifier
        )
        return
    }

    if (porzadki) {
        PorzadkiScreen(
            przejazdy = przejazdy,
            onWstecz = { porzadki = false },
            onUsun = { ids ->
                onUsunWiele(ids)
                porzadki = false
            },
            modifier = modifier
        )
        return
    }

    val porownywany = porownanie?.let { id -> przejazdy.firstOrNull { it.id == id.id } }
    if (porownywany != null) {
        PorownanieScreen(
            ten = porownywany,
            kandydaci = przejazdy,
            onWstecz = { porownanie = null },
            modifier = modifier
        )
        return
    }

    val sesja = wybrany?.let { id -> przejazdy.firstOrNull { it.id == id.id } }
    if (sesja != null) {
        SessionDetailScreen(
            przejazd = sesja,
            onWstecz = { wybrany = null },
            onUsun = { doPotwierdzenia = sesja },
            onChroniony = { onChroniony(sesja.id, it) },
            onPorownaj = { porownanie = sesja },
            onPid = onPid,
            onHaslo = onHaslo,
            modifier = modifier
        )
        return
    }

    val widoczne = filtr.zastosuj(przejazdy)
    val dniSesji = widoczne.map { SiatkaMiesiaca.poczatekDnia(it.poczatekMs, cal) }.toSet()
    val cells = SiatkaMiesiaca.komorki(miesiac, dniSesji, cal)
    val dniTygodnia = SiatkaMiesiaca.skrotyDni(cal)
    val listaDnia = dzien?.let { SiatkaMiesiaca.sesjeDnia(it, widoczne, cal) }.orEmpty()

    Column(modifier.fillMaxSize().background(kolory.tlo).padding(12.dp).verticalScroll(rememberScrollState())) {
        if (tryb.aktywny) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    "✕",
                    modifier = Modifier.clickable { tryb = tryb.zakoncz() }.padding(12.dp),
                    style = TextStyle(color = kolory.tekst, fontSize = 18.sp)
                )
                BasicText(
                    "Zaznaczono ${tryb.liczba}",
                    modifier = Modifier.weight(1f),
                    style = TextStyle(color = kolory.tekst, fontSize = 16.sp)
                )
                BasicText(
                    "Zaznacz dzień",
                    modifier = Modifier.clickable { tryb = tryb.zaznaczDzien(listaDnia) }.padding(8.dp),
                    style = TextStyle(color = kolory.akcent, fontSize = 14.sp)
                )
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    "<",
                    modifier = Modifier.clickable {
                        miesiac = SiatkaMiesiaca.przesunMiesiac(miesiac, -1, cal)
                        dzien = null
                    }.padding(12.dp),
                    style = TextStyle(color = kolory.tekst, fontSize = 20.sp)
                )
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    BasicText(
                        SiatkaMiesiaca.tytulMiesiaca(miesiac, cal),
                        style = TextStyle(color = kolory.tekst, fontSize = 18.sp)
                    )
                }
                BasicText(
                    ">",
                    modifier = Modifier.clickable {
                        miesiac = SiatkaMiesiaca.przesunMiesiac(miesiac, 1, cal)
                        dzien = null
                    }.padding(12.dp),
                    style = TextStyle(color = kolory.tekst, fontSize = 20.sp)
                )
                val znacznik = if (odPoczatku) "od początku ✓" else "od początku"
                BasicText(
                    znacznik,
                    modifier = Modifier.clickable { odPoczatku = !odPoczatku }.padding(8.dp),
                    style = TextStyle(color = if (odPoczatku) kolory.akcent else kolory.tekstDrugi, fontSize = 12.sp)
                )
            }
        }
        if (odPoczatku) {
            val kartaCalej = FormatKartyOdPoczatku.zPrzejazdow(przejazdy, cal)
            BasicText("OD POCZĄTKU", style = TextStyle(color = kolory.akcent, fontSize = 13.sp))
            for (w in kartaCalej.wiersze) {
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    BasicText(
                        w.etykieta,
                        modifier = Modifier.weight(1.4f),
                        style = TextStyle(color = kolory.tekstDrugi, fontSize = 12.sp)
                    )
                    BasicText(
                        w.wartosc,
                        modifier = Modifier.weight(1f),
                        style = TextStyle(color = kolory.tekst, fontSize = 12.sp, fontFamily = I40CzcionkaWartosci)
                    )
                }
            }
            BasicText(
                "Porządki",
                modifier = Modifier.clickable { porzadki = true }.padding(8.dp),
                style = TextStyle(color = kolory.akcent, fontSize = 14.sp)
            )
        } else {
            val karta = FormatKartyMiesiaca.zPrzejazdow(przejazdy, miesiac, cal)
            KartaMiesiacaUi(karta)
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            ZnacznikFiltra("z kodami", filtr.zKodami) { filtr = filtr.copy(zKodami = !filtr.zKodami) }
            ZnacznikFiltra("przerwane", filtr.przerwane) { filtr = filtr.copy(przerwane = !filtr.przerwane) }
            ZnacznikFiltra("chronione", filtr.chronione) { filtr = filtr.copy(chronione = !filtr.chronione) }
        }
        Row(Modifier.fillMaxWidth()) {
            for (s in dniTygodnia) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    BasicText(s.uppercase(), style = TextStyle(color = kolory.tekstWyciszony, fontSize = 11.sp))
                }
            }
        }
        cells.chunked(7).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                for (cell in row) {
                    Box(Modifier.weight(1f).height(44.dp), contentAlignment = Alignment.Center) {
                        when (cell) {
                            is KomorkaMiesiaca.Pusta -> {}
                            is KomorkaMiesiaca.Dzien -> {
                                val selected = dzien?.let { SiatkaMiesiaca.tenSamDzien(it, cell.dzienMs, cal) } == true
                                val c2 = cal.clone() as Calendar
                                c2.timeInMillis = cell.dzienMs
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable {
                                        dzien = if (selected) null else cell.dzienMs
                                    }
                                ) {
                                    BasicText(
                                        text = "${c2.get(Calendar.DAY_OF_MONTH)}",
                                        style = TextStyle(
                                            color = if (selected) kolory.tlo else kolory.tekst,
                                            fontSize = 14.sp
                                        )
                                    )
                                    Box(
                                        Modifier.size(5.dp).background(if (cell.maSesje) kolory.akcent else kolory.tlo)
                                    )
                                }
                            }
                        }
                    }
                }
                repeat(7 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
        if (filtr.aktywny && widoczne.isEmpty()) {
            BasicText(FiltrHistorii.KOMUNIKAT_PUSTY, style = TextStyle(color = kolory.tekstWyciszony, fontSize = 13.sp))
        } else if (dzien == null) {
            BasicText(
                "Dotknij dnia z kropką, żeby zobaczyć przejazdy.",
                style = TextStyle(color = kolory.tekstWyciszony, fontSize = 13.sp)
            )
        } else if (listaDnia.isEmpty()) {
            val msg = if (filtr.aktywny) FiltrHistorii.KOMUNIKAT_PUSTY else "Brak przejazdów tego dnia."
            BasicText(msg, style = TextStyle(color = kolory.tekstWyciszony, fontSize = 13.sp))
        } else {
            for (p in listaDnia) {
                WierszPrzejazdu(
                    p = p,
                    cal = cal,
                    tryb = tryb,
                    onClick = {
                        if (tryb.aktywny) {
                            tryb = tryb.przelacz(p)
                        } else {
                            wybrany = p
                        }
                    },
                    onPrzytrzymanie = { tryb = tryb.poPrzytrzymaniu(p) },
                    onGestUsuniecia = {
                        if (!tryb.aktywny && p.status != StatusPrzejazdu.WToku) doPotwierdzenia = p
                    }
                )
            }
            if (tryb.aktywny) {
                val n = tryb.liczba
                BasicText(
                    "Usuń zaznaczone ($n)",
                    modifier = Modifier
                        .clickable(enabled = n > 0) {
                            val wybrane = listaDnia.filter { it.id in tryb.zaznaczone }
                            if (wybrane.isNotEmpty()) doPotwierdzeniaWiele = wybrane
                        }
                        .padding(16.dp),
                    style = TextStyle(
                        color = if (n > 0) kolory.uwaga else kolory.tekstWyciszony,
                        fontSize = 16.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun KartaMiesiacaUi(karta: KartaMiesiaca) {
    val kolory = LocalI40Kolory.current
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        for (w in karta.wiersze) {
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                BasicText(
                    w.etykieta,
                    modifier = Modifier.weight(1.4f),
                    style = TextStyle(color = kolory.tekstDrugi, fontSize = 12.sp)
                )
                BasicText(
                    w.wartosc,
                    modifier = Modifier.weight(1f),
                    style = TextStyle(color = kolory.tekst, fontSize = 12.sp, fontFamily = I40CzcionkaWartosci)
                )
                BasicText(
                    w.poprzedni,
                    modifier = Modifier.weight(1.2f),
                    style = TextStyle(color = kolory.tekstWyciszony, fontSize = 12.sp)
                )
                BasicText(
                    w.roznica,
                    modifier = Modifier.weight(0.8f),
                    style = TextStyle(color = kolory.tekst, fontSize = 12.sp, fontFamily = I40CzcionkaWartosci)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WierszPrzejazdu(
    p: Przejazd,
    cal: Calendar,
    tryb: TrybZaznaczania,
    onClick: () -> Unit,
    onPrzytrzymanie: () -> Unit,
    onGestUsuniecia: () -> Unit
) {
    val kolory = LocalI40Kolory.current
    var offset by remember { mutableFloatStateOf(0f) }
    val c = cal.clone() as Calendar
    c.timeInMillis = p.poczatekMs
    val godz = String.format(Locale("pl", "PL"), "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
    val dur = p.podsumowanie.czasTrwaniaS.toInt()
    val czas = "%d min".format(dur / 60)
    val km = p.podsumowanie.dystansKm?.let { FormatPomiaru.liczba(it, 1, "km") } ?: FormatPomiaru.NIEDOSTEPNE
    val przerwany = p.status == StatusPrzejazdu.Odzyskany
    val wToku = p.status == StatusPrzejazdu.WToku
    val gest = if (wToku || tryb.aktywny) {
        Modifier
    } else {
        Modifier.pointerInput(p.id) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    if (offset < -80f) onGestUsuniecia()
                    offset = 0f
                },
                onHorizontalDrag = { _, d -> offset += d }
            )
        }
    }
    val znacznik = when {
        !tryb.aktywny -> null
        wToku -> "·"
        p.id in tryb.zaznaczone -> "☑"
        else -> "☐"
    }
    Column(
        Modifier
            .fillMaxWidth()
            .offset { IntOffset(offset.roundToInt(), 0) }
            .then(gest)
            .combinedClickable(onClick = onClick, onLongClick = onPrzytrzymanie)
            .padding(8.dp)
            .background(kolory.powierzchnia)
            .padding(8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (znacznik != null) {
                BasicText(
                    znacznik,
                    modifier = Modifier.padding(end = 8.dp),
                    style = TextStyle(color = kolory.tekst, fontSize = 16.sp)
                )
            }
            BasicText(godz, modifier = Modifier.weight(1f), style = TextStyle(color = kolory.tekst, fontSize = 16.sp))
            if (p.chroniony) {
                BasicText("🔒", style = TextStyle(color = kolory.tekstDrugi, fontSize = 14.sp))
            }
        }
        val dopisek = when {
            wToku -> " · nagrywanie trwa"
            przerwany -> " · przerwany"
            else -> ""
        }
        BasicText(
            "$czas · $km$dopisek",
            style = TextStyle(color = if (przerwany || wToku) kolory.uwaga else kolory.tekstDrugi, fontSize = 13.sp)
        )
    }
}

internal fun liczbaPunktowPrzejazdu(p: Przejazd, punkty: List<PunktOdniesienia>): Int {
    val vin = p.vin ?: return 0
    val koniec = p.koniecMs ?: Long.MAX_VALUE
    return punkty.count { it.vin == vin && it.kiedyMs >= p.poczatekMs && it.kiedyMs <= koniec }
}

@Composable
private fun ZnacznikFiltra(etykieta: String, wlaczony: Boolean, onClick: () -> Unit) {
    val kolory = LocalI40Kolory.current
    BasicText(
        etykieta,
        modifier = Modifier.clickable(onClick = onClick).padding(8.dp),
        style = TextStyle(color = if (wlaczony) kolory.akcent else kolory.tekstWyciszony, fontSize = 13.sp)
    )
}
