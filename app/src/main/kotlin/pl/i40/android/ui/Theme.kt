package pl.i40.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import pl.i40.android.R
import pl.i40.android.checkup.Werdykt

/** Motyw — §3 warstwy wyglądu. NOC domyślny, DZIEŃ obok. */
enum class MotywI40 {
    Noc,
    Dzien,
}

/**
 * Osiem ról koloru — te same nazwy w obu motywach (§3.2 / §3.3).
 * Literały Color wyłącznie w tym pliku.
 */
data class I40Paleta(
    val tloGlebia: Color,
    val pole: Color,
    val siatka: Color,
    val odczyt: Color,
    val przygasle: Color,
    val model: Color,
    val uwaga: Color,
    val usterka: Color
) {
    /** Alias: podłoże ekranu. */
    val tlo: Color get() = tloGlebia

    /** Alias: powierzchnia kafla / karty. */
    val powierzchnia: Color get() = pole
    val powierzchniaPodniesiona: Color get() = pole

    /** Alias: linie / kreski. */
    val kreska: Color get() = siatka

    /** Alias: wartości i tekst główny. */
    val tekst: Color get() = odczyt

    /** Alias: etykiety drugoplanowe. */
    val tekstDrugi: Color get() = przygasle
    val tekstWyciszony: Color get() = przygasle

    /**
     * Elementy interaktywne / nagłówki sekcji — bez osobnego tokenu w §3;
     * używamy `odczyt`, nie `model` (model tylko przy tyldzie).
     */
    val akcent: Color get() = odczyt

    /** Werdykt OK — brak zieleni w palecie; odczyt bez wyróżnienia. */
    val ok: Color get() = odczyt

    fun werdykt(w: Werdykt): Color = when (w) {
        Werdykt.Ok -> ok
        Werdykt.Uwaga -> uwaga
        Werdykt.Usterka -> usterka
    }
}

/** Palety z tabel §3.2 i §3.3 — wartości dosłownie. */
object I40Motywy {
    val noc = I40Paleta(
        tloGlebia = Color(0xFF0E1114),
        pole = Color(0xFF171B1F),
        siatka = Color(0xFF2A3138),
        odczyt = Color(0xFFE8E4DC),
        przygasle = Color(0xFF8A9299),
        model = Color(0xFF7FA8B8),
        uwaga = Color(0xFFE0A030),
        usterka = Color(0xFFD9433A)
    )

    val dzien = I40Paleta(
        tloGlebia = Color(0xFFECE7DF),
        pole = Color(0xFFF7F4EF),
        siatka = Color(0xFFC3BCB0),
        odczyt = Color(0xFF16181A),
        przygasle = Color(0xFF5E6468),
        model = Color(0xFF2A5A6B),
        uwaga = Color(0xFFA85E00),
        usterka = Color(0xFFB3261E)
    )

    fun dla(motyw: MotywI40): I40Paleta = when (motyw) {
        MotywI40.Noc -> noc
        MotywI40.Dzien -> dzien
    }
}

/** Kontrast względny WCAG 2.1 — do testu §3.7. */
object KontrastWcag {
    fun luminancja(c: Color): Double {
        fun kanal(v: Float): Double {
            val s = v.toDouble()
            return if (s <= 0.03928) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * kanal(c.red) + 0.7152 * kanal(c.green) + 0.0722 * kanal(c.blue)
    }

    fun stosunek(tekst: Color, tlo: Color): Double {
        val l1 = luminancja(tekst)
        val l2 = luminancja(tlo)
        val jasny = maxOf(l1, l2)
        val ciemny = minOf(l1, l2)
        return (jasny + 0.05) / (ciemny + 0.05)
    }
}

val LocalI40Kolory = staticCompositionLocalOf { I40Motywy.noc }
val LocalMotywI40 = staticCompositionLocalOf { MotywI40.Noc }

@Composable
fun I40Theme(motyw: MotywI40 = MotywI40.Noc, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalMotywI40 provides motyw,
        LocalI40Kolory provides I40Motywy.dla(motyw),
        content = content
    )
}

/** Dane — JetBrains Mono, cyfry tabelaryczne (§4.1). */
val I40CzcionkaWartosci: FontFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium)
)

/** Tekst i etykiety — Inter (§4.1). */
val I40CzcionkaTekstu: FontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium)
)

/** @deprecated Użyj [I40Motywy.noc]; zostawione dla kompilacji testów oczekujących obiektu. */
@Deprecated("Użyj LocalI40Kolory / I40Motywy", ReplaceWith("I40Motywy.noc"))
object I40Kolory {
    val tlo get() = I40Motywy.noc.tlo
    val powierzchnia get() = I40Motywy.noc.powierzchnia
    val powierzchniaPodniesiona get() = I40Motywy.noc.powierzchniaPodniesiona
    val kreska get() = I40Motywy.noc.kreska
    val tekst get() = I40Motywy.noc.tekst
    val tekstDrugi get() = I40Motywy.noc.tekstDrugi
    val tekstWyciszony get() = I40Motywy.noc.tekstWyciszony
    val akcent get() = I40Motywy.noc.akcent
    val ok get() = I40Motywy.noc.ok
    val uwaga get() = I40Motywy.noc.uwaga
    val usterka get() = I40Motywy.noc.usterka
    fun werdykt(w: Werdykt): Color = I40Motywy.noc.werdykt(w)
}
