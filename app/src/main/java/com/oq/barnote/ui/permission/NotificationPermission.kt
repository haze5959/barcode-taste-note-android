package com.oq.barnote.ui.permission

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Android 13+ POST_NOTIFICATIONS 권한 요청 헬퍼.
 *
 * iOS `NotificationClient.requestAuthorization` 의 Compose 등가물.
 * Accompanist Permissions 가 deprecated 됨에 따라
 * `androidx.activity.compose.rememberLauncherForActivityResult` 기반으로 마이그레이션.
 *
 * 사용 예:
 * ```
 * val rememberNoti = rememberNotificationPermission()
 * Button(onClick = { rememberNoti.requestIfNeeded() }) { Text("알림 켜기") }
 * ```
 */
@Composable
fun rememberNotificationPermission(
    onResult: (granted: Boolean) -> Unit = {},
): NotificationPermissionState {
    val context = LocalContext.current

    // Android 12 이하는 권한 자동 부여
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return remember {
            object : NotificationPermissionState {
                override val isGranted: Boolean = true
                override fun requestIfNeeded() = onResult(true)
            }
        }
    }

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { result ->
        granted = result
        onResult(result)
    }

    return remember(granted) {
        object : NotificationPermissionState {
            override val isGranted: Boolean = granted
            override fun requestIfNeeded() {
                if (granted) onResult(true)
                else launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

interface NotificationPermissionState {
    val isGranted: Boolean
    fun requestIfNeeded()
}
