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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
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
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.qr_title)) },
                navigationIcon = {
                    IconButton(onClick = { 
                        onResult(QRScanResult.Cancelled)
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.qr_close))
                    }
                },
                actions = {
                    IconButton(onClick = { flashEnabled = !flashEnabled }) {
                        Icon(
                            imageVector = if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = if (flashEnabled) stringResource(R.string.qr_flash_off) else stringResource(R.string.qr_flash_on)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.7f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                !cameraPermissionState.status.isGranted -> {
                    PermissionDeniedContent(
                        shouldShowRationale = cameraPermissionState.status.shouldShowRationale,
                        onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
                        onDismiss = {
                            onResult(QRScanResult.Error(context.getString(R.string.qr_error_permission)))
                            onDismiss()
                        }
                    )
                }
                
                else -> {
                    CameraPreview(
                        flashEnabled = flashEnabled,
                        onBarcodeDetected = { barcode ->
                            if (scanningState is ScanningState.Scanning && barcode != lastScannedCode) {
                                lastScannedCode = barcode
                                val result = QRAddressData.parse(barcode)
                                scanningState = result.fold(
                                    onSuccess = { ScanningState.Success(it) },
                                    onFailure = { ScanningState.Error(it.message ?: context.getString(R.string.qr_error_unknown)) }
                                )
                            }
                        }
                    )
                    
                    // Scanning overlay
                    ScannerOverlay(
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
        }
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

@Composable
private fun ScannerOverlay(
    scanningState: ScanningState,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Scanning frame
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(280.dp)
                .background(Color.Transparent)
        ) {
            // Corner indicators
            val cornerSize = 40.dp
            val cornerWidth = 4.dp
            val cornerColor = when (scanningState) {
                is ScanningState.Scanning -> Color.White
                is ScanningState.Success -> Color.Green
                is ScanningState.Error -> Color.Red
            }
            
            // Top-left
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(cornerSize)
            ) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(cornerWidth)
                    .background(cornerColor))
                Box(modifier = Modifier
                    .fillMaxHeight()
                    .width(cornerWidth)
                    .background(cornerColor))
            }
            
            // Top-right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(cornerSize)
            ) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(cornerWidth)
                    .background(cornerColor))
                Box(modifier = Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxHeight()
                    .width(cornerWidth)
                    .background(cornerColor))
            }
            
            // Bottom-left
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(cornerSize)
            ) {
                Box(modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(cornerWidth)
                    .background(cornerColor))
                Box(modifier = Modifier
                    .fillMaxHeight()
                    .width(cornerWidth)
                    .background(cornerColor))
            }
            
            // Bottom-right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(cornerSize)
            ) {
                Box(modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxWidth()
                    .height(cornerWidth)
                    .background(cornerColor))
                Box(modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxHeight()
                    .width(cornerWidth)
                    .background(cornerColor))
            }
        }
        
        // Instructions and status
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (scanningState) {
                is ScanningState.Scanning -> {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.qr_instruction_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.qr_instruction_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
                
                is ScanningState.Success -> {
                    CircularProgressIndicator(
                        color = Color.Green,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.qr_success_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Green,
                        textAlign = TextAlign.Center
                    )
                    scanningState.data.displayName?.let { name ->
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                is ScanningState.Error -> {
                    Text(
                        text = "❌",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.qr_error_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Red,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = scanningState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            )
                        ) {
                            Text(stringResource(R.string.qr_cancel_btn))
                        }
                        Button(onClick = onRetry) {
                            Text(stringResource(R.string.qr_retry_btn))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionDeniedContent(
    shouldShowRationale: Boolean,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.qr_permission_title),
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (shouldShowRationale) {
                stringResource(R.string.qr_permission_rationale)
            } else {
                stringResource(R.string.qr_permission_request)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.qr_permission_btn))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(onClick = onDismiss) {
            Text(stringResource(R.string.qr_cancel_btn), color = Color.White)
        }
    }
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
