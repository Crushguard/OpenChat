package com.piptechnologies.openchat.screenshots

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.LayoutDirection

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

    private const val THEME = "android:Theme.Material.Light.NoActionBar"

    /** The English screenshots: the default values/ strings, left to right. */
    fun paparazzi(): Paparazzi = Paparazzi(
        deviceConfig = config,
        theme = THEME,
        showSystemUi = false,
    )

    /**
     * [config] in one translation: the strings of values-<[qualifier]> (a resource qualifier, "pt-rBR", "in", "iw"), laid
     * out right to left when [rtl]. layoutlib mirrors only an app that supports RTL, as the manifest's
     * android:supportsRtl="true" declares.
     */
    fun paparazzi(qualifier: String, rtl: Boolean): Paparazzi = Paparazzi(
        deviceConfig = config.copy(locale = qualifier, layoutDirection = if (rtl) LayoutDirection.RTL else LayoutDirection.LTR),
        theme = THEME,
        showSystemUi = false,
        supportsRtl = true,
    )
}
