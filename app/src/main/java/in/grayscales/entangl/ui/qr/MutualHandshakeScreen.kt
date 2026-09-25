package `in`.grayscales.entangl.ui.qr

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.grayscales.entangl.domain.model.TransferQrPayload
import `in`.grayscales.entangl.ui.chat.ChatViewModel
import `in`.grayscales.entangl.ui.theme.CyberDark
import `in`.grayscales.entangl.ui.theme.DarkMatter
import `in`.grayscales.entangl.ui.theme.ParticleBorder
import `in`.grayscales.entangl.ui.theme.QuantumCyan
import `in`.grayscales.entangl.ui.theme.QuantumGreen
import `in`.grayscales.entangl.ui.theme.QuantumMonospace
import `in`.grayscales.entangl.ui.theme.SubatomicGray
import `in`.grayscales.entangl.ui.theme.VoidBackground

/**
 * Unified Mutual Handshake Screen:
 * Clean, un-congested optical key exchange suite with side-by-side foldable/tablet
 * split-pane layout and vertical segmented tab workflow for single-handed phones.
 * Enforces FLAG_SECURE strictly at the Compose layer.
 */
@Composable
fun MutualHandshakeScreen(
    chatViewModel: ChatViewModel,
    localUsername: String,
    localProfileColor: String,
    onPeerConfirmed: (uid: String, key: ByteArray, onion: String, safetyNum: String, peerUsername: String, peerProfileColor: String) -> Unit,
    modifier: Modifier = Modifier,
    onTransferDetected: ((TransferQrPayload) -> Unit)? = null,
    onBack: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current

    // Apply FLAG_SECURE strictly at the Compose layer
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        val originalFlags = window?.attributes?.flags ?: 0
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            if (window != null && (originalFlags and WindowManager.LayoutParams.FLAG_SECURE) == 0) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBackground)
    ) {
        // Clean, un-congested top header using Row with Arrangement.SpaceBetween
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkMatter)
                    .border(1.dp, ParticleBorder, RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = QuantumCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Centered "MUTUAL HANDSHAKE" title (bold) with a green shield icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Secure Shield",
                    tint = QuantumGreen,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "MUTUAL HANDSHAKE",
                    fontFamily = QuantumMonospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 2.sp,
                    color = QuantumCyan
                )
            }

            // Right-side Settings icon
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkMatter)
                    .border(1.dp, ParticleBorder, RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = QuantumCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Split-pane layout using BoxWithConstraints
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val isDualPane = maxWidth > 600.dp

            if (isDualPane) {
                // Side-by-side split-pane for foldables / tablets / landscape
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Pane: TRANSMIT [BEACON]
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkMatter)
                                .border(1.dp, ParticleBorder, RoundedCornerShape(8.dp))
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = null,
                                    tint = QuantumCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "TRANSMIT [BEACON]",
                                    fontFamily = QuantumMonospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp,
                                    color = QuantumCyan
                                )
                            }
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            MyQrScreen(
                                handshakeManager = chatViewModel.handshakeManager,
                                localUid = chatViewModel.localUid,
                                localOnion = chatViewModel.localOnion,
                                localUsername = localUsername,
                                localProfileColor = localProfileColor,
                                onBack = onBack
                            )
                        }
                    }

                    // Vertical partition line
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(ParticleBorder)
                    )

                    // Right Pane: RECEIVE [SENSOR]
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkMatter)
                                .border(1.dp, ParticleBorder, RoundedCornerShape(8.dp))
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = QuantumGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "RECEIVE [SENSOR]",
                                    fontFamily = QuantumMonospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp,
                                    color = QuantumGreen
                                )
                            }
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            QrScannerView(
                                handshakeManager = chatViewModel.handshakeManager,
                                onPeerConfirmed = onPeerConfirmed,
                                onTransferDetected = onTransferDetected,
                                onBack = onBack
                            )
                        }
                    }
                }
            } else {
                // Vertical layout for phones with segmented tab row
                Column(modifier = Modifier.fillMaxSize()) {
                    SegmentedHandshakeTabRow(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (selectedTab) {
                            0 -> {
                                MyQrScreen(
                                    handshakeManager = chatViewModel.handshakeManager,
                                    localUid = chatViewModel.localUid,
                                    localOnion = chatViewModel.localOnion,
                                    localUsername = localUsername,
                                    localProfileColor = localProfileColor,
                                    onBack = onBack
                                )
                            }
                            1 -> {
                                QrScannerView(
                                    handshakeManager = chatViewModel.handshakeManager,
                                    onPeerConfirmed = onPeerConfirmed,
                                    onTransferDetected = onTransferDetected,
                                    onBack = onBack
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentedHandshakeTabRow(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(DarkMatter)
            .border(1.dp, ParticleBorder, RoundedCornerShape(10.dp))
            .padding(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // TRANSMIT [BEACON]
            val transmitSelected = selectedTab == 0
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (transmitSelected) QuantumCyan else Color.Transparent)
                    .clickable { onTabSelected(0) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        tint = if (transmitSelected) CyberDark else SubatomicGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "TRANSMIT [BEACON]",
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = if (transmitSelected) CyberDark else SubatomicGray
                    )
                }
            }

            // RECEIVE [SENSOR]
            val receiveSelected = selectedTab == 1
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (receiveSelected) QuantumCyan else Color.Transparent)
                    .clickable { onTabSelected(1) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = if (receiveSelected) CyberDark else SubatomicGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "RECEIVE [SENSOR]",
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = if (receiveSelected) CyberDark else SubatomicGray
                    )
                }
            }
        }
    }
}
