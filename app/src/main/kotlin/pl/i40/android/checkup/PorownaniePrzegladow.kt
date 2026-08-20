package pl.i40.android.checkup

import pl.i40.android.obd.Dtc
import pl.i40.android.obd.ReadinessMonitor

data class PorownanieKodow(
    val kodyNowe: List<Dtc>,
    val kodyZnikniete: List<Dtc>,
    val monitoryStracone: List<ReadinessMonitor>,
    val monitoryOdzyskane: List<ReadinessMonitor>,
    val milPoprzednio: Boolean?,
    val milTeraz: Boolean?
) {
    val bezZmian: Boolean
        get() = kodyNowe.isEmpty() &&
            kodyZnikniete.isEmpty() &&
            monitoryStracone.isEmpty() &&
            monitoryOdzyskane.isEmpty() &&
            milPoprzednio == milTeraz
}

data class ZapisanyPrzeglad(val id: String, val kiedyMs: Long, val vin: String?, val stan: String?, val raport: Raport)

interface MagazynPrzegladow {
    fun zapisz(wpis: ZapisanyPrzeglad)
    fun dlaVin(vin: String): List<ZapisanyPrzeglad>
    fun poprzedniDlaVin(vin: String, pozaId: String): ZapisanyPrzeglad?
}

class PamiecPrzegladow : MagazynPrzegladow {
    private val wpisy = mutableListOf<ZapisanyPrzeglad>()

    override fun zapisz(wpis: ZapisanyPrzeglad) {
        wpisy.add(wpis)
    }

    override fun dlaVin(vin: String): List<ZapisanyPrzeglad> = wpisy.filter { it.vin == vin }.sortedBy { it.kiedyMs }

    override fun poprzedniDlaVin(vin: String, pozaId: String): ZapisanyPrzeglad? =
        dlaVin(vin).filter { it.id != pozaId }.maxByOrNull { it.kiedyMs }
}

/**
 * Porównanie kodów, monitorów i MIL — sekcja 9.3 warstwy odniesienia.
 * Działa niezależnie od stanu jałowego rozgrzanego: kody nie zależą od temperatury.
 */
object PorownaniePrzegladow {
    const val STAN_JALOWY_ROZGRZANY = "jalowy_rozgrzany"

    fun porownaj(teraz: Raport, poprzedni: Raport): PorownanieKodow {
        val terazKody = teraz.kodyZapisane.associateBy { it.code }
        val popKody = poprzedni.kodyZapisane.associateBy { it.code }
        val nowe = terazKody.keys.minus(popKody.keys).mapNotNull { terazKody[it] }
        val znikniete = popKody.keys.minus(terazKody.keys).mapNotNull { popKody[it] }
        val terazMon = monitory(teraz).associateBy { it.name }
        val popMon = monitory(poprzedni).associateBy { it.name }
        val wspolne = terazMon.keys.intersect(popMon.keys)
        val stracone = wspolne.mapNotNull { nazwa ->
            val p = popMon[nazwa] ?: return@mapNotNull null
            val t = terazMon[nazwa] ?: return@mapNotNull null
            if (!p.incomplete && t.incomplete) t else null
        }
        val odzyskane = wspolne.mapNotNull { nazwa ->
            val p = popMon[nazwa] ?: return@mapNotNull null
            val t = terazMon[nazwa] ?: return@mapNotNull null
            if (p.incomplete && !t.incomplete) t else null
        }
        return PorownanieKodow(
            kodyNowe = nowe,
            kodyZnikniete = znikniete,
            monitoryStracone = stracone,
            monitoryOdzyskane = odzyskane,
            milPoprzednio = poprzedni.gotowosc?.milOn,
            milTeraz = teraz.gotowosc?.milOn
        )
    }

    fun poprzedniPorownywalny(magazyn: MagazynPrzegladow, vin: String?, pozaId: String): ZapisanyPrzeglad? {
        if (vin == null) return null
        return magazyn.poprzedniDlaVin(vin, pozaId)
    }

    fun stanZRaportu(raport: Raport): String? = if (raport.jalowyRozgrzany) STAN_JALOWY_ROZGRZANY else null

    private fun monitory(raport: Raport): List<ReadinessMonitor> {
        val g = raport.gotowosc ?: return emptyList()
        return g.continuous + g.monitors
    }
}
