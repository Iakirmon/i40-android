package pl.i40.android.elm

/**
 * Komunikaty tekstowe adaptera ELM327 — przychodzą tam, gdzie oczekujemy danych.
 * Rozpoznawane po dokładnej treści linii, nie po podłańcuchu (sekcja 8.3).
 */
enum class ElmMessage {
    NoData,
    Searching,
    UnableToConnect,
    BusInitError,
    CanError,
    Stopped,
    BufferFull,
    UnknownCommand,
    ;

    enum class Reaction {
        MarkUnavailableAndContinue,
        WaitForData,
        Abort,
        Retry,
        LogBug,
    }

    val reaction: Reaction
        get() = when (this) {
            NoData -> Reaction.MarkUnavailableAndContinue
            Searching -> Reaction.WaitForData
            UnableToConnect, BusInitError, CanError -> Reaction.Abort
            Stopped, BufferFull -> Reaction.Retry
            UnknownCommand -> Reaction.LogBug
        }

    val polishDescription: String
        get() = when (this) {
            NoData -> "Auto nie obsługuje PID-u lub nie odpowiedziało"
            Searching -> "Trwa negocjacja protokołu"
            UnableToConnect ->
                "Brak łączności z magistralą — zapłon wyłączony lub adapter nie w gnieździe"
            BusInitError, CanError -> "Problem magistrali"
            Stopped -> "Operacja przerwana"
            BufferFull -> "Przepełnienie bufora adaptera"
            UnknownCommand -> "Adapter nie zrozumiał polecenia"
        }
}

object ElmResponse {
    fun classify(line: String): ElmMessage? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null
        return when (trimmed.uppercase()) {
            "NO DATA" -> ElmMessage.NoData
            "SEARCHING..." -> ElmMessage.Searching
            "UNABLE TO CONNECT" -> ElmMessage.UnableToConnect
            "BUS INIT: ERROR" -> ElmMessage.BusInitError
            "CAN ERROR" -> ElmMessage.CanError
            "STOPPED" -> ElmMessage.Stopped
            "BUFFER FULL" -> ElmMessage.BufferFull
            "?" -> ElmMessage.UnknownCommand
            else -> null
        }
    }

    fun isAdapterMessage(line: String): Boolean = classify(line) != null

    fun messages(inText: String): List<ElmMessage> = inText.split(Regex("\\R")).mapNotNull { classify(it) }
}
