package pl.i40.android.transport

/** Jedna odpowiedź atrapy na konkretne polecenie ELM (tekst bez końcowego CR). */
data class MockScriptEntry(
    /** Polecenie bez CR, porównanie bez względu na wielkość liter / spacje. */
    val command: String,
    /** Kawałki jak notyfikacje BLE — kolejność i treść jak w nagraniu. */
    val chunks: List<ByteArray>,
    /** Czy nagranie zawierało echo polecenia (stan ATE w momencie zapisu). */
    val recordedWithEcho: Boolean = false,
) {
    constructor(command: String, response: String, recordedWithEcho: Boolean = false) : this(
        command = command,
        chunks = listOf(response.toByteArray(Charsets.UTF_8)),
        recordedWithEcho = recordedWithEcho,
    )
}
