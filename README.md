# Instinct Companion for Android

**Email underneath. Conversation on top.**

An unofficial, open-source Android app that turns your Instinct email conversation into a focused messaging experience. Your phone connects directly to Gmail. No relay server, subscription to this app, or running desktop is required.

[Download the APK](https://github.com/aribradshaw/instinct-android/releases/latest) · [Public DevLog](https://aribradshaw.github.io/instinct-android/) · [Report an issue](https://github.com/aribradshaw/instinct-android/issues)

> This is an independent community companion, not an official Instinct application. You need your own Instinct account with an active email channel. This app does not create an Instinct account or restore access to another messaging service.

## What it does

- Displays incoming and sent Instinct emails as selectable chat bubbles in a dark olive and lime interface with a saved light/dark switch.
- Sends plain-text messages from your own Gmail account, preserving the subject and reply headers.
- Saves drafts, conversation history, pending sends, and credentials encrypted on the phone.
- Shows explicit sent, failed, and unconfirmed states. It never automatically retries an uncertain send.
- Refreshes every 15 seconds while open and schedules periodic background reply notifications.
- Provides the full original email, clickable links, attachment links to Gmail, and Android text sharing into the composer.
- Includes a public DevLog powered by [@aribradshaw/devlog](https://github.com/aribradshaw/devlog).

## Install

1. Download `instinct-companion-0.1.0.apk` from [Releases](https://github.com/aribradshaw/instinct-android/releases/latest).
2. Open it on an Android 11 or newer phone. Allow installation from the browser or file manager if Android asks.
3. Open **Instinct** and connect your account below.

The first release is a small, non-debuggable APK signed with the maintainer's local certificate. The checksum file accompanies the release. Updates from this repository retain the same application identity. GitHub Actions debug artifacts are development builds and use a different signing key from the release.

## Connect your account

1. Tap **Connect my Gmail**.
2. Enter the **Gmail address Instinct recognizes** and the **exact email address Instinct gave you**, ending in `@mail.instinct.com`.
3. Tap **Create password**, verify the selected Google account, and create a Google app password named **Instinct**.
4. Return to the app, paste the 16-letter app password, and tap **Connect**. Use an app password, never your normal Google password.
5. Allow notifications when Android asks. Existing incoming and sent messages will sync from Gmail All Mail.
6. Write a message and tap the arrow to send an actual email to your configured Instinct address.

Google app passwords require 2-Step Verification and are unavailable on some accounts, including certain Advanced Protection configurations. See [Google's app password instructions](https://support.google.com/accounts/answer/185833). This first version supports personal `@gmail.com` accounts. Google OAuth sign-in is not implemented.

## Privacy and delivery

The app connects directly to `imap.gmail.com:993` and `smtp.gmail.com:465` over TLS with hostname verification. Your account information is configured on the device and is not included in the public source or APK. A Google app password grants broader email access, though this app only displays messages exchanged with the exact configured Instinct address.

Android Keystore protects AES-GCM encrypted account configuration, password, message cache, draft, and pending sends. Cloud backup and device transfer are disabled. The credential dialog blocks screenshots. The interface uses bundled assets, renders email content as text, and loads no trackers or remote email images. Disconnect clears local data; revoke the app password separately in Google Account settings when you no longer need it.

**Sent via Gmail** means Gmail accepted the email, not that Instinct read or acted on it. If a connection drops during send, the app preserves an **Unconfirmed** entry and does not retry automatically. Check Gmail before manually sending again. Subsequent sync reconciles messages by Message-ID.

## Current limits

- Text composition only. Incoming attachment names open Gmail; file uploads and voice notes are not implemented.
- Background checks run about every 15 minutes and can take longer under Android battery restrictions. This is periodic email sync, not instant push.
- Each sync reads up to 200 latest matching messages and retains previously cached history.
- Gmail All Mail must be available through IMAP. This is normally available by default; check Gmail label settings if you encounter an error.
- The app does not mark messages read, change labels, delete emails, or send messages automatically.
- The UI is bundled HTML/CSS in a native Android WebView, with native account setup, mail transport, encrypted storage, and background jobs.

## Build from source

Requirements: Java 17+, Android SDK 36, and Node 22+ for the optional public DevLog.

```sh
git clone https://github.com/aribradshaw/instinct-android.git
cd instinct-android
# Set ANDROID_HOME, or create local.properties containing sdk.dir=/your/Android/Sdk
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

On Windows, use `gradlew.bat`. The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. To install through USB:

```sh
adb devices -l
adb -s YOUR_DEVICE_SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
```

Custom builds have their own signing identity and may require uninstalling an existing release first. Uninstalling removes its local connection and cache.

For a signed release, add `signing.storeFile`, `signing.storePassword`, `signing.keyAlias`, and `signing.keyPassword` to your untracked `local.properties`, then run `./gradlew :app:assembleRelease`. Never commit signing keys or passwords.

### Public DevLog

```sh
npm ci
npm run build:devlog
```

The static output is in `site/dist/`. The site imports the shared DevLog package for release validation, version alignment, search, and source metadata. The host controls the styling and public content. GitHub Pages deploys it from `main` through `.github/workflows/pages.yml`.

To publish an update, edit `config/devlog-releases.json` and align the version in `package.json`, Android's `versionName`, and visible app labels. The build fails on DevLog/package version mismatch. Use only public release copy and synthetic preview messages.

## Verification

Eight unit tests cover quoted-history trimming, exact address comparisons, recipient identity, reply threading, UTF-8 MIME round trips, unique Message-IDs, and header-injection rejection. Android build and lint are included in CI. The initial version was installed on a physical Android 16 device; private Gmail authentication, incoming history, and a user-initiated outgoing message were observed. No build or test sends email.

## Contributing and license

See [CONTRIBUTING.md](CONTRIBUTING.md) and [SECURITY.md](SECURITY.md). Contributions are welcome, especially OAuth account connection, attachment composition, and improved background delivery.

[MIT](LICENSE), copyright 2026 Ari Bradshaw. MIT permits reuse, modification, and redistribution with attribution and the license notice. Dependencies retain their own licenses; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
