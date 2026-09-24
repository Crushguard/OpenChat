package com.piptechnologies.openchat.screenshots

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.Snapshot
import app.cash.paparazzi.SnapshotHandler
import com.android.resources.Density
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/** 390 x 844 dp at 3x, the frame every design screen was drawn in. */
object ScreenshotDevice {
    val config: DeviceConfig = DeviceConfig.PIXEL_5.copy(
        screenWidth = 1170,
        screenHeight = 2532,
        xdpi = 460,
        ydpi = 460,
        density = Density.XXHIGH,
    )

    /** Where the PNGs land: `app/src/test/snapshots/images/<test method>.png`, full size. */
    val imagesDir: File = File(System.getProperty("paparazzi.snapshot.dir") ?: "src/test/snapshots", "images")

    fun paparazzi(): Paparazzi = Paparazzi(
        deviceConfig = config,
        theme = "android:Theme.Material.Light.NoActionBar",
        showSystemUi = false,
        snapshotHandler = FullSizeSnapshotHandler(imagesDir),
    )
}

/**
 * Writes each snapshot as a full-resolution PNG named after the test method (plus the snapshot name
 * when one is given), instead of Paparazzi's default 1000 px-tall report images. CI publishes this
 * directory to the `screenshots` branch.
 */
class FullSizeSnapshotHandler(private val directory: File) : SnapshotHandler {
    override fun newFrameHandler(snapshot: Snapshot, frameCount: Int, fps: Int): SnapshotHandler.FrameHandler =
        object : SnapshotHandler.FrameHandler {
            override fun handle(image: BufferedImage) {
                directory.mkdirs()
                val method = snapshot.testName.methodName
                val extra = snapshot.name
                val fileName = if (extra.isNullOrBlank()) "$method.png" else "${method}_$extra.png"
                ImageIO.write(image, "png", File(directory, fileName))
            }

            override fun close() = Unit
        }

    override fun close() = Unit
}
