package pl.i40.android.storage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.i40.android.acquisition.SampleTick
import pl.i40.android.obd.DecodedPid
import pl.i40.android.obd.MultiPidReading

class SessionRecorderTest {
    @Test
    fun startWstawiaWierszWTokuCzytelnyZMagazynu() {
        val magazyn = PamiecPrzejazdow()
        var teraz = 1_700_000_000_000L
        val recorder = SessionRecorder(magazyn, terazMs = { teraz }, vin = "KMHLC41DAFU066558")
        val id = recorder.start(kodyNaStarcie = listOf("P0171"))
        val wiersz = checkNotNull(magazyn.czytaj(id))
        assertEquals(StatusPrzejazdu.WToku, wiersz.status)
        assertNull(wiersz.koniecMs)
        assertEquals("KMHLC41DAFU066558", wiersz.vin)
        assertTrue(wiersz.przebieg.series.isEmpty())
        assertEquals(listOf("P0171"), wiersz.podsumowanie.kodyNaStarcie)
    }

    @Test
    fun checkpointZapisujeRosnacyPrzebiegPrzyWToku() {
        val magazyn = PamiecPrzejazdow()
        var teraz = 1_000L
        val recorder = SessionRecorder(magazyn, terazMs = { teraz }, checkpointCoMs = 30_000L)
        val id = recorder.start()
        recorder.append(tickPredkosc(0.0, 36.0))
        teraz = 31_000L
        recorder.checkpointJesliPotrzeba()
        val poPierwszym = checkNotNull(magazyn.czytaj(id))
        assertEquals(StatusPrzejazdu.WToku, poPierwszym.status)
        assertEquals(1, poPierwszym.przebieg.series.first { it.pid == 0x0D }.times.size)

        recorder.append(tickPredkosc(1.0, 40.0))
        teraz = 62_000L
        recorder.checkpointJesliPotrzeba()
        val poDrugim = checkNotNull(magazyn.czytaj(id))
        assertEquals(2, poDrugim.przebieg.series.first { it.pid == 0x0D }.times.size)
        assertEquals(StatusPrzejazdu.WToku, poDrugim.status)
    }

    @Test
    fun stopZamykaZPodsumowaniem() {
        val magazyn = PamiecPrzejazdow()
        var teraz = 0L
        val recorder = SessionRecorder(magazyn, terazMs = { teraz })
        recorder.start()
        recorder.append(tickPredkosc(0.0, 36.0))
        recorder.append(tickPredkosc(1.0, 36.0))
        teraz = 2_000L
        val zapisany = recorder.stop(kodyNaKoncu = listOf("P0301"))
        assertEquals(StatusPrzejazdu.Zamkniety, zapisany.status)
        assertEquals(listOf("P0301"), zapisany.podsumowanie.kodyNaKoncu)
        assertNull(zapisany.podsumowanie.paliwoL)
        assertEquals(36.0, zapisany.podsumowanie.maxPredkoscKmh)
    }

    @Test
    fun odzyskanieZamykaWTokuZPustymKodyNaKoncu() {
        val magazyn = PamiecPrzejazdow()
        var teraz = 0L
        val recorder = SessionRecorder(magazyn, terazMs = { teraz })
        val id = recorder.start(kodyNaStarcie = listOf("P0171"))
        recorder.append(tickPredkosc(0.0, 50.0))
        recorder.append(tickPredkosc(2.0, 50.0))
        teraz = 30_000L
        recorder.checkpointJesliPotrzeba()

        val odzyskane = SessionRecorder.odzyskajPrzerwane(magazyn)
        assertEquals(1, odzyskane.size)
        val wiersz = odzyskane[0]
        assertEquals(id, wiersz.id)
        assertEquals(StatusPrzejazdu.Odzyskany, wiersz.status)
        assertEquals(listOf("P0171"), wiersz.podsumowanie.kodyNaStarcie)
        assertTrue(wiersz.podsumowanie.kodyNaKoncu.isEmpty())
        assertNull(wiersz.podsumowanie.paliwoL)
        assertEquals(50.0, wiersz.podsumowanie.maxPredkoscKmh)
        assertEquals(30_000L, wiersz.koniecMs)
    }

    @Test
    fun podwojnyStartOdrzucany() {
        val recorder = SessionRecorder(PamiecPrzejazdow(), terazMs = { 0L })
        recorder.start()
        assertThrows<SessionRecorderError> { recorder.start() }
    }

    @Test
    fun zapisujeKodStatusuUkladuPaliwowego0103() {
        val magazyn = PamiecPrzejazdow()
        val recorder = SessionRecorder(magazyn, terazMs = { 0L })
        recorder.start()
        recorder.append(
            SampleTick(
                kind = SampleTick.Kind.Medium,
                time = 1.0,
                readings = listOf(MultiPidReading(0x03, listOf(2, 0), DecodedPid.Code(2)))
            )
        )
        val seria = recorder.currentTrack().series.first { it.pid == 0x03 }
        assertEquals(2.0f, seria.values.first())
    }

    private fun tickPredkosc(t: Double, kmh: Double) = SampleTick(
        kind = SampleTick.Kind.Hot,
        time = t,
        readings = listOf(MultiPidReading(0x0D, listOf(kmh.toInt()), DecodedPid.Numeric(kmh)))
    )
}
