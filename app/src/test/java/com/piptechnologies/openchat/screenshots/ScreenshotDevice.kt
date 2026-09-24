package com.piptechnologies.openchat.screenshots

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density

/** 390 x 844 dp at 3x, the frame every design screen was drawn in. */
object ScreenshotDevice {
    val config: DeviceConfig = DeviceConfig.PIXEL_5.copy(
        screenWidth = 1170,
        screenHeight = 2532,
        xdpi = 460,
        ydpi = 460,
        density = Density.XXHIGH,
    )

    fun paparazzi(): Paparazzi = Paparazzi(
        deviceConfig = config,
        theme = "android:Theme.Material.Light.NoActionBar",
        showSystemUi = false,
    )
}
