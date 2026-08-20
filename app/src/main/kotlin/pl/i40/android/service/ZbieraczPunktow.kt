package pl.i40.android.service

import pl.i40.android.acquisition.SampleStream
import pl.i40.android.storage.PunktOdniesienia
import pl.i40.android.storage.SummaryCalculator

/**
 * Okno jałowego rozgrzanego — sekcja 7 warstwy odniesienia.
 * Minimalne okno to jeden obieg poziomu wolnego ([SampleStream.SLOW_EVERY_N] cykli gorących),
 * nie próg w sekundach.
 */
class ZbieraczPunktow(private val terazMs: () -> Long, private val nowyId: () -> String) {
    private var otwarte: Boolean = false
    private val serie = mutableMapOf<Int, MutableList<Float>>()
    private var cykle: Int = 0

    fun naCyklGoracy(jalowy: Boolean, vin: String?, wartosci: Map<Int, Double>): PunktOdniesienia? {
        if (jalowy) {
            if (!otwarte) {
                otwarte = true
                serie.clear()
                cykle = 0
            }
            cykle += 1
            for ((pid, v) in wartosci) {
                serie.getOrPut(pid) { mutableListOf() }.add(v.toFloat())
            }
            return null
        }
        return zamknij(vin)
    }

    fun zakonczSesje(vin: String?): PunktOdniesienia? = zamknij(vin)

    private fun zamknij(vin: String?): PunktOdniesienia? {
        if (!otwarte) return null
        otwarte = false
        val n = cykle
        val kopia = serie.toMap()
        serie.clear()
        cykle = 0
        if (vin == null || n < SampleStream.SLOW_EVERY_N) return null
        val odczyty = mutableMapOf<Int, Double>()
        for ((pid, wartosci) in kopia) {
            val m = SummaryCalculator.mediana(wartosci) ?: continue
            odczyty[pid] = m
        }
        if (odczyty.isEmpty()) return null
        return PunktOdniesienia(
            id = nowyId(),
            kiedyMs = terazMs(),
            vin = vin,
            stan = STAN_JALOWY_ROZGRZANY,
            zrodlo = ZRODLO_PRZEJAZD,
            probek = n,
            odczyty = odczyty
        )
    }

    companion object {
        const val STAN_JALOWY_ROZGRZANY = "jalowy_rozgrzany"
        const val ZRODLO_PRZEJAZD = "przejazd"
    }
}
