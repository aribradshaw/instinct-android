# Instinct Companion

A focused Android inbox for your Instinct email conversation, powered directly by your Gmail account.

[Download the APK](https://github.com/aribradshaw/instinct-android/releases/latest) · [DevLog](https://aribradshaw.github.io/instinct-android/) · [Issues](https://github.com/aribradshaw/instinct-android/issues)

> Independent community software. You need an active Instinct email channel. This app does not create or restore an Instinct account.

## What you get

- A clean, searchable chat view of the Gmail thread with drafts, replies, copy, older history, and editable voice dictation.
- Attach up to 8 files, 12 MB combined. Files remain encrypted on the phone until sent or removed.
- Reply alerts, configurable foreground and background checks, appearance choices, and a Gmail guide for preventing duplicate Gmail notifications.
- Clear delivered, failed, and unconfirmed send states. Unconfirmed email is never retried automatically.
- Local-only encrypted account settings, message cache, drafts, and pending sends.

## Install

1. Download the current APK from [Releases](https://github.com/aribradshaw/instinct-android/releases/latest).
2. Open it on Android 11 or newer and allow the install prompt if Android asks.
3. Open **Instinct**, then connect Gmail.

Release APKs retain the app identity for future upgrades. GitHub Actions artifacts are development builds and use a different signing key.

## Connect Gmail

1. Tap **Connect my Gmail**.
2. Enter the Gmail address Instinct recognizes and your exact `@mail.instinct.com` address.
3. Create a Google app password named **Instinct**, then paste its 16-character value into the app.
4. Allow notifications and send a message.

Use an app password, never your regular Google password. App passwords require 2-Step Verification and may not be available for every Google account. [Google's instructions](https://support.google.com/accounts/answer/185833)

## Privacy and limits

Instinct connects directly to Gmail IMAP and SMTP over TLS. It has no relay server, trackers, remote email images, cloud backup, or device transfer. Android Keystore encrypts the local connection details and cache. Disconnect clears app data. Revoke the app password in your Google Account when you are done.

This is periodic Gmail sync, not instant push. While the app is open it checks as often as every 15 seconds. Background checks run about every 15 minutes and Android battery controls may delay them. The app cannot modify Gmail notifications, labels, or read status.

## Build

Requires Java 17+, Android SDK 36, and Node 22+ for the public DevLog.

```sh
git clone https://github.com/aribradshaw/instinct-android.git
cd instinct-android
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
npm ci
npm run build:devlog
npm run check:public
```

On Windows, use `gradlew.bat`. The debug APK is at `app/build/outputs/apk/debug/app-debug.apk`.

## Contribute

See [CONTRIBUTING.md](CONTRIBUTING.md) and [SECURITY.md](SECURITY.md). The project is [MIT licensed](LICENSE).