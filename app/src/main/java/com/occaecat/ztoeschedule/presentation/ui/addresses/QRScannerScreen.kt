package com.occaecat.ztoeschedule.presentation.ui.addresses

import android.Manifest
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import java.util.concurrent.Executors
import androidx.activity.compose.BackHandler

import androidx.compose.ui.res.stringResource
import com.occaecat.ztoeschedule.R
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import com.occaecat.ztoeschedule.presentation.ui.components.StepActions
import com.occaecat.ztoeschedule.presentation.ui.components.StepGutter
import com.occaecat.ztoeschedule.presentation.ui.components.StepHeroIcon
import com.occaecat.ztoeschedule.presentation.ui.components.StepHeroPage
import com.occaecat.ztoeschedule.presentation.ui.components.StepListHeader
import com.occaecat.ztoeschedule.presentation.ui.components.StepPrimaryButton
import com.occaecat.ztoeschedule.presentation.ui.components.StepSecondaryButton

private const val TAG = "QRScanner"

/**
 * Full-screen QR code scanner with camera preview
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QRScannerScreen(
    onResult: (QRScanResult) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val qrPermissionError = stringResource(R.string.qr_error_permission)
    val qrUnknownError = stringResource(R.string.qr_error_unknown)
    
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    var flashEnabled by remember { mutableStateOf(false) }
    var scanningState by remember { mutableStateOf<ScanningState>(ScanningState.Scanning) }
    var lastScannedCode by remember { mutableStateOf<String?>(null) }
    
    // Request permission on launch
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }
    
    // Handle successful scan with debounce
    LaunchedEffect(scanningState) {
        if (scanningState is ScanningState.Success) {
            delay(500) // Brief delay to show success state
            val data = (scanningState as ScanningState.Success).data
            onResult(QRScanResult.Success(data))
        }
    }
    
    // Handle back gesture - close scanner gracefully
    BackHandler {
        onResult(QRScanResult.Cancelled)
        onDismiss()
    }
    
    if (!cameraPermissionState.status.isGranted) {
        PermissionDeniedContent(
            shouldShowRationale = cameraPermissionState.status.shouldShowRationale,
            onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
            onDismiss = {
                onResult(QRScanResult.Error(qrPermissionError))
                onDismiss()
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
    ) {
        StepListHeader(
            title = stringResource(R.string.qr_instruction_title),
            subtitle = stringResource(R.string.qr_instruction_desc)
        )

        // Camera viewport as a large rounded card instead of a full-bleed black screen
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = StepGutter)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.Black)
        ) {
            CameraPreview(
                flashEnabled = flashEnabled,
                onBarcodeDetected = { barcode ->
                    if (scanningState is ScanningState.Scanning && barcode != lastScannedCode) {
                        lastScannedCode = barcode
                        val result = QRAddressData.parse(barcode)
                        scanningState = result.fold(
                            onSuccess = { ScanningState.Success(it) },
                            onFailure = { ScanningState.Error(it.message ?: qrUnknownError) }
                        )
                    }
                }
            )

            ScannerFrame(
                scanningState = scanningState,
                modifier = Modifier.align(Alignment.Center)
            )

            FilledTonalIconToggleButton(
                checked = flashEnabled,
                onCheckedChange = { flashEnabled = it },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = if (flashEnabled) stringResource(R.string.qr_flash_off) else stringResource(R.string.qr_flash_on)
                )
            }
        }

        ScanStatus(
            scanningState = scanningState,
            onRetry = {
                scanningState = ScanningState.Scanning
                lastScannedCode = null
            },
            onDismiss = {
                onResult(QRScanResult.Error((scanningState as? ScanningState.Error)?.message ?: ""))
                onDismiss()
            }
        )
    }
}

@Composable
private fun CameraPreview(
    flashEnabled: Boolean,
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    
    // Update flash
    LaunchedEffect(flashEnabled, camera) {
        camera?.cameraControl?.enableTorch(flashEnabled)
    }
    
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { previewView ->
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder()
                .build()
                .also { it.surfaceProvider = previewView.surfaceProvider }
            
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(
                        Executors.newSingleThreadExecutor(),
                        BarcodeAnalyzer { barcode ->
                            onBarcodeDetected(barcode)
                        }
                    )
                }
            
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera bind failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }
}

/** Corner brackets marking the scan area; tinted by scan state. */
@Composable
private fun ScannerFrame(
    scanningState: ScanningState,
    modifier: Modifier = Modifier
) {
    val cornerColor by androidx.compose.animation.animateColorAsState(
        targetValue = when (scanningState) {
            is ScanningState.Scanning -> Color.White
            is ScanningState.Success -> MaterialTheme.colorScheme.primary
            is ScanningState.Error -> MaterialTheme.colorScheme.error
        },
        label = "scanner_frame_color"
    )
    val cornerSize = 40.dp
    val stroke = 5.dp
    val corners = listOf(Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd)

    Box(modifier = modifier.size(240.dp)) {
        corners.forEach { corner ->
            val top = corner == Alignment.TopStart || corner == Alignment.TopEnd
            val start = corner == Alignment.TopStart || corner == Alignment.BottomStart
            Box(Modifier.align(corner).size(cornerSize)) {
                Box(
                    Modifier
                        .align(if (top) Alignment.TopCenter else Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(stroke)
                        .background(cornerColor, RoundedCornerShape(50))
                )
                Box(
                    Modifier
                        .align(if (start) Alignment.CenterStart else Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(stroke)
                        .background(cornerColor, RoundedCornerShape(50))
                )
            }
        }
    }
}

/** Status area under the viewport: progress on success, message and actions on error. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScanStatus(
    scanningState: ScanningState,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    androidx.compose.animation.AnimatedContent(
        targetState = scanningState,
        contentKey = { it::class },
        label = "scan_status"
    ) { state ->
        when (state) {
            is ScanningState.Scanning -> Spacer(Modifier.fillMaxWidth().height(16.dp))

            is ScanningState.Success -> Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = StepGutter + 8.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LoadingIndicator(modifier = Modifier.size(40.dp), color = colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.qr_success_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                    state.data.displayName?.let { name ->
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is ScanningState.Error -> StepActions {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                StepPrimaryButton(
                    text = stringResource(R.string.qr_retry_btn),
                    onClick = onRetry
                )
                StepSecondaryButton(
                    text = stringResource(R.string.qr_cancel_btn),
                    onClick = onDismiss
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PermissionDeniedContent(
    shouldShowRationale: Boolean,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    StepHeroPage(
        hero = {
            StepHeroIcon(
                icon = Icons.Default.QrCodeScanner,
                shape = MaterialShapes.Square.toShape(),
                containerColor = colorScheme.secondaryContainer,
                contentColor = colorScheme.onSecondaryContainer
            )
        },
        title = stringResource(R.string.qr_permission_title),
        subtitle = if (shouldShowRationale) {
            stringResource(R.string.qr_permission_rationale)
        } else {
            stringResource(R.string.qr_permission_request)
        },
        actions = {
            StepPrimaryButton(
                text = stringResource(R.string.qr_permission_btn),
                onClick = onRequestPermission
            )
            StepSecondaryButton(
                text = stringResource(R.string.qr_cancel_btn),
                onClick = onDismiss
            )
        }
    )
}

private sealed class ScanningState {
    data object Scanning : ScanningState()
    data class Success(val data: QRAddressData) : ScanningState()
    data class Error(val message: String) : ScanningState()
}

/**
 * ML Kit barcode analyzer
 */
private class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {
    
    private val scanner = BarcodeScanning.getClient(
        com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )
    
    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(
                mediaImage, 
                imageProxy.imageInfo.rotationDegrees
            )
            
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    barcodes.firstOrNull()?.rawValue?.let { value ->
                        onBarcodeDetected(value)
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
