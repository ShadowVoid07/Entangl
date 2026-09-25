package `in`.grayscales.entangl.ui.qr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.grayscales.entangl.domain.model.TransferQrPayload
import `in`.grayscales.entangl.ui.chat.ChatViewModel
import `in`.grayscales.entangl.ui.theme.DarkMatter
import `in`.grayscales.entangl.ui.theme.QuantumCyan
import `in`.grayscales.entangl.ui.theme.QuantumMonospace
import `in`.grayscales.entangl.ui.theme.SubatomicGray
import `in`.grayscales.entangl.ui.theme.VoidBackground

/**
 * Unified Mutual Handshake Screen:
 * Consolidates optical QR beacon transmission and peer camera scanning
 * into a single high-security cryptographic key exchange portal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MutualHandshakeScreen(
    chatViewModel: ChatViewModel,
    localUsername: String,
    localProfileColor: String,
    onPeerConfirmed: (uid: String, key: ByteArray, onion: String, safetyNum: String, peerUsername: String, peerProfileColor: String) -> Unit,
    onTransferDetected: ((TransferQrPayload) -> Unit)? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBackground)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "MUTUAL HANDSHAKE",
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        letterSpacing = 2.sp,
                        color = QuantumCyan
                    )
                    Text(
                        text = "POST-QUANTUM OPTICAL KEY EXCHANGE",
                        fontFamily = QuantumMonospace,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        color = SubatomicGray
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = QuantumCyan
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = VoidBackground
            )
        )

        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkMatter,
            contentColor = QuantumCyan
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = "SCAN PEER",
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (selectedTab == 0) QuantumCyan else SubatomicGray
                    )
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (selectedTab == 0) QuantumCyan else SubatomicGray
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = "MY BEACON",
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (selectedTab == 1) QuantumCyan else SubatomicGray
                    )
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (selectedTab == 1) QuantumCyan else SubatomicGray
                    )
                }
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> {
                    QrScannerView(
                        handshakeManager = chatViewModel.handshakeManager,
                        onPeerConfirmed = onPeerConfirmed,
                        onTransferDetected = onTransferDetected,
                        onBack = onBack
                    )
                }
                1 -> {
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
        }
    }
}
