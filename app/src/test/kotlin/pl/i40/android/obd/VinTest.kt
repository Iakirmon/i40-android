package pl.i40.android.obd

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class VinTest {
    private val liveVin = "014\r0:4902014B4D48\r1:4C433431444146\r2:55303636353538\r>"

    @Test
    fun skladaVinZRamek0902() {
        assertEquals("KMHLC41DAFU066558", Mode09.decodeVin(liveVin))
    }

    @Test
    fun kalibracjaIEcu() {
        val cal = "013\r0:490401474756\r1:462D4545354146\r2:53303136303000\r>"
        assertEquals("GGVF-EE5AFS01600", Mode09.decodeCalibrationId(cal))
        val ecu = "017\r0:490A0145434D\r1:002D456E67696E\r2:65436F6E74726F\r3:6C000000000000\r>"
        assertEquals("ECM-EngineControl", Mode09.decodeEcuName(ecu))
    }

    @Test
    fun pewnePolaZVina() {
        val info = VinInfo("KMHLC41DAFU066558")
        assertEquals("Hyundai Motor Company, Korea", info.manufacturer)
        assertEquals(2015, info.modelYear)
        assertEquals("Ulsan", info.plant)
        assertNull(VinInfo.modelYear('U'))
        assertEquals("Ulsan", VinInfo.plant('U'))
    }
}
