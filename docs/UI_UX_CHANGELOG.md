# Entangl UI/UX Changelog

### Entry 1
- **Timestamp**: 2026-09-26 01:45:00
- **File**: `app/src/main/java/in/grayscales/entangl/ui/theme/Color.kt`, `app/src/main/java/in/grayscales/entangl/ui/theme/Theme.kt`, `app/src/main/java/in/grayscales/entangl/ui/chat/ContactsScreen.kt`, `app/src/main/java/in/grayscales/entangl/ui/navigation/QuantumTwoPaneLayout.kt`, `app/src/main/java/in/grayscales/entangl/ui/qr/MutualHandshakeScreen.kt`, `app/src/main/java/in/grayscales/entangl/MainActivity.kt`
- **Lines**: `Color.kt`: 22 (Added); `Theme.kt`: 14, 18, 22 (Modified); `ContactsScreen.kt`: 28, 51, 66-71, 113-160, 305-325, 489 (Modified); `QuantumTwoPaneLayout.kt`: 41, 57-62, 88-89, 128-132, 160-161, 180-184, 228-243 (Modified); `MutualHandshakeScreen.kt`: 1-175 (Added); `MainActivity.kt`: 67, 70, 85-86, 237-244, 304-338, 400-435, 545-550 (Modified)
- **Purpose**: Phase 1 Theme Foundation & Main Navigation Refactor:
  1. **Theme Contrast Enforcement**: Defined `CyberDark` (`Color(0xFF0F0F13)`) in `Color.kt` and updated Material 3's `QuantumDarkColorScheme` to set `onPrimary`, `onSecondary`, and `onTertiary` to `CyberDark`. This universally guarantees that any Jetpack Compose button or component utilizing the high-luminance `QuantumCyan` primary container automatically renders ultra-crisp, high-contrast dark typography and icons, eliminating low-contrast illegibility.
  2. **Top App Bar Ergonomic Consolidation**: Refactored the main top bar in `ContactsScreen.kt` and `QuantumTwoPaneLayout.kt`. Consolidated the separate QR display and scanning buttons into a single primary action button with `Icons.Default.QrCodeScanner` styled with `QuantumCyan` background and `CyberDark` icon. Replaced the raw terminal/console button with a standard settings gear (`Icons.Default.Settings`). Swapped button placement for single-handed thumb ergonomics (Settings positioned on the inside, Handshake positioned on the far edge for optimal reachability).
  3. **Navigation & Screen Routing**: Expanded `AppScreen` in `MainActivity.kt` with `HANDSHAKE` and `SETTINGS` routes. Wired the consolidated Handshake action to `MutualHandshakeScreen` in `ui/qr/` (supporting mutual optical key exchange) and routed Settings to `SettingsScreen` (updating header nomenclature from "CONSOLE" to "SETTINGS" with contrast-corrected actions).

---

### Entry 2
- **Timestamp**: 2026-09-26 02:25:00
- **File**: `app/src/main/java/in/grayscales/entangl/ui/qr/MutualHandshakeScreen.kt`, `app/src/main/java/in/grayscales/entangl/ui/qr/HandshakeConfirmDialog.kt`
- **Lines**: `MutualHandshakeScreen.kt`: 1-325 (Modified/Rebuilt); `HandshakeConfirmDialog.kt`: 26-45, 215-265, 365-455 (Modified)
- **Purpose**: Phase 2 The Handshake Suite:
  1. **Clean De-congested Mutual Handshake Screen**: Rebuilt `MutualHandshakeScreen.kt` with an un-congested top header using a `Row` with `Arrangement.SpaceBetween`, housing a back navigation trigger, centered bold `"MUTUAL HANDSHAKE"` title paired with a green cryptographic shield (`Icons.Default.Security`), and a right-side settings gear button, discarding all cluttered subtext.
  2. **Segmented Tab Control & Split-Pane Layout**: Implemented a responsive `BoxWithConstraints` architecture: on wide screens (> 600dp, foldables/tablets/landscape), renders a simultaneous side-by-side dual-pane layout (`TRANSMIT [BEACON]` on the left partition and `RECEIVE [SENSOR]` on the right partition separated by a 1.dp `ParticleBorder`); on compact phones (<= 600dp), provides a high-contrast segmented tab bar (`[TRANSMIT [BEACON]]` and `[RECEIVE [SENSOR]]`) for thumb-friendly optical mode switching.
  3. **Compose-Layer Security Isolation**: Enforced hardware window capture protection (`FLAG_SECURE`) strictly within Compose using `DisposableEffect` over `Activity.window`, ensuring screens and beacons cannot be recorded, screenshotted, or leaked in recent app snapshots.
  4. **Cryptographic 3x4 Matrix Scramble & Haptic Ceremony**: Enhanced `HandshakeConfirmDialog.kt` by transforming the 60-digit mutual safety number into a 3x4 monospace matrix (12 discrete 5-digit blocks). Built a dynamic `LaunchedEffect` entry sequence that rapidly cycles random alphanumeric cipher characters for 500ms accompanied by ~60ms throttled `TextHandleMove` haptic pulses, culminating in a definitive `LongPress` haptic buzz upon locking into the true cryptographic safety number.

---

### Entry 3
- **Timestamp**: 2026-09-26 02:45:00
- **File**: `app/src/main/java/in/grayscales/entangl/ui/home/HomeChatLayout.kt`, `app/src/main/java/in/grayscales/entangl/ui/home/ChatListRow.kt`, `app/src/main/java/in/grayscales/entangl/ui/chat/QuantumTypingIndicator.kt`, `app/src/main/java/in/grayscales/entangl/ui/navigation/QuantumTwoPaneLayout.kt`, `app/src/main/java/in/grayscales/entangl/ui/chat/ContactsScreen.kt`, `app/src/main/java/in/grayscales/entangl/ui/chat/ChatScreen.kt`, `app/src/main/java/in/grayscales/entangl/MainActivity.kt`
- **Lines**: `HomeChatLayout.kt`: 1-285 (Added); `ChatListRow.kt`: 1-260 (Added); `QuantumTypingIndicator.kt`: 1-175 (Added); `QuantumTwoPaneLayout.kt`: 1-52 (Refactored/Delegated); `ContactsScreen.kt`: 55, 79, 377-386 (Modified); `ChatScreen.kt`: 105, 517-531 (Modified); `MainActivity.kt`: 44-45, 181, 316, 344-345, 411-430, 530-585 (Modified)
- **Purpose**: Phase 3 Chat UX & Trust Indicators:
  1. **Adaptive Foldable Layout (`ListDetailPaneScaffold`)**: Built `HomeChatLayout.kt` implementing Material 3's `ListDetailPaneScaffold` layout. For foldables, tablets, and wide screens (`maxWidth >= 600.dp`), it renders a simultaneous split-pane interface with the Contact List anchored on the left (360.dp) and the active Conversation or Mission Control Standby dashboard on the right, separated by a 1.dp cybernetic `ParticleBorder`. On compact candybar smartphones (`maxWidth < 600.dp`), it smoothly pushes list-to-detail using `AnimatedContent` slide/fade transitions, hooking system `BackHandler` to fluidly pop back to the conversation list without losing state.
  2. **SDK-Safe Anti-Shoulder-Surfing Privacy Blur & Hold-to-Reveal**: Built `ChatListRow.kt` featuring `PrivacyBlurText` and `Modifier.privacyBlur`. Encrypted message previews are blurred by default using an 8.dp Gaussian blur strictly gated behind `Build.VERSION.SDK_INT >= Build.VERSION_CODES.S` (Android 12+). On devices running API < 31, an automatic fallback renders a solid black 90% opacity redaction box covering the preview text, preventing unsupported RenderEffect crashes while preserving zero-knowledge privacy. Utilized `pointerInput` with `detectTapGestures(onPress = ...)` and `TextHandleMove` haptics to reveal plaintext *only* while the user physically presses and holds the row, immediately re-engaging obfuscation upon release. Added a global toggle in `SettingsScreen` with live SDK detection status.
  3. **Quantum Typing Visualization**: Replaced standard "typing..." text with `QuantumTypingIndicator.kt`. Built a custom `Canvas` driven by `rememberInfiniteTransition` that animates 3 `QuantumCyan` dots oscillating across prime-harmonic frequencies (620ms, 890ms, 810ms). The dots asynchronously fluctuate in scale (0.50x - 1.45x) and alpha (0.20f - 1.0f) with outer quantum aura halos and luminescent white core flares, providing a rich, cybernetic visual simulation of an encrypted post-quantum payload being actively synthesized.

---

### Entry 4
- **Timestamp**: 2026-09-26 14:30:00
- **File**: `app/src/main/java/in/grayscales/entangl/ui/settings/SettingsScreen.kt`, `app/src/main/java/in/grayscales/entangl/ui/settings/NetworkStatusSheet.kt`, `app/src/main/java/in/grayscales/entangl/ui/chat/EmptyPeersState.kt`, `app/src/main/java/in/grayscales/entangl/ui/chat/MessageBubble.kt`, `app/src/main/java/in/grayscales/entangl/ui/chat/ContactsScreen.kt`, `app/src/main/java/in/grayscales/entangl/ui/chat/ChatScreen.kt`, `app/src/main/java/in/grayscales/entangl/MainActivity.kt`
- **Lines**: `SettingsScreen.kt`: 1-355 (Extracted/Rebuilt); `NetworkStatusSheet.kt`: 1-320 (Added); `EmptyPeersState.kt`: 1-225 (Added); `MessageBubble.kt`: 1-340 (Added); `ContactsScreen.kt`: 327-336 (Modified); `ChatScreen.kt`: 111, 515-525, 630-794 (Refactored/Integrated); `MainActivity.kt`: 72, 404-405 (Modified)
- **Purpose**: Phase 4 Diagnostics & Micro-Animations:
  1. **Interactive Settings & Tor Network Diagnostics Sheet**: De-congested the Settings header to a clean, bold "ENTANGL SETTINGS" title without distracting subtext. Integrated a clickable 3-bar `NetworkSignalIndicator` with a 5000ms infinite sweep animation that sequentially pulses cyan across the signal bars (0ms-800ms) to simulate live Tor keep-alive beacons. Tapping the indicator opens `NetworkStatusSheet` via `ModalBottomSheet` with `.windowInsetsPadding(WindowInsets.navigationBars)` to protect against gesture bar cutoff. Renders an interactive cybernetic circuit flowchart ("Device -> Tor Entry -> Onion Service") alongside real-time latency ping metrics and a circuit renewal action.
  2. **Empty State Pulsing Radar**: Created `EmptyPeersState.kt` featuring a tactical radar background driven by `rememberInfiniteTransition`: 3 concentric `QuantumCyan` rings continuously pulse outward (scale 0f to 2.5f) and fade (alpha 0.5f to 0f) with 1/3-cycle phase offsets and fine crosshair grid lines. Embedded cleanly in `ContactsScreen.kt` when the peer roster is empty, providing immediate call-to-action triggers for mutual handshakes.
  3. **Message Bubble Zeroization Glitch & Ratcheted Epoch Badge**: Built `MessageBubble.kt` featuring two advanced security micro-animations:
     - **Digital Shredding Zeroization:** Accepts an `isZeroizing: Boolean` state. When activated, the message bubble executes a 300ms digital shredding glitch (jittering horizontal offset between -8.dp and +8.dp while rapidly flickering alpha), followed by a smooth 250ms vertical collapse to 0.dp height.
     - **3D Flip Ratcheted Epoch Badge:** Placed an interactive miniature lock badge adjacent to message timestamps. On tap, applies a 3D `rotationY` card flip (0° to 180° with camera distance perspective correction) accompanied by tactile haptic feedback, revealing the active Double Ratchet epoch key (e.g. "EPOCH: 4A9F").

---

### Entry 5
- **Timestamp**: 2026-09-26 19:35:00
- **File**: `app/src/main/java/in/grayscales/entangl/ui/settings/SettingsScreen.kt`, `app/src/main/java/in/grayscales/entangl/ui/chat/ContactsScreen.kt`, `app/src/main/java/in/grayscales/entangl/ui/chat/EmptyPeersState.kt`, `app/src/main/java/in/grayscales/entangl/ui/transfer/DeviceTransferScreen.kt`, `app/src/main/java/in/grayscales/entangl/ui/chat/EditProfileDialog.kt`, `app/src/main/java/in/grayscales/entangl/ui/qr/DeviceMigrationConfirmDialog.kt`, `app/src/main/java/in/grayscales/entangl/ui/qr/HandshakeConfirmDialog.kt`, `app/src/main/java/in/grayscales/entangl/ui/chat/SafetyNumberDialog.kt`, `app/src/main/java/in/grayscales/entangl/ui/chat/ChatScreen.kt`, `app/src/main/java/in/grayscales/entangl/ui/qr/QrScannerView.kt`
- **Lines**: `SettingsScreen.kt`: 28, 260-281 (Modified); `ContactsScreen.kt`: 33, 308-328 (Modified); `EmptyPeersState.kt`: 31, 212-232 (Modified); `DeviceTransferScreen.kt`: 41, 70, 347-362, 562-583, 679-693 (Modified); `EditProfileDialog.kt`: 26, 50, 248-268 (Modified); `DeviceMigrationConfirmDialog.kt`: 23, 187-208 (Modified); `HandshakeConfirmDialog.kt`: 25, 237-258 (Modified); `SafetyNumberDialog.kt`: 22, 127-142 (Modified); `ChatScreen.kt`: 53, 404-424, 483-504, 692-705 (Modified); `QrScannerView.kt`: 41, 180-195 (Modified)
- **Purpose**: Primary Button Contrast Normalization & WCAG AAA Compliance:
  Stripped hardcoded white text and icon tint overrides across primary action buttons (including "OPEN DEVICE TRANSFER", "Initiate Mutual Handshake", "INITIATE SECURE EXPORT", "SCAN OLD DEVICE QR CODE", and the profile "APPLY" dialog action, along with related optical permission, contact acceptance, and cryptographic verification buttons). Refactored button configurations to explicitly bind `containerColor = MaterialTheme.colorScheme.primary` and `contentColor = MaterialTheme.colorScheme.onPrimary`, with child `Icon` and `Text` composables explicitly inheriting or passing `MaterialTheme.colorScheme.onPrimary` (`CyberDark = Color(0xFF0F0F13)`). This permanently resolves low-contrast white-on-cyan button states and enforces WCAG AAA compliant text legibility throughout the app.

---

### Entry 6
- **Timestamp**: 2026-09-26 21:40:00
- **File**: `app/src/main/java/in/grayscales/entangl/MainActivity.kt`, `app/src/main/java/in/grayscales/entangl/ui/chat/ContactsScreen.kt`, `app/src/main/java/in/grayscales/entangl/ui/qr/MutualHandshakeScreen.kt`
- **Lines**: `MainActivity.kt`: 336 (Modified); `ContactsScreen.kt`: 123-160 (Modified); `MutualHandshakeScreen.kt`: 70, 140 (Modified)
- **Purpose**: Top Bar Ergonomics Refactor & Mutual Handshake Route Fix:
  1. **Top App Bar De-congestion & Spacing**: Resolved visual congestion between action buttons in the Home Top App Bar (`ContactsScreen.kt`). Replaced tight layout with a dedicated `Spacer(modifier = Modifier.width(20.dp))` between action targets to ensure comfortable touch targets and generous breathing room.
  2. **Top Bar Button Reordering**: Swapped the relative positions of the action buttons so that the primary Mutual Handshake button (`Icons.Default.QrCodeScanner`) is situated first, followed by the breathing spacer and the secondary Settings trigger (`Icons.Default.Settings`), optimizing visual hierarchy and single-handed accessibility.
  3. **Mutual Handshake Settings Route Wiring**: Fixed the unresponsive Settings button inside `MutualHandshakeScreen.kt` by wiring its `IconButton.onClick` to `onSettingsClick`. Added `onSettingsClick = { currentScreen = AppScreen.SETTINGS }` in `MainActivity.kt`'s `AppScreen.HANDSHAKE` destination, properly linking top bar settings navigation directly to `SettingsScreen`.

---

### Entry 7
- **Timestamp**: 2026-09-26 21:45:00
- **File**: `app/src/main/java/in/grayscales/entangl/ui/settings/NetworkStatusSheet.kt`, `app/src/main/java/in/grayscales/entangl/ui/settings/SettingsScreen.kt`
- **Lines**: `NetworkStatusSheet.kt`: 39, 325-345 (Modified); `SettingsScreen.kt`: 169-176 (Modified)
- **Purpose**: Tor Circuit Contrast Enforcement & Core Protocol Matrix Hierarchy Optimization:
  1. **Tor Circuit Action Button Contrast Fix**: Stripped hardcoded white text and icon tint styling from the "RENEW TOR CIRCUIT" button inside `NetworkStatusSheet.kt`. Bound `colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)` and explicitly enforced `tint = MaterialTheme.colorScheme.onPrimary` on `Icon(Refresh)` and `color = MaterialTheme.colorScheme.onPrimary` on `Text`, guaranteeing dark `CyberDark` (`#0F0F13`) rendering over bright cyan for full WCAG AAA compliance.
  2. **Core Protocol Matrix Restructuring**: In `SettingsScreen.kt`, elevated the "ACTIVE CODENAME" readout to the very top of the Core Protocol Matrix immediately below the section header for prominent identity awareness. Completely removed the redundant "DISPLAY INTEGRITY" row, eliminating UI clutter and streamlining technical security telemetry.

---

### Entry 8
- **Timestamp**: 2026-09-26 23:15:00
- **File**: `app/src/main/java/in/grayscales/entangl/MainActivity.kt`, `app/src/main/java/in/grayscales/entangl/ui/settings/SettingsScreen.kt`, `app/src/main/java/in/grayscales/entangl/ui/profile/EditProfileScreen.kt`
- **Lines**: `MainActivity.kt`: 68, 89, 168-385 (Modified); `SettingsScreen.kt`: 169-176 (Read-only); `EditProfileScreen.kt`: 1-255 (Added); `ContactsScreen.kt`: 167-240 (Modified)
- **Purpose**: Node Profile Configuration Transition to One-Time Onboarding Flow & Identity Immutability Lock:
  1. **Settings & Contacts Lock Down**: Removed all edit entry points and interactive controls for the node profile. In `SettingsScreen.kt`, ensured the "ACTIVE CODENAME" row within the Core Protocol Matrix is strictly a read-only textual readout with zero `IconButton` or `clickable` modifiers. In `ContactsScreen.kt`, stripped the `.clickable` modifier, removed the `Icons.Default.Edit` button, removed the dialog trigger, and updated the badge label to "NODE IDENTITY • VERIFIED IMMUTABLE" to enforce complete identity immutability.
  2. **Dedicated Initialization Screen UI**: Refactored the profile screen (`EditProfileScreen.kt`) into a dedicated first-launch onboarding step titled "INITIALIZE IDENTITY". Added descriptive cryptography subtext clarifying that handles and avatar colors are permanently bound to the local hardware key upon setup. Equipped the primary "APPLY" button with high-contrast `MaterialTheme.colorScheme.primary` and `onPrimary` styling, and wired its callback to transition directly to the Home screen.
  3. **One-Way Navigation & Backstack Zeroization**: In `MainActivity.kt`, added `INITIALIZE_IDENTITY` to `AppScreen`. Checked existing identity state (`isIdentityConfigured = isUsernameSet && currentUsername.isNotBlank()`) to assign `startDestination = AppScreen.INITIALIZE_IDENTITY` when uninitialized. Configured the onboarding completion callback to perform a one-way forward navigation to `AppScreen.MESSAGES`, and gated `BackHandler` logic so users cannot press the Android back button to return to the setup screen (equivalent to `popUpTo` root inclusive).

---

### Entry 9
- **Timestamp**: 2026-09-27 00:15:00
- **File**: `app/src/main/java/in/grayscales/entangl/ui/chat/ContactsScreen.kt`
- **Lines**: 195-215 (Modified)
- **Purpose**: Raw Hex Color Code Removal & Node Identity Card Aesthetic Declutter:
  1. **Hex Text Chip Elimination**: Removed the raw hex color text chip (`localProfileColor.ifBlank { ColorUtils.DEFAULT_PROFILE_HEX }`) and its surrounding container box from the Node Identity profile card in `ContactsScreen.kt`. Eliminates unnecessary technical clutter and enhances visual elegance.
  2. **Preserved Avatar Tinting & Contrast**: Maintained the cryptographic avatar circle's border (`1.5.dp`), background tint (`alpha = 0.18f`), and monogram text styling bound dynamically to `profileColor`, preserving visual color identity without rendering raw hex strings.
  3. **Vertical Alignment Refinement**: Restructured the profile text Column to stack the active codename directly above the "NODE IDENTITY • VERIFIED IMMUTABLE" readout with clean `2.dp` vertical spacing, creating an unencumbered, premium card presentation.

---

---

### Entry 11
- **Timestamp**: 2026-09-27 05:40:00
- **File**: `data/network/LocalTransferManager.kt`, `res/xml/data_extraction_rules.xml`, `shared/src/commonMain/kotlin/.../DefaultCryptoManager.kt`, `data/repository/MessageRepositoryImpl.kt`, `core/identity/NodeIdentityManager.kt`, `data/notification/EntanglNotificationManager.kt`, `core/security/KeyDestructionService.kt`, `data/network/NetworkTransport.kt`
- **Purpose**: Alpha Stage Comprehensive Security Hardening & Zero-Leakage Remediations (SEC-01 through SEC-09):
  1. **Transfer Payload Bound (SEC-01)**: Enforced a strict 50 MB upper bound (`MAX_TRANSFER_PAYLOAD_BYTES`) on incoming stream lengths in `LocalTransferManager` to eliminate potential remote `OutOfMemoryError` allocation exploits.
  2. **Backup & D2D Transfer Isolation (SEC-02)**: Configured explicit exclude rules in `data_extraction_rules.xml` preventing Android 12+ cloud backups and device-to-device transfers from capturing SQLCipher vaults, encrypted preferences, or session keystores.
  3. **Ratchet Sequence Skip Cap (SEC-03)**: Added a 1,000-message skip gap limit (`MAX_SKIP_GAP`) in `DefaultCryptoManager` to eliminate CPU/memory exhaustion denial-of-service vectors from fabricated wire sequence numbers.
  4. **Anti-Replay Window Verification (SEC-04)**: Bound incoming envelope processing in `MessageRepositoryImpl` to a ±10 minute timestamp validity window (`ENVELOPE_MAX_AGE_MS` and `ENVELOPE_TIME_DRIFT_MS`), preventing stale or replayed envelopes from resurrecting expired self-destruct messages.
  5. **Encrypted Identity Storage (SEC-05)**: Migrated `NodeIdentityManager` from plaintext SharedPreferences to `EncryptedSharedPreferences` backed by Android Keystore's MasterKey AES256_GCM, implementing automatic legacy migration and zeroization.
  6. **Release Logging Metadata Elimination (SEC-06 & SEC-07)**: Downgraded notification and Keystore decommission telemetry to `Log.d` to ensure ProGuard `-assumenosideeffects` strips all contact UIDs, peer names, and Keystore aliases in release builds.
  7. **Database Ciphertext Notice Sanitization (SEC-08)**: Replaced raw peer names in the database ciphertext column for connection notices with non-identifying tokens (`STATUS_NOTICE:CONNECTION_ACCEPTED`), resolving display names dynamically.
  8. **JSON Field Sanitization (SEC-09)**: Sanitized field tokens in `NetworkTransport.extractJsonField` and `extractJsonLong` with `Regex.escape()` to prevent regex injection attacks.

---

### Entry 13
- **Timestamp**: 2026-09-27 08:25:00
- **File**: `app/src/main/java/in/grayscales/entangl/MainActivity.kt`, `app/src/main/java/in/grayscales/entangl/ui/qr/MutualHandshakeScreen.kt`, `app/src/main/java/in/grayscales/entangl/ui/chat/EmptyPeersState.kt`, `app/src/main/java/in/grayscales/entangl/ui/home/ChatListRow.kt`, `app/src/main/java/in/grayscales/entangl/ui/chat/ContactsScreen.kt`, `app/src/main/java/in/grayscales/entangl/ui/chat/ChatScreen.kt`, `app/src/main/java/in/grayscales/entangl/ui/home/HomeChatLayout.kt`, `app/src/main/java/in/grayscales/entangl/ui/chat/DeleteContactConfirmDialog.kt`, `docs/USERFLOW_AND_USAGE_ARCHITECTURE.md`
- **Purpose**: Comprehensive Userflow, Navigation Hierarchy & Usage Architecture Overhaul:
  1. **Decoupled Plaintext Reveal from Contact Deletion**: Fixed critical gesture collision in `ChatListRow.kt` where holding down a row to reveal privacy-blurred message previews triggered `onLongPress` and immediately destroyed the contact and ratchet session. Removed `onLongClick` deletion from `ChatListRow`, ensuring press-and-hold purely reveals the preview without risking accidental deletion.
  2. **Cryptographic Deletion Guardrail (`DeleteContactConfirmDialog`)**: Created `DeleteContactConfirmDialog.kt` to guard all permanent contact and key destruction with a tactical confirmation dialog ("TERMINATE ENTANGLEMENT?"), warning that Double Ratchet session keys and encrypted history will be permanently wiped. Integrated into `ChatScreen.kt` via `NetworkInfoDialog`.
  3. **Real-Time Peer Detection on Transmitter (Stranded Beacon Fix)**: In `MutualHandshakeScreen.kt`, observed `chatViewModel.contacts`. When an incoming peer scans the beacon and `SCAN_PING` creates a pending connection, an immediate in-app prompt ("PEER BEACON DETECTED") appears with 1-tap options: `[ ACCEPT & SCAN ]` (switches directly to Tab 1 to scan peer's QR code) or `[ ACCEPT & CHAT ]` (opens the conversation immediately).
  4. **Intentional Tab Threading**: Added `initialTab: Int = 0` to `MutualHandshakeScreen.kt`. Updated `EmptyPeersState.kt` with explicit dual actions: `[ SCAN PEER QR ]` (Primary cyan button opening Tab 1 / Camera directly) and `[ SHOW MY BEACON ]` (Outlined button opening Tab 0 / QR code). Updated `ChatScreen.kt` reciprocal verification banner with `[ SCAN PEER ]` routing directly to Tab 1.
  5. **Unified Navigation Backstack & Reactive Notification Deep-Linking**: Refactored `MainActivity.kt` from a flat enum to a Compose navigation stack (`screenStack = mutableStateListOf(...)`). Standardized `navigateTo(screen, initialTab)` and `navigateBack()`. Navigating `SETTINGS` -> `DEVICE_TRANSFER` -> Back now cleanly returns to `SETTINGS`. Bound `pendingNotificationContactUid` to reactively reset the stack to `[MESSAGES]` and select the contact upon notification tap.

### Entry 14
- **Timestamp**: 2026-09-27 08:35:00
- **File**: `app/src/main/java/in/grayscales/entangl/ui/profile/EditProfileScreen.kt`, `app/src/main/java/in/grayscales/entangl/ui/chat/ContactsScreen.kt`, `app/src/main/java/in/grayscales/entangl/ui/chat/ChatScreen.kt`, `app/src/main/java/in/grayscales/entangl/ui/chat/MessageBubble.kt`, `app/src/main/java/in/grayscales/entangl/ui/qr/MutualHandshakeScreen.kt`, `app/src/main/java/in/grayscales/entangl/ui/qr/MyQrScreen.kt`, `app/src/main/java/in/grayscales/entangl/ui/home/HomeChatLayout.kt`
- **Purpose**: System-Wide Usability, Messaging Comfort & Human-Centered UX Overhaul:
  1. **Conversational Empty Chat State (`EmptyChatBanner`)**: Eliminated the barren empty void when opening a new chat in `ChatScreen.kt`. Replaced with a cybernetic security welcome card displaying the peer's custom avatar, initials, codename, post-quantum encryption reassurance (ML-KEM-768 + Double Ratchet), active ephemeral self-destruct status, zero-knowledge relay architecture, and a 1-tap `[ 👋 SAY HELLO ]` icebreaker button.
  2. **Multi-Line Message Composing & Dynamic Send Feedback**: Upgraded `OutlinedTextField` from `singleLine = true` to `singleLine = false, maxLines = 4`, comfortably supporting multiline text, paragraphs, and formatted keys. Upgraded send button with active / dimmed states: glows vibrant `QuantumCyan` when text is typed and subtly mutes (`DarkMatterVariant`, 0.5f alpha) when empty.
  3. **Message Long-Press Copy to Clipboard & Visual Pip**: In `MessageBubble.kt`, added long-press gesture handling on chat bubbles that copies the message plaintext directly to the Android `ClipboardManager`, triggers a tactile `LongPress` haptic pulse, and renders an animated `COPIED TO CLIPBOARD` floating badge.
  4. **Dynamic Peer Search in Contact Roster**: In `ContactsScreen.kt`, added an interactive search bar with real-time filtering whenever a user has more than 2 peers. Allows searching contacts instantly by codename or UID with an instant clear button (X) and an elegant "NO PEERS MATCHING" empty state.
  5. **Guarded Request Ignore**: In `ContactsScreen.kt`, protected the "Ignore" button on incoming connection cards by routing through `DeleteContactConfirmDialog` before discarding, preventing accidental deletion of pending connection requests.
  6. **Human-Centered Handshake Terminology**: In `MutualHandshakeScreen.kt` and `HomeChatLayout.kt`, updated confusing sci-fi tabs ("TRANSMIT [BEACON]" / "RECEIVE [SENSOR]") to intuitive hybrid labels: `MY QR [BEACON]` and `SCAN QR [SENSOR]`. In `MyQrScreen.kt`, added a clear guidance subtitle: "Hold this screen out for your peer to scan with their camera."
  7. **Smoother Onboarding Completion**: In `EditProfileScreen.kt`, replaced the generic "APPLY" button with "INITIALIZE IDENTITY", added real-time helper validation hints, and enabled keyboard `ImeAction.Done` to submit the profile immediately when valid.

---

### Entry 15
- **Timestamp**: 2026-09-27 10:00:00
- **File**: `app/src/main/java/in/grayscales/entangl/ui/chat/EmptyPeersState.kt`, `app/src/main/java/in/grayscales/entangl/ui/qr/QrScannerView.kt`, `app/src/main/java/in/grayscales/entangl/ui/qr/QuantumScannerOverlay.kt`, `app/src/main/java/in/grayscales/entangl/ui/qr/MyQrScreen.kt`, `app/src/main/java/in/grayscales/entangl/ui/qr/MutualHandshakeScreen.kt`, `app/src/main/java/in/grayscales/entangl/ui/qr/HandshakeConfirmDialog.kt`, `app/src/main/java/in/grayscales/entangl/ui/chat/ChatScreen.kt`
- **Purpose**: Optical Mutual Handshake Flow & Empty State Humanization:
  1. **Empty State Single-Action Clarity**: Consolidated the cramped dual buttons in `EmptyPeersState.kt` into a single, prominent, full-width `[ SCAN QR ]` button with high-contrast text and clean copy ("Your peer roster is empty.\nScan a friend's QR code to establish an encrypted connection."), eliminating decision paralysis when first launching the app.
  2. **Mutual Camera Deadlock Elimination**: Resolved the real-world impasse where two peers both tap "Scan QR" and face each other with active cameras. Added a floating pill button in `QrScannerView.kt` (`[ 🔲 SHOW MY QR INSTEAD ]`) and a secondary button in `MyQrScreen.kt` (`[ 📷 SCAN PEER'S QR INSTEAD ]`), wired bidirectionally through `MutualHandshakeScreen.kt`.
  3. **Rolling Nonce Forward-Secrecy Reassurance**: In `MyQrScreen.kt`, added reassuring micro-copy under the 60s countdown: "⏳ Nonce auto-renews every 60s for forward secrecy — take your time." to eliminate countdown panic.
  4. **Tactile Optical Haptic Buzz**: In `QrScannerView.kt`, fired an immediate `LongPress` haptic buzz the exact millisecond either bundled ZXing or Google MLKit locks onto a valid QR frame.
  5. **Personalized Peer Detection Ceremony**: In `MutualHandshakeScreen.kt`, replaced the generic green checkmark in the real-time detection dialog with the peer's actual avatar circle, initials, and profile color. Updated action buttons to intuitive labels: `[ 📷 SCAN PEER BACK ]` and `[ 💬 OPEN CHAT ]`.
  6. **Safety Fingerprint Context Normalization**: In `HandshakeConfirmDialog.kt`, eliminated the confusing instruction asking the scanner to "match this with peer's screen" (since the transmitter's device is still showing their QR code). Replaced with clear context: "60-digit safety fingerprint generated. You can compare this anytime in chat settings to guarantee zero man-in-the-middle risk." and renamed action from "ESTABLISH ENTANGLEMENT" to `[ CONNECT WITH PEER ]`.
  7. **Dual-Action Reciprocal Banner**: In `ChatScreen.kt`, provided both `[ MY QR ]` and `[ SCAN PEER ]` actions on the reciprocal verification prompt, accommodating whichever peer scanned first.

---

### Entry 16
- **Timestamp**: 2026-09-27 16:25:00
- **File**: `shared/src/commonMain/kotlin/in/grayscales/entangl/core/crypto/DefaultCryptoManager.kt`, `app/src/main/java/in/grayscales/entangl/data/local/dao/MessageDao.kt`, `app/src/main/java/in/grayscales/entangl/data/repository/MessageRepositoryImpl.kt`, `app/src/main/java/in/grayscales/entangl/ui/chat/MessageBubble.kt`, `app/src/main/java/in/grayscales/entangl/ui/chat/ChatScreen.kt`, `app/src/main/java/in/grayscales/entangl/ui/chat/ChatViewModel.kt`, `app/src/test/java/in/grayscales/entangl/CryptoAndNetworkSecurityTest.kt`
- **Purpose**: Bilateral QR Scan Decryption, Symmetric AAD Wire Alignment & In-Message Unlock UI:
  1. **Fixed Cryptographic Tag Mismatch & Secret Symmetry (`DefaultCryptoManager`)**:
     - *Deterministic Shared Secret*: Replaced asymmetric single-side ephemeral DH generation in `deriveSharedSecret` with a deterministic lexicographical ordering of `localPub` and `peerPublicKey`, ensuring Device A and Device B always derive the exact same root key regardless of who initiates or responds.
     - *Wire-Invariant AEAD Binding*: In `encryptMessage` and `decryptMessage`, removed recipient-divergent AAD (`contactUid + ":" + seq`) which caused `AEADBadTagException` on the receiver (since sender used receiver's UID while receiver used sender's UID). Standardized to wire-invariant `"seq:$seq"`.
  2. **Reactive Message Cache Invalidation & DB Unlock (`MessageRepositoryImpl`)**:
     - Updated `observeForContact` to never permanently cache `"Encrypted message"` in `decryptedCache`. If a message was initially undecryptable (e.g. peer QR code not yet scanned), it automatically re-evaluates decryption as soon as the session key is initialized.
     - Enhanced `unlockPendingMessages` to query all messages for the contact via `MessageDao.getMessagesForContact` and decrypt all incoming ciphertexts, updating their status to `DELIVERED` and posting them directly to the active Room stream.
  3. **In-Bubble Interactive Decrypt Action (`EncryptedMessagePlaceholder`)**:
     - In `MessageBubble.kt`, when a message body is undecrypted (`message.plaintext == "Encrypted message"`), replaced plain text with an interactive cybernetic card: displays an `IsotopeMagenta` lock badge, explanatory helper text ("Peer's cryptographic key needed to decrypt this transmission."), and a prominent cyan `[ 📷 SCAN PEER'S QR TO DECRYPT ]` button.
     - Tapping the button directly opens the QR scanner from either device.
  4. **Chat Header Decryption Alert Banner (`ChatScreen`)**:
     - Evaluated `hasEncryptedMessages = messages.any { it.plaintext == "Encrypted message" }`.
     - When encrypted messages arrive on either end, dynamically converts the top banner to high-visibility magenta ("Encrypted messages received • Scan peer's QR code to decrypt incoming messages.") with dual 1-tap actions: `[ 🔲 MY QR ]` and `[ 📷 SCAN & DECRYPT ]`.
  5. **Handshake Contact Attribute Preservation (`ChatViewModel`)**:
     - In `addContactFromHandshake`, preserved existing contact metadata (displayName, profileColor, createdAt) when completing a reciprocal scan, preventing unintended overwriting of established contact nicknames.
  6. **Bilateral Cryptographic End-to-End Verification (`CryptoAndNetworkSecurityTest`)**:
     - Added comprehensive unit test `testTwoEndedMutualSessionEncryptionAndDecryption`: verifies Alice scanning Bob, Alice sending a message, Bob attempting decryption before scanning (verifying initial encrypted state), Bob scanning Alice's QR code, Bob decrypting Alice's message, Bob replying, and Alice decrypting Bob's reply.

