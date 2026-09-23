# Security Policy

## Supported Versions

Only the latest release of Entangl is actively supported for security updates. Given the nature of a peer-to-peer messaging application, users are strongly encouraged to always run the latest version to ensure protocol compatibility and security.

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

## Reporting a Vulnerability

Security is the absolute highest priority for Entangl. If you discover a vulnerability, **do not open a public issue.**

Please report security issues responsibly by emailing:
**security@entangl.app** (Placeholder email)

### What to include in your report:
* Description of the vulnerability.
* Steps to reproduce.
* Potential impact (e.g., Remote Code Execution, Key Extraction, MITM).
* Proof of concept code or logs if available.

### What to expect:
* We will acknowledge receipt of your vulnerability report within 48 hours.
* We will send you regular updates about our progress.
* Once the issue is fixed, we will coordinate a public disclosure with you (and credit you if desired).

### Scope
We are particularly interested in vulnerabilities concerning:
* Bypassing the QR Handshake authentication.
* Extraction of cryptographic keys from the Android Keystore or JNI memory.
* De-anonymization of Tor Onion routing endpoints.
* Flaws in the implementation of the `libsignal-client` Double Ratchet or PQXDH bindings.
* Bypasses of our runtime hardening (ADB block, screen capture block, accessibility abuse).
