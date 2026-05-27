package com.oq.barnote.ui.permission

import android.Manifest
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState

/**
 * Android 13+ POST_NOTIFICATIONS 권한 요청 헬퍼.
 *
 * iOS `NotificationClient.requestAuthorization` 의 Compose 등가물.
 *
 * 사용 예:
 * ```
 * val rememberNoti = rememberNotificationPermission()
 * Button(onClick = { rememberNoti.requestIfNeeded() }) { Text("알림 켜기") }
 * ```
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberNotificationPermission(
    onResult: (granted: Boolean) -> Unit = {},
): NotificationPermissionState {
    // Android 12 이하는 권한 자동 부여
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return object : NotificationPermissionState {
            override val isGranted: Boolean = true
            override fun requestIfNeeded() = onResult(true)
        }
    }

    val state = rememberPermissionState(
        permission = Manifest.permission.POST_NOTIFICATIONS,
        onPermissionResult = onResult,
    )
    var lastStatus by remember { mutableStateOf(state.status) }
    LaunchedEffect(state.status) {
        if (lastStatus != state.status) {
            lastStatus = state.status
        }
    }
    return object : NotificationPermissionState {
        override val isGranted: Boolean = state.status is PermissionStatus.Granted
        override fun requestIfNeeded() {
            if (!isGranted) state.launchPermissionRequest() else onResult(true)
        }
    }
}

interface NotificationPermissionState {
    val isGranted: Boolean
    fun requestIfNeeded()
}
