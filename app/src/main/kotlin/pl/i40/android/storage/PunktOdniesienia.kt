package pl.i40.android.storage

import org.json.JSONObject

data class PunktOdniesienia(
    val id: String,
    val kiedyMs: Long,
    val vin: String,
    val stan: String,
    val zrodlo: String,
    val probek: Int,
    val odczyty: Map<Int, Double>
)

interface MagazynPunktowOdniesienia {
    fun zapisz(punkt: PunktOdniesienia)
    fun dlaVin(vin: String): List<PunktOdniesienia>
}

class PamiecPunktowOdniesienia : MagazynPunktowOdniesienia {
    private val punkty = mutableListOf<PunktOdniesienia>()

    override fun zapisz(punkt: PunktOdniesienia) {
        punkty.add(punkt)
    }

    override fun dlaVin(vin: String): List<PunktOdniesienia> = punkty.filter { it.vin == vin }.sortedBy { it.kiedyMs }
}

/** JSON PID → mediana — pole `odczyty` w §8.1 warstwy odniesienia. */
object OdczytyPunktuJson {
    fun encode(odczyty: Map<Int, Double>): ByteArray {
        val o = JSONObject()
        for ((pid, v) in odczyty) {
            o.put("%02X".format(pid), v)
        }
        return o.toString().toByteArray(Charsets.UTF_8)
    }

    fun decode(bytes: ByteArray): Map<Int, Double> {
        val o = JSONObject(String(bytes, Charsets.UTF_8))
        val out = mutableMapOf<Int, Double>()
        val klucze = o.keys()
        while (klucze.hasNext()) {
            val klucz = klucze.next()
            out[klucz.toInt(16)] = o.getDouble(klucz)
        }
        return out
    }
}

data class WpisPrzegladu(
    val id: String,
    val kiedyMs: Long,
    val vin: String?,
    val stan: String?,
    val raportBlob: ByteArray
)
