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
