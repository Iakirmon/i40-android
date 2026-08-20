package pl.i40.android.ui

/**
 * Preferencja motywu poza usługą nagrywania — §3.6.
 * Zmiana wyboru nie dotyka [pl.i40.android.service.StanZywy.nagrywa].
 */
data class StanUstawienWygladu(val wyborMotywu: WyborMotywu = WyborMotywu.Noc, val sygnalDzienZRadia: Boolean? = null) {
    val motyw: MotywI40 get() = RozstrzyganieMotywu.motyw(wyborMotywu, sygnalDzienZRadia)

    fun zWyboorem(wybor: WyborMotywu): StanUstawienWygladu = copy(wyborMotywu = wybor)
}
