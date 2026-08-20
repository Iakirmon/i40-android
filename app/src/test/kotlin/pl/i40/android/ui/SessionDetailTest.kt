package pl.i40.android.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pl.i40.android.charts.SeriesLookup
import pl.i40.android.rules.PasmaOdniesienia
import pl.i40.android.storage.PodsumowaniePrzejazdu
import pl.i40.android.storage.TrackBlob

class SessionDetailTest {
    @Test
    fun valueAtBierzeOstatniaProbkeNiePozniejsza() {
        val times = listOf(0f, 1f, 2f, 3f)
        val values = listOf(10f, 20f, 30f, 40f)
        assertEquals(20f, SeriesLookup.value(at = 1.5f, times, values))
        assertEquals(30f, SeriesLookup.value(at = 2.0f, times, values))
        assertNull(SeriesLookup.value(at = -1f, times, values))
        assertEquals(40f, SeriesLookup.value(at = 99f, times, values))
    }

    @Test
    fun suwakWspolnyDlaSerii() {
        val track = TrackBlob()
        track.append(0x0C, 0f, 800f)
        track.append(0x0C, 1f, 2000f)
        track.append(0x06, 0f, -5f)
        track.append(0x06, 1f, 8f)
        val model = ModelSzczegolowSesji(PodsumowaniePrzejazdu(czasTrwaniaS = 1.0, liczbaProbek = 4), track)
        model.czasSuwaka = 0.5f
        val rpm = model.serie.first { it.pid == 0x0C }
        val stft = model.serie.first { it.pid == 0x06 }
        assertEquals(800f, model.wartoscPrzySuwaku(rpm))
        assertEquals(-5f, model.wartoscPrzySuwaku(stft))
        model.czasSuwaka = 1.0f
        assertEquals(2000f, model.wartoscPrzySuwaku(rpm))
        assertEquals(8f, model.wartoscPrzySuwaku(stft))
    }

    @Test
    fun setScrubZFrakcjiX() {
        val track = TrackBlob()
        track.append(0x0C, 0f, 1f)
        track.append(0x0C, 10f, 2f)
        val model = ModelSzczegolowSesji(PodsumowaniePrzejazdu(czasTrwaniaS = 10.0), track)
        model.ustawSuwak(50f, 100f)
        assertEquals(5f, model.czasSuwaka, 0.01f)
    }

    @Test
    fun decymacjaWWidocznymOknie() {
        val track = TrackBlob()
        for (i in 0 until 200) {
            track.append(0x0C, i.toFloat(), i.toFloat())
        }
        val model = ModelSzczegolowSesji(PodsumowaniePrzejazdu(czasTrwaniaS = 200.0), track)
        model.okno = 50f..100f
        model.szerokoscWykresu = 20
        val points = model.punktyZdecymowane(model.serie[0])
        assertTrue(points.isNotEmpty())
        assertTrue(points.all { it.time >= 50f && it.time <= 100f })
        assertTrue(points.size <= 40)
    }

    @Test
    fun suwakNaRzadkimPasmieBierzeNajblizszaCzasowo() {
        val track = TrackBlob()
        for (i in 0..4) {
            track.append(0x0C, i * 0.25f, 800f + i)
        }
        track.append(0x23, 0f, 3800f)
        track.append(0x23, 1f, 14800f)
        val model = ModelSzczegolowSesji(PodsumowaniePrzejazdu(czasTrwaniaS = 1.0, liczbaProbek = 7), track)
        model.czasSuwaka = 0.7f
        val szyna = model.stosWykresow.first { it.pid == 0x23 }
        val bar = model.wartoscPrzySuwaku(szyna)
        assertEquals(PasmaOdniesienia.kpaNaBar(14800.0).toFloat(), bar!!, 0.01f)
        val lastNotLater = SeriesLookup.value(at = 0.7f, times = listOf(0f, 1f), values = listOf(3800f, 14800f))
        assertEquals(3800f, lastNotLater)
        assertTrue(bar != PasmaOdniesienia.kpaNaBar(3800.0).toFloat())
    }
}
