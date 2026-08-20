package pl.i40.android.checkup

import org.json.JSONArray
import org.json.JSONObject
import pl.i40.android.obd.Dtc
import pl.i40.android.obd.DtcKind
import pl.i40.android.obd.IgnitionType
import pl.i40.android.obd.ReadinessMonitor
import pl.i40.android.obd.ReadinessStatus

/** Pełny [Raport] jako JSON — pole `raport` w §8.2 warstwy odniesienia. */
object RaportJson {
    fun encode(r: Raport): ByteArray {
        val o = JSONObject()
        o.put("startMs", r.startMs)
        o.put("koniecMs", r.koniecMs ?: JSONObject.NULL)
        o.put("zrodlo", r.zrodlo.name)
        o.put("pojazd", encodePojazd(r.pojazd))
        o.put("adapter", encodeAdapter(r.adapter))
        o.put("obslugiwanePid", JSONArray(r.obslugiwanePid))
        o.put("gotowosc", r.gotowosc?.let { encodeGotowosc(it) } ?: JSONObject.NULL)
        o.put("kodyZapisane", encodeKody(r.kodyZapisane))
        o.put("kodyOczekujace", encodeKody(r.kodyOczekujace))
        o.put("kodyTrwale", r.kodyTrwale?.let { encodeKody(it) } ?: JSONObject.NULL)
        o.put("odczyty", encodeOdczyty(r.odczyty))
        return o.toString().toByteArray(Charsets.UTF_8)
    }

    fun decode(bytes: ByteArray): Raport {
        val o = JSONObject(String(bytes, Charsets.UTF_8))
        val kodyTrwale = if (o.isNull("kodyTrwale")) null else decodeKody(o.getJSONArray("kodyTrwale"))
        val raport = Raport(
            startMs = o.getLong("startMs"),
            koniecMs = o.optionalLong("koniecMs"),
            zrodlo = ZrodloRaportu.valueOf(o.getString("zrodlo")),
            pojazd = decodePojazd(o.getJSONObject("pojazd")),
            adapter = decodeAdapter(o.getJSONObject("adapter")),
            obslugiwanePid = o.getJSONArray("obslugiwanePid").toIntList(),
            gotowosc = if (o.isNull("gotowosc")) null else decodeGotowosc(o.getJSONObject("gotowosc")),
            kodyZapisane = decodeKody(o.getJSONArray("kodyZapisane")),
            kodyOczekujace = decodeKody(o.getJSONArray("kodyOczekujace")),
            kodyTrwale = kodyTrwale,
            odczyty = decodeOdczyty(o.getJSONArray("odczyty")),
            wnioski = emptyList()
        )
        return raport.odswiezWnioski()
    }

    private fun encodePojazd(p: MigawkaPojazdu): JSONObject = JSONObject()
        .put("vin", p.vin ?: JSONObject.NULL)
        .put("producent", p.producent ?: JSONObject.NULL)
        .put("rokModelu", p.rokModelu ?: JSONObject.NULL)
        .put("fabryka", p.fabryka ?: JSONObject.NULL)
        .put("kalibracja", p.kalibracja ?: JSONObject.NULL)
        .put("nazwaEcu", p.nazwaEcu ?: JSONObject.NULL)

    private fun decodePojazd(o: JSONObject) = MigawkaPojazdu(
        vin = o.optionalString("vin"),
        producent = o.optionalString("producent"),
        rokModelu = o.optionalInt("rokModelu"),
        fabryka = o.optionalString("fabryka"),
        kalibracja = o.optionalString("kalibracja"),
        nazwaEcu = o.optionalString("nazwaEcu")
    )

    private fun encodeAdapter(a: MigawkaAdaptera): JSONObject = JSONObject()
        .put("firmware", a.firmware ?: JSONObject.NULL)
        .put("opis", a.opis ?: JSONObject.NULL)
        .put("kodProtokolu", a.kodProtokolu ?: JSONObject.NULL)
        .put("nazwaProtokolu", a.nazwaProtokolu ?: JSONObject.NULL)
        .put("napieciePin16", a.napieciePin16 ?: JSONObject.NULL)

    private fun decodeAdapter(o: JSONObject) = MigawkaAdaptera(
        firmware = o.optionalString("firmware"),
        opis = o.optionalString("opis"),
        kodProtokolu = o.optionalString("kodProtokolu"),
        nazwaProtokolu = o.optionalString("nazwaProtokolu"),
        napieciePin16 = o.optionalDouble("napieciePin16")
    )

    private fun encodeGotowosc(g: ReadinessStatus): JSONObject {
        val o = JSONObject()
        o.put("milOn", g.milOn)
        o.put("storedDtcCount", g.storedDtcCount)
        o.put("ignition", g.ignition.name)
        o.put("continuous", encodeMonitory(g.continuous))
        o.put("monitors", encodeMonitory(g.monitors))
        return o
    }

    private fun decodeGotowosc(o: JSONObject): ReadinessStatus {
        val continuous = decodeMonitory(o.getJSONArray("continuous"))
        val monitors = decodeMonitory(o.getJSONArray("monitors"))
        val incomplete = (continuous + monitors).filter { it.incomplete }
        return ReadinessStatus(
            milOn = o.getBoolean("milOn"),
            storedDtcCount = o.getInt("storedDtcCount"),
            ignition = IgnitionType.valueOf(o.getString("ignition")),
            continuous = continuous,
            monitors = monitors,
            incomplete = incomplete,
            ready = incomplete.isEmpty()
        )
    }

    private fun encodeMonitory(m: List<ReadinessMonitor>): JSONArray {
        val a = JSONArray()
        for (x in m) {
            a.put(JSONObject().put("name", x.name).put("incomplete", x.incomplete))
        }
        return a
    }

    private fun decodeMonitory(a: JSONArray): List<ReadinessMonitor> = List(a.length()) { i ->
        val o = a.getJSONObject(i)
        ReadinessMonitor(o.getString("name"), o.getBoolean("incomplete"))
    }

    private fun encodeKody(kody: List<Dtc>): JSONArray {
        val a = JSONArray()
        for (d in kody) {
            a.put(
                JSONObject()
                    .put("code", d.code)
                    .put("kind", d.kind.name)
                    .put("description", d.description)
            )
        }
        return a
    }

    private fun decodeKody(a: JSONArray): List<Dtc> = List(a.length()) { i ->
        val o = a.getJSONObject(i)
        Dtc(o.getString("code"), DtcKind.valueOf(o.getString("kind")), o.getString("description"))
    }

    private fun encodeOdczyty(odczyty: List<MigawkaOdczytu>): JSONArray {
        val a = JSONArray()
        for (x in odczyty) {
            a.put(
                JSONObject()
                    .put("pid", x.pid)
                    .put("nazwa", x.nazwa)
                    .put("jednostka", x.jednostka)
                    .put("wartosc", x.wartosc ?: JSONObject.NULL)
                    .put("dostepny", x.dostepny)
                    .put("podejrzany", x.podejrzany)
            )
        }
        return a
    }

    private fun decodeOdczyty(a: JSONArray): List<MigawkaOdczytu> = List(a.length()) { i ->
        val o = a.getJSONObject(i)
        MigawkaOdczytu(
            pid = o.getInt("pid"),
            nazwa = o.getString("nazwa"),
            jednostka = o.getString("jednostka"),
            wartosc = o.optionalDouble("wartosc"),
            dostepny = o.getBoolean("dostepny"),
            podejrzany = o.getBoolean("podejrzany")
        )
    }

    private fun JSONObject.optionalString(key: String): String? = if (!has(key) || isNull(key)) null else getString(key)

    private fun JSONObject.optionalInt(key: String): Int? = if (!has(key) || isNull(key)) null else getInt(key)

    private fun JSONObject.optionalLong(key: String): Long? = if (!has(key) || isNull(key)) null else getLong(key)

    private fun JSONObject.optionalDouble(key: String): Double? = if (!has(key) || isNull(key)) null else getDouble(key)

    private fun JSONArray.toIntList(): List<Int> = List(length()) { i -> getInt(i) }
}
