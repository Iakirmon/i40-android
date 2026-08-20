package pl.i40.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pl.i40.android.R
import pl.i40.android.checkup.CheckupOrchestrator
import pl.i40.android.checkup.PorownaniePrzegladow
import pl.i40.android.checkup.Raport
import pl.i40.android.checkup.RaportJson
import pl.i40.android.checkup.SlownikDtc
import pl.i40.android.checkup.ZrodloRaportu
import pl.i40.android.storage.DriveSessionDao
import pl.i40.android.transport.MockI40Script
import pl.i40.android.transport.MockTransport

@Composable
fun CheckupScreen(wRuchu: Boolean, modifier: Modifier = Modifier) {
    val kolory = LocalI40Kolory.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Na postoju — pełny przegląd na atrapie.") }
    var raport by remember { mutableStateOf<Raport?>(null) }
    var zmiany by remember { mutableStateOf<String?>(null) }
    var naglowek by remember { mutableStateOf<String?>(null) }
    var punkt by remember { mutableStateOf<pl.i40.android.storage.PunktOdniesienia?>(null) }
    val slownik = remember {
        val json = context.resources.openRawResource(R.raw.dtc_dictionary).bufferedReader().use { it.readText() }
        SlownikDtc.zJson(json)
    }

    Column(
        modifier
            .fillMaxSize()
            .background(kolory.tlo)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        BasicText("Przegląd", style = TextStyle(color = kolory.tekst, fontSize = 22.sp))
        BasicText(status, style = TextStyle(color = kolory.tekstDrugi, fontSize = 14.sp))
        val mozna = !wRuchu
        BasicText(
            text = "Uruchom przegląd",
            modifier = Modifier.padding(top = 16.dp).clickable(enabled = mozna) {
                scope.launch {
                    status = "Łączenie…"
                    val transport = MockTransport(MockI40Script.make(), timeScale = 0.0)
                    val wynik = CheckupOrchestrator(slownikDtc = slownik).uruchom(
                        transport = transport,
                        zrodlo = ZrodloRaportu.Atrapa,
                        scope = scope
                    )
                    val id = java.util.UUID.randomUUID().toString()
                    val dao = DriveSessionDao(context)
                    val vin = wynik.pojazd.vin
                    val poprzedni = if (vin != null) dao.poprzedniPrzeglad(vin, id) else null
                    val poprzedniZapis = poprzedni?.let { wpis ->
                        pl.i40.android.checkup.ZapisanyPrzeglad(
                            id = wpis.id,
                            kiedyMs = wpis.kiedyMs,
                            vin = wpis.vin,
                            stan = wpis.stan,
                            raport = RaportJson.decode(wpis.raportBlob)
                        )
                    }
                    zmiany = if (poprzedniZapis != null) {
                        FormatZmianPrzegladu.blok(
                            PorownaniePrzegladow.porownaj(wynik, poprzedniZapis.raport),
                            poprzedniZapis.kiedyMs
                        )
                    } else {
                        null
                    }
                    naglowek = FormatNaglowkaPrzegladu.tekst(wynik, poprzedniZapis)
                    dao.zapiszPrzeglad(
                        id,
                        wynik.startMs,
                        vin,
                        PorownaniePrzegladow.stanZRaportu(wynik),
                        RaportJson.encode(wynik)
                    )
                    punkt = if (vin != null) dao.dlaVin(vin).lastOrNull() else null
                    raport = wynik
                    status = wynik.werdykt.tytul
                }
            }.padding(12.dp).background(if (mozna) kolory.powierzchnia else kolory.powierzchnia),
            style = TextStyle(color = if (mozna) kolory.akcent else kolory.tekstWyciszony, fontSize = 16.sp)
        )
        val r = raport
        if (r != null) {
            BasicText(
                text = r.werdykt.tytul,
                style = TextStyle(color = kolory.werdykt(r.werdykt), fontSize = 28.sp, fontFamily = I40CzcionkaWartosci)
            )
            BasicText(
                text = r.pojazd.vin ?: FormatPomiaru.NIEDOSTEPNE,
                style = TextStyle(color = kolory.tekstDrugi, fontSize = 14.sp)
            )
            val nag = naglowek
            if (nag != null) {
                BasicText(
                    text = nag,
                    modifier = Modifier.padding(top = 8.dp),
                    style = TextStyle(color = kolory.tekst, fontSize = 14.sp)
                )
            }
            val blokZmian = zmiany
            if (blokZmian != null) {
                BasicText(
                    text = blokZmian,
                    modifier = Modifier.padding(top = 12.dp),
                    style = TextStyle(color = kolory.tekst, fontSize = 14.sp)
                )
            }
            KartaGdi(FormatPrzegladu.kartaGdi(r))
            KartaKatalizator(FormatPrzegladu.kartaKatalizator(r))
            KartaOdczytow(r, punkt)
        }
        if (wRuchu) {
            BasicText(
                "Blokada prędkościowa — przegląd tylko na postoju.",
                style = TextStyle(color = kolory.uwaga, fontSize = 14.sp)
            )
        }
    }
}

@Composable
private fun KartaGdi(karta: KartaGdiPrzegladu) {
    val kolory = LocalI40Kolory.current
    Column(Modifier.fillMaxWidth().padding(top = 16.dp).background(kolory.powierzchnia).padding(12.dp)) {
        BasicText("WTRYSK GDI", style = TextStyle(color = kolory.akcent, fontSize = 16.sp))
        WierszKarty(karta.cisnienie)
        WierszKarty(karta.obciazenie)
        WierszKarty(karta.obroty)
        BasicText(
            karta.stopka,
            modifier = Modifier.padding(top = 8.dp),
            style = TextStyle(color = kolory.tekstDrugi, fontSize = 13.sp)
        )
    }
}

@Composable
private fun KartaKatalizator(karta: KartaKatalizatorPrzegladu) {
    val kolory = LocalI40Kolory.current
    Column(Modifier.fillMaxWidth().padding(top = 16.dp).background(kolory.powierzchnia).padding(12.dp)) {
        BasicText("KATALIZATOR", style = TextStyle(color = kolory.akcent, fontSize = 16.sp))
        WierszKarty(karta.temperatura)
        BasicText(
            "zapłon od  ${karta.zaplon}",
            style = TextStyle(color = kolory.tekstDrugi, fontSize = 13.sp)
        )
        WierszKarty(karta.sonda)
        val powod = karta.sonda.powod
        if (powod != null) {
            BasicText(powod, style = TextStyle(color = kolory.tekstDrugi, fontSize = 13.sp))
        }
        BasicText(
            "Monitor katalizatora  ${karta.monitorKatalizatora}",
            modifier = Modifier.padding(top = 8.dp),
            style = TextStyle(color = kolory.tekst, fontSize = 14.sp)
        )
        BasicText(
            "Monitor sond tlenu  ${karta.monitorSond}",
            style = TextStyle(color = kolory.tekst, fontSize = 14.sp)
        )
    }
}

@Composable
private fun KartaOdczytow(raport: Raport, punkt: pl.i40.android.storage.PunktOdniesienia? = null) {
    val kolory = LocalI40Kolory.current
    val powietrze = FormatPrzegladu.grupaPowietrze(raport, punkt)
    val reszta = FormatPrzegladu.wierszeOdczytow(raport, punkt).filter { wiersz ->
        !wiersz.wyliczony &&
            wiersz.pid != FormatPrzegladu.PID_KOLEKTOR &&
            wiersz.pid != FormatPrzegladu.PID_ATMOSFERA
    }
    Column(Modifier.fillMaxWidth().padding(top = 16.dp).background(kolory.powierzchnia).padding(12.dp)) {
        BasicText("ODCZYTY", style = TextStyle(color = kolory.akcent, fontSize = 16.sp))
        BasicText(
            FormatPrzegladu.NAGLOWEK_KOLUMN,
            modifier = Modifier.padding(top = 4.dp),
            style = TextStyle(color = kolory.tekstWyciszony, fontSize = 12.sp)
        )
        if (punkt != null) {
            BasicText(
                FormatZmianPrzegladu.dataDoNaglowka(punkt.kiedyMs),
                style = TextStyle(color = kolory.tekstWyciszony, fontSize = 12.sp)
            )
        }
        BasicText(
            FormatPrzegladu.NAGLOWEK_POWIETRZE_WTRYSK,
            modifier = Modifier.padding(top = 8.dp),
            style = TextStyle(color = kolory.tekstDrugi, fontSize = 13.sp)
        )
        for (wiersz in powietrze) {
            WierszKarty(wiersz)
        }
        for (wiersz in reszta) {
            WierszKarty(wiersz)
        }
    }
}

@Composable
private fun WierszKarty(wiersz: WierszPrzegladu) {
    val kolory = LocalI40Kolory.current
    BasicText(
        text = "${wiersz.etykieta}  ${wiersz.wartosc}",
        modifier = Modifier.padding(top = 6.dp),
        style = TextStyle(color = kolory.tekst, fontSize = 14.sp)
    )
    BasicText(
        text = "poprzednio  ${wiersz.poprzednio}",
        style = TextStyle(color = kolory.tekstDrugi, fontSize = 13.sp)
    )
    BasicText(
        text = "norma  ${wiersz.norma}",
        style = TextStyle(color = kolory.tekstWyciszony, fontSize = 12.sp)
    )
}
