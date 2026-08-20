package pl.i40.android.storage

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException

/**
 * Drabinka schematu — sekcja 11.2 warstwy historii.
 *
 * Historia deklaruje wersję 3 (`chroniony`). O2/O3 zdążyły zużyć wersję 3 na tabelę
 * `przeglad`, więc `chroniony` stoi na wersji 4. Osobne `if`, nigdy if/else.
 */
object MigracjaSchematu {
    const val WERSJA = 4

    const val SQL_CHRONIONY =
        "ALTER TABLE przejazd ADD COLUMN chroniony INTEGER NOT NULL DEFAULT 0"

    fun onUpgrade(db: SQLiteDatabase, oldVersion: Int) {
        if (oldVersion < 2) {
            DriveSessionDao.utworzTabelePunktow(db)
        }
        if (oldVersion < 3) {
            DriveSessionDao.utworzTabelePrzegladow(db)
        }
        if (oldVersion < 4) {
            db.execSQL(SQL_CHRONIONY)
        }
    }

    fun onDowngrade(oldVersion: Int, newVersion: Int): Nothing =
        throw SQLiteException("Cofnięcie schematu $oldVersion → $newVersion jest zabronione.")

    /** Do testów drabinki bez Androida: które kroki wykona aktualizacja z [oldVersion]. */
    fun kroki(oldVersion: Int): List<String> {
        val out = mutableListOf<String>()
        if (oldVersion < 2) out.add("punkt_odniesienia")
        if (oldVersion < 3) out.add("przeglad")
        if (oldVersion < 4) out.add("chroniony")
        return out
    }
}
