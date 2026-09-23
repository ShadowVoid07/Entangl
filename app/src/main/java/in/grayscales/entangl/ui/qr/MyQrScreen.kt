package `in`.grayscales.entangl.ui.qr

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.grayscales.entangl.core.crypto.HandshakeManager
import `in`.grayscales.entangl.core.util.toHex
import `in`.grayscales.entangl.domain.model.HandshakePayload
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
import kotlinx.coroutines.delay

@Composable
fun MyQrScreen(
    handshakeManager: HandshakeManager,
    localUid: String,
    localOnion: String,
    localUsername: String = "",
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var payload by remember {
        mutableStateOf(handshakeManager.generateInitiatorPayload(localUid, localOnion, localUsername))
    }
    var qrBitmap by remember {
        mutableStateOf<ImageBitmap?>(null)
    }
    var secondsRemaining by remember {
        mutableIntStateOf(60)
    }

    fun regenerate() {
        payload = handshakeManager.generateInitiatorPayload(localUid, localOnion, localUsername)
        qrBitmap = QrCodeGenerator.generate(payload.toQrString(), sizePx = 640)
        secondsRemaining = 60
    }

    // Generate initial QR bitmap
    LaunchedEffect(payload) {
        qrBitmap = QrCodeGenerator.generate(payload.toQrString(), sizePx = 640)
    }

    // 60-second rolling countdown timer
    LaunchedEffect(payload) {
        while (secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining -= 1
        }
        regenerate()
    }

    val timerColor by animateColorAsState(
        targetValue = if (secondsRemaining <= 10) IsotopeMagenta else QuantumCyan,
        label = "timerColor"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBackground)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = QuantumCyan
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "QUANTUM UPLINK BEACON",
                    fontFamily = QuantumMonospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 2.sp,
                    color = QuantumCyan
                )
                Text(
                    text = "STAGE 1: INITIATE PHYSICAL HANDSHAKE",
                    fontFamily = QuantumMonospace,
                    fontSize = 10.sp,
                    color = SubatomicGray
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // QR Frame Card
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .background(DarkMatter)
                .border(1.dp, timerColor, RoundedCornerShape(16.dp))
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
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                TelemetryRow(label = "TOR ONION", value = localOnion.take(16) + "..." + localOnion.takeLast(10))
                TelemetryRow(label = "IDENTITY KEY", value = payload.identityPub.take(8).toByteArray().toHex() + "...")
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
