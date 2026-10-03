package `in`.grayscales.entangl.ui.qr

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BinaryBitmap
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import `in`.grayscales.entangl.core.crypto.HandshakeManager
import `in`.grayscales.entangl.domain.model.HandshakePayload
import `in`.grayscales.entangl.domain.model.TransferQrPayload
import `in`.grayscales.entangl.ui.theme.DarkMatter
import `in`.grayscales.entangl.ui.theme.IsotopeMagenta
import `in`.grayscales.entangl.ui.theme.NeutronWhite
import `in`.grayscales.entangl.ui.theme.QuantumCyan
import `in`.grayscales.entangl.ui.theme.QuantumMonospace
import `in`.grayscales.entangl.ui.theme.SubatomicGray
import `in`.grayscales.entangl.ui.theme.VoidBackground
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalGetImage::class)
@Composable
fun QrScannerView(
    handshakeManager: HandshakeManager,
    onPeerConfirmed: (peerUid: String, peerPublicKey: ByteArray, peerOnion: String, safetyNumber: String, peerUsername: String, peerProfileColor: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onTransferDetected: ((TransferQrPayload) -> Unit)? = null,
    onSwitchToMyQr: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var useFrontCamera by remember { mutableStateOf(false) }
    var isTargetLocked by remember { mutableStateOf(false) }
    var scannedPayload by remember { mutableStateOf<HandshakePayload?>(null) }
    var scannedTransferPayload by remember { mutableStateOf<TransferQrPayload?>(null) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    // ML Kit client creation can throw on devices without Google Play Services
    // components (common on Transsion/MediaTek ROMs). A null client degrades to the
    // bundled ZXing engine instead of crashing composition.
    val barcodeScanner = remember {
        runCatching {
            BarcodeScanning.getClient(
                BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
            )
        }.getOrNull()
    }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var cameraAttempt by remember { mutableIntStateOf(0) }

    // Fetch CameraProvider once (retryable). getInstance/future.get() throw on
    // devices whose camera HAL rejects CameraX — surface error UI, never crash.
    LaunchedEffect(cameraAttempt) {
        try {
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                try {
                    cameraProvider = future.get()
                } catch (_: Exception) {
                    cameraError = "Camera unavailable on this device."
                }
            }, ContextCompat.getMainExecutor(context))
        } catch (_: Exception) {
            cameraError = "Camera unavailable on this device."
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            cameraProvider?.unbindAll()
            try { barcodeScanner?.close() } catch (_: Exception) {}
        }
    }

    if (!hasCameraPermission) {
        // Cyberpunk styled permission rationale card
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(VoidBackground)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkMatter)
                    .border(1.dp, IsotopeMagenta, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = IsotopeMagenta,
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = "OPTICAL MATRIX ACCESS REQUIRED",
                    fontFamily = QuantumMonospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp,
                    color = IsotopeMagenta,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Entangl requires hardware camera access to scan peer QR handshakes and establish cryptographic P2P entanglement.",
                    fontFamily = QuantumMonospace,
                    fontSize = 11.sp,
                    color = NeutronWhite,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "AUTHORIZE OPTICAL SENSOR",
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = SubatomicGray
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "RETURN TO CONSOLE",
                        fontFamily = QuantumMonospace,
                        fontSize = 11.sp
                    )
                }
            }
        }
        return
    }

    // Camera failure card: CameraX/MLKit init can throw on OEM ROMs with broken
    // camera HAL or missing Play Services components. This used to crash the app
    // from composition — now it degrades to an explanatory card with retry.
    if (cameraError != null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(VoidBackground)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkMatter)
                    .border(1.dp, IsotopeMagenta, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = IsotopeMagenta,
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = "OPTICAL SENSOR UNAVAILABLE",
                    fontFamily = QuantumMonospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp,
                    color = IsotopeMagenta,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = cameraError
                        ?: "The camera could not be started on this device. You can still receive your peer's scan and show your code instead.",
                    fontFamily = QuantumMonospace,
                    fontSize = 11.sp,
                    color = NeutronWhite,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = {
                        cameraError = null
                        cameraAttempt++
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "RETRY OPTICAL SENSOR",
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = SubatomicGray
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "RETURN TO CONSOLE",
                        fontFamily = QuantumMonospace,
                        fontSize = 11.sp
                    )
                }
            }
        }
        return
    }

    // Reactive camera binding on cameraProvider, previewView, front/back toggle, or target lock
    LaunchedEffect(useFrontCamera, cameraProvider, previewView, isTargetLocked, cameraAttempt) {
        try {
            val provider = cameraProvider ?: return@LaunchedEffect
            val pView = previewView ?: return@LaunchedEffect

        if (isTargetLocked) {
            // Power & thermal optimization: unbind camera and disable torch immediately when QR code is acquired
            try {
                camera?.cameraControl?.enableTorch(false)
                provider.unbindAll()
                camera = null
                isTorchOn = false
            } catch (_: Exception) {}
            return@LaunchedEffect
        }

        val resolutionSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    Size(1280, 720),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                )
            )
            .build()

        val preview = Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .build()
            .also {
                it.surfaceProvider = pView.surfaceProvider
            }

        val imageAnalysis = ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        val mainExecutor = ContextCompat.getMainExecutor(context)

        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            if (scannedPayload != null || scannedTransferPayload != null || isTargetLocked) {
                imageProxy.close()
                return@setAnalyzer
            }

            // Engine 1: Bundled local ZXing (Instant, offline, normal + inverted + cropped)
            val zxingText = decodeQrWithZxing(imageProxy)
            if (!zxingText.isNullOrBlank()) {
                val transfer = TransferQrPayload.fromQrString(zxingText)
                if (transfer != null) {
                    mainExecutor.execute {
                        if (scannedTransferPayload == null) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isTargetLocked = true
                            scannedTransferPayload = transfer
                        }
                    }
                    imageProxy.close()
                    return@setAnalyzer
                }
                try {
                    val payload = HandshakePayload.fromQrString(zxingText)
                    mainExecutor.execute {
                        if (scannedPayload == null) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isTargetLocked = true
                            scannedPayload = payload
                        }
                    }
                    imageProxy.close()
                    return@setAnalyzer
                } catch (_: Exception) {
                    // Non-handshake QR payload, proceed to MLKit
                }
            }

            // Engine 2: Google MLKit Hardware-Accelerated Barcode Detector.
            // Null-safe: without Play Services components the client is null and the
            // bundled ZXing engine above carries scanning alone.
            val mediaImage = imageProxy.image
            val mlkitScanner = barcodeScanner
            if (mediaImage != null && mlkitScanner != null) {
                val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                mlkitScanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        if (scannedPayload != null || scannedTransferPayload != null) return@addOnSuccessListener
                        for (barcode in barcodes) {
                            val rawText = barcode.rawValue ?: barcode.displayValue
                            if (!rawText.isNullOrBlank()) {
                                val transfer = TransferQrPayload.fromQrString(rawText)
                                if (transfer != null) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isTargetLocked = true
                                    scannedTransferPayload = transfer
                                    break
                                }
                                try {
                                    val payload = HandshakePayload.fromQrString(rawText)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isTargetLocked = true
                                    scannedPayload = payload
                                    break
                                } catch (_: Exception) {
                                }
                            }
                        }
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }

        val cameraSelector = if (useFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        try {
            provider.unbindAll()
            val boundCamera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
            camera = boundCamera
            if (isTorchOn && !useFrontCamera) {
                boundCamera.cameraControl.enableTorch(true)
            } else {
                isTorchOn = false
            }
        } catch (_: Exception) {
            // Binding failure (HAL rejects use-case combo on this device): error UI.
            cameraError = "Camera unavailable on this device."
        }
        } catch (_: Exception) {
            // Binding setup failure (resolution config, selector init): error UI.
            cameraError = "Camera unavailable on this device."
        }
    }

    // Camera Preview + HUD
    Box(modifier = modifier.fillMaxSize().background(VoidBackground)) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    setOnTouchListener { v, event ->
                        if (event.action == MotionEvent.ACTION_UP) {
                            val currentCamera = camera
                            if (currentCamera != null) {
                                // Metering factory is a platform type: guard against OEM
                                // PreviewView builds that return null before attach.
                                val factory = runCatching { meteringPointFactory }.getOrNull()
                                if (factory != null) {
                                    val point = factory.createPoint(event.x, event.y)
                                    val action = FocusMeteringAction.Builder(
                                        point,
                                        FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                                    ).setAutoCancelDuration(3, TimeUnit.SECONDS).build()
                                    currentCamera.cameraControl.startFocusAndMetering(action)
                                }
                            }
                            v.performClick()
                        }
                        true
                    }
                    previewView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Futuristic Quantum Reticle HUD Overlay
        QuantumScannerOverlay(isTargetLocked = isTargetLocked)

        // Top Control Bar (torch + lens toggles, aligned to end)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Flashlight toggle
                IconButton(
                    onClick = {
                        val newTorch = !isTorchOn
                        isTorchOn = newTorch
                        camera?.cameraControl?.enableTorch(newTorch)
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkMatter.copy(alpha = 0.8f))
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Toggle Torch",
                        tint = if (isTorchOn) QuantumCyan else SubatomicGray
                    )
                }

                // Lens switch toggle
                IconButton(
                    onClick = {
                        useFrontCamera = !useFrontCamera
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkMatter.copy(alpha = 0.8f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = QuantumCyan
                    )
                }
            }
        }

        // Bottom Quick Switcher: "SHOW MY QR INSTEAD" (resolves mutual camera impasse)
        if (onSwitchToMyQr != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
            ) {
                Button(
                    onClick = onSwitchToMyQr,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkMatter.copy(alpha = 0.90f),
                        contentColor = QuantumCyan
                    ),
                    border = BorderStroke(1.dp, QuantumCyan.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = QuantumCyan
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SHOW MY QR INSTEAD",
                        fontFamily = QuantumMonospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = QuantumCyan
                    )
                }
            }
        }

        // Handshake Confirmation Modal
        scannedPayload?.let { payload ->
            HandshakeConfirmDialog(
                payload = payload,
                handshakeManager = handshakeManager,
                onConfirm = { result ->
                    onPeerConfirmed(
                        result.peerUid,
                        result.peerIdentityPub,
                        result.peerOnion,
                        result.safetyNumber,
                        result.peerUsername,
                        result.peerProfileColor
                    )
                    scannedPayload = null
                    isTargetLocked = false
                },
                onDismiss = {
                    scannedPayload = null
                    isTargetLocked = false
                }
            )
        }

        // Device Migration Confirmation Modal
        scannedTransferPayload?.let { transferPayload ->
            DeviceMigrationConfirmDialog(
                payload = transferPayload,
                onConfirm = {
                    onTransferDetected?.invoke(transferPayload)
                    scannedTransferPayload = null
                    isTargetLocked = false
                },
                onDismiss = {
                    scannedTransferPayload = null
                    isTargetLocked = false
                }
            )
        }
    }
}

/**
 * High-performance, offline QR code decoder using bundled ZXing.
 * Decodes both standard (dark modules on light background) and inverted (light modules on dark background),
 * testing full-frame and center-cropped regions to maximize optical detection rate and reduce blur.
 */
private fun decodeQrWithZxing(imageProxy: ImageProxy): String? {
    try {
        val planes = imageProxy.planes
        if (planes.isEmpty()) return null
        val yPlane = planes[0]
        val buffer = yPlane.buffer ?: return null
        val width = imageProxy.width
        val height = imageProxy.height
        val rowStride = yPlane.rowStride

        val yBytes = ByteArray(rowStride * height)
        buffer.position(0)
        val remaining = buffer.remaining().coerceAtMost(yBytes.size)
        buffer.get(yBytes, 0, remaining)
        buffer.position(0)

        val source = PlanarYUVLuminanceSource(
            yBytes,
            rowStride,
            height,
            0,
            0,
            width,
            height,
            false
        )

        val reader = QRCodeReader()

        // Pass 1: Standard contrast (Black on White)
        try {
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            return reader.decode(bitmap).text
        } catch (_: Exception) {
        }

        // Pass 2: Inverted contrast (White on Black)
        try {
            val bitmapInverted = BinaryBitmap(HybridBinarizer(source.invert()))
            return reader.decode(bitmapInverted).text
        } catch (_: Exception) {
        }

        // Pass 3: Center cropped region (reduces peripheral noise, accelerates finder pattern lock)
        try {
            val cropWidth = (width * 0.70f).toInt().coerceAtMost(width)
            val cropHeight = (height * 0.70f).toInt().coerceAtMost(height)
            val cropLeft = (width - cropWidth) / 2
            val cropTop = (height - cropHeight) / 2
            val croppedSource = source.crop(cropLeft, cropTop, cropWidth, cropHeight)
            val bitmapCropped = BinaryBitmap(HybridBinarizer(croppedSource))
            return reader.decode(bitmapCropped).text
        } catch (_: Exception) {
        }

        // Pass 4: Center cropped inverted
        try {
            val cropWidth = (width * 0.70f).toInt().coerceAtMost(width)
            val cropHeight = (height * 0.70f).toInt().coerceAtMost(height)
            val cropLeft = (width - cropWidth) / 2
            val cropTop = (height - cropHeight) / 2
            val croppedSource = source.crop(cropLeft, cropTop, cropWidth, cropHeight)
            val bitmapCroppedInverted = BinaryBitmap(HybridBinarizer(croppedSource.invert()))
            return reader.decode(bitmapCroppedInverted).text
        } catch (_: Exception) {
        }
    } catch (_: Exception) {
        // Safe catch for any buffer or format irregularity
    }
    return null
}
