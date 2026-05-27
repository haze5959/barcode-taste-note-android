package com.oq.barnote.core.oqcore.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OQUtil @Inject constructor(
    @ApplicationContext private val context: Context,
    private val keyChain: OQKeyChain
) {
    fun device(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }

    fun bundleID(): String {
        return context.packageName
    }

    fun appVersion(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "1.0"
        }
    }

    fun buildVersion(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toString()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            "1"
        }
    }

    fun timezone(): String {
        val secondsGMT = TimeZone.getDefault().rawOffset / 1000
        val plus = if (secondsGMT < 0) "-" else "+"
        val minutes = (Math.abs(secondsGMT) / 60) % 60
        val hours = (Math.abs(secondsGMT) / 60) / 60
        return String.format("%s%02d:%02d", plus, hours, minutes)
    }

    fun languageCode(): String {
        return Locale.getDefault().language
    }

    fun uuid(): String {
        val keyName = "UUID"
        val existing = keyChain.load(keyName)
        if (existing != null) {
            return String(existing, Charsets.UTF_8)
        }
        val newUuid = UUID.randomUUID().toString()
        keyChain.save(keyName, newUuid.toByteArray(Charsets.UTF_8))
        return newUuid
    }

    fun safari(url: String): Boolean {
        if (url.isEmpty()) return false
        var aUrl = url
        if (!url.contains("://") && !url.startsWith("tel:")) {
            aUrl = if (url.matches(Regex("^[0-9\\-\\+]{9,15}$"))) {
                "tel:$url"
            } else {
                "http://$url"
            }
        }
        
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(aUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            OQLog.e("safari failed to open: $aUrl")
            false
        }
    }

    fun displayName(): String {
        val applicationInfo = context.applicationInfo
        val stringId = applicationInfo.labelRes
        return if (stringId == 0) {
            applicationInfo.nonLocalizedLabel.toString()
        } else {
            context.getString(stringId)
        }
    }

    fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
}
