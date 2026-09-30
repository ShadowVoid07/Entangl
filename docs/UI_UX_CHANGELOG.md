# Entangl UI/UX Changelog

> v1.0.0 is the first build. Prior per-change entries are intentionally not retained; this file describes the shipped baseline exactly.

### v1.0.0 — First Stable Build (2026-09-30)
- **Identity onboarding:** `EditProfileScreen` one-time codename (1–25) + HSV/hex avatar color, permanently bound, no presets.
- **Handshake suite:** `MutualHandshakeScreen` dual-pane (>600dp) / segmented tabs, `MyQrScreen` 60s rolling QR, `QrScannerView` ZXing+MLKit with circular reticle + crosshairs, direction-aware peer dialog, mutual ledger with safety-gated unlock, safety matrix + audit dialog.
- **Chat:** `ChatScreen`/`MessageBubble` data frames with neon edge rails, day dividers, open-at-newest-unseen + read marking, `18:23` time with `[SENDING]/[SENT]/[DELIVERED]` tags, live TTL countdown + glitch vaporization, encrypted-message placeholders, blocked bar, `ContactOptionsDialog` (clear / block / delete) via row overflow, `EmptyPeersState` + `StandbyPane` dual actions.
- **Settings/transfer:** `SettingsScreen` protocol matrix + privacy shield + succession entry, `DeviceTransferScreen` export/import with transfer-QR mode separation.
- **Quality:** 47 `:app` + 8 `:shared` tests green; `lintDebug` 0 code findings; zero `@Suppress`.

### Follow-up hardening (post-baseline, same v1.0.0)
- **Handshake success moment:** after both scans complete, the app internally re-checks persisted mutual flags + pinned key + ratchet HMAC (`verifyHandshakeComplete`) and shows `HANDSHAKE SUCCESSFUL` with the safety number exactly once per peer (persisted), offering `Chat now` / `Later`. Pre-existing chats are seeded silently — no retroactive popups.
- **Stale-prompt cleanup:** the handshake peer dialog skips already-accepted peers; the legacy reciprocal prompt only fires for genuine inbound-only rows.
