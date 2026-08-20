package pl.i40.android.service

import pl.i40.android.acquisition.SampleStream

/**
 * Wejście w tryb porównania panelu Podstawowy — sekcja 10.2.
 * Wejście po pełnym obiegu poziomu wolnego, wyjście natychmiast.
 */
class LicznikWejsciaPorownania {
    private var cykleJalowego: Int = 0
    private var wTrybie: Boolean = false

    fun naCyklGoracy(jalowy: Boolean): Boolean {
        if (!jalowy) {
            cykleJalowego = 0
            wTrybie = false
            return false
        }
        cykleJalowego += 1
        if (cykleJalowego >= SampleStream.SLOW_EVERY_N) wTrybie = true
        return wTrybie
    }
}
