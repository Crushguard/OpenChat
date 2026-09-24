package com.piptechnologies.openchat.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Runtime permissions the media watcher needs to read WhatsApp media (ruling R17, §5.3). */
object StoragePermissions {
    /** 33+: READ_MEDIA_IMAGES/VIDEO/AUDIO; 29–32: READ_EXTERNAL_STORAGE; 24–28: plus WRITE_EXTERNAL_STORAGE. */
    fun required(): List<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
        )
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.P -> listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )
        else -> listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun hasAll(context: Context): Boolean =
        required().all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
}
