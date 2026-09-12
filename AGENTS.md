# Repository rules

- Avoid em dashes.
- Every source push to main must advance year.month.version and appear in the public DevLog. The baseline is 1.0.1, using America/Phoenix calendar boundaries and the shared @aribradshaw/devlog version helper.
- The Pages workflow runs scripts/record-devlog.mjs before publishing. It records every previously unrecorded source commit, so a push containing several commits can advance several versions. Generated chore(release): commits are excluded. Never use that prefix for source changes.
- Write concise, factual commit subjects suitable for public release notes. Never put credentials, private email addresses, or conversation content in commits or DevLog entries.
- Automation synchronizes package.json, package-lock.json, Android versionName/versionCode, the visible app version, and config/devlog-releases.json. Do not manually bump these for normal pushes. Pull the generated release commit before starting the next change.
- Keep config/devlog-state.json intact. It is the durable checkpoint used for duplicate prevention and catching up skipped workflow runs.
- Automatic entries describe source updates. They do not claim that a signed APK was released or installed. Signed APK publication remains a separate release step.
- Before publishing, run npm run build:devlog and the public source check. Deployment must fail if generation or validation fails.
