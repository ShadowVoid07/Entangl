<p align="center">
  <img src="entangl.png" alt="Entangl Logo" width="140" height="140" />
</p>

<h1 align="center">Entangl</h1>

<p align="center">
  <strong>Zero-Knowledge, Post-Quantum Secure Peer-to-Peer Android Messenger</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-API%2035-brightgreen.svg" alt="Android SDK 35" />
  <img src="https://img.shields.io/badge/Kotlin-2.0-blue.svg" alt="Kotlin 2.0" />
  <img src="https://img.shields.io/badge/Security-PQXDH%20%2B%20DoubleRatchet-purple.svg" alt="Quantum Encryption" />
  <img src="https://img.shields.io/badge/License-AGPLv3-red.svg" alt="License" />
</p>

---

**Entangl** is a privacy-first, decentralized-identity Android messaging application. Unlike traditional chat apps that rely on phone numbers, usernames, or public directories, Entangl establishes communication channels exclusively through a **Mutual Physical Handshake** using two-way dynamic QR code verification.

Neither user can send a message until both parties have physically scanned each other's visual public identity keys and nonces.

## Core Features

* **Zero Trust Discovery:** Eradicate spam, unsolicited requests, and bulk scraping by enforcing in-person or out-of-band mutual scanning.
* **True Peer-to-Peer (P2P) Architecture:** Communicate directly device-to-device via Tor Onion Services (`arti`). Messages are never stored on any backend or third-party infrastructure.
* **Post-Quantum End-to-End Encryption:** Utilizes a hybrid **PQXDH (X25519 + ML-KEM-768 Kyber)** ratchet via `libsignal-client` to defend against Store Now, Decrypt Later (SNDL) attacks.
* **Two-Layer Encryption Model:** Messages are encrypted via the Double Ratchet Protocol (Forward Secrecy, Post-Compromise Security), and the encrypted payloads are then transported over Tor (anonymity and transport encryption).
* **Strict On-Device Security:** Keys are kept in native memory and zeroed out explicitly. State integrity is backed by Android Keystore. Includes defenses against tapjacking, ADB backups, and memory scraping.

## Getting Started

### Prerequisites
* Android Studio Ladybug (or newer)
* Java 17
* Android SDK API 35

### Building the Project
1. Clone the repository:
   ```bash
   git clone https://github.com/your-org/entangl-android.git
   ```
2. Open the project in Android Studio.
3. Sync Gradle.
4. Build and run on an emulator or physical device.

## Architecture Highlights
* **UI:** 100% Jetpack Compose (Material 3). Designed with adaptive layouts for foldables.
* **Cryptography:** `libsignal-client` (Rust via JNI) and `sodium_memzero` for key lifecycle management.
* **Local Storage:** `SQLCipher` for encrypted Room database.
* **Networking:** `arti` (Tor implementation in Rust) for .onion endpoint generation and P2P routing.

## Contributing
We welcome contributions from the community! Please read our [Contributing Guidelines](CONTRIBUTING.md) and [Code of Conduct](CODE_OF_CONDUCT.md).

## Security
If you discover a security vulnerability within Entangl, please refer to our [Security Policy](SECURITY.md) for reporting instructions.

## License
This project is licensed under the **GNU Affero General Public License v3.0 (AGPLv3)** - see the [LICENSE](LICENSE) file for details.
