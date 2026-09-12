# Security

Do not include credentials or private conversations in public issues. For a security issue, use GitHub's private vulnerability reporting for this repository when available. If it is unavailable, open a minimal issue requesting a private reporting channel without exploit details or sensitive data.

The app stores its account configuration, Google app password, cache, pending sends, and draft encrypted with AES-GCM. Keys are generated in Android Keystore and are not exported. Backups and device transfer are disabled. A compromised or unlocked device remains a risk.

The application connects only to Gmail's IMAP and SMTP servers using TLS with hostname checks. The local UI loads bundled assets only. Remote email HTML is converted to text, scripts are blocked, and links open in the external browser after a tap. No telemetry, analytics, remote fonts, or relay service are included.

Google app passwords allow broader mail access than this single-conversation UI. Revoke the password in Google Account settings if you lose the device or stop using the app. Disconnecting in the app deletes local data but does not revoke the password in Google.

Release APKs are non-debuggable and signed by the maintainer. GitHub Actions debug artifacts are for development and are not the published release APK. Keep signing keys private; custom builds use a different signature and may require uninstalling an existing build.
