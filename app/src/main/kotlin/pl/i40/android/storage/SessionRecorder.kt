package pl.i40.android.storage

import pl.i40.android.acquisition.SampleTick
import pl.i40.android.obd.DecodedPid

class SessionRecorderError(message: String) : Exception(message) {
    companion object {
        val alreadyRecording = SessionRecorderError("Nagrywanie już trwa.")
        val notRecording = SessionRecorderError("Brak aktywnego nagrywania.")
    }
}

/**
 * Gromadzenie próbek i zapis przejazdu. Wiersz powstaje przy starcie (`w_toku`);
 * checkpoint co 30 s; zamknięcie zawsze zapisuje.
 */
class SessionRecorder(
    private val magazyn: PrzejazdMagazyn,
    private val terazMs: () -> Long = { System.currentTimeMillis() },
    private val checkpointCoMs: Long = 30_000L,
    private val vin: String? = null,
    private val notatka: String = ""
) {
    private val zamek = Any()
    private var id: String? = null
    private var track = TrackBlob()
    private var kodyNaStarcie: List<String> = emptyList()
    private var poczatekMs: Long = 0
    private var ostatniCheckpointMs: Long = 0

    val recording: Boolean get() = synchronized(zamek) { id != null }

    fun currentTrack(): TrackBlob = synchronized(zamek) { track.kopia() }

    fun start(kodyNaStarcie: List<String> = emptyList()): String {
        synchronized(zamek) {
            if (id != null) throw SessionRecorderError.alreadyRecording
            val now = terazMs()
            val nowyId = java.util.UUID.randomUUID().toString()
            track = TrackBlob()
            this.kodyNaStarcie = kodyNaStarcie
            poczatekMs = now
            ostatniCheckpointMs = now
            id = nowyId
            val wiersz = Przejazd(
                id = nowyId,
                poczatekMs = now,
                koniecMs = null,
                status = StatusPrzejazdu.WToku,
                vin = vin,
                notatka = notatka,
                podsumowanie = PodsumowaniePrzejazdu(kodyNaStarcie = kodyNaStarcie),
                przebieg = TrackBlob(),
                checkpointMs = now
            )
            magazyn.wstaw(wiersz)
            return nowyId
        }
    }

    fun append(tick: SampleTick) {
        synchronized(zamek) {
            if (id == null) return
            val t = tick.time.toFloat()
            for (reading in tick.readings) {
                val numeric = reading.decoded as? DecodedPid.Numeric ?: continue
                track.append(reading.pid, t, numeric.value.toFloat())
            }
        }
    }

    fun checkpointJesliPotrzeba() {
        val snapshot: Pair<String, TrackBlob>
        val now = terazMs()
        synchronized(zamek) {
            val biezace = id ?: return
            if (now - ostatniCheckpointMs < checkpointCoMs) return
            ostatniCheckpointMs = now
            snapshot = biezace to track.kopia()
        }
        magazyn.zapiszPrzebieg(snapshot.first, snapshot.second, now)
    }

    fun stop(kodyNaKoncu: List<String> = emptyList()): Przejazd {
        val snapshot: Snapshot
        val now = terazMs()
        synchronized(zamek) {
            val biezace = id ?: throw SessionRecorderError.notRecording
            snapshot = Snapshot(biezace, track.kopia(), kodyNaStarcie, poczatekMs)
            id = null
        }
        return zapiszZamkniecie(snapshot, now, StatusPrzejazdu.Zamkniety, kodyNaKoncu)
    }

    companion object {
        fun odzyskajPrzerwane(magazyn: PrzejazdMagazyn): List<Przejazd> {
            val wynik = mutableListOf<Przejazd>()
            for (wiersz in magazyn.listaWToku()) {
                val koniec = wiersz.checkpointMs
                val duration = durationSeconds(wiersz.przebieg, wiersz.poczatekMs, koniec)
                val podsumowanie = SummaryCalculator.make(
                    from = wiersz.przebieg,
                    durationSeconds = duration,
                    kodyNaStarcie = wiersz.podsumowanie.kodyNaStarcie,
                    kodyNaKoncu = emptyList()
                )
                magazyn.zamknij(
                    id = wiersz.id,
                    koniecMs = koniec,
                    status = StatusPrzejazdu.Odzyskany,
                    podsumowanie = podsumowanie,
                    przebieg = wiersz.przebieg
                )
                wynik.add(checkNotNull(magazyn.czytaj(wiersz.id)))
            }
            return wynik
        }

        private fun durationSeconds(track: TrackBlob, startMs: Long, endMs: Long): Double {
            val trackDuration = track.series.flatMap { it.times }.maxOrNull()?.toDouble() ?: 0.0
            val wall = maxOf(0.0, (endMs - startMs) / 1000.0)
            return maxOf(trackDuration, wall)
        }
    }

    private data class Snapshot(val id: String, val track: TrackBlob, val kodyStart: List<String>, val startMs: Long)

    private fun zapiszZamkniecie(
        snapshot: Snapshot,
        koniecMs: Long,
        status: StatusPrzejazdu,
        kodyNaKoncu: List<String>
    ): Przejazd {
        val duration = durationSeconds(snapshot.track, snapshot.startMs, koniecMs)
        val podsumowanie = SummaryCalculator.make(
            from = snapshot.track,
            durationSeconds = duration,
            kodyNaStarcie = snapshot.kodyStart,
            kodyNaKoncu = kodyNaKoncu
        )
        magazyn.zamknij(snapshot.id, koniecMs, status, podsumowanie, snapshot.track)
        return checkNotNull(magazyn.czytaj(snapshot.id))
    }
}
