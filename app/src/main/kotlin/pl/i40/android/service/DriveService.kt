package pl.i40.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.i40.android.R
import pl.i40.android.acquisition.SampleStream
import pl.i40.android.alerts.AlertEngine
import pl.i40.android.elm.ElmSession
import pl.i40.android.storage.DriveSessionDao
import pl.i40.android.storage.SessionRecorder
import pl.i40.android.transport.MockI40Script
import pl.i40.android.transport.MockTransport

/**
 * Usługa pierwszoplanowa i właściciel stanu żywego (§11.4).
 * Bufory, model oleju i karencje alarmów mieszkają tu, nie w ViewModel.
 * Transport: atrapa do STYK 1; prawdziwy Bluetooth w etapie 9.
 */
class DriveService : Service() {
    inner class Lokalny : android.os.Binder() {
        fun usluga(): DriveService = this@DriveService
    }

    private val binder = Lokalny()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Default)

    private val _stan = MutableStateFlow(StanPrzejazdu.Rozlaczony)
    val stan: StateFlow<StanPrzejazdu> = _stan.asStateFlow()

    val zywy = StanZywy()
    private val _migawka = MutableStateFlow(MigawkaZywego())
    val migawka: StateFlow<MigawkaZywego> = _migawka.asStateFlow()

    val alertEngine = AlertEngine()
    val maszyna = TripStateMachine()

    private var wakeLock: PowerManager.WakeLock? = null
    private var petlaJob: Job? = null

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        SessionRecorder.odzyskajPrzerwane(DriveSessionDao(applicationContext))
        utworzKanal()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = Notification.Builder(this, KANAL)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.drive_notification))
            .setOngoing(true)
            .build()
        startForeground(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        )
        if (petlaJob == null) {
            petlaJob = scope.launch { petlaAtrapy() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        petlaJob?.cancel()
        job.cancel()
        zwolnijWakeLock()
        super.onDestroy()
    }

    fun zatrzymajNaZadanie() {
        zywy.nagrywa = false
        wyjdzZNagrywania()
        publikuj()
    }

    fun listaPrzejazdow(): List<pl.i40.android.storage.Przejazd> = DriveSessionDao(applicationContext).lista()

    fun usunPrzejazd(id: String) {
        DriveSessionDao(applicationContext).usun(id)
    }

    fun wejdzWNagrywanie() {
        _stan.value = StanPrzejazdu.Nagrywa
        zywy.nagrywa = true
        if (wakeLock == null) {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "i40:nagrywa").also {
                it.setReferenceCounted(false)
                it.acquire()
            }
        }
        publikuj()
    }

    fun wyjdzZNagrywania() {
        zwolnijWakeLock()
        _stan.value = StanPrzejazdu.Czuwanie
        zywy.nagrywa = false
        publikuj()
    }

    private suspend fun petlaAtrapy() {
        val transport = MockTransport(MockI40Script.make(), timeScale = 0.0)
        val session = ElmSession(transport, timeout = 5.seconds, maxRetries = 1)
        try {
            transport.open()
            session.start(scope)
            for (cmd in listOf("ATZ", "ATE0", "ATL0", "ATS0", "ATH0", "ATSP0")) {
                session.send(cmd)
            }
            maszyna.on(ZdarzeniePrzejazdu.Polaczono)
            _stan.value = maszyna.stan
            publikuj()
            val stream = SampleStream(session)
            stream.events().collect { tick ->
                zywy.zastosuj(tick)
                zywy.measuredHz = stream.measuredHotHz
                zywy.totalQueries = stream.totalQueries
                val rpm = zywy.wartosc(0x0C)
                if (rpm != null) {
                    val wynik = maszyna.on(ZdarzeniePrzejazdu.Obroty(rpm, System.currentTimeMillis()))
                    _stan.value = wynik.stan
                    if (AkcjaPrzejazdu.StartSesji in wynik.akcje) wejdzWNagrywanie()
                    if (AkcjaPrzejazdu.ZamknijSesje in wynik.akcje) wyjdzZNagrywania()
                }
                publikuj()
            }
        } finally {
            session.stop()
            transport.close()
        }
    }

    private fun publikuj() {
        _migawka.value = zywy.migawka(_stan.value)
    }

    private fun zwolnijWakeLock() {
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
        } finally {
            wakeLock = null
        }
    }

    private fun utworzKanal() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(KANAL, getString(R.string.drive_channel), NotificationManager.IMPORTANCE_LOW)
        )
    }

    companion object {
        const val KANAL = "i40.drive"
        const val NOTIFICATION_ID = 40
    }
}
