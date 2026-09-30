# Entangl: Comprehensive User Flow & Usage Architecture Audit

## Executive Summary
This document provides a thorough audit of the user and usage flows across **Entangl Android**.
While Entangl possesses enterprise-grade post-quantum cryptography (ML-KEM-768, Double Ratchet, SQLCipher, 16 KB memory alignment), its navigation, handshake states, gesture handlers, and edge-case journeys suffer from disjointed transitions, ambiguous call-to-actions, conflicting gestures, and silent state updates.

---

## 1. Critical Flow Deficiencies & Friction Points

### 1.1 The "Stranded Beacon" Handshake Failure (Silent Receiver)
* **The Scenario**: Alice and Bob are standing next to each other. Alice wants to share her QR code; Bob will scan it.
* **The Current Failure**:
  1. Alice opens `MutualHandshakeScreen` (defaults to Tab 0: `TRANSMIT [BEACON]`).
  2. Bob opens `MutualHandshakeScreen`, switches to Tab 1 (`RECEIVE [SENSOR]`), and scans Alice's QR code.
  3. Bob sees a confirmation dialog, taps connect, enters `ChatScreen`, and a background `SCAN_PING` is sent to Alice.
  4. Alice's device receives `SCAN_PING` and creates a pending contact for Bob in her database.
  5. **BUT Alice remains stranded on the QR screen with zero visual feedback.** Her screen does not transition, emit a haptic chirp, or display a prompt.
  6. Alice assumes the scan failed. If Alice presses Back, she is dropped onto `ContactsScreen` where Bob is under "Incoming Requests", requiring multiple manual taps to accept, view an alert, and re-open the scanner.
* **Architectural Remedy**:
  * `MutualHandshakeScreen` must reactively observe incoming contact state.
  * When a `SCAN_PING` arrives while the transmitter screen is active, display an immediate bilateral confirmation card:
    > **PEER BEACON SCANNED**  
    > `[Bob]` scanned your beacon and requested entanglement.  
    > `[ ACCEPT & SCAN PEER ]` (direct swap to Tab 1 to scan Bob's QR) | `[ ACCEPT & CHAT ]`

---

### 1.2 Ambiguous & Redundant Navigation Entry Points
* **The Flaw**:
  * In `EmptyPeersState.kt`, there are two primary action buttons: `[ HANDSHAKE ]` and `[ MY QR ]`.
  * Both buttons navigate to `AppScreen.HANDSHAKE`.
  * Because `MutualHandshakeScreen` has no `initialTab` parameter, **both buttons open Tab 0 (Transmit Beacon)**.
  * In `StandbyPane.kt` (tablet/foldable detail pane), `[ SCAN QR ]` and `[ MY QR ]` also both open Tab 0!
  * In `ChatScreen.kt`, when reciprocal verification is pending, the banner button says `[ SHARE QR ]` even though the user already shared their QR and needs to *scan* the peer's QR code.
* **Architectural Remedy**:
  * Parameterize `MutualHandshakeScreen` with `initialTab: Int = 0` (0 = Transmit Beacon, 1 = Receive Sensor).
  * Direct all scanner actions (`SCAN PEER`, `SCAN QR`, `VERIFY RECIPROCAL`, `SCAN IMPORT QR`) to `initialTab = 1`.
  * Direct all beacon actions (`MY BEACON`, `SHARE QR`) to `initialTab = 0`.
  * In `EmptyPeersState`, provide explicit peer role choices:
    * `[ SCAN PEER QR ]` (Primary Cyan - opens Camera directly)
    * `[ SHOW MY BEACON ]` (Outlined - opens QR beacon directly)

---

### 1.3 Catastrophic Gesture Collision (Plaintext Reveal vs. Contact Deletion)
* **The Flaw**:
  * In `SettingsScreen`, users are instructed:  
    *"Anti-Shoulder-Surfing Shield: Hold down any conversation row to temporarily reveal the plaintext."*
  * In `ChatListRow.kt`, the row attaches `detectTapGestures`:
    * `onPress`: sets `isHolding = true` to reveal the blurred text.
    * `onLongPress`: triggers `onLongClick?.invoke()`.
  * In `ContactsScreen.kt`, line 215:
    `onLongClick = { onDeleteContact(contact) }`
  * In `MainActivity.kt`:
    `onDeleteContact = { contact -> chatViewModel.deleteContact(contact) }`
  * **Result**: When a user holds down a contact row for more than 400ms to read their message, **the app immediately and permanently wipes the contact, destroys the Double Ratchet session keys, and deletes all chat history with zero confirmation!**
* **Architectural Remedy**:
  * Remove `onLongClick` deletion from `ChatListRow`.
  * Plaintext reveal should be purely `onPress` (hold to reveal, release to blur).
  * Contact deletion must be an explicit, guarded action with a confirmation dialog:
    > **TERMINATE ENTANGLEMENT?**  
    > Are you sure you want to terminate entanglement with `[Peer]`?  
    > All Double Ratchet session keys and encrypted history will be permanently wiped.  
    > `[ PURGE & TERMINATE ]` | `[ CANCEL ]`

---

### 1.4 Backstack Amnesia & Notification Drop
* **The Flaw**:
  * `MainActivity.kt` uses a single `currentScreen` state variable rather than a navigation backstack:
    * `SETTINGS` -> `DEVICE_TRANSFER` -> Back drops the user at `MESSAGES` instead of `SETTINGS`.
    * `HANDSHAKE` -> `SETTINGS` -> Back drops the user at `MESSAGES` instead of `HANDSHAKE`.
  * `onNewIntent` Notification Failure:
    * `onNewIntent` updates `currentScreenState.value = AppScreen.MESSAGES`, but Compose uses a local `var currentScreen by remember(isIdentityConfigured) { ... }` that does not observe `currentScreenState` after startup.
    * Tapping an incoming notification while in `SETTINGS` or `HANDSHAKE` fails to navigate to the chat.
* **Architectural Remedy**:
  * Implement a lightweight, predictable navigation stack:
    * Maintain a backstack list `screenStack = remember { mutableStateListOf(startDestination) }`.
    * Forward navigation pushes onto the stack.
    * Back navigation pops the stack.
    * At root (`MESSAGES`), back navigation deselects the active contact (if any) or exits.
  * Synchronize incoming notification intents to reset the stack to `[MESSAGES]` and select the contact.

---

### 1.5 Dead Screen Enum Remnants
* In `MainActivity.kt`, `enum class AppScreen` contains:
  * `MY_QR` (lines 468–477)
  * `SCAN_QR` (lines 479–492)
  * `DASHBOARD` (aliased to `SETTINGS`)
* These routes are completely unnavigable because `onScanQr` and `onShowMyQr` both point to `HANDSHAKE`.
* `MutualHandshakeScreen` already houses both tabs. Removing dead screen entries cleans the routing table.

---

## 2. Redesigned End-to-End User Flow Architecture

```mermaid
flowchart TD
    %% App Startup
    Start([App Launch]) --> DecomCheck{Device Decommissioned?}
    DecomCheck -- Yes --> Tombstone[Decommissioned Screen / Lockout]
    DecomCheck -- No --> IdCheck{Identity Configured?}

    %% Onboarding Flow
    IdCheck -- No --> SetupIdentity[INITIALIZE IDENTITY\n- Codename max 25\n- Cyber Color Picker\n- Irreversible Lock]
    SetupIdentity --> SetProfile[chatViewModel.setProfile\nOne-Way Forward Transition]
    SetProfile --> RosterHub

    %% Core Messages Hub
    IdCheck -- Yes --> RosterHub[MESSAGES HUB\nAdaptive Dual-Pane / Push]
    
    subgraph Roster [Messages & Contacts Roster]
        RosterHub --> RosterState{Contacts Empty?}
        RosterState -- Yes --> EmptyView[EMPTY PEERS RADAR\n1. [SCAN PEER QR] -> Handshake Tab 1\n2. [SHOW MY BEACON] -> Handshake Tab 0]
        RosterState -- No --> ContactList[Contact List\n- Incoming Requests\n- Established Peers\n- Anti-Shoulder Blur]
    end

    %% Handshake Flow
    EmptyView -- SCAN PEER QR --> ScannerTab[HANDSHAKE: SENSOR TAB\nPoint Camera at Peer QR]
    EmptyView -- SHOW MY BEACON --> BeaconTab[HANDSHAKE: BEACON TAB\nDisplay Local QR Code]
    ContactList -- Top Bar Scanner Icon --> HandshakeChooser[HANDSHAKE\nDual Tab: Beacon / Sensor]

    subgraph MutualExchange [Optical Handshake Flow]
        BeaconTab -. Peer Scans Beacon .-> RealtimeNotice[REAL-TIME PEER DETECTED!\n'Bob scanned your code'\n[ACCEPT & SCAN] | [ACCEPT & CHAT]]
        ScannerTab --> CameraDetected[QR Detected & Decoded]
        CameraDetected --> PeerConfirmDialog[Peer Verification Dialog\nSafety Number Prefix & Codename]
        PeerConfirmDialog --> AddContact[Add Contact & Send SCAN_PING]
        AddContact --> ChatDirect[Open Chat With Peer]
        RealtimeNotice -- ACCEPT & SCAN --> ScannerTab
        RealtimeNotice -- ACCEPT & CHAT --> ChatDirect
    end

    %% Active Chat Flow
    ContactList -- Select Contact --> ChatDirect[ACTIVE CHAT SCREEN\n- Double Ratchet\n- TTL Self-Destruct\n- Zero-Leak Storage]
    ChatDirect --> ChatStates{Contact State}
    ChatStates -- Unaccepted --> AcceptCard[Connection Request Card\n[Accept & Chat] | [Decline]]
    ChatStates -- Reciprocal Pending --> ReciprocalBanner[Verification Pending Banner\n[SCAN PEER QR] -> Handshake Tab 1]
    ChatStates -- Fully Verified --> FullChat[Bidirectional Direct Messaging]

    %% Navigation & Settings
    RosterHub -- Settings Icon --> SettingsView[SETTINGS & PROTOCOL MATRIX\n- Node Profile Card\n- Cryptographic Parameters\n- Privacy Blur Toggle\n- Tor Diagnostics Sheet]
    SettingsView -- Device Migration --> MigrationView[DEVICE SUCCESSION\n- Export Old Device\n- Import New Device P2P]
    MigrationView -- Scan Import QR --> ScannerTab
```

---

## 3. Detailed Screen-by-Screen Usage Specifications

### Screen 1: Initialize Identity (`INITIALIZE_IDENTITY`)
* **Purpose**: First-run zero-knowledge identity generation.
* **Interactions**:
  * Inputs: Codename (1-25 characters), Cybernetic Color Palette (7 neon hues).
  * Action: `[ INITIALIZE IDENTITY ]` button (disabled until valid codename provided).
  * Security: Once confirmed, `NodeIdentityManager` binds codename and color permanently.
  * Back Navigation: Exits application (user cannot bypass setup).

### Screen 2: Messages & Roster Hub (`MESSAGES`)
* **Purpose**: Main communication dashboard.
* **Adaptive Behavior**:
  * Single-pane on phones (< 600dp): Push list-to-detail navigation with slide transitions.
  * Dual-pane on foldables/tablets (>= 600dp): List pane on left, Chat / Standby pane on right.
* **Empty State**:
  * Concentric pulsing radar animation.
  * Dual primary actions:
    * `[ SCAN PEER QR ]` (Primary cyan button, camera icon): Launches `HANDSHAKE` directly on Tab 1 (Scanner).
    * `[ SHOW MY BEACON ]` (Outlined cyan button, QR icon): Launches `HANDSHAKE` directly on Tab 0 (Beacon).
* **Roster Gestures**:
  * Tap: Opens conversation.
  * Press-and-hold: Temporarily strips Gaussian blur to reveal message preview. Releasing re-blurs.
  * Swipe or info menu: Triggers `DeleteContactDialog` with confirmation guardrail.

### Screen 3: Optical Mutual Handshake (`HANDSHAKE`)
* **Purpose**: In-person zero-knowledge cryptographic key exchange.
* **Top Bar**: Back button, `MUTUAL HANDSHAKE` title with green shield, Settings gear icon.
* **Tabs**:
  * Tab 0: `TRANSMIT [BEACON]` (`MyQrScreen`) with high brightness and `FLAG_SECURE`.
  * Tab 1: `RECEIVE [SENSOR]` (`QrScannerView`) with targeting reticle and torch toggle.
* **Real-Time Peer Detection (The "Stranded Beacon" Fix)**:
  * While Tab 0 is displayed, the screen observes `chatViewModel.contacts`.
  * When a peer scans the code, an inline dialog or bottom card immediately presents:
    > **PEER BEACON SCANNED**  
    > `[Peer Codename]` scanned your beacon.  
    > `[ ACCEPT & SCAN PEER ]` (switches to Tab 1 to scan peer's QR)  
    > `[ ACCEPT & OPEN CHAT ]` (accepts peer and opens chat directly)

### Screen 4: Active Chat (`MESSAGES` Detail)
* **Top Bar**: Peer avatar, codename, connection dot, self-destruct TTL toggle pill, safety number audit button, diagnostics uplink button.
* **Pending States**:
  * Unaccepted: Banner and bottom bar with `[ Accept & Chat ]` and `[ Decline ]`.
  * Reciprocal Pending: Banner with `[ SCAN PEER QR ]` (opens Tab 1).
* **Messages LazyColumn**: Animated bubbles with delivery status pips, zeroizing burn animation upon expiry.
* **Input Bar**: Monospaced text input with send action.

### Screen 5: Settings & Diagnostics (`SETTINGS`)
* **Header**: Back button, bold title `ENTANGL SETTINGS`, clickable `NetworkSignalIndicator` with 5-second pulse opening `NetworkStatusSheet`.
* **Cards**:
  1. **Node Identity Card**: Avatar with initials, immutable codename, verified badge.
  2. **Core Protocol Matrix**: Cryptographic standards status (ML-KEM-768, Ed25519, Double Ratchet, SQLCipher).
  3. **Anti-Shoulder-Surfing Privacy Shield**: Toggle for 8.dp Gaussian blur on contact rows.
  4. **Device Succession & Migration**: Direct navigation to `DEVICE_TRANSFER`.
  5. **Threats Advisory**: Live status if debugger/root/keylogger detected.

### Screen 6: Device Transfer & Succession (`DEVICE_TRANSFER`)
* **Purpose**: P2P hardware migration with cryptographic handoff.
* **Tabs**:
  * Tab 0: Export (Old Device) -> generates migration QR code.
  * Tab 1: Import (New Device) -> `[ SCAN MIGRATION QR ]` button (opens Tab 1 of Handshake).
* **Back Navigation**: Returns cleanly to `SETTINGS`.
