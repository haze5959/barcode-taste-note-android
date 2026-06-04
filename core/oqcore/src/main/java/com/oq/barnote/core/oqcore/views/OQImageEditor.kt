package com.oq.barnote.core.oqcore.views

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.oq.barnote.core.oqcore.models.Palette
import com.oq.barnote.core.oqcore.R
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * iOS `OQImageEditor` 의 안드로이드 Compose 포팅.
 *
 * - 아스펙트 비율 (Free/1:1/3:4/4:3/16:9) 선택
 * - Pinch zoom + Pan + 더블탭 리셋
 * - 4개 코너 핸들로 크롭 박스 크기 조절
 * - 크롭 박스 영역 이동
 * - 90도 회전
 * - 원본으로 리셋
 *
 * 입력: 원본 이미지 바이트 (jpg/png 등 [BitmapFactory] 디코딩 가능 포맷)
 * 출력: 크롭된 이미지 바이트 (JPEG 90% 품질) 또는 null (취소)
 *
 * iOS 의 `applyBlurFace()` (얼굴 모자이크) 는 ML Kit Face Detection 추가 의존이 필요하여
 * 본 첫 포팅에서는 제외했습니다. 추후 필요 시 [com.google.mlkit:face-detection]
 * 의존 추가 + 별도 helper 로 확장 가능합니다.
 */
@Composable
fun OQImageEditor(
    imageBytes: ByteArray,
    palette: Palette,
    onComplete: (ByteArray?) -> Unit,
) {
    Dialog(
        onDismissRequest = { onComplete(null) },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        OQImageEditorContent(
            imageBytes = imageBytes,
            palette = palette,
            onComplete = onComplete,
        )
    }
}

private enum class EditorAspectRatio(val ratio: Float?, val label: String) {
    Free(null, "Free"),
    Square(1f, "1:1"),
    R3x4(3f / 4f, "3:4"),
    R4x3(4f / 3f, "4:3"),
    R16x9(16f / 9f, "16:9"),
}

private enum class HandleCorner { TopLeft, TopRight, BottomLeft, BottomRight }

@Composable
private fun OQImageEditorContent(
    imageBytes: ByteArray,
    palette: Palette,
    onComplete: (ByteArray?) -> Unit,
) {
    val density = LocalDensity.current

    // currentBitmap = 회전이 적용된 작업본. originalBitmap = 원본 (Reset 용).
    val originalBitmap = remember(imageBytes) {
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
    var currentBitmap by remember(originalBitmap) { mutableStateOf(originalBitmap) }

    // Transform states
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var aspectRatio by remember { mutableStateOf(EditorAspectRatio.Free) }

    // Geometry (px in editor coordinate space)
    var containerSize by remember { mutableStateOf(Size.Zero) }
    var imageFrame by remember { mutableStateOf(Rect.Zero) }
    var cropRect by remember { mutableStateOf(Rect.Zero) }
    var isInitialized by remember { mutableStateOf(false) }

    var menuExpanded by remember { mutableStateOf(false) }

    if (originalBitmap == null) {
        // 디코딩 실패 시 즉시 취소 처리.
        LaunchedEffect(Unit) { onComplete(null) }
        return
    }

    fun calculateAspectFitFrame(): Rect {
        val bmp = currentBitmap ?: return Rect.Zero
        if (containerSize.width <= 0f || containerSize.height <= 0f) return Rect.Zero
        val imgW = bmp.width.toFloat()
        val imgH = bmp.height.toFloat()
        if (imgW <= 0f || imgH <= 0f) return Rect.Zero

        val imgAspect = imgW / imgH
        val containerAspect = containerSize.width / containerSize.height
        val displayed = if (imgAspect > containerAspect) {
            Size(containerSize.width, containerSize.width / imgAspect)
        } else {
            Size(containerSize.height * imgAspect, containerSize.height)
        }
        val x = (containerSize.width - displayed.width) / 2f
        val y = (containerSize.height - displayed.height) / 2f
        return Rect(x, y, x + displayed.width, y + displayed.height)
    }

    fun initialCropRect(frame: Rect): Rect {
        val aspect = aspectRatio.ratio ?: return frame
        var width = frame.width
        var height = width / aspect
        if (height > frame.height) {
            height = frame.height
            width = height * aspect
        }
        return Rect(
            left = frame.left + (frame.width - width) / 2f,
            top = frame.top + (frame.height - height) / 2f,
            right = frame.left + (frame.width - width) / 2f + width,
            bottom = frame.top + (frame.height - height) / 2f + height,
        )
    }

    fun updateInitialFrame() {
        val frame = calculateAspectFitFrame()
        if (frame.width <= 0f || frame.height <= 0f) return
        val oldFrame = imageFrame
        imageFrame = frame

        if (!isInitialized) {
            cropRect = initialCropRect(frame)
            isInitialized = true
            return
        }

        // 컨테이너 사이즈가 변했을 때 cropRect 비례 리스케일.
        if (oldFrame.width <= 0f || oldFrame.height <= 0f || oldFrame == frame) return
        val relX = (cropRect.left - oldFrame.left) / oldFrame.width
        val relY = (cropRect.top - oldFrame.top) / oldFrame.height
        val relW = cropRect.width / oldFrame.width
        val relH = cropRect.height / oldFrame.height
        var left = frame.left + relX * frame.width
        var top = frame.top + relY * frame.height
        var width = relW * frame.width
        var height = relH * frame.height
        if (left < frame.left) left = frame.left
        if (top < frame.top) top = frame.top
        if (left + width > frame.right) width = frame.right - left
        if (top + height > frame.bottom) height = frame.bottom - top
        cropRect = Rect(left, top, left + width, top + height)
    }

    fun applyAspectRatio(ratio: EditorAspectRatio) {
        aspectRatio = ratio
        if (imageFrame.width <= 0f || imageFrame.height <= 0f) return
        val aspect = ratio.ratio ?: return

        val centerX = cropRect.left + cropRect.width / 2f
        val centerY = cropRect.top + cropRect.height / 2f

        var newWidth: Float
        var newHeight: Float
        if (cropRect.width / aspect <= cropRect.height) {
            newWidth = cropRect.width
            newHeight = cropRect.width / aspect
        } else {
            newHeight = cropRect.height
            newWidth = cropRect.height * aspect
        }

        if (newWidth > imageFrame.width) {
            newWidth = imageFrame.width
            newHeight = newWidth / aspect
        }
        if (newHeight > imageFrame.height) {
            newHeight = imageFrame.height
            newWidth = newHeight * aspect
        }

        var left = centerX - newWidth / 2f
        var top = centerY - newHeight / 2f
        if (left < imageFrame.left) left = imageFrame.left
        if (top < imageFrame.top) top = imageFrame.top
        if (left + newWidth > imageFrame.right) left = imageFrame.right - newWidth
        if (top + newHeight > imageFrame.bottom) top = imageFrame.bottom - newHeight

        cropRect = Rect(left, top, left + newWidth, top + newHeight)
    }

    fun moveCropRect(initial: Rect, dx: Float, dy: Float) {
        if (imageFrame.width <= 0f || imageFrame.height <= 0f) return
        val width = min(initial.width, imageFrame.width)
        val height = min(initial.height, imageFrame.height)
        var newLeft = initial.left + dx
        var newTop = initial.top + dy
        newLeft = max(imageFrame.left, min(imageFrame.right - width, newLeft))
        newTop = max(imageFrame.top, min(imageFrame.bottom - height, newTop))
        cropRect = Rect(newLeft, newTop, newLeft + width, newTop + height)
    }

    fun updateCropRect(
        corner: HandleCorner,
        initial: Rect,
        dx: Float,
        dy: Float,
    ) {
        // iOS 와 동일하게 최소 50 px (논리 좌표) — Android editor 좌표계도 px 단위라 그대로 사용.
        val minSize = 50f
        val aspect = aspectRatio.ratio

        if (aspect != null) {
            // 고정 비율 리사이즈: 반대쪽 코너가 anchor.
            val anchorX: Float
            val anchorY: Float
            val dirX: Float
            val dirY: Float
            when (corner) {
                HandleCorner.TopLeft -> {
                    anchorX = initial.right; anchorY = initial.bottom; dirX = -1f; dirY = -1f
                }
                HandleCorner.TopRight -> {
                    anchorX = initial.left; anchorY = initial.bottom; dirX = 1f; dirY = -1f
                }
                HandleCorner.BottomLeft -> {
                    anchorX = initial.right; anchorY = initial.top; dirX = -1f; dirY = 1f
                }
                HandleCorner.BottomRight -> {
                    anchorX = initial.left; anchorY = initial.top; dirX = 1f; dirY = 1f
                }
            }

            val maxWidthAllowed = if (dirX > 0f) imageFrame.right - anchorX else anchorX - imageFrame.left
            val maxHeightAllowed = if (dirY > 0f) imageFrame.bottom - anchorY else anchorY - imageFrame.top

            var newWidth = max(minSize, initial.width + dirX * dx)
            newWidth = min(newWidth, maxWidthAllowed)
            var newHeight = newWidth / aspect

            if (newHeight > maxHeightAllowed) {
                newHeight = maxHeightAllowed
                newWidth = newHeight * aspect
            }
            if (newHeight < minSize) {
                newHeight = minSize
                newWidth = newHeight * aspect
            }

            val originX = if (dirX > 0f) anchorX else anchorX - newWidth
            val originY = if (dirY > 0f) anchorY else anchorY - newHeight
            cropRect = Rect(originX, originY, originX + newWidth, originY + newHeight)
            return
        }

        // Free ratio: 축별로 클램프.
        when (corner) {
            HandleCorner.TopLeft -> {
                val newLeft = max(imageFrame.left, min(initial.right - minSize, initial.left + dx))
                val newTop = max(imageFrame.top, min(initial.bottom - minSize, initial.top + dy))
                cropRect = Rect(newLeft, newTop, initial.right, initial.bottom)
            }
            HandleCorner.TopRight -> {
                val newTop = max(imageFrame.top, min(initial.bottom - minSize, initial.top + dy))
                val newRight = min(imageFrame.right, max(initial.left + minSize, initial.right + dx))
                cropRect = Rect(initial.left, newTop, newRight, initial.bottom)
            }
            HandleCorner.BottomLeft -> {
                val newLeft = max(imageFrame.left, min(initial.right - minSize, initial.left + dx))
                val newBottom = min(imageFrame.bottom, max(initial.top + minSize, initial.bottom + dy))
                cropRect = Rect(newLeft, initial.top, initial.right, newBottom)
            }
            HandleCorner.BottomRight -> {
                val newRight = min(imageFrame.right, max(initial.left + minSize, initial.right + dx))
                val newBottom = min(imageFrame.bottom, max(initial.top + minSize, initial.bottom + dy))
                cropRect = Rect(initial.left, initial.top, newRight, newBottom)
            }
        }
    }

    fun handleComplete() {
        val cropped = renderCroppedBitmap(
            bitmap = currentBitmap ?: return onComplete(null),
            cropRect = cropRect,
            imageFrame = imageFrame,
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY,
        )
        val bytes = cropped?.let { bmp ->
            ByteArrayOutputStream().use { out ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.toByteArray()
            }
        }
        onComplete(bytes)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onComplete(null) }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White,
                )
            }
            Text(
                text = stringResource(R.string.image_editor_title),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = { handleComplete() }) {
                Text(
                    text = stringResource(R.string.common_done),
                    color = palette.accent,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Editor canvas
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }

            // 컨테이너 사이즈 / 비트맵 변경시 imageFrame + cropRect 재계산.
            LaunchedEffect(widthPx, heightPx, currentBitmap) {
                containerSize = Size(widthPx, heightPx)
                updateInitialFrame()
            }

            // Pan/zoom + double-tap reset.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(currentBitmap) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 8f)
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    }
                    .pointerInput(currentBitmap) {
                        detectTapGestures(
                            onDoubleTap = {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            },
                        )
                    },
            ) {
                val bmp = currentBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY,
                            ),
                    )
                }

                // Dim overlay (cropRect 영역은 투명, 외부는 검정 50%).
                if (cropRect.width > 0f && cropRect.height > 0f) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val outer = Path().apply { addRect(Rect(0f, 0f, size.width, size.height)) }
                        val inner = Path().apply { addRect(cropRect) }
                        val combined = Path().apply {
                            op(outer, inner, PathOperation.Difference)
                        }
                        drawPath(combined, color = Color.Black.copy(alpha = 0.5f))
                    }
                }

                // Crop border + drag area
                if (cropRect.width > 0f && cropRect.height > 0f) {
                    CropBoxOverlay(
                        cropRect = cropRect,
                        onMove = { initial, dx, dy -> moveCropRect(initial, dx, dy) },
                        onCornerDrag = { corner, initial, dx, dy ->
                            updateCropRect(corner, initial, dx, dy)
                        },
                        density = density,
                    )
                }
            }
        }

        // Bottom bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 아스펙트 비율 메뉴.
            Box {
                ToolbarButton(
                    icon = Icons.Default.AspectRatio,
                    label = stringResource(R.string.image_editor_ratio),
                    onClick = { menuExpanded = true },
                )
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    EditorAspectRatio.entries.forEach { ratio ->
                        DropdownMenuItem(
                            text = { Text(ratio.label) },
                            leadingIcon = {
                                if (ratio == aspectRatio) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                    )
                                }
                            },
                            onClick = {
                                menuExpanded = false
                                applyAspectRatio(ratio)
                            },
                        )
                    }
                }
            }

            // 회전.
            ToolbarButton(
                icon = Icons.Default.RotateRight,
                label = stringResource(R.string.image_editor_rotate),
                onClick = {
                    val rotated = withRotation(currentBitmap)
                    if (rotated != null) {
                        currentBitmap = rotated
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                        // 회전 시 cropRect 가 새 frame 으로 재초기화 되도록 플래그 리셋.
                        isInitialized = false
                    }
                },
            )

            // 리셋.
            ToolbarButton(
                icon = Icons.AutoMirrored.Filled.Undo,
                label = stringResource(R.string.image_editor_reset),
                onClick = {
                    currentBitmap = originalBitmap
                    scale = 1f
                    offsetX = 0f
                    offsetY = 0f
                    aspectRatio = EditorAspectRatio.Free
                    isInitialized = false
                },
            )
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
            )
        }
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun CropBoxOverlay(
    cropRect: Rect,
    onMove: (initial: Rect, dx: Float, dy: Float) -> Unit,
    onCornerDrag: (HandleCorner, initial: Rect, dx: Float, dy: Float) -> Unit,
    density: Density,
) {
    val handleTapPx = with(density) { 60.dp.toPx() }
    val handleDotDp = 12.dp
    val borderPx = with(density) { 2.dp.toPx() }

    // 단일 Box: 흰색 테두리 + drag(이동) 핸들러.
    var moveInitial by remember { mutableStateOf<Rect?>(null) }
    var accumulatedDx by remember { mutableStateOf(0f) }
    var accumulatedDy by remember { mutableStateOf(0f) }
    Box(
        modifier = Modifier
            .offset {
                IntOffset(cropRect.left.roundToInt(), cropRect.top.roundToInt())
            }
            .size(
                width = with(density) { cropRect.width.toDp() },
                height = with(density) { cropRect.height.toDp() },
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        moveInitial = cropRect
                        accumulatedDx = 0f
                        accumulatedDy = 0f
                    },
                    onDragEnd = {
                        moveInitial = null
                        accumulatedDx = 0f
                        accumulatedDy = 0f
                    },
                    onDragCancel = {
                        moveInitial = null
                        accumulatedDx = 0f
                        accumulatedDy = 0f
                    },
                ) { change, drag ->
                    change.consume()
                    accumulatedDx += drag.x
                    accumulatedDy += drag.y
                    val start = moveInitial ?: cropRect
                    onMove(start, accumulatedDx, accumulatedDy)
                }
            },
    ) {
        // White border outline.
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                color = Color.White,
                topLeft = Offset.Zero,
                size = size,
                style = Stroke(width = borderPx),
            )
        }
    }

    // Corner handles — Z-order 상 박스 위에 그려져 우선권을 갖도록 마지막에 배치.
    HandleCorner.entries.forEach { corner ->
        CornerHandle(
            corner = corner,
            cropRect = cropRect,
            handleTapPx = handleTapPx,
            handleDotDp = handleDotDp,
            density = density,
            onDrag = onCornerDrag,
        )
    }
}

@Composable
private fun CornerHandle(
    corner: HandleCorner,
    cropRect: Rect,
    handleTapPx: Float,
    handleDotDp: Dp,
    density: Density,
    onDrag: (HandleCorner, initial: Rect, dx: Float, dy: Float) -> Unit,
) {
    val center = when (corner) {
        HandleCorner.TopLeft -> Offset(cropRect.left, cropRect.top)
        HandleCorner.TopRight -> Offset(cropRect.right, cropRect.top)
        HandleCorner.BottomLeft -> Offset(cropRect.left, cropRect.bottom)
        HandleCorner.BottomRight -> Offset(cropRect.right, cropRect.bottom)
    }
    val tapHalf = handleTapPx / 2f
    val tapHalfDp = with(density) { tapHalf.toDp() }
    val tapSizeDp = tapHalfDp * 2

    var initial by remember { mutableStateOf<Rect?>(null) }
    var accumulatedDx by remember { mutableStateOf(0f) }
    var accumulatedDy by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (center.x - tapHalf).roundToInt(),
                    (center.y - tapHalf).roundToInt(),
                )
            }
            .size(tapSizeDp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        initial = cropRect
                        accumulatedDx = 0f
                        accumulatedDy = 0f
                    },
                    onDragEnd = {
                        initial = null
                        accumulatedDx = 0f
                        accumulatedDy = 0f
                    },
                    onDragCancel = {
                        initial = null
                        accumulatedDx = 0f
                        accumulatedDy = 0f
                    },
                ) { change, drag ->
                    change.consume()
                    accumulatedDx += drag.x
                    accumulatedDy += drag.y
                    val start = initial ?: cropRect
                    onDrag(corner, start, accumulatedDx, accumulatedDy)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(handleDotDp)
                .background(Color.White, CircleShape),
        )
    }
}

/** 90도 시계방향 회전. */
private fun withRotation(bitmap: Bitmap?): Bitmap? {
    if (bitmap == null) return null
    return runCatching {
        val matrix = Matrix().apply { postRotate(90f) }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }.getOrNull()
}

/**
 * 화면상의 [cropRect] (editor 좌표계, scale/offset 적용된 화면) 을 실제 비트맵 픽셀 좌표로
 * 역변환해서 잘라낸다.
 *
 * imageFrame: scale=1, offset=0 일 때 비트맵이 aspect-fit 으로 그려지는 사각형.
 * scale/offset: graphicsLayer 로 적용한 변환 (pivot = 중앙).
 */
private fun renderCroppedBitmap(
    bitmap: Bitmap,
    cropRect: Rect,
    imageFrame: Rect,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
): Bitmap? = runCatching {
    if (imageFrame.width <= 0f || imageFrame.height <= 0f) return null
    if (cropRect.width <= 0f || cropRect.height <= 0f) return null

    val centerX = imageFrame.left + imageFrame.width / 2f
    val centerY = imageFrame.top + imageFrame.height / 2f

    // 1. cropRect 코너 4점 (또는 좌상/우하) 을 transform 의 역변환으로 imageFrame 좌표계로 환산.
    //    transformed = center + (original - center) * scale + offset
    //    → original = center + (transformed - center - offset) / scale
    fun untransformX(x: Float) = centerX + (x - centerX - offsetX) / scale
    fun untransformY(y: Float) = centerY + (y - centerY - offsetY) / scale

    val srcLeftInFrame = untransformX(cropRect.left)
    val srcTopInFrame = untransformY(cropRect.top)
    val srcRightInFrame = untransformX(cropRect.right)
    val srcBottomInFrame = untransformY(cropRect.bottom)

    // 2. imageFrame 좌표 → bitmap 픽셀 좌표 변환 (단순 비례).
    val sxRatio = bitmap.width.toFloat() / imageFrame.width
    val syRatio = bitmap.height.toFloat() / imageFrame.height

    val pxLeft = ((srcLeftInFrame - imageFrame.left) * sxRatio).coerceIn(0f, bitmap.width.toFloat())
    val pxTop = ((srcTopInFrame - imageFrame.top) * syRatio).coerceIn(0f, bitmap.height.toFloat())
    val pxRight = ((srcRightInFrame - imageFrame.left) * sxRatio).coerceIn(0f, bitmap.width.toFloat())
    val pxBottom = ((srcBottomInFrame - imageFrame.top) * syRatio).coerceIn(0f, bitmap.height.toFloat())

    val x = pxLeft.roundToInt()
    val y = pxTop.roundToInt()
    val w = (pxRight - pxLeft).roundToInt().coerceAtLeast(1)
    val h = (pxBottom - pxTop).roundToInt().coerceAtLeast(1)

    val safeW = min(w, bitmap.width - x)
    val safeH = min(h, bitmap.height - y)
    if (safeW <= 0 || safeH <= 0) return null

    Bitmap.createBitmap(bitmap, x, y, safeW, safeH)
}.getOrNull()
