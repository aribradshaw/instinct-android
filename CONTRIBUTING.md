# Contributing

Start with an issue describing the problem, expected behavior, Android version, and a redacted error. Never include an app password, private email, screenshot of a real conversation, or raw account storage.

1. Fork and clone the repository.
2. Install Java 17 or newer and Android SDK 36. Set `ANDROID_HOME`, or use an untracked `local.properties` with `sdk.dir`.
3. Run `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.
4. For the DevLog, use Node 22+, `npm ci`, then `npm run build:devlog`.
5. Use example identities and synthetic messages in tests and previews. A build must never send real email.

Keep changes focused. Mail transport changes need tests for recipient identity, threading, and failure behavior. UI changes should be checked on a small portrait viewport and with the keyboard open. Add user-facing changes to `config/devlog-releases.json`, and keep its latest version aligned with `package.json`, the Android version, and visible version labels.

By contributing, you agree that your contributions are licensed under the repository's MIT license.
