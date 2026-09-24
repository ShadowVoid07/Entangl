package `in`.grayscales.entangl.ui.transfer

import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.grayscales.entangl.data.network.TransferProgress
import `in`.grayscales.entangl.domain.model.TransferQrPayload
import `in`.grayscales.entangl.ui.chat.ChatViewModel
import `in`.grayscales.entangl.ui.qr.QrCodeGenerator
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
import kotlin.system.exitProcess

@Composable
fun DeviceTransferScreen(
    chatViewModel: ChatViewModel,
    onBack: () -> Unit,
    onScanQrForImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Export (Old Device), 1 = Import (New Device)
    val transferProgress by chatViewModel.localTransferManager.progress.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                chatViewModel.localTransferManager.cancelActiveTransfer()
                onBack()
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = QuantumCyan
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "DEVICE SUCCESSION",
                    fontFamily = QuantumMonospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = 2.sp,
                    color = QuantumCyan
                )
                Text(
                    text = "HARDWARE-BACKED LOCAL P2P MIGRATION",
                    fontFamily = QuantumMonospace,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                    color = SubatomicGray
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mode Switcher Tabs
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkMatter,
            contentColor = QuantumCyan,
            indicator = {
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(selectedTab),
                    color = QuantumCyan
                )
            },
            divider = {},
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, ParticleBorder, RoundedCornerShape(10.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = {
                    chatViewModel.localTransferManager.cancelActiveTransfer()
                    selectedTab = 0
                },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (selectedTab == 0) QuantumCyan else SubatomicGray
                        )
                        Text(
                            text = "SEND (OLD)",
                            fontFamily = QuantumMonospace,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp,
                            color = if (selectedTab == 0) QuantumCyan else SubatomicGray
                        )
                    }
                }
            )

            Tab(
                selected = selectedTab == 1,
                onClick = {
                    chatViewModel.localTransferManager.cancelActiveTransfer()
                    selectedTab = 1
                },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (selectedTab == 1) QuantumCyan else SubatomicGray
                        )
                        Text(
                            text = "RECEIVE (NEW)",
                            fontFamily = QuantumMonospace,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp,
                            color = if (selectedTab == 1) QuantumCyan else SubatomicGray
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Main Tab Content
        if (selectedTab == 0) {
            ExportTabContent(
                progress = transferProgress,
                pulseAlpha = pulseAlpha,
                onStartExport = { chatViewModel.localTransferManager.startExportServer() },
                onCancelExport = { chatViewModel.localTransferManager.cancelActiveTransfer() }
            )
        } else {
            ImportTabContent(
                progress = transferProgress,
                pulseAlpha = pulseAlpha,
                onScanQr = onScanQrForImport,
                onManualSubmit = { qrText ->
                    val payload = TransferQrPayload.fromQrString(qrText)
                    if (payload != null) {
                        chatViewModel.localTransferManager.startImportClient(payload)
                    } else {
                        Toast.makeText(context, "Invalid Transfer QR Payload", Toast.LENGTH_SHORT).show()
                    }
                },
                onCompleteReturn = {
                    chatViewModel.localTransferManager.cancelActiveTransfer()
                    onBack()
                }
            )
        }
    }
}

@Composable
private fun ExportTabContent(
    progress: TransferProgress,
    pulseAlpha: Float,
    onStartExport: () -> Unit,
    onCancelExport: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Warning Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkMatterVariant)
                .border(1.dp, IsotopeMagenta.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = IsotopeMagenta,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "DECOMMISSION PROTOCOL",
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = IsotopeMagenta
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Migrating to a new device will generate a signed Succession Certificate delegating your hardware identity key. Once verified by the new device, THIS device will permanently zeroize all Keystore aliases and erase all local data to eliminate ghost device vulnerabilities.",
                        fontFamily = QuantumMonospace,
                        fontSize = 10.sp,
                        lineHeight = 15.sp,
                        color = NeutronWhite.copy(alpha = 0.9f)
                    )
                }
            }
        }

        // State Machine Renderer
        when (progress) {
            is TransferProgress.Idle -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkMatter)
                        .border(1.dp, ParticleBorder, RoundedCornerShape(14.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = QuantumCyan,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "READY FOR LOCAL HANDOFF",
                            fontFamily = QuantumMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = NeutronWhite
                        )
                        Text(
                            text = "1. Ensure both devices are on the same Wi-Fi network or Mobile Hotspot.\n2. Tap 'INITIATE SECURE EXPORT' to start local P2P server.\n3. Scan the generated QR code on your new device.",
                            fontFamily = QuantumMonospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = SubatomicGray,
                            textAlign = TextAlign.Start
                        )
                        Button(
                            onClick = onStartExport,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = QuantumCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "INITIATE SECURE EXPORT",
                                fontFamily = QuantumMonospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            is TransferProgress.WaitingForPeer -> {
                val qrBitmap = remember(progress.qrPayload) {
                    QrCodeGenerator.generate(progress.qrPayload.toQrString(), sizePx = 640)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkMatter)
                        .border(1.dp, QuantumCyan.copy(alpha = pulseAlpha), RoundedCornerShape(16.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "SCAN WITH NEW DEVICE",
                            fontFamily = QuantumMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp,
                            color = QuantumCyan
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = qrBitmap,
                                contentDescription = "Transfer QR",
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Text(
                            text = "LISTENING ON ${progress.qrPayload.ip}:${progress.qrPayload.port}",
                            fontFamily = QuantumMonospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = QuantumGreen
                        )

                        Text(
                            text = "Awaiting optical connection from new device...",
                            fontFamily = QuantumMonospace,
                            fontSize = 10.sp,
                            color = SubatomicGray
                        )

                        OutlinedButton(
                            onClick = onCancelExport,
                            modifier = Modifier.fillMaxWidth(0.7f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = IsotopeMagenta),
                            border = androidx.compose.foundation.BorderStroke(1.dp, IsotopeMagenta),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "CANCEL EXPORT",
                                fontFamily = QuantumMonospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            is TransferProgress.Transferring -> {
                TransferringCard(message = progress.message, pulseAlpha = pulseAlpha)
            }

            is TransferProgress.Completed -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkMatter)
                        .border(1.dp, QuantumGreen, RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = QuantumGreen,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "SUCCESSION COMPLETE",
                            fontFamily = QuantumMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 2.sp,
                            color = QuantumGreen
                        )
                        Text(
                            text = progress.message,
                            fontFamily = QuantumMonospace,
                            fontSize = 11.sp,
                            color = NeutronWhite,
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = { exitProcess(0) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IsotopeMagenta,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "EXIT APPLICATION",
                                fontFamily = QuantumMonospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            is TransferProgress.Failed -> {
                FailedCard(error = progress.error, onRetry = onStartExport)
            }
        }
    }
}

@Composable
private fun ImportTabContent(
    progress: TransferProgress,
    pulseAlpha: Float,
    onScanQr: () -> Unit,
    onManualSubmit: (String) -> Unit,
    onCompleteReturn: () -> Unit
) {
    var manualQrText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (progress) {
            is TransferProgress.Idle -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkMatter)
                        .border(1.dp, ParticleBorder, RoundedCornerShape(14.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = QuantumCyan,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "IMPORT FROM OLD DEVICE",
                            fontFamily = QuantumMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = NeutronWhite
                        )
                        Text(
                            text = "Scan the QR code displayed on your old device to establish a direct, encrypted local P2P link (AES-256-GCM) and transfer all contacts and message records.",
                            fontFamily = QuantumMonospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = SubatomicGray,
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = onScanQr,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = QuantumCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SCAN OLD DEVICE QR CODE",
                                fontFamily = QuantumMonospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Manual fallback text field
                        OutlinedTextField(
                            value = manualQrText,
                            onValueChange = { manualQrText = it },
                            label = {
                                Text(
                                    text = "OR PASTE TRANSFER PAYLOAD",
                                    fontFamily = QuantumMonospace,
                                    fontSize = 10.sp
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = QuantumCyan,
                                unfocusedBorderColor = ParticleBorder,
                                focusedTextColor = NeutronWhite,
                                unfocusedTextColor = NeutronWhite
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = QuantumMonospace,
                                fontSize = 10.sp
                            ),
                            singleLine = true
                        )

                        if (manualQrText.isNotBlank()) {
                            OutlinedButton(
                                onClick = { onManualSubmit(manualQrText) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = QuantumCyan),
                                border = androidx.compose.foundation.BorderStroke(1.dp, QuantumCyan),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "CONNECT VIA PAYLOAD",
                                    fontFamily = QuantumMonospace,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            is TransferProgress.WaitingForPeer -> {
                // Should not occur on import client
                CircularProgressIndicator(color = QuantumCyan)
            }

            is TransferProgress.Transferring -> {
                TransferringCard(message = progress.message, pulseAlpha = pulseAlpha)
            }

            is TransferProgress.Completed -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkMatter)
                        .border(1.dp, QuantumGreen, RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = QuantumGreen,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "IMPORT SUCCESSFUL",
                            fontFamily = QuantumMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 2.sp,
                            color = QuantumGreen
                        )
                        Text(
                            text = progress.message,
                            fontFamily = QuantumMonospace,
                            fontSize = 11.sp,
                            color = NeutronWhite,
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = onCompleteReturn,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = QuantumGreen,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "OPEN CONSOLE & MESSAGES",
                                fontFamily = QuantumMonospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            is TransferProgress.Failed -> {
                FailedCard(error = progress.error, onRetry = onScanQr)
            }
        }
    }
}

@Composable
private fun TransferringCard(message: String, pulseAlpha: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkMatter)
            .border(1.dp, QuantumCyan.copy(alpha = pulseAlpha), RoundedCornerShape(16.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = QuantumCyan,
                modifier = Modifier.size(48.dp),
                strokeWidth = 3.dp
            )
            Text(
                text = "P2P ENCRYPTED TRANSFER ACTIVE",
                fontFamily = QuantumMonospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                color = QuantumCyan
            )
            Text(
                text = message,
                fontFamily = QuantumMonospace,
                fontSize = 11.sp,
                color = NeutronWhite,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FailedCard(error: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkMatter)
            .border(1.dp, IsotopeMagenta, RoundedCornerShape(16.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = IsotopeMagenta,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "TRANSFER ERROR",
                fontFamily = QuantumMonospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = IsotopeMagenta
            )
            Text(
                text = error,
                fontFamily = QuantumMonospace,
                fontSize = 11.sp,
                color = NeutronWhite,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IsotopeMagenta,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "RETRY OPERATION",
                    fontFamily = QuantumMonospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
