package com.alvaropassalacqua.squishflow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType

@Composable
actual fun rememberHaptics(): Haptics = remember { IosHaptics() }

/**
 * Taptic Engine accents.
 *
 * The generators are kept alive and re-armed with `prepare()` after each hit:
 * without that the engine spins down between gestures and the first tap of a new
 * burst arrives noticeably late.
 */
private class IosHaptics : Haptics {

    private val light = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
    private val medium = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
    private val heavy = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy)
    private val notification = UINotificationFeedbackGenerator()

    init {
        light.prepare()
        medium.prepare()
    }

    override fun play(accent: HapticAccent) {
        when (accent) {
            HapticAccent.TOUCH -> light.fire()
            HapticAccent.DEFORM -> light.fire()
            HapticAccent.RELEASE -> medium.fire()
            HapticAccent.REWARD -> {
                notification.notificationOccurred(
                    UINotificationFeedbackType.UINotificationFeedbackTypeSuccess,
                )
                notification.prepare()
            }

            HapticAccent.REFUSAL -> heavy.fire()
        }
    }

    private fun UIImpactFeedbackGenerator.fire() {
        impactOccurred()
        prepare()
    }
}
