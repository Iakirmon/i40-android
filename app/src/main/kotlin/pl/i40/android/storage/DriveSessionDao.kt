package pl.i40.android.storage

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

/**
 * SQLite — `przejazd` (v1), `punkt_odniesienia` (v2), `przeglad` (v3).
 * Osobne `if` w [onUpgrade], nigdy łańcuch if/else — H1 dołoży kolejny stopień.
 */
class DriveSessionDao(context: Context) :
    SQLiteOpenHelper(context, NAZWA_BAZY, null, WERSJA_SCHEMATU),
    PrzejazdMagazyn,
    MagazynPunktowOdniesienia {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE)
        db.execSQL("CREATE INDEX idx_przejazd_poczatek ON przejazd (poczatek)")
        db.execSQL("CREATE INDEX idx_przejazd_status ON przejazd (status)")
        utworzTabelePunktow(db)
        utworzTabelePrzegladow(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        MigracjaSchematu.onUpgrade(db, oldVersion)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        MigracjaSchematu.onDowngrade(oldVersion, newVersion)
    }

    override fun zapisz(punkt: PunktOdniesienia) {
        writableDatabase.execSQL(
            """
            INSERT INTO punkt_odniesienia (id, kiedy, vin, stan, zrodlo, probek, odczyty)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf(
                punkt.id,
                punkt.kiedyMs,
                punkt.vin,
                punkt.stan,
                punkt.zrodlo,
                punkt.probek,
                OdczytyPunktuJson.encode(punkt.odczyty)
            )
        )
    }

    override fun dlaVin(vin: String): List<PunktOdniesienia> {
        val out = mutableListOf<PunktOdniesienia>()
        readableDatabase.rawQuery(
            "SELECT * FROM punkt_odniesienia WHERE vin = ? ORDER BY kiedy ASC",
            arrayOf(vin)
        ).use { c ->
            while (c.moveToNext()) out.add(punktZKursora(c))
        }
        return out
    }

    fun zapiszPrzeglad(id: String, kiedyMs: Long, vin: String?, stan: String?, raportBlob: ByteArray) {
        writableDatabase.execSQL(
            """
            INSERT INTO przeglad (id, kiedy, vin, stan, raport)
            VALUES (?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf(id, kiedyMs, vin, stan, raportBlob)
        )
    }

    fun przegladyDlaVin(vin: String): List<WpisPrzegladu> {
        val out = mutableListOf<WpisPrzegladu>()
        readableDatabase.rawQuery(
            "SELECT * FROM przeglad WHERE vin = ? ORDER BY kiedy ASC",
            arrayOf(vin)
        ).use { c ->
            while (c.moveToNext()) out.add(przegladZKursora(c))
        }
        return out
    }

    fun poprzedniPrzeglad(vin: String, pozaId: String): WpisPrzegladu? =
        przegladyDlaVin(vin).filter { it.id != pozaId }.maxByOrNull { it.kiedyMs }

    override fun wstaw(przejazd: Przejazd) {
        writableDatabase.execSQL(
            """
            INSERT INTO przejazd (id, poczatek, koniec, status, vin, notatka, podsumowanie, przebieg, chroniony)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf(
                przejazd.id,
                przejazd.poczatekMs,
                przejazd.koniecMs,
                przejazd.status.sql,
                przejazd.vin,
                przejazd.notatka,
                PodsumowanieJson.encode(przejazd.podsumowanie),
                przejazd.przebieg.encode(),
                if (przejazd.chroniony) 1 else 0
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
        usunWiele(listOf(id))
    }

    override fun usunWiele(ids: Collection<String>): WynikKasowania {
        val wynik = KasowaniePrzejazdow.przygotuj(lista(), ids)
        if (wynik.doUsuniecia.isEmpty()) return wynik
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (id in wynik.doUsuniecia) {
                db.execSQL("DELETE FROM przejazd WHERE id = ?", arrayOf(id))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        if (wynik.wymagaVacuum) {
            db.execSQL("VACUUM")
        }
        return wynik
    }

    override fun ustawChroniony(id: String, chroniony: Boolean) {
        writableDatabase.execSQL(
            "UPDATE przejazd SET chroniony = ? WHERE id = ?",
            arrayOf(if (chroniony) 1 else 0, id)
        )
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
            checkpointMs = koniec ?: c.getLong(c.getColumnIndexOrThrow("poczatek")),
            chroniony = c.getInt(c.getColumnIndexOrThrow("chroniony")) != 0
        )
    }

    private fun punktZKursora(c: android.database.Cursor): PunktOdniesienia = PunktOdniesienia(
        id = c.getString(c.getColumnIndexOrThrow("id")),
        kiedyMs = c.getLong(c.getColumnIndexOrThrow("kiedy")),
        vin = c.getString(c.getColumnIndexOrThrow("vin")),
        stan = c.getString(c.getColumnIndexOrThrow("stan")),
        zrodlo = c.getString(c.getColumnIndexOrThrow("zrodlo")),
        probek = c.getInt(c.getColumnIndexOrThrow("probek")),
        odczyty = OdczytyPunktuJson.decode(c.getBlob(c.getColumnIndexOrThrow("odczyty")))
    )

    private fun przegladZKursora(c: android.database.Cursor): WpisPrzegladu = WpisPrzegladu(
        id = c.getString(c.getColumnIndexOrThrow("id")),
        kiedyMs = c.getLong(c.getColumnIndexOrThrow("kiedy")),
        vin = if (c.isNull(c.getColumnIndexOrThrow("vin"))) {
            null
        } else {
            c.getString(c.getColumnIndexOrThrow("vin"))
        },
        stan = if (c.isNull(c.getColumnIndexOrThrow("stan"))) {
            null
        } else {
            c.getString(c.getColumnIndexOrThrow("stan"))
        },
        raportBlob = c.getBlob(c.getColumnIndexOrThrow("raport"))
    )

    companion object {
        const val NAZWA_BAZY = "i40.db"
        const val WERSJA_SCHEMATU = MigracjaSchematu.WERSJA
        const val SQL_CREATE = """
            CREATE TABLE przejazd (
                id TEXT PRIMARY KEY,
                poczatek INTEGER NOT NULL,
                koniec INTEGER,
                status TEXT NOT NULL,
                vin TEXT,
                notatka TEXT NOT NULL DEFAULT '',
                podsumowanie BLOB NOT NULL,
                przebieg BLOB NOT NULL,
                chroniony INTEGER NOT NULL DEFAULT 0
            )
            """

        /** Schemat z §8.1 warstwy odniesienia — bez zmian w nazwach kolumn. */
        const val SQL_CREATE_PUNKT = """
            CREATE TABLE punkt_odniesienia (
                id       TEXT PRIMARY KEY,
                kiedy    INTEGER NOT NULL,
                vin      TEXT NOT NULL,
                stan     TEXT NOT NULL,
                zrodlo   TEXT NOT NULL,
                probek   INTEGER NOT NULL,
                odczyty  BLOB NOT NULL
            )
            """

        internal fun utworzTabelePunktow(db: SQLiteDatabase) {
            db.execSQL(SQL_CREATE_PUNKT)
            db.execSQL("CREATE INDEX idx_punkt_vin_kiedy ON punkt_odniesienia (vin, kiedy)")
        }

        /** Schemat z §8.2 warstwy odniesienia. */
        const val SQL_CREATE_PRZEGLAD = """
            CREATE TABLE przeglad (
                id      TEXT PRIMARY KEY,
                kiedy   INTEGER NOT NULL,
                vin     TEXT,
                stan    TEXT,
                raport  BLOB NOT NULL
            )
            """

        internal fun utworzTabelePrzegladow(db: SQLiteDatabase) {
            db.execSQL(SQL_CREATE_PRZEGLAD)
            db.execSQL("CREATE INDEX idx_przeglad_vin_kiedy ON przeglad (vin, kiedy)")
        }
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
        o.put("maxCisnienieSzynyBar", p.maxCisnienieSzynyBar ?: JSONObject.NULL)
        o.put("obciazeniePrzyMaxCisnieniu", p.obciazeniePrzyMaxCisnieniu ?: JSONObject.NULL)
        o.put("maxTempKatalizatoraC", p.maxTempKatalizatoraC ?: JSONObject.NULL)
        o.put("czasDo90CSekundy", p.czasDo90CSekundy ?: JSONObject.NULL)
        o.put(
            "czasPozaPasmemWPetliZamknietejSekundy",
            p.czasPozaPasmemWPetliZamknietejSekundy ?: JSONObject.NULL
        )
        o.put("czasWPetliZamknietejSekundy", p.czasWPetliZamknietejSekundy ?: JSONObject.NULL)
        o.put("medianaKorektyDlugoterminowej", p.medianaKorektyDlugoterminowej ?: JSONObject.NULL)
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
            sredniaHz = o.getDouble("sredniaHz"),
            maxCisnienieSzynyBar = o.optionalDouble("maxCisnienieSzynyBar"),
            obciazeniePrzyMaxCisnieniu = o.optionalDouble("obciazeniePrzyMaxCisnieniu"),
            maxTempKatalizatoraC = o.optionalDouble("maxTempKatalizatoraC"),
            czasDo90CSekundy = o.optionalDouble("czasDo90CSekundy"),
            czasPozaPasmemWPetliZamknietejSekundy = o.optionalDouble("czasPozaPasmemWPetliZamknietejSekundy"),
            czasWPetliZamknietejSekundy = o.optionalDouble("czasWPetliZamknietejSekundy"),
            medianaKorektyDlugoterminowej = o.optionalDouble("medianaKorektyDlugoterminowej")
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
