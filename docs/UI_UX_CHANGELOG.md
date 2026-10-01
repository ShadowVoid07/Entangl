# Entangl UI/UX Changelog

> v1.0.0 is the first build. This file describes the shipped baseline exactly.

### v1.0.0 — First Stable Build (2026-10-01)
- **Identity onboarding:** `EditProfileScreen` one-time codename (1–25) + HSV/hex avatar color, permanently bound, no presets.
- **Handshake suite:** `MutualHandshakeScreen` dual-pane (>600dp) / segmented tabs, `MyQrScreen` 60s rolling QR (background encode), `QrScannerView` ZXing+MLKit with circular reticle + crosshairs, direction-aware peer dialog, mutual ledger with safety-gated unlock, safety matrix + audit dialog, one-time `HANDSHAKE SUCCESSFUL` celebration (persisted, internally verified) with `Chat now` / `Later`, exit-snapshot back transitions with no placeholder flash.
- **Chat:** `ChatScreen`/`MessageBubble` data frames with tinted borders, day dividers, open-at-newest-unseen + read marking, `18:23` time with `[SENDING]/[SENT]/[DELIVERED]` tags, live TTL countdown + glitch vaporization, encrypted-message placeholders, blocked bar, `ContactOptionsDialog` (clear / block / delete) via row overflow, plain roster previews (latest-message only), live presence dots, `EmptyPeersState` dual actions.
- **Settings/transfer:** `SettingsScreen` protocol matrix (shipped primitives) + succession entry + `Build: 1.0.0` footer, live signal bars + internet dot (`NetworkQuality`), honest relay diagnostics sheet with working Renew, `DeviceTransferScreen` export/import with transfer-QR mode separation.
- **Transport/storage:** signed relay envelopes with verified ACKs/TTL, ping echo + handshake retry convergence, self-healing chains, exactly-once processing (`processed_envelopes`, replay-proof notifications gated to live arrivals), SQLCipher Room v5, downgrade-only destructive fallback.
- **Quality:** 51 `:app` + 8 `:shared` tests green; `lintDebug` + `lintRelease` 0 code findings; zero `@Suppress`; release assembles with R8.

### Follow-up: UI-state audit + storage audit fixes (same v1.0.0)
- **Chat priority unified** (`Blocked > Unaccepted > Encrypted > Reciprocal`) across bottom bar, banners, header dot/subtext, and diagnostics dialog — blocked chats get an Unblock path everywhere, no more stacked or dead states; empty-chat banner suppressed when blocked.
- **Roster honesty:** header counts `N peers • M requests`; dedicated `BLOCKED` section; blocked pip forced gray.
- **Deep-link cold start:** unknown UIDs retained 8s for roster load before giving up, with a toast instead of silent drop.
- **Transfer hardening:** succession certificate verified before any import insert; trust/block flags merged (migrated accepted chats stay verified, blocks preserved); row-count caps; FK pre-check (all-or-nothing validation); export/import length bounds both sides; profile fields sanitized on import.
- **Notification hygiene:** collision-free per-contact IDs/requestCodes; inbound names length/control-char sanitized at repo and shade; PII stripped from logs.
- **A11y:** TTL pill carries button role + label; row overflow is 48dp.
