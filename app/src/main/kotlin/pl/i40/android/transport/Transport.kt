package pl.i40.android.transport

import kotlinx.coroutines.flow.Flow

/**
 * Jedyny kontrakt między sprzętem a resztą aplikacji. Sekcja 8.1 specu.
 *
 * `BluetoothSocket`, `BluetoothGatt`, `BluetoothAdapter` i `java.net.Socket`
 * nie mają prawa wystąpić poza pakietem `transport/`.
 */
interface Transport {
    suspend fun open()

    suspend fun close()

    suspend fun write(bytes: ByteArray)

    val chunks: Flow<ByteArray>

    val disconnects: Flow<Unit>
}
