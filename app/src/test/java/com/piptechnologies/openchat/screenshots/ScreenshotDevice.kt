package com.piptechnologies.openchat.screenshots

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density

/**
 * 390 x 844 dp at 3x, the frame every design screen was drawn in. Paparazzi scales recorded
 * images to 1000 px on the long side; rendering at 3x keeps text crisp after that downscale.
 * CI renames `<package>_<Class>ScreenshotTests_<method>.png` to `<method>.png`.
 */
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
