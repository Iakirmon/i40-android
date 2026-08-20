package pl.i40.android.service

enum class StanPrzejazdu { Rozlaczony, Czuwanie, Nagrywa, Zamykanie }

sealed class ZdarzeniePrzejazdu {
    data object Polaczono : ZdarzeniePrzejazdu()
    data object Rozlaczono : ZdarzeniePrzejazdu()
    data class Obroty(val rpm: Double, val terazMs: Long) : ZdarzeniePrzejazdu()
    data class Runtime(val seconds: Double) : ZdarzeniePrzejazdu()
    data object ZadaniePrzegladu : ZdarzeniePrzejazdu()
    data object BrakMiejsca : ZdarzeniePrzejazdu()
    data object ZamkniecieUkonczone : ZdarzeniePrzejazdu()
}

enum class AkcjaPrzejazdu {
    Polacz,
    StartSesji,
    ZamknijSesje,
    ZamknijIOtworzNowa,
    UruchomPrzeglad,
}

data class WynikPrzejscia(val stan: StanPrzejazdu, val akcje: List<AkcjaPrzejazdu>)

/**
 * Czysta maszyna stanów przejazdu — zero Androida. Progi z §10.6 i §11.2.
 */
class TripStateMachine {
    var stan: StanPrzejazdu = StanPrzejazdu.Rozlaczony
        private set

    private var rpmZeroOdMs: Long? = null
    private var lastRuntime: Double? = null
    private var przegladPoZamknieciu = false
    private var nowaSesjaPoZamknieciu = false

    fun on(zdarzenie: ZdarzeniePrzejazdu): WynikPrzejscia {
        val akcje = mutableListOf<AkcjaPrzejazdu>()
        when (zdarzenie) {
            ZdarzeniePrzejazdu.Polaczono -> if (stan == StanPrzejazdu.Rozlaczony) {
                stan = StanPrzejazdu.Czuwanie
            }
            ZdarzeniePrzejazdu.Rozlaczono -> when (stan) {
                StanPrzejazdu.Nagrywa -> zamknij(akcje, otworzNowa = false)
                StanPrzejazdu.Zamykanie -> Unit
                else -> {
                    stan = StanPrzejazdu.Rozlaczony
                    akcje.add(AkcjaPrzejazdu.Polacz)
                }
            }
            is ZdarzeniePrzejazdu.Obroty -> obroty(zdarzenie, akcje)
            is ZdarzeniePrzejazdu.Runtime -> runtime(zdarzenie, akcje)
            ZdarzeniePrzejazdu.ZadaniePrzegladu -> if (stan == StanPrzejazdu.Nagrywa) {
                przegladPoZamknieciu = true
                zamknij(akcje, otworzNowa = false)
            }
            ZdarzeniePrzejazdu.BrakMiejsca -> if (stan == StanPrzejazdu.Nagrywa) {
                zamknij(akcje, otworzNowa = false)
            }
            ZdarzeniePrzejazdu.ZamkniecieUkonczone -> if (stan == StanPrzejazdu.Zamykanie) {
                if (nowaSesjaPoZamknieciu) {
                    nowaSesjaPoZamknieciu = false
                    stan = StanPrzejazdu.Nagrywa
                    akcje.add(AkcjaPrzejazdu.StartSesji)
                } else {
                    stan = StanPrzejazdu.Czuwanie
                    if (przegladPoZamknieciu) {
                        przegladPoZamknieciu = false
                        akcje.add(AkcjaPrzejazdu.UruchomPrzeglad)
                    }
                }
            }
        }
        return WynikPrzejscia(stan, akcje)
    }

    private fun obroty(zdarzenie: ZdarzeniePrzejazdu.Obroty, akcje: MutableList<AkcjaPrzejazdu>) {
        when (stan) {
            StanPrzejazdu.Czuwanie -> if (zdarzenie.rpm > 0) {
                stan = StanPrzejazdu.Nagrywa
                rpmZeroOdMs = null
                akcje.add(AkcjaPrzejazdu.StartSesji)
            }
            StanPrzejazdu.Nagrywa -> if (zdarzenie.rpm > 0) {
                rpmZeroOdMs = null
            } else {
                val od = rpmZeroOdMs
                if (od == null) {
                    rpmZeroOdMs = zdarzenie.terazMs
                } else if (zdarzenie.terazMs - od >= POSTOJ_MS) {
                    zamknij(akcje, otworzNowa = false)
                }
            }
            else -> Unit
        }
    }

    private fun runtime(zdarzenie: ZdarzeniePrzejazdu.Runtime, akcje: MutableList<AkcjaPrzejazdu>) {
        if (stan != StanPrzejazdu.Nagrywa) {
            lastRuntime = zdarzenie.seconds
            return
        }
        val previous = lastRuntime
        lastRuntime = zdarzenie.seconds
        if (previous != null && zdarzenie.seconds + 2 < previous) {
            nowaSesjaPoZamknieciu = true
            zamknij(akcje, otworzNowa = true)
        }
    }

    private fun zamknij(akcje: MutableList<AkcjaPrzejazdu>, otworzNowa: Boolean) {
        stan = StanPrzejazdu.Zamykanie
        rpmZeroOdMs = null
        if (otworzNowa) {
            akcje.add(AkcjaPrzejazdu.ZamknijIOtworzNowa)
        } else {
            akcje.add(AkcjaPrzejazdu.ZamknijSesje)
        }
    }

    companion object {
        const val POSTOJ_MS = 30_000L
        const val POLACZ_CO_MS = 5_000L
        const val CZUL_RPM_CO_MS = 2_000L
    }
}

/** Siatka faz: gorący zawsze, A `n%4==0`, B `n%10==5`, C `n%20==13`, tryb 03 `n%200==150`. */
object PetlaFaz {
    fun szybkiA(n: Int): Boolean = n % 4 == 0
    fun sredniB(n: Int): Boolean = n % 10 == 5
    fun wolnyC(n: Int): Boolean = n % 20 == 13
    fun kody03(n: Int): Boolean = n % 200 == 150
    fun liczbaZapytan(n: Int): Int {
        var q = 1
        if (szybkiA(n)) q += 1
        if (sredniB(n)) q += 1
        if (wolnyC(n)) q += 1
        if (kody03(n)) q += 1
        return q
    }
}
