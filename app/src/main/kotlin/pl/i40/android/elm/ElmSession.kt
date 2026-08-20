package pl.i40.android.elm

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import pl.i40.android.transport.Transport

/**
 * Sesja ELM327: jedno polecenie w powietrzu, ramkowanie na `>`, timeouty i ponowienia.
 *
 * Kolejność gwarantuje [Channel] — `Mutex` szereguje dostęp, ale nie jest kolejką FIFO.
 */
class ElmSession(
    private val transport: Transport,
    private val timeout: Duration = 5.seconds,
    private val maxRetries: Int = 2
) {
    private data class Zadanie(val command: String, val wynik: CompletableDeferred<String>)

    private val zadania = Channel<Zadanie>(Channel.UNLIMITED)
    private val monitor = Any()
    private val buffer = StringBuilder()
    private var waiter: CompletableDeferred<String>? = null
    private var started = false

    private var pumpJob: Job? = null
    private var chunkJob: Job? = null
    private var disconnectJob: Job? = null

    fun start(scope: CoroutineScope) {
        synchronized(monitor) {
            if (started) return
            started = true
        }
        chunkJob = scope.launch {
            transport.chunks.collect { feed(it) }
        }
        disconnectJob = scope.launch {
            transport.disconnects.collect { failWaiter(ElmSessionError.Disconnected) }
        }
        pumpJob = scope.launch {
            for (zadanie in zadania) {
                try {
                    zadanie.wynik.complete(execute(zadanie.command))
                } catch (e: Throwable) {
                    zadanie.wynik.completeExceptionally(e)
                }
            }
        }
    }

    fun stop() {
        zadania.close()
        pumpJob?.cancel()
        chunkJob?.cancel()
        disconnectJob?.cancel()
        failWaiter(ElmSessionError.Disconnected)
        synchronized(monitor) {
            started = false
            buffer.setLength(0)
        }
    }

    /** Wysyła polecenie (bez CR) i zwraca tekst odpowiedzi **bez** końcowego `>`. */
    suspend fun send(command: String): String {
        val isStarted = synchronized(monitor) { started }
        if (!isStarted) throw ElmSessionError.NotStarted
        val deferred = CompletableDeferred<String>()
        zadania.send(Zadanie(command, deferred))
        return deferred.await()
    }

    private suspend fun execute(command: String): String {
        var lastError: Throwable = ElmSessionError.Timeout(command)
        val attempts = maxRetries + 1
        repeat(attempts) { attemptIndex ->
            try {
                return sendOnce(command)
            } catch (error: ElmSessionError) {
                lastError = error
                when (error) {
                    is ElmSessionError.Timeout -> {
                        if (attemptIndex == attempts - 1) throw error
                    }
                    is ElmSessionError.Aborted -> {
                        val retryable =
                            error.komunikat == ElmMessage.Stopped || error.komunikat == ElmMessage.BufferFull
                        if (!retryable || attemptIndex == attempts - 1) throw error
                    }
                    is ElmSessionError.Disconnected,
                    is ElmSessionError.NotStarted,
                    is ElmSessionError.WriteFailed,
                    -> throw error
                }
            }
        }
        throw lastError
    }

    private suspend fun sendOnce(command: String): String {
        synchronized(monitor) { buffer.setLength(0) }
        val payload = "$command\r".toByteArray(Charsets.UTF_8)
        val deferred = CompletableDeferred<String>()

        val already = synchronized(monitor) {
            val at = buffer.indexOf(">")
            if (at >= 0) {
                val complete = buffer.substring(0, at)
                buffer.delete(0, at + 1)
                complete
            } else {
                waiter = deferred
                null
            }
        }

        val text = if (already != null) {
            already
        } else {
            try {
                withTimeout(timeout) {
                    try {
                        transport.write(payload)
                    } catch (e: Exception) {
                        val failed = ElmSessionError.WriteFailed(e.message ?: e.toString())
                        failWaiter(failed)
                        throw failed
                    }
                    deferred.await()
                }
            } catch (e: TimeoutCancellationException) {
                val timeoutError = ElmSessionError.Timeout(command)
                failWaiter(timeoutError)
                throw timeoutError
            }
        }

        abortMessage(text)?.let { throw ElmSessionError.Aborted(it) }
        return text
    }

    private fun abortMessage(text: String): ElmMessage? {
        for (message in ElmResponse.messages(text)) {
            when (message) {
                ElmMessage.Stopped,
                ElmMessage.BufferFull,
                ElmMessage.UnableToConnect,
                ElmMessage.BusInitError,
                ElmMessage.CanError,
                -> return message
                ElmMessage.NoData, ElmMessage.Searching, ElmMessage.UnknownCommand -> continue
            }
        }
        return null
    }

    private fun feed(bytes: ByteArray) {
        val piece = bytes.toString(Charsets.UTF_8)
        var toComplete: Pair<CompletableDeferred<String>, String>? = null
        synchronized(monitor) {
            buffer.append(piece)
            val w = waiter
            val at = buffer.indexOf(">")
            // Bez waitera nie zjadamy ramek — zapis często kończy się zanim
            // sendOnce zdąży zawiesić się na oczekiwaniu (wyścig z BLE/atrapą).
            if (w != null && at >= 0) {
                val complete = buffer.substring(0, at)
                buffer.delete(0, at + 1)
                waiter = null
                toComplete = w to complete
            }
        }
        toComplete?.let { (w, complete) -> w.complete(complete) }
    }

    private fun failWaiter(error: Throwable) {
        val w = synchronized(monitor) {
            val current = waiter
            waiter = null
            buffer.setLength(0)
            current
        }
        w?.completeExceptionally(error)
    }
}
