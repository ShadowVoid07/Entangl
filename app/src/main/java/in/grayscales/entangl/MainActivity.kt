package `in`.grayscales.entangl

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import `in`.grayscales.entangl.core.security.PlatformSecurity
import `in`.grayscales.entangl.core.security.SecurityEvent
import `in`.grayscales.entangl.data.network.EntanglRelayService
import `in`.grayscales.entangl.ui.chat.ChatViewModel
import `in`.grayscales.entangl.ui.navigation.QuantumTwoPaneLayout
import `in`.grayscales.entangl.ui.onboarding.UsernameSetupScreen
import `in`.grayscales.entangl.ui.qr.MyQrScreen
import `in`.grayscales.entangl.ui.qr.QrScannerView
import `in`.grayscales.entangl.ui.theme.DarkMatter
import `in`.grayscales.entangl.ui.theme.DarkMatterVariant
import `in`.grayscales.entangl.ui.theme.EntanglTheme
import `in`.grayscales.entangl.ui.theme.IsotopeMagenta
import `in`.grayscales.entangl.ui.theme.NeutronWhite
import `in`.grayscales.entangl.ui.theme.ParticleBorder
import `in`.grayscales.entangl.ui.theme.QuantumCyan
import `in`.grayscales.entangl.ui.theme.QuantumGreen
import `in`.grayscales.entangl.ui.theme.QuantumMonospace
import `in`.grayscales.entangl.ui.theme.SubatomicGray
import `in`.grayscales.entangl.ui.theme.VoidBackground
import `in`.grayscales.entangl.ui.transfer.DeviceTransferScreen
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

enum class AppScreen {
    MESSAGES,
    MY_QR,
    SCAN_QR,
    DASHBOARD,
    DEVICE_TRANSFER
}

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CONTACT_UID = "in.grayscales.entangl.extra.CONTACT_UID"
    }

    private val platformSecurity: PlatformSecurity by inject()
    private val keyDestructionService: `in`.grayscales.entangl.core.security.KeyDestructionService by inject()
    private val chatViewModel: ChatViewModel by viewModel()
    private val currentScreenState = mutableStateOf(AppScreen.MESSAGES)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Enforce FLAG_SECURE & touch filtering
        platformSecurity.applyWindowProtection(window)

        if (keyDestructionService.isDeviceDecommissioned()) {
            setContent {
                EntanglTheme {
                    Box(
                        modifier = Modifier.fillMaxSize().background(VoidBackground).padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "DEVICE PERMANENTLY DECOMMISSIONED\n\nAll cryptographic keys and data have been wiped following device migration handoff.",
                            color = IsotopeMagenta,
                            fontFamily = QuantumMonospace,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
            return
        }

        val activeThreats = platformSecurity.checkThreats()

        handleIncomingIntent(intent)

        // Start background relay service
        val serviceIntent = Intent(this, EntanglRelayService::class.java)
        startForegroundService(serviceIntent)

        setContent {
            EntanglTheme {
                // Request notification permission on Android 13+ (Tiramisu)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val notifLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { /* permission handled */ }

                    LaunchedEffect(Unit) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                val isUsernameSet by chatViewModel.isUsernameSet.collectAsState()
                val currentUsername by chatViewModel.username.collectAsState()
                val currentProfileColor by chatViewModel.profileColor.collectAsState()
                val promptReciprocalScanContact by chatViewModel.promptReciprocalScanForContact.collectAsState()

                if (!isUsernameSet) {
                    UsernameSetupScreen(
                        onConfirm = { chosenName, chosenColor ->
                            chatViewModel.setProfile(chosenName, chosenColor)
                        },
                        initialColorHex = currentProfileColor
                    )
                } else {
                    var currentScreen by remember { currentScreenState }

                    val contacts by chatViewModel.contacts.collectAsState()
                    val activeContact by chatViewModel.activeContact.collectAsState()
                    val activeMessages by chatViewModel.activeMessages.collectAsState()
                    val selfDestructDuration by chatViewModel.selfDestructDuration.collectAsState()

                    // Reciprocal scan prompt dialog
                    promptReciprocalScanContact?.let { peer ->
                        Dialog(onDismissRequest = { chatViewModel.dismissReciprocalScanPrompt() }) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, QuantumCyan, RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = DarkMatter)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(QuantumCyan.copy(alpha = 0.15f))
                                            .border(1.dp, QuantumCyan, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = null,
                                            tint = QuantumCyan,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Text(
                                        text = "CONNECTION ACCEPTED",
                                        fontFamily = QuantumMonospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        letterSpacing = 2.sp,
                                        color = QuantumCyan,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )

                                    Text(
                                        text = "${peer.displayName ?: "Peer"} can now send you messages. Scan their QR code to enable full 2-way encryption and unlock replies.",
                                        fontFamily = QuantumMonospace,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        color = SubatomicGray,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                chatViewModel.dismissReciprocalScanPrompt()
                                                currentScreen = AppScreen.SCAN_QR
                                            },
                                            modifier = Modifier.weight(1.2f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = QuantumCyan,
                                                contentColor = Color.Black
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "Scan QR Code",
                                                fontFamily = QuantumMonospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = { chatViewModel.dismissReciprocalScanPrompt() },
                                            modifier = Modifier.weight(0.8f),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SubatomicGray),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, ParticleBorder),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "Later",
                                                fontFamily = QuantumMonospace,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Intercept back gesture to return to main messages list
                    BackHandler(enabled = currentScreen != AppScreen.MESSAGES) {
                        currentScreen = AppScreen.MESSAGES
                    }

                    // Intercept back gesture in candybar mode to return to contacts
                    BackHandler(enabled = currentScreen == AppScreen.MESSAGES && activeContact != null) {
                        chatViewModel.selectContact(null)
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = VoidBackground
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .consumeWindowInsets(innerPadding)
                                .imePadding()
                        ) {
                            when (currentScreen) {
                                AppScreen.MESSAGES -> {
                                    QuantumTwoPaneLayout(
                                        contacts = contacts,
                                        activeContact = activeContact,
                                        messages = activeMessages,
                                        selfDestructDuration = selfDestructDuration,
                                        onSelectContact = { contact -> chatViewModel.selectContact(contact) },
                                        onDeleteContact = { contact -> chatViewModel.deleteContact(contact) },
                                        onAcceptContact = { contact -> chatViewModel.acceptContact(contact) },
                                        onSendMessage = { text -> chatViewModel.sendMessage(text) },
                                        onSetSelfDestruct = { dur -> chatViewModel.setSelfDestructDuration(dur) },
                                        onScanQr = { currentScreen = AppScreen.SCAN_QR },
                                        onShowMyQr = { currentScreen = AppScreen.MY_QR },
                                        onOpenDashboard = { currentScreen = AppScreen.DASHBOARD },
                                        localUsername = currentUsername,
                                        localProfileColor = currentProfileColor,
                                        onUpdateProfile = { name, color -> chatViewModel.setProfile(name, color) }
                                    )
                                }

                                AppScreen.MY_QR -> {
                                    MyQrScreen(
                                        handshakeManager = chatViewModel.handshakeManager,
                                        localUid = chatViewModel.localUid,
                                        localOnion = chatViewModel.localOnion,
                                        localUsername = currentUsername,
                                        localProfileColor = currentProfileColor,
                                        onBack = { currentScreen = AppScreen.MESSAGES }
                                    )
                                }

                                AppScreen.SCAN_QR -> {
                                    QrScannerView(
                                        handshakeManager = chatViewModel.handshakeManager,
                                        onPeerConfirmed = { uid, key, onion, safetyNum, peerUsername, peerProfileColor ->
                                            chatViewModel.addContactFromHandshake(uid, key, onion, safetyNum, peerUsername, peerProfileColor)
                                            currentScreen = AppScreen.MESSAGES
                                        },
                                        onTransferDetected = { payload ->
                                            chatViewModel.localTransferManager.startImportClient(payload)
                                            currentScreen = AppScreen.DEVICE_TRANSFER
                                        },
                                        onBack = { currentScreen = AppScreen.MESSAGES }
                                    )
                                }

                                AppScreen.DASHBOARD -> {
                                    QuantumDashboardScreen(
                                        threats = activeThreats,
                                        localUsername = currentUsername,
                                        onBack = { currentScreen = AppScreen.MESSAGES },
                                        onDeviceTransfer = { currentScreen = AppScreen.DEVICE_TRANSFER }
                                    )
                                }

                                AppScreen.DEVICE_TRANSFER -> {
                                    DeviceTransferScreen(
                                        chatViewModel = chatViewModel,
                                        onBack = { currentScreen = AppScreen.MESSAGES },
                                        onScanQrForImport = { currentScreen = AppScreen.SCAN_QR }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val contactUid = intent?.getStringExtra(EXTRA_CONTACT_UID)
        if (!contactUid.isNullOrBlank()) {
            currentScreenState.value = AppScreen.MESSAGES
            chatViewModel.selectContactByUid(contactUid)
        }
    }
}

@Composable
fun QuantumDashboardScreen(
    threats: List<SecurityEvent>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    localUsername: String = "",
    onDeviceTransfer: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBackground)
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // App header with Back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = QuantumCyan
                    )
                }
                Column {
                    Text(
                        text = "ENTANGL CONSOLE",
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        letterSpacing = 3.sp,
                        color = QuantumCyan
                    )
                    Text(
                        text = "QUANTUM-RESISTANT ZERO-KNOWLEDGE P2P",
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.Normal,
                        fontSize = 9.sp,
                        letterSpacing = 1.sp,
                        color = SubatomicGray
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (threats.isEmpty()) QuantumGreen else IsotopeMagenta)
            )
        }

        // Security Status Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkMatter)
                .border(1.dp, ParticleBorder, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "CORE PROTOCOL MATRIX",
                        fontFamily = QuantumMonospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = QuantumCyan
                    )
                    Text(
                        text = if (threats.isEmpty()) "SECURE" else "WARNING",
                        fontFamily = QuantumMonospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (threats.isEmpty()) QuantumGreen else IsotopeMagenta
                    )
                }

                SecurityRow(label = "ASYMMETRIC IDENTITY", value = "Ed25519 (Hardware Keystore)")
                SecurityRow(label = "POST-QUANTUM KEM", value = "ML-KEM-768 (PQXDH)")
                SecurityRow(label = "FORWARD SECRECY", value = "Double Ratchet + HMAC-SHA256")
                SecurityRow(label = "STORAGE ENCRYPTION", value = "SQLCipher + Double-Encrypted")
                SecurityRow(label = "MEMORY ZEROIZATION", value = "Off-Heap NativeKeyBuffer")
                SecurityRow(label = "ACTIVE CODENAME", value = localUsername.ifBlank { "Anonymous Node" })
                SecurityRow(label = "DISPLAY INTEGRITY", value = "FLAG_SECURE + Obscured Touch Filter")
                SecurityRow(label = "ZERO LEAK NOTIFICATION", value = "VISIBILITY_SECRET Enforced")
            }
        }

        // Device Migration & Succession Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkMatter)
                .border(1.dp, ParticleBorder, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "DEVICE SUCCESSION & MIGRATION",
                    fontFamily = QuantumMonospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = QuantumCyan
                )
                Text(
                    text = "Transfer account, contacts, and message history to a new device over local encrypted P2P with hardware identity succession and atomic decommissioning.",
                    fontFamily = QuantumMonospace,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = SubatomicGray
                )
                Button(
                    onClick = onDeviceTransfer,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = QuantumCyan,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "OPEN DEVICE TRANSFER",
                        fontFamily = QuantumMonospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Active threat indicator if any detected
        if (threats.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkMatterVariant)
                    .border(1.dp, IsotopeMagenta, RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "ENVIRONMENT ANOMALIES DETECTED",
                        fontFamily = QuantumMonospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = IsotopeMagenta
                    )
                    threats.forEach { threat ->
                        Text(
                            text = "• ${threat::class.simpleName}",
                            fontFamily = QuantumMonospace,
                            fontSize = 11.sp,
                            color = NeutronWhite
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "ENTANGL v1.0.0 — ALL SYSTEMS NOMINAL",
            fontFamily = QuantumMonospace,
            fontSize = 10.sp,
            color = SubatomicGray,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun SecurityRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = QuantumMonospace,
            fontSize = 11.sp,
            color = SubatomicGray
        )
        Text(
            text = value,
            fontFamily = QuantumMonospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = NeutronWhite
        )
    }
}