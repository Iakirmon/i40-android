package pl.i40.android.storage

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

/**
 * SQLite — jedna tabela `przejazd`, schemat wersji 1 z §8.6.
 * Historia podniesie wersję w etapie H1; tu nie wyprzedzamy drabinki.
 */
class DriveSessionDao(context: Context) :
    SQLiteOpenHelper(context, NAZWA_BAZY, null, WERSJA_SCHEMATU),
    PrzejazdMagazyn {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE)
        db.execSQL("CREATE INDEX idx_przejazd_poczatek ON przejazd (poczatek)")
        db.execSQL("CREATE INDEX idx_przejazd_status ON przejazd (status)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    override fun wstaw(przejazd: Przejazd) {
        writableDatabase.execSQL(
            """
            INSERT INTO przejazd (id, poczatek, koniec, status, vin, notatka, podsumowanie, przebieg)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf(
                przejazd.id,
                przejazd.poczatekMs,
                przejazd.koniecMs,
                przejazd.status.sql,
                przejazd.vin,
                przejazd.notatka,
                PodsumowanieJson.encode(przejazd.podsumowanie),
                przejazd.przebieg.encode()
            )
        )
    }

    override fun zapiszPrzebieg(id: String, przebieg: TrackBlob, checkpointMs: Long) {
        writableDatabase.execSQL(
            "UPDATE przejazd SET przebieg = ?, koniec = ? WHERE id = ?",
            arrayOf(przebieg.encode(), checkpointMs, id)
        )
    }

    override fun zamknij(
        id: String,
        koniecMs: Long,
        status: StatusPrzejazdu,
        podsumowanie: PodsumowaniePrzejazdu,
        przebieg: TrackBlob
    ) {
        writableDatabase.execSQL(
            """
            UPDATE przejazd SET koniec = ?, status = ?, podsumowanie = ?, przebieg = ?
            WHERE id = ?
            """.trimIndent(),
            arrayOf(
                koniecMs,
                status.sql,
                PodsumowanieJson.encode(podsumowanie),
                przebieg.encode(),
                id
            )
        )
    }

    override fun czytaj(id: String): Przejazd? {
        readableDatabase.rawQuery("SELECT * FROM przejazd WHERE id = ?", arrayOf(id)).use { c ->
            if (!c.moveToFirst()) return null
            return fromCursor(c)
        }
    }

    override fun listaWToku(): List<Przejazd> = listaGdzie("status = ?", arrayOf(StatusPrzejazdu.WToku.sql))

    override fun lista(): List<Przejazd> = listaGdzie("1 = 1", emptyArray())

    override fun usun(id: String) {
        writableDatabase.execSQL("DELETE FROM przejazd WHERE id = ?", arrayOf(id))
    }

    private fun listaGdzie(where: String, args: Array<String>): List<Przejazd> {
        val out = mutableListOf<Przejazd>()
        readableDatabase.rawQuery(
            "SELECT * FROM przejazd WHERE $where ORDER BY poczatek DESC",
            args
        ).use { c ->
            while (c.moveToNext()) out.add(fromCursor(c))
        }
        return out
    }

    private fun fromCursor(c: android.database.Cursor): Przejazd {
        val koniec = if (c.isNull(c.getColumnIndexOrThrow("koniec"))) {
            null
        } else {
            c.getLong(c.getColumnIndexOrThrow("koniec"))
        }
        return Przejazd(
            id = c.getString(c.getColumnIndexOrThrow("id")),
            poczatekMs = c.getLong(c.getColumnIndexOrThrow("poczatek")),
            koniecMs = koniec,
            status = StatusPrzejazdu.fromSql(c.getString(c.getColumnIndexOrThrow("status"))),
            vin = c.getString(c.getColumnIndexOrThrow("vin")),
            notatka = c.getString(c.getColumnIndexOrThrow("notatka")),
            podsumowanie = PodsumowanieJson.decode(c.getBlob(c.getColumnIndexOrThrow("podsumowanie"))),
            przebieg = TrackBlob.decode(c.getBlob(c.getColumnIndexOrThrow("przebieg"))),
            checkpointMs = koniec ?: c.getLong(c.getColumnIndexOrThrow("poczatek"))
        )
    }

    companion object {
        const val NAZWA_BAZY = "i40.db"
        const val WERSJA_SCHEMATU = 1
        const val SQL_CREATE = """
            CREATE TABLE przejazd (
                id TEXT PRIMARY KEY,
                poczatek INTEGER NOT NULL,
                koniec INTEGER,
                status TEXT NOT NULL,
                vin TEXT,
                notatka TEXT NOT NULL DEFAULT '',
                podsumowanie BLOB NOT NULL,
                przebieg BLOB NOT NULL
            )
            """
    }
}

object PodsumowanieJson {
    fun encode(p: PodsumowaniePrzejazdu): ByteArray {
        val o = JSONObject()
        o.put("czasTrwaniaS", p.czasTrwaniaS)
        o.put("dystansKm", p.dystansKm ?: JSONObject.NULL)
        o.put("maxObroty", p.maxObroty ?: JSONObject.NULL)
        o.put("srednieObroty", p.srednieObroty ?: JSONObject.NULL)
        o.put("maxPredkoscKmh", p.maxPredkoscKmh ?: JSONObject.NULL)
        o.put("maxPlynC", p.maxPlynC ?: JSONObject.NULL)
        o.put("minNapiecie", p.minNapiecie ?: JSONObject.NULL)
        o.put("maxNapiecie", p.maxNapiecie ?: JSONObject.NULL)
        o.put("paliwoL", p.paliwoL ?: JSONObject.NULL)
        o.put("kodyNaStarcie", JSONArray(p.kodyNaStarcie))
        o.put("kodyNaKoncu", JSONArray(p.kodyNaKoncu))
        o.put("liczbaProbek", p.liczbaProbek)
        o.put("sredniaHz", p.sredniaHz)
        return o.toString().toByteArray(Charsets.UTF_8)
    }

    fun decode(bytes: ByteArray): PodsumowaniePrzejazdu {
        val o = JSONObject(String(bytes, Charsets.UTF_8))
        return PodsumowaniePrzejazdu(
            czasTrwaniaS = o.getDouble("czasTrwaniaS"),
            dystansKm = o.optionalDouble("dystansKm"),
            maxObroty = o.optionalDouble("maxObroty"),
            srednieObroty = o.optionalDouble("srednieObroty"),
            maxPredkoscKmh = o.optionalDouble("maxPredkoscKmh"),
            maxPlynC = o.optionalDouble("maxPlynC"),
            minNapiecie = o.optionalDouble("minNapiecie"),
            maxNapiecie = o.optionalDouble("maxNapiecie"),
            paliwoL = o.optionalDouble("paliwoL"),
            kodyNaStarcie = o.stringList("kodyNaStarcie"),
            kodyNaKoncu = o.stringList("kodyNaKoncu"),
            liczbaProbek = o.getInt("liczbaProbek"),
            sredniaHz = o.getDouble("sredniaHz")
        )
    }

    private fun JSONObject.optionalDouble(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        return getDouble(key)
    }

    private fun JSONObject.stringList(key: String): List<String> {
        val arr = optJSONArray(key) ?: return emptyList()
        return List(arr.length()) { arr.getString(it) }
    }
}
