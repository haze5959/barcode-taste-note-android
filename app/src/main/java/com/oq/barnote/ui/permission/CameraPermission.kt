package com.oq.barnote.ui.permission

import android.Manifest
import android.content.pm.PackageManager
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
 * 카메라 권한 요청 헬퍼.
 * iOS `AppController.checkCameraPermission` 의 Compose 등가물.
 *
 * 사용 예:
 * ```
 * val rememberCam = rememberCameraPermission()
 * LaunchedEffect(Unit) { rememberCam.requestIfNeeded() }
 * if (rememberCam.isGranted) { CameraPreview(...) }
 * ```
 */
@Composable
fun rememberCameraPermission(
    onResult: (granted: Boolean) -> Unit = {},
): CameraPermissionState {
    val context = LocalContext.current

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
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
        object : CameraPermissionState {
            override val isGranted: Boolean = granted
            override fun requestIfNeeded() {
                if (granted) onResult(true)
                else launcher.launch(Manifest.permission.CAMERA)
            }
        }
    }
}

interface CameraPermissionState {
    val isGranted: Boolean
    fun requestIfNeeded()
}
