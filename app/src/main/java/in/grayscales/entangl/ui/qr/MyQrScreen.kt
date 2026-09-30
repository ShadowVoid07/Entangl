package `in`.grayscales.entangl.ui.qr

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.grayscales.entangl.core.crypto.HandshakeManager
import `in`.grayscales.entangl.core.util.toHex
import `in`.grayscales.entangl.ui.theme.CyberDark
import `in`.grayscales.entangl.ui.theme.DarkMatter
import `in`.grayscales.entangl.ui.theme.DarkMatterVariant
import `in`.grayscales.entangl.ui.theme.IsotopeMagenta
import `in`.grayscales.entangl.ui.theme.NeutronWhite
import `in`.grayscales.entangl.ui.theme.ParticleBorder
import `in`.grayscales.entangl.ui.theme.QuantumCyan
import `in`.grayscales.entangl.ui.theme.QuantumGreen
import `in`.grayscales.entangl.ui.theme.QuantumMonospace
import `in`.grayscales.entangl.ui.theme.SubatomicGray
import `in`.grayscales.entangl.ui.theme.VoidBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

@Composable
fun MyQrScreen(
    handshakeManager: HandshakeManager,
    localUid: String,
    localOnion: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    localUsername: String = "",
    localProfileColor: String = "",
    onSwitchToScanner: (() -> Unit)? = null
) {
    var payload by remember {
        mutableStateOf(handshakeManager.generateInitiatorPayload(localUid, localOnion, localUsername, localProfileColor))
    }
    var qrBitmap by remember {
        mutableStateOf<ImageBitmap?>(null)
    }
    var secondsRemaining by remember {
        mutableIntStateOf(60)
    }

    fun regenerate() {
        payload = handshakeManager.generateInitiatorPayload(localUid, localOnion, localUsername, localProfileColor)
        // Bitmap follows asynchronously via LaunchedEffect(payload) below.
        secondsRemaining = 60
    }

    // Generate QR bitmap off the Main thread: 640px ZXing encode janked rotation.
    LaunchedEffect(payload) {
        val bitmap = withContext(Dispatchers.Default) {
            QrCodeGenerator.generate(payload.toQrString(), sizePx = 640)
        }
        qrBitmap = bitmap
    }

    // 60-second rolling countdown timer
    LaunchedEffect(payload) {
        while (secondsRemaining > 0) {
            delay(1.seconds)
            secondsRemaining -= 1
        }
        regenerate()
    }

    val timerColor by animateColorAsState(
        targetValue = if (secondsRemaining <= 10) IsotopeMagenta else QuantumCyan,
        label = "timerColor"
    )

    val ttlProgress by animateFloatAsState(
        targetValue = (secondsRemaining / 60f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "ttlProgress"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBackground)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar - Cleaned of redundant back arrow
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "MY BEACON QR",
                fontFamily = QuantumMonospace,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 2.sp,
                color = QuantumCyan
            )
            Text(
                text = "Hold this screen out for your peer to scan with their camera.",
                fontFamily = QuantumMonospace,
                fontSize = 11.sp,
                color = NeutronWhite
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // QR Frame Card with Dynamic Animated TTL Depleting Border
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .background(DarkMatter)
                .drawWithCache {
                    val strokeWidth = 2.dp.toPx()
                    val halfStroke = strokeWidth / 2f
                    val cornerRadiusPx = 16.dp.toPx() - halfStroke

                    val bounds = Rect(
                        left = halfStroke,
                        top = halfStroke,
                        right = size.width - halfStroke,
                        bottom = size.height - halfStroke
                    )

                    val topCenterX = (bounds.left + bounds.right) / 2f
                    val r = cornerRadiusPx

                    val fullPath = Path().apply {
                        // Start at top-middle anchor (12 o'clock)
                        moveTo(topCenterX, bounds.top)
                        // Top edge to top-right corner
                        lineTo(bounds.right - r, bounds.top)
                        arcTo(
                            rect = Rect(bounds.right - 2 * r, bounds.top, bounds.right, bounds.top + 2 * r),
                            startAngleDegrees = 270f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false
                        )
                        // Right edge to bottom-right corner
                        lineTo(bounds.right, bounds.bottom - r)
                        arcTo(
                            rect = Rect(bounds.right - 2 * r, bounds.bottom - 2 * r, bounds.right, bounds.bottom),
                            startAngleDegrees = 0f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false
                        )
                        // Bottom edge to bottom-left corner
                        lineTo(bounds.left + r, bounds.bottom)
                        arcTo(
                            rect = Rect(bounds.left, bounds.bottom - 2 * r, bounds.left + 2 * r, bounds.bottom),
                            startAngleDegrees = 90f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false
                        )
                        // Left edge to top-left corner
                        lineTo(bounds.left, bounds.top + r)
                        arcTo(
                            rect = Rect(bounds.left, bounds.top, bounds.left + 2 * r, bounds.top + 2 * r),
                            startAngleDegrees = 180f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false
                        )
                        // Back to top-middle
                        lineTo(topCenterX, bounds.top)
                        close()
                    }

                    val pathMeasure = PathMeasure().apply {
                        setPath(fullPath, false)
                    }
                    val totalLength = pathMeasure.length

                    val segmentPath = Path()
                    if (ttlProgress > 0f) {
                        val startDistance = (totalLength * (1f - ttlProgress)).coerceIn(0f, totalLength)
                        pathMeasure.getSegment(
                            startDistance = startDistance,
                            stopDistance = totalLength,
                            destination = segmentPath,
                            startWithMoveTo = true
                        )
                    }

                    onDrawWithContent {
                        drawContent()

                        // Subtle base perimeter track
                        drawRoundRect(
                            color = ParticleBorder,
                            topLeft = bounds.topLeft,
                            size = bounds.size,
                            cornerRadius = CornerRadius(cornerRadiusPx),
                            style = Stroke(width = strokeWidth)
                        )

                        // Dynamic depleting TTL progress border in QuantumCyan / timerColor
                        drawPath(
                            path = segmentPath,
                            color = timerColor,
                            style = Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round
                            )
                        )
                    }
                }
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (qrBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = qrBitmap!!,
                            contentDescription = "Dynamic Handshake QR",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .background(DarkMatterVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = QuantumCyan)
                    }
                }

                if (localUsername.isNotBlank()) {
                    Text(
                        text = "SHARING CODENAME: $localUsername",
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = QuantumCyan
                    )
                }

                // Rolling Nonce Timer Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(timerColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ROLLING NONCE TTL: ${secondsRemaining}s",
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = timerColor
                    )
                }

                // Reassuring forward-secrecy micro-copy
                Text(
                    text = "⏳ Nonce auto-renews every 60s for forward secrecy — take your time.",
                    fontFamily = QuantumMonospace,
                    fontSize = 10.sp,
                    color = SubatomicGray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Switch to Camera Scanner button if switcher callback is provided
        if (onSwitchToScanner != null) {
            Button(
                onClick = onSwitchToScanner,
                modifier = Modifier.fillMaxWidth(0.92f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = QuantumGreen,
                    contentColor = CyberDark
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = CyberDark
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SCAN PEER'S QR INSTEAD",
                    fontFamily = QuantumMonospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberDark
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Regenerate Button
        OutlinedButton(
            onClick = { regenerate() },
            modifier = Modifier.fillMaxWidth(0.92f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = QuantumCyan
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, QuantumCyan),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "REGENERATE UPLINK NONCE",
                fontFamily = QuantumMonospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Cryptographic Telemetry Box
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkMatterVariant)
                .border(1.dp, ParticleBorder, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "TRANSMISSION PARAMETERS",
                    fontFamily = QuantumMonospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = QuantumCyan
                )
                TelemetryRow(label = "PROTOCOL", value = "Entangl v1.0 (CBOR/PQXDH)")
                TelemetryRow(label = "KEY ALGORITHM", value = "Ed25519 (Identity) + X25519 (Eph)")
                TelemetryRow(label = "ROUTING MESH", value = localOnion.take(16) + "..." + localOnion.takeLast(10))
                TelemetryRow(label = "IDENTITY KEY", value = payload.identityPub.take(8).toByteArray().toHex() + "...")
                TelemetryRow(label = "PROFILE COLOR", value = payload.profileColor.ifBlank { "#00F0FF" })
                TelemetryRow(label = "NONCE SAMPLE", value = payload.nonce.take(8).toByteArray().toHex() + "...")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Peer must scan this QR with their Entangl camera to verify cryptographic signature.",
            fontFamily = QuantumMonospace,
            fontSize = 11.sp,
            color = SubatomicGray,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = QuantumMonospace,
            fontSize = 10.sp,
            color = SubatomicGray
        )
        Text(
            text = value,
            fontFamily = QuantumMonospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = NeutronWhite
        )
    }
}
