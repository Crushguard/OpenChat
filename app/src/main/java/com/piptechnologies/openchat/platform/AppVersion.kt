package com.piptechnologies.openchat.platform

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.piptechnologies.openchat.BuildConfig

/** "1.0.0 (1)": versionName and version code, for Settings and the feedback email body. */
object AppVersion {
    fun label(context: Context): String {
        val info = packageInfo(context) ?: return "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else legacyVersionCode(info)
        return "${info.versionName ?: BuildConfig.VERSION_NAME} ($code)"
    }

    private fun packageInfo(context: Context): PackageInfo? =
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0L))
            } else {
                legacyPackageInfo(context)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }

    @Suppress("DEPRECATION")
    private fun legacyPackageInfo(context: Context): PackageInfo =
        context.packageManager.getPackageInfo(context.packageName, 0)

    @Suppress("DEPRECATION")
    private fun legacyVersionCode(info: PackageInfo): Long = info.versionCode.toLong()
}
