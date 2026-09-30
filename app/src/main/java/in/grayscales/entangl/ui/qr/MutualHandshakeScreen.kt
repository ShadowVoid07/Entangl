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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.window.Dialog
import `in`.grayscales.entangl.domain.model.TransferQrPayload
import `in`.grayscales.entangl.ui.chat.ChatViewModel
import `in`.grayscales.entangl.ui.theme.ColorUtils
import `in`.grayscales.entangl.ui.theme.CyberDark
import `in`.grayscales.entangl.ui.theme.DarkMatter
import `in`.grayscales.entangl.ui.theme.IsotopeMagenta
import `in`.grayscales.entangl.ui.theme.NeutronWhite
import `in`.grayscales.entangl.ui.theme.ParticleBorder
import `in`.grayscales.entangl.ui.theme.QuantumCyan
import `in`.grayscales.entangl.ui.theme.QuantumGreen
import `in`.grayscales.entangl.ui.theme.QuantumMonospace
import `in`.grayscales.entangl.ui.theme.SubatomicGray
import `in`.grayscales.entangl.ui.theme.VoidBackground
import kotlinx.coroutines.launch

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
    initialTab: Int = 0,
    onTransferDetected: ((TransferQrPayload) -> Unit)? = null,
    onBack: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onOpenSettings: () -> Unit = onSettingsClick,
    onMutualUnlocked: () -> Unit = {}
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

    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }

    // Bind local UID for self-scan rejection (a device cannot entangle with itself).
    androidx.compose.runtime.LaunchedEffect(chatViewModel.localUid) {
        chatViewModel.handshakeManager.localUidHint = chatViewModel.localUid
    }

    // Real-time peer detection: surfaces newly arriving handshake rows so the
    // scanner side can complete the reciprocal scan without leaving the screen.
    val contacts by chatViewModel.contacts.collectAsState()
    val initialUids = remember { contacts.map { it.uid }.toSet() }
    var dismissedPeerUids by remember { mutableStateOf(setOf<String>()) }
    val newlyDetectedPeer = contacts.firstOrNull {
        it.uid !in initialUids && it.uid !in dismissedPeerUids && !it.isAccepted
    }

    if (newlyDetectedPeer != null) {
        // Direction-aware handshake dialog. The old code showed "PEER SCANNED YOUR
        // BEACON" for every new row — including contacts WE created by scanning the
        // peer (outbound). That is why users saw "Peer scanned" right after THEY
        // scanned. Derive direction from flags set ONLY by real proofs:
        // hasScannedPeer <- our verified optical scan; hasBeenScanned <- verified SCAN_PING.
        val dlgYouScanned = newlyDetectedPeer.hasScannedPeer
        val dlgTheyScanned = newlyDetectedPeer.hasBeenScanned
        val dlgPeerName = newlyDetectedPeer.displayName?.ifBlank { null }
            ?: "Peer ${newlyDetectedPeer.uid.take(6).uppercase()}"
        val dlgTitle = when {
            dlgYouScanned && dlgTheyScanned -> "MUTUAL SCAN COMPLETE"
            dlgYouScanned -> "YOU SCANNED PEER"
            else -> "PEER SCANNED YOUR CODE"
        }
        val dlgBody = when {
            dlgYouScanned && dlgTheyScanned ->
                "Both codes scanned with $dlgPeerName.\nCompare safety numbers, then unlock below."
            dlgYouScanned ->
                "You scanned $dlgPeerName.\nKeep your code up for them to scan back — chat stays locked until then."
            else ->
                "$dlgPeerName scanned your code.\nScan them back to complete mutual verification."
        }
        // Outbound (we scanned): point at Show tab so peer can scan us.
        // Inbound (they scanned): point at Scan tab. Mutual: just dismiss to ledger.
        val dlgButtonText = when {
            dlgYouScanned && !dlgTheyScanned -> "SHOW MY CODE"
            else -> "SCAN PEER CODE"
        }
        val dlgButtonTab = if (dlgYouScanned && !dlgTheyScanned) 0 else 1
        Dialog(onDismissRequest = {
            dismissedPeerUids = dismissedPeerUids + newlyDetectedPeer.uid
        }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, QuantumGreen, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkMatter)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val peerColor = ColorUtils.parseColorOrDefault(newlyDetectedPeer.profileColor, QuantumCyan)
                    val peerName = newlyDetectedPeer.displayName?.ifBlank { null } ?: "Peer ${newlyDetectedPeer.uid.take(6).uppercase()}"
                    val peerInitials = peerName.take(2).uppercase()

                    // Peer Avatar Pod with actual profile color and initials
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(peerColor.copy(alpha = 0.2f))
                            .border(2.dp, peerColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = peerInitials,
                            fontFamily = QuantumMonospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = peerColor
                        )
                    }

                    Text(
                        text = dlgTitle,
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp,
                        color = QuantumGreen,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = dlgBody,
                        fontFamily = QuantumMonospace,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = SubatomicGray,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Navigation only — no state mutation. Scan proofs come exclusively
                        // from verified optical scans (outbound) and SCAN_PING receipts (inbound).
                        Button(
                            onClick = {
                                dismissedPeerUids = dismissedPeerUids + newlyDetectedPeer.uid
                                selectedTab = dlgButtonTab
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = QuantumCyan,
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
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = dlgButtonText,
                                fontFamily = QuantumMonospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = CyberDark
                            )
                        }
                    }

                    // Mutual progress ledger (stays on profile until both directions verified)
                    val youScanned = newlyDetectedPeer.hasScannedPeer
                    val theyScanned = newlyDetectedPeer.hasBeenScanned
                    Text(
                        text = "MUTUAL PROGRESS: YOU SCANNED ${if (youScanned) "✓" else "○"} • " +
                            "THEY SCANNED ${if (theyScanned) "✓" else "○"} • CHAT ${if (newlyDetectedPeer.isAccepted) "UNLOCKED" else "LOCKED"}",
                        fontFamily = QuantumMonospace,
                        fontSize = 9.sp,
                        color = SubatomicGray,
                        textAlign = TextAlign.Center
                    )
                    if (!youScanned) {
                        Text(
                            text = "You must scan their QR while on this profile, then compare 60-digit safety numbers.",
                            fontFamily = QuantumMonospace,
                            fontSize = 9.sp,
                            color = SubatomicGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

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
                onClick = onSettingsClick,
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
                                    text = "MY QR [BEACON]",
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

                    // Right Pane: SCAN QR [SENSOR]
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
                                    text = "SCAN QR [SENSOR]",
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
                                    onSwitchToScanner = { selectedTab = 1 },
                                    onBack = onBack
                                )
                            }
                            1 -> {
                                QrScannerView(
                                    handshakeManager = chatViewModel.handshakeManager,
                                    onPeerConfirmed = onPeerConfirmed,
                                    onTransferDetected = onTransferDetected,
                                    onSwitchToMyQr = { selectedTab = 0 },
                                    onBack = onBack
                                )
                            }
                        }
                    }
                }
            }
        }

        // Mutual unlock ledger: stays on profile until A scans B AND B scans A AND safety confirmed.
        // Lists half-complete handshakes with explicit progress and final unlock gate.
        val mutualPending = contacts.filter { !it.isAccepted && (it.hasScannedPeer || it.hasBeenScanned) }
        if (mutualPending.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                mutualPending.take(3).forEach { pending ->
                    MutualPendingCard(
                        peerName = pending.displayName?.ifBlank { null } ?: "Peer ${pending.uid.take(6).uppercase()}",
                        peerUid = pending.uid,
                        youScanned = pending.hasScannedPeer,
                        theyScanned = pending.hasBeenScanned,
                        safetyNumber = pending.safetyNumber,
                        onUnlock = {
                            // Suspend confirm returns real success; navigate only then.
                            // Scope here is composable-safe via rememberCoroutineScope in card.
                        },
                        onUnlockSuspend = { uid ->
                            chatViewModel.confirmMutualHandshake(uid, safetyConfirmed = true)
                        },
                        onUnlocked = onMutualUnlocked
                    )
                }
            }
        }
    }
}

@Composable
private fun MutualPendingCard(
    peerName: String,
    peerUid: String,
    youScanned: Boolean,
    theyScanned: Boolean,
    safetyNumber: String,
    modifier: Modifier = Modifier,
    onUnlock: () -> Unit = {},
    onUnlockSuspend: (suspend (String) -> Boolean)? = null,
    onUnlocked: () -> Unit = {}
) {
    val ready = youScanned && theyScanned && !safetyNumber.startsWith("Pending")
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var failed by androidx.compose.runtime.remember { mutableStateOf(false) }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, if (ready) QuantumGreen else QuantumCyan, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkMatter),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "MUTUAL: $peerName",
                fontFamily = QuantumMonospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = NeutronWhite
            )
            Text(
                text = "YOU SCANNED ${if (youScanned) "✓" else "○"} • THEY SCANNED ${if (theyScanned) "✓" else "○"}",
                fontFamily = QuantumMonospace,
                fontSize = 10.sp,
                color = SubatomicGray
            )
            if (!ready) {
                Text(
                    text = if (!youScanned) "Step 1/2: scan their code on the Scan tab."
                    else "Step 2/2: keep your code up for them to scan back, then compare safety numbers.",
                    fontFamily = QuantumMonospace,
                    fontSize = 10.sp,
                    color = SubatomicGray
                )
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            val ok = onUnlockSuspend?.invoke(peerUid) == true
                            if (ok) {
                                failed = false
                                onUnlock()
                                onUnlocked()
                            } else {
                                failed = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = QuantumGreen, contentColor = CyberDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "VERIFY SAFETY MATCH & UNLOCK CHAT",
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = CyberDark
                    )
                }
                if (failed) {
                    Text(
                        text = "Still incomplete — both scans and safety match are required.",
                        fontFamily = QuantumMonospace,
                        fontSize = 10.sp,
                        color = IsotopeMagenta
                    )
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
            // MY QR [BEACON]
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
                        text = "MY QR [BEACON]",
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = if (transmitSelected) CyberDark else SubatomicGray
                    )
                }
            }

            // SCAN QR [SENSOR]
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
                        text = "SCAN QR [SENSOR]",
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
