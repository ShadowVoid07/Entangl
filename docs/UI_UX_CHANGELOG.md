# Entangl UI/UX Changelog

> v1.0.0 is the first build. This file describes the shipped baseline exactly.

### v1.0.0 — First Stable Build (2026-10-01)
- **Identity onboarding:** `EditProfileScreen` one-time codename (1–25) + HSV/hex avatar color, permanently bound, no presets.
- **Handshake suite:** `MutualHandshakeScreen` dual-pane (>600dp) / segmented tabs, `MyQrScreen` 60s rolling QR (background encode), `QrScannerView` ZXing+MLKit with circular reticle + crosshairs, direction-aware peer dialog, mutual ledger with safety-gated unlock, safety matrix + audit dialog, one-time `HANDSHAKE SUCCESSFUL` celebration (persisted, internally verified) with `Chat now` / `Later`, exit-snapshot back transitions with no placeholder flash.
- **Chat:** `ChatScreen`/`MessageBubble` data frames with tinted borders, day dividers, open-at-newest-unseen + read marking, `18:23` time with `[SENDING]/[SENT]/[DELIVERED]` tags, live TTL countdown + glitch vaporization, encrypted-message placeholders, blocked bar, `ContactOptionsDialog` (clear / block / delete) via row overflow, plain roster previews (latest-message only), live presence dots, `EmptyPeersState` dual actions.
- **Settings/transfer:** `SettingsScreen` protocol matrix + succession entry + `Build: 1.0.0` footer, `DeviceTransferScreen` export/import with transfer-QR mode separation.
- **Transport/storage:** signed relay envelopes with verified ACKs/TTL, ping echo + handshake retry convergence, self-healing chains, exactly-once processing (`processed_envelopes`, replay-proof notifications gated to live arrivals), SQLCipher Room v5, downgrade-only destructive fallback.
- **Quality:** 47 `:app` + 8 `:shared` tests green; `lintDebug` + `lintRelease` 0 code findings; zero `@Suppress`; release assembles with R8.
