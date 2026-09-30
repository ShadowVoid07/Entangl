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
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import `in`.grayscales.entangl.core.security.PlatformSecurity
import `in`.grayscales.entangl.core.security.SecurityEvent
import `in`.grayscales.entangl.data.network.EntanglRelayService
import `in`.grayscales.entangl.domain.model.Contact
import `in`.grayscales.entangl.ui.chat.ChatViewModel
import `in`.grayscales.entangl.ui.navigation.QuantumTwoPaneLayout
import `in`.grayscales.entangl.ui.profile.EditProfileScreen
import `in`.grayscales.entangl.ui.qr.MutualHandshakeScreen
import `in`.grayscales.entangl.ui.settings.SettingsScreen
import `in`.grayscales.entangl.ui.theme.CyberDark
import `in`.grayscales.entangl.ui.theme.DarkMatter
import `in`.grayscales.entangl.ui.theme.DarkMatterVariant
import `in`.grayscales.entangl.ui.theme.EntanglTheme
import `in`.grayscales.entangl.ui.theme.IsotopeMagenta
import `in`.grayscales.entangl.ui.theme.NeutronWhite
import `in`.grayscales.entangl.ui.theme.ParticleBorder
import `in`.grayscales.entangl.ui.theme.QuantumCyan
import `in`.grayscales.entangl.ui.theme.QuantumMonospace
import `in`.grayscales.entangl.ui.theme.SubatomicGray
import `in`.grayscales.entangl.ui.theme.VoidBackground
import `in`.grayscales.entangl.ui.transfer.DeviceTransferScreen
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

enum class AppScreen {
    INITIALIZE_IDENTITY,
    MESSAGES,
    HANDSHAKE,
    SETTINGS,
    DEVICE_TRANSFER
}

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CONTACT_UID = "in.grayscales.entangl.extra.CONTACT_UID"
    }

    private val platformSecurity: PlatformSecurity by inject()
    private val keyDestructionService: `in`.grayscales.entangl.core.security.KeyDestructionService by inject()
    private val chatViewModel: ChatViewModel by viewModel()
    private val pendingNotificationContactUid = mutableStateOf<String?>(null)

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

        handleIncomingIntent(intent)

        // Start background relay service
        try {
            val serviceIntent = Intent(this, EntanglRelayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Failed to start EntanglRelayService: ${e.message}")
        }

        setContent {
            EntanglTheme {
                // Asynchronously check platform threats off the main thread
                var activeThreats by remember { mutableStateOf<List<SecurityEvent>>(emptyList()) }
                var showSecurityAdvisory by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        val threats = platformSecurity.checkThreats()
                        if (threats.isNotEmpty()) {
                            activeThreats = threats
                            showSecurityAdvisory = true
                        }
                    }
                }

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

                val isIdentityConfigured = isUsernameSet && currentUsername.isNotBlank()
                val startDestination = if (!isIdentityConfigured) AppScreen.INITIALIZE_IDENTITY else AppScreen.MESSAGES

                // Navigation backstack maintaining exact back-stack history
                val screenStack = remember(isIdentityConfigured) {
                    mutableStateListOf(startDestination)
                }
                var handshakeInitialTab by remember { mutableIntStateOf(0) }

                val currentScreen = screenStack.lastOrNull() ?: AppScreen.MESSAGES

                fun navigateTo(screen: AppScreen, initialTab: Int = 0) {
                    handshakeInitialTab = initialTab
                    if (screen == AppScreen.INITIALIZE_IDENTITY) {
                        screenStack.clear()
                        screenStack.add(AppScreen.INITIALIZE_IDENTITY)
                    } else if (screen == AppScreen.MESSAGES) {
                        screenStack.clear()
                        screenStack.add(AppScreen.MESSAGES)
                    } else if (screen == AppScreen.HANDSHAKE) {
                        // Single handshake instance: update tab in place, no duplicates.
                        screenStack.remove(AppScreen.HANDSHAKE)
                        screenStack.add(screen)
                    } else {
                        if (screenStack.lastOrNull() != screen) {
                            screenStack.add(screen)
                        }
                    }
                }

                val contacts by chatViewModel.contacts.collectAsState()
                val lastMessages by chatViewModel.lastMessages.collectAsState()
                val activeContact by chatViewModel.activeContact.collectAsState()
                val activeMessages by chatViewModel.activeMessages.collectAsState()
                val selfDestructDuration by chatViewModel.selfDestructDuration.collectAsState()

                fun navigateBack() {
                    if (screenStack.size > 1) {
                        screenStack.removeAt(screenStack.size - 1)
                    } else {
                        if (activeContact != null) {
                            chatViewModel.selectContact(null)
                        } else {
                            finish()
                        }
                    }
                }

                // Reactive handler for incoming notification deep-links.
                // Validated: pending/deleted contacts route to HANDSHAKE (not a dead chat),
                // and an in-progress scan is never yanked out from under the user.
                LaunchedEffect(pendingNotificationContactUid.value) {
                    val uid = pendingNotificationContactUid.value
                    if (!uid.isNullOrBlank()) {
                        val target = contacts.firstOrNull { it.uid == uid }
                        if (target == null) {
                            pendingNotificationContactUid.value = null
                        } else if (!target.isAccepted) {
                            pendingNotificationContactUid.value = null
                            chatViewModel.selectContact(null)
                            navigateTo(AppScreen.HANDSHAKE, initialTab = 1)
                        } else if (currentScreen == AppScreen.HANDSHAKE) {
                            // Stay on profile during reciprocal scan; just select for later.
                            chatViewModel.selectContactByUid(uid)
                            pendingNotificationContactUid.value = null
                        } else {
                            screenStack.clear()
                            screenStack.add(AppScreen.MESSAGES)
                            chatViewModel.selectContactByUid(uid)
                            pendingNotificationContactUid.value = null
                        }
                    }
                }

                // Security Advisory Dialog on startup when host OS threats are active
                if (showSecurityAdvisory && activeThreats.isNotEmpty()) {
                    Dialog(onDismissRequest = { showSecurityAdvisory = false }) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, IsotopeMagenta, RoundedCornerShape(16.dp)),
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
                                        .background(IsotopeMagenta.copy(alpha = 0.15f))
                                        .border(1.dp, IsotopeMagenta, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = IsotopeMagenta,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Text(
                                    text = "SECURITY ADVISORY",
                                    fontFamily = QuantumMonospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    letterSpacing = 2.sp,
                                    color = IsotopeMagenta,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )

                                Text(
                                    text = "Platform security detected active environment threats:\n" +
                                        activeThreats.joinToString("\n") { threat ->
                                            when (threat) {
                                                is SecurityEvent.DebuggerDetected -> "• Debugger Attached"
                                                is SecurityEvent.RootDetected -> "• Root/Jailbreak Detected"
                                                is SecurityEvent.AccessibilityServiceActive -> "• Active Accessibility Keylogger: ${threat.serviceName}"
                                                is SecurityEvent.RatchetTampered -> "• Ratchet Tampering Detected"
                                                is SecurityEvent.ClipboardCleared -> "• Insecure Clipboard"
                                            }
                                        } +
                                        "\n\nCryptographic guarantees may be compromised by the host operating system.",
                                    fontFamily = QuantumMonospace,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = SubatomicGray,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Start
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            showSecurityAdvisory = false
                                            navigateTo(AppScreen.SETTINGS)
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = DarkMatterVariant),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("INSPECT", fontFamily = QuantumMonospace, fontSize = 11.sp, color = QuantumCyan)
                                    }

                                    Button(
                                        onClick = { showSecurityAdvisory = false },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = IsotopeMagenta),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("PROCEED", fontFamily = QuantumMonospace, fontSize = 11.sp, color = NeutronWhite)
                                    }
                                }
                            }
                        }
                    }
                }

                // Handshake success celebration: exactly once per peer, only after an
                // internal health re-check (persisted flags + pinned key + ratchet HMAC).
                // Pre-existing accepted chats are seeded silently on first run so only
                // fresh completions announce "HANDSHAKE SUCCESSFUL — chat now or later".
                val celebratePrefs = remember {
                    getSharedPreferences("entangl_handshake_celebrated", MODE_PRIVATE)
                }
                var celebratedUids by remember {
                    mutableStateOf(
                        celebratePrefs.getStringSet("uids", null)?.toSet() ?: emptySet()
                    )
                }
                var celebrationSeeded by remember {
                    mutableStateOf(celebratePrefs.contains("uids"))
                }
                var successContact by remember { mutableStateOf<Contact?>(null) }

                LaunchedEffect(contacts) {
                    if (!celebrationSeeded) {
                        celebrationSeeded = true
                        val baseline = contacts.filter { it.isAccepted }.map { it.uid }.toSet()
                        celebratedUids = baseline
                        celebratePrefs.edit { putStringSet("uids", baseline) }
                        return@LaunchedEffect
                    }
                    val target = contacts.firstOrNull {
                        it.isAccepted && it.uid !in celebratedUids && it.uid != successContact?.uid
                    } ?: return@LaunchedEffect
                    if (chatViewModel.verifyHandshakeComplete(target.uid)) {
                        celebratedUids = celebratedUids + target.uid
                        celebratePrefs.edit { putStringSet("uids", celebratedUids) }
                        successContact = target
                    }
                }

                if (isIdentityConfigured && currentScreen != AppScreen.INITIALIZE_IDENTITY) {
                    val liveSuccess = successContact?.let { pending ->
                        contacts.firstOrNull { it.uid == pending.uid } ?: pending
                    }
                    if (liveSuccess != null) {
                        Dialog(onDismissRequest = { successContact = null }) {
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
                                    Text(
                                        text = "HANDSHAKE SUCCESSFUL",
                                        fontFamily = QuantumMonospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        letterSpacing = 2.sp,
                                        color = QuantumCyan,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    val peerName = liveSuccess.displayName?.ifBlank { null }
                                        ?: "Peer ${liveSuccess.uid.take(6).uppercase()}"
                                    Text(
                                        text = "Secure channel with $peerName is verified and unlocked.",
                                        fontFamily = QuantumMonospace,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        color = SubatomicGray,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(DarkMatterVariant)
                                            .border(1.dp, ParticleBorder, RoundedCornerShape(8.dp))
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = liveSuccess.safetyNumber,
                                            fontFamily = QuantumMonospace,
                                            fontSize = 11.sp,
                                            lineHeight = 17.sp,
                                            color = NeutronWhite,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                    Text(
                                        text = "Safety match confirmed internally — compare anytime under the shield icon.",
                                        fontFamily = QuantumMonospace,
                                        fontSize = 9.sp,
                                        color = SubatomicGray,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                successContact = null
                                                screenStack.clear()
                                                screenStack.add(AppScreen.MESSAGES)
                                                chatViewModel.selectContact(liveSuccess)
                                            },
                                            modifier = Modifier.weight(1.2f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = QuantumCyan,
                                                contentColor = CyberDark
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "Chat now",
                                                fontFamily = QuantumMonospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = CyberDark
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = { successContact = null },
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
                }

                // Reciprocal scan prompt: single source of truth lives in the handshake
                // ledger, so suppress this dialog while already on HANDSHAKE (avoids
                // triple-prompt with the peer dialog + ledger card). Show ONLY for genuine
                // inbound (they scanned us, we have NOT scanned them) — outbound and mutual
                // rows are covered by the handshake dialog + ledger and must not re-prompt.
                if (isIdentityConfigured && currentScreen != AppScreen.INITIALIZE_IDENTITY &&
                    currentScreen != AppScreen.HANDSHAKE
                ) {
                    promptReciprocalScanContact
                        ?.takeIf { it.hasBeenScanned && !it.hasScannedPeer && !it.isAccepted }
                        ?.let { peer ->
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
                                        text = "SCAN RECORDED",
                                        fontFamily = QuantumMonospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        letterSpacing = 2.sp,
                                        color = QuantumCyan,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )

                                    Text(
                                        text = "${peer.displayName ?: "Peer"} scanned your code. Chat unlocks only after you scan them back and safety numbers match.",
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
                                                navigateTo(AppScreen.HANDSHAKE, initialTab = 1)
                                            },
                                            modifier = Modifier.weight(1.2f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = QuantumCyan,
                                                contentColor = CyberDark
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "Scan peer code",
                                                fontFamily = QuantumMonospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = CyberDark
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
                }

                // Intercept back gesture during initialization - exit application (cannot bypass setup)
                BackHandler(enabled = currentScreen == AppScreen.INITIALIZE_IDENTITY) {
                    finish()
                }

                // Intercept back gesture when deeper in navigation stack
                BackHandler(enabled = screenStack.size > 1) {
                    navigateBack()
                }

                // Intercept back gesture in candybar mode to return to contacts
                BackHandler(enabled = screenStack.size <= 1 && currentScreen == AppScreen.MESSAGES && activeContact != null) {
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
                            AppScreen.INITIALIZE_IDENTITY -> {
                                EditProfileScreen(
                                    initialUsername = currentUsername,
                                    initialColorHex = currentProfileColor,
                                    onConfirm = { chosenName, chosenColor ->
                                        chatViewModel.setProfile(chosenName, chosenColor)
                                        screenStack.clear()
                                        screenStack.add(AppScreen.MESSAGES)
                                    }
                                )
                            }

                            AppScreen.MESSAGES -> {
                                QuantumTwoPaneLayout(
                                    contacts = contacts,
                                    activeContact = activeContact,
                                    messages = activeMessages,
                                    selfDestructDuration = selfDestructDuration,
                                    onSelectContact = { contact -> chatViewModel.selectContact(contact) },
                                    onDeleteContact = { contact -> chatViewModel.deleteContact(contact) },
                                    onAcceptContact = { contact -> chatViewModel.acceptContact(contact) },
                                    onClearChat = { contact -> chatViewModel.clearChatHistory(contact) },
                                    onBlockToggle = { contact ->
                                        chatViewModel.setBlocked(contact, !contact.isBlocked)
                                    },
                                    onUnblockContact = {
                                        activeContact?.let { contact ->
                                            chatViewModel.setBlocked(contact, false)
                                        }
                                    },
                                    onMarkRead = { chatViewModel.markActiveChatRead() },
                                    onDeleteMessage = { id -> chatViewModel.deleteMessage(id) },
                                    onSendMessage = { text -> chatViewModel.sendMessage(text) },
                                    onSetSelfDestruct = { dur -> chatViewModel.setSelfDestructDuration(dur) },
                                    onHandshake = { navigateTo(AppScreen.HANDSHAKE, initialTab = 0) },
                                    onOpenSettings = { navigateTo(AppScreen.SETTINGS) },
                                    onScanQr = { navigateTo(AppScreen.HANDSHAKE, initialTab = 1) },
                                    onShowMyQr = { navigateTo(AppScreen.HANDSHAKE, initialTab = 0) },
                                    onOpenDashboard = { navigateTo(AppScreen.SETTINGS) },
                                    localUsername = currentUsername,
                                    localProfileColor = currentProfileColor,
                                    onUpdateProfile = null,
                                    lastMessages = lastMessages
                                )
                            }

                            AppScreen.HANDSHAKE -> {
                                MutualHandshakeScreen(
                                    chatViewModel = chatViewModel,
                                    localUsername = currentUsername,
                                    localProfileColor = currentProfileColor,
                                    initialTab = handshakeInitialTab,
                                    onPeerConfirmed = { uid, key, onion, safetyNum, peerUsername, peerProfileColor ->
                                        // Records the outbound scan only and stays on the handshake
                                        // profile: chat unlocks after both directions verify.
                                        chatViewModel.addContactFromHandshake(uid, key, onion, safetyNum, peerUsername, peerProfileColor)
                                    },
                                    onMutualUnlocked = {
                                        screenStack.clear()
                                        screenStack.add(AppScreen.MESSAGES)
                                    },
                                    onTransferDetected = { payload ->
                                        chatViewModel.localTransferManager.startImportClient(payload)
                                        navigateTo(AppScreen.DEVICE_TRANSFER)
                                    },
                                    onBack = { navigateBack() },
                                    onSettingsClick = { navigateTo(AppScreen.SETTINGS) }
                                )
                            }

                            AppScreen.SETTINGS -> {
                                SettingsScreen(
                                    threats = activeThreats,
                                    localUsername = currentUsername,
                                    localProfileColor = currentProfileColor,
                                    onBack = { navigateBack() },
                                    onDeviceTransfer = { navigateTo(AppScreen.DEVICE_TRANSFER) }
                                )
                            }

                            AppScreen.DEVICE_TRANSFER -> {
                                DeviceTransferScreen(
                                    chatViewModel = chatViewModel,
                                    onBack = { navigateBack() },
                                    onScanQrForImport = { navigateTo(AppScreen.HANDSHAKE, initialTab = 1) }
                                )
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
            pendingNotificationContactUid.value = contactUid
        }
    }
}
