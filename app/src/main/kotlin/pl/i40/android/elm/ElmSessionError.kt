package pl.i40.android.elm

sealed class ElmSessionError : Exception() {
    data class Timeout(val command: String) : ElmSessionError()
    data object Disconnected : ElmSessionError()
    data object NotStarted : ElmSessionError()
    data class Aborted(val komunikat: ElmMessage) : ElmSessionError()
    data class WriteFailed(val reason: String) : ElmSessionError()
}
