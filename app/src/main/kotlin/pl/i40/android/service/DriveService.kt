package pl.i40.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pl.i40.android.R
import pl.i40.android.acquisition.OilTempEstimator
import pl.i40.android.acquisition.RingBufferStore
import pl.i40.android.alerts.AlertEngine
import pl.i40.android.storage.DriveSessionDao
import pl.i40.android.storage.SessionRecorder

/**
 * Usługa pierwszoplanowa i właściciel stanu żywego (§11.4).
 * Bufory, model oleju i karencje alarmów mieszkają tu, nie w ViewModel.
 */
class DriveService : Service() {
    private val _stan = MutableStateFlow(StanPrzejazdu.Rozlaczony)
    val stan: StateFlow<StanPrzejazdu> = _stan.asStateFlow()

    val ringBuffers = RingBufferStore()
    val oil = OilTempEstimator()
    val alertEngine = AlertEngine()
    val maszyna = TripStateMachine()

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

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
        return START_STICKY
    }

    override fun onDestroy() {
        zwolnijWakeLock()
        super.onDestroy()
    }

    fun wejdzWNagrywanie() {
        _stan.value = StanPrzejazdu.Nagrywa
        if (wakeLock == null) {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "i40:nagrywa").also {
                it.setReferenceCounted(false)
                it.acquire()
            }
        }
    }

    fun wyjdzZNagrywania() {
        zwolnijWakeLock()
        _stan.value = StanPrzejazdu.Czuwanie
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
