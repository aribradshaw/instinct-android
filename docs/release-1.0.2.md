# Instinct Companion 1.0.2

Send attachments directly from the composer, find saved messages, reply to a selected email, copy text, and dictate editable drafts. Files remain encrypted locally until removed or sent. Up to 8 files and 12 MB total per message.

Sync now tracks progress across batches and offers older history. The first incoming message after an empty completed sync can trigger an alert. The header shows the last successful check, and rejected credentials ask you to reconnect. Definite failures can be recovered for editing; uncertain sends retain the Gmail check action.

The lightning logo refreshes on tap and shows an animated hint on hold. Settings includes the latest download and connection guide.

## Verification and remaining work

Android unit tests, lint, signed release build, and synthetic mobile browser interactions are checked before publication. MIME tests verify attachment bytes, filenames, threading, and aggregate size limits without sending email.

Google OAuth is not enabled: a Cloud project, registered Android release identity, consent configuration, and Gmail restricted-scope verification are required. App passwords remain the supported connection method.

Instinct attachment interpretation, overnight notification delivery, reboot behavior, and interrupted SMTP delivery need real-device acceptance testing. The app transmits standard attachments but does not promise support for every format at the agent end. Background notifications remain periodic rather than instant.

## Research informing this release

- [Instinct task stress-test discussion](https://www.reddit.com/r/AI_Agents/comments/1wak4is/ive_been_stresstesting_instinct_with_reallife/): anecdotal reports of slow tasks and interruptions informed clear delivery state instead of inferred agent progress.
- [Gmail-access discussion](https://www.reddit.com/r/AI_Agents/comments/1w4ufna/working_on_an_idea_to_avoid_handing_agents/): a promotional thread illustrating access concerns, not an independent security assessment. It informed explicit local-storage and permission boundaries.
- [Google attachment documentation](https://support.google.com/mail/answer/6584): Gmail supports attachments; this client uses a smaller limit and does not implement Gmail's Drive-link fallback.
- [Google scope documentation](https://developers.google.com/workspace/gmail/api/auth/scopes): future API authorization should use read-only and send scopes. Reading remains mailbox-wide and restricted.
