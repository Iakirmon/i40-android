package pl.i40.android

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import pl.i40.android.service.DriveService
import pl.i40.android.service.MigawkaZywego
import pl.i40.android.storage.Przejazd
import pl.i40.android.ui.I40App

class MainActivity : ComponentActivity() {
    private var usluga by mutableStateOf<DriveService?>(null)

    private val polaczenie = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            usluga = (binder as DriveService.Lokalny).usluga()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            usluga = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startForegroundService(Intent(this, DriveService::class.java))
        bindService(Intent(this, DriveService::class.java), polaczenie, BIND_AUTO_CREATE)
        setContent {
            val svc = usluga
            var migawka by remember { mutableStateOf(MigawkaZywego()) }
            var przejazdy by remember { mutableStateOf(listOf<Przejazd>()) }
            var punkty by remember { mutableStateOf(listOf<pl.i40.android.storage.PunktOdniesienia>()) }
            LaunchedEffect(svc) {
                if (svc == null) {
                    migawka = MigawkaZywego()
                    przejazdy = emptyList()
                    punkty = emptyList()
                } else {
                    przejazdy = svc.listaPrzejazdow()
                    punkty = svc.listaPunktow()
                    svc.migawka.collect {
                        migawka = it
                        punkty = svc.listaPunktow()
                    }
                }
            }
            DisposableEffect(migawka.nagrywa) {
                if (migawka.nagrywa) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
            }
            I40App(
                migawka = migawka,
                onStop = { svc?.zatrzymajNaZadanie() },
                przejazdy = przejazdy,
                onUsun = { id ->
                    svc?.usunPrzejazd(id)
                    przejazdy = svc?.listaPrzejazdow().orEmpty()
                },
                onChroniony = { id, v ->
                    svc?.ustawChroniony(id, v)
                    przejazdy = svc?.listaPrzejazdow().orEmpty()
                },
                punkty = punkty
            )
        }
    }

    override fun onDestroy() {
        unbindService(polaczenie)
        super.onDestroy()
    }
}
