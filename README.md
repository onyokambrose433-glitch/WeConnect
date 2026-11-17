# WeConnect Android

WeConnect is a real-time, end-to-end encrypted messaging and social networking Android application built with **Kotlin**, **Jetpack Compose**, **Material Design 3**, **Jetpack Credential Manager (Google Sign-In)**, and **Cloud Firestore**.

## Core Capabilities

- **Real User Accounts (Zero Demo Accounts)**:
  - Authenticated via Google Sign-In (`CredentialManager` + `GetSignInWithGoogleOption`).
  - Unique `@username` registration checked atomically against `/usernames/{username}` and stored alongside public profiles (`/public_profiles/{uid}`) and owner-isolated private PII/settings (`/users/{uid}`).
  - Two or more real users on separate devices can search each other by `@username` or display name, follow one another, chat in real time, and place voice/video calls.
- **End-to-End Encrypted Messaging**:
  - Per-device **NIST P-256 ECDH** keypairs generated locally via Android JCA and persisted privately in `DataStore`.
  - Direct 1-to-1 messages derive a shared **AES-256-GCM** key (`ECDH` + `SHA-256` + 12-byte random IV) and store ciphertext in Firestore (`/conversations/{chatId}/messages/{messageId}`).
  - Supports 1-to-1 and group chats, replies, forwarding, editing, deleting for all, emoji reactions, pinning, disappearing messages (`Off` / `24h` / `7d`), typing indicators, read/delivered receipts, photo/document attachments, and playable AAC voice notes.
- **Social Feed & 24-Hour Stories**:
  - Publish public or followers-only posts with captions, `#hashtags`, and up to 3 compressed photos via the zero-permission Android Photo Picker (`PickVisualMedia`).
  - React, comment, share, bookmark (`saved_posts`), and report posts.
  - Share 24-hour expiring photo/text stories with viewer tracking and reactions.
- **Communities, Channels & Call Signaling**:
  - Create and join public communities and announcement channels with pinned announcements and community rules.
  - Real-time voice and video call signaling and call history via `/calls/{callId}` with terminal state locking (`ENDED`, `DECLINED`, `MISSED`).
- **Security & Privacy Controls**:
  - Zero-trust `firestore.rules` validated locally with `@firebase/rules-unit-testing` (`firestore.test.js`).
  - Device management & revocation, cryptographic key fingerprint display, blocking/unblocking, and account data export.

## Brand Palette

- Deep Emerald Green: `#075E45`
- Fresh Green: `#16A34A`
- Metallic Gold: `#D4AF37`
- White: `#FFFFFF`
- Dark Forest: `#0B1F14`
