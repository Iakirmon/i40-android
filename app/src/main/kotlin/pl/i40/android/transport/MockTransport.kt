package pl.i40.android.transport

import kotlin.time.Duration.Companion.nanoseconds
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

sealed class MockTransportError : Exception() {
    data object NotOpen : MockTransportError()
}

/**
 * Atrapa transportu: odtwarza zapisany skrypt odpowiedzi, bez adaptera i bez auta.
 *
 * Pięć obowiązkowych zachowań z sekcji 8.1: porównanie bez wielkości liter i spacji,
 * symulacja stanu echa, licznik powtórzeń, `?` na nieznane polecenie, `drop()` ≠ `close()`.
 */
class MockTransport(private val script: List<MockScriptEntry>, private val timeScale: Double = 0.0) : Transport {
    private var chunksChannel = Channel<ByteArray>(Channel.UNLIMITED)
    private var disconnectsChannel = Channel<Unit>(Channel.UNLIMITED)

    override val chunks: Flow<ByteArray>
        get() = chunksChannel.receiveAsFlow()

    override val disconnects: Flow<Unit>
        get() = disconnectsChannel.receiveAsFlow()

    private var openFlag = false
    private var echoEnabled = true
    private var inputBuffer = ByteArray(0)
    private val useCounts = mutableMapOf<String, Int>()

    /** Licznik `close()` — testy ścieżki „nie rozłączaj po przeglądzie”. */
    var closeCount: Int = 0
        private set

    val isOpen: Boolean
        get() = openFlag

    override suspend fun open() {
        rotateStreams()
        openFlag = true
        echoEnabled = true
        inputBuffer = ByteArray(0)
        useCounts.clear()
    }

    override suspend fun close() {
        closeCount += 1
        openFlag = false
        inputBuffer = ByteArray(0)
    }

    /**
     * Zerwanie łącza w trakcie sesji — budzi `disconnects` (w przeciwieństwie do `close()`).
     */
    fun drop() {
        openFlag = false
        disconnectsChannel.trySend(Unit)
    }

    override suspend fun write(bytes: ByteArray) {
        if (!openFlag) throw MockTransportError.NotOpen
        inputBuffer += bytes

        while (true) {
            val cr = inputBuffer.indexOf(0x0D.toByte())
            if (cr < 0) break
            val commandData = inputBuffer.copyOfRange(0, cr)
            inputBuffer = inputBuffer.copyOfRange(cr + 1, inputBuffer.size)
            val command = commandData.toString(Charsets.UTF_8).trim()
            if (command.isNotEmpty()) {
                respond(command)
            }
        }
    }

    private suspend fun respond(command: String) {
        if (!openFlag) return
        applyAdapterState(command)
        val pieces = chunksFor(command)
        for (piece in pieces) {
            if (timeScale > 0) {
                delay((timeScale * 1_000_000).toLong().nanoseconds)
            }
            if (!openFlag) return
            chunksChannel.trySend(piece)
        }
    }

    private fun chunksFor(command: String): List<ByteArray> {
        val key = normalize(command)
        val matches = script.indices.filter { normalize(script[it].command) == key }
        if (matches.isEmpty()) {
            return withEcho(unknownReply, command, recordedWithEcho = false)
        }
        val used = useCounts[key] ?: 0
        useCounts[key] = used + 1
        val index = matches[minOf(used, matches.size - 1)]
        val entry = script[index]
        return withEcho(entry.chunks, command, entry.recordedWithEcho)
    }

    private val unknownReply: List<ByteArray>
        get() = listOf("?\r\r>".toByteArray(Charsets.UTF_8))

    private fun withEcho(chunks: List<ByteArray>, command: String, recordedWithEcho: Boolean): List<ByteArray> {
        val copy = chunks.toMutableList()
        val prefix = "$command\r".toByteArray(Charsets.UTF_8)

        if (recordedWithEcho && !echoEnabled) {
            val first = copy.firstOrNull() ?: return copy
            if (first.contentEquals(prefix)) {
                copy.removeAt(0)
            } else if (first.startsWithPrefix(prefix)) {
                copy[0] = first.copyOfRange(prefix.size, first.size)
            }
        } else if (!recordedWithEcho && echoEnabled) {
            if (copy.isEmpty()) {
                copy.add(prefix)
            } else {
                copy[0] = prefix + copy[0]
            }
        }
        return copy
    }

    private fun applyAdapterState(command: String) {
        when (normalize(command)) {
            "ATZ", "ATD", "ATE1" -> echoEnabled = true
            "ATE0" -> echoEnabled = false
        }
    }

    private fun normalize(command: String): String = command.uppercase().replace(" ", "")

    private fun rotateStreams() {
        chunksChannel.close()
        disconnectsChannel.close()
        chunksChannel = Channel(Channel.UNLIMITED)
        disconnectsChannel = Channel(Channel.UNLIMITED)
    }
}

private fun ByteArray.startsWithPrefix(prefix: ByteArray): Boolean {
    if (size < prefix.size) return false
    return copyOfRange(0, prefix.size).contentEquals(prefix)
}
