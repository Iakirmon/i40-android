package pl.i40.android.alerts

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Alarm przycisza nawigację (`AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`) zamiast ją zabijać.
 * Brak silniczka w radiu nie jest błędem — i nie dokładamy `VIBRATE` poza zestaw z §13.1.
 */
class AlertPlayer(context: Context) {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val vibrator = vibrator(context)

    @SuppressLint("MissingPermission")
    fun play(severity: AlertSeverity) {
        val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
        audio.requestAudioFocus(focus)
        try {
            val tone = when (severity) {
                AlertSeverity.Urgent -> ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
                AlertSeverity.Warning -> ToneGenerator.TONE_PROP_ACK
                AlertSeverity.Info -> ToneGenerator.TONE_PROP_BEEP
            }
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80).use { it.startTone(tone, 250) }
            try {
                vibrator?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            } catch (_: SecurityException) {
                // radio bez silniczka / bez VIBRATE — dźwięk wystarczy
            }
        } finally {
            audio.abandonAudioFocusRequest(focus)
        }
    }

    private fun vibrator(context: Context): Vibrator? {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        return manager?.defaultVibrator
    }
}

private inline fun ToneGenerator.use(block: (ToneGenerator) -> Unit) {
    try {
        block(this)
    } finally {
        release()
    }
}
