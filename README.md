# OpenChat

Native Android companion app for WhatsApp, package `com.piptechnologies.openchat`. Kotlin, Jetpack
Compose, Material 3, Hilt, Room, DataStore, Coil; one Gradle module; minSdk 24 (Android 7.0), targetSdk 35
(Android 15).

Free, no ads, no in-app purchases, no account or login, no backend, no analytics or crash reporting.
Everything the app keeps (numbers, messages, media copies, settings) lives in its private storage on the
phone. The only thing that uses the network is the WhatsApp Web session of the second-account tool; the app
itself makes no requests and has no network library. Debug builds only: nothing is published on Google Play
and no release keystore exists.

The product spec is [docs/brief.md](docs/brief.md); the design map with the rulings is
[docs/design-map.md](docs/design-map.md); a copy of the design prototype is under `docs/design/`.

## What it is

Five tools behind one Home screen.

**Open a chat with any number.** Pick a country (detected from the SIM, then the network, then the locale;
flags are emoji), type or paste a number, add an optional message and tap Send. The Paste pill (and text
pasted with the keyboard) keeps digits only, drops spaces, dashes, dots, parentheses, a leading `+` or `00`
and the trunk `0`, and switches the country when the text starts with a dial code (`+62 812-3456-7890` →
Indonesia, `812 3456 7890`). The right segment of the Send button picks the app: WhatsApp, WhatsApp Business
or Telegram; only installed apps are listed and the choice is remembered. Send opens
`https://wa.me/<number>?text=…` in WhatsApp or WhatsApp Business, or `tg://resolve?phone=<number>&text=…` in
Telegram; when the chosen app is missing, the web link (`wa.me`, `t.me/+<number>`) opens in the browser. The
last five numbers stay as Recent rows: tap to refill, long-press or swipe to delete. Nothing is saved to your
contacts.

**Read without blue ticks (Unseen).** Once Notification Access is granted, a notification listener reads
WhatsApp and WhatsApp Business chat notifications and stores every message in the app's own database. The
Messages screen shows them as conversations you can read without opening WhatsApp, so no read receipt is
sent; "seen" is recorded locally only. The app never cancels or otherwise touches WhatsApp's notifications.

**Recover deleted messages.** The same listener notices when a notification line turns into WhatsApp's
"This message was deleted" placeholder and marks the stored original. The "Deleted only" filter of the
Messages screen lists those conversations, and the conversation view labels each message "Deleted by
sender". Pause/resume, exclude chats and clear all live in the tool-settings sheet (sliders icon).

**Recover deleted media.** With the media/storage permission, a watcher (a FileObserver plus a MediaStore
observer, also woken by every WhatsApp notification) copies new files from WhatsApp's media folders into the
app's private storage and keeps the copy when the original disappears. Images embedded in notifications are
copied too. The Deleted media screen has Photos, Videos, Audio, Documents and Stickers tabs and an item
detail with Save to gallery, Share and Delete.

**Second account.** WhatsApp Web inside a WebView with a desktop user agent, linked by scanning its QR code
from the other phone. Cookies and web storage persist, a foreground service with a "Second account linked"
notification keeps the session alive while linked, and the top bar has back, reload and log out.

Around them: a launch screen, a two-slide onboarding on the first run, Settings (notification access,
default app, language, clear recents, rate, contact, share, privacy policy, version), a rating sheet, and a
Contact us screen that opens your email app.

## Build

Requirements: JDK 17 and an Android SDK with platform 35. The Gradle wrapper (8.10.2) fetches the rest: AGP
8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01, Paparazzi 1.3.5.

```
./gradlew assembleDebug           # APK at app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest       # JVM unit tests (number normalisation, links, deletion detection, media planning …)
./gradlew recordPaparazziDebug    # screenshots into app/src/test/snapshots/images/
```

Without an Android SDK, `tools/jvmcheck` (`cd tools/jvmcheck && gradle test`) compiles the `core` package
and runs its tests on a plain JVM.

Or take the APK from CI: every push to `main` runs [the CI workflow](.github/workflows/ci.yml) at
https://github.com/Crushguard/OpenChat/actions. The `build` job runs `assembleDebug` and the unit tests and
uploads the `app-debug` artifact (and `unit-test-reports`); the `screenshots` job renders the screenshots
(see below). Download `app-debug` from the latest green run on `main` and unzip it.

Only the debug build type is set up. The release build type has no signing config and no keystore, on
purpose.

## Install

```
adb install -r app-debug.apk
```

Or copy the APK to the phone and open it; Android asks you to allow installs from that source (Settings ›
Apps › Special app access › Install unknown apps; wording varies). Runs on Android 7.0 (API 24) up to
Android 15 (API 35).

Debug APKs are signed with the debug key of the machine that built them, and each CI run signs with a fresh
key. Installing a newer CI build over an older one therefore fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`;
run `adb uninstall com.piptechnologies.openchat` first. Uninstalling deletes the app's data, recovered
messages and media included.

## Permissions

**Notification Access**, the one grant for the messages and media tools. The Unseen, deleted-messages and
deleted-media tools read WhatsApp's notifications; until the grant exists they show a gate screen. Tap its
"Open settings" button: on Android 11+ it opens OpenChat's own notification-access page, on older versions
(or when that page cannot be opened) the list of apps. By hand: Settings › Notifications › Device & app
notification access › OpenChat › Allow; some makers call it "Notification access" or file it under Apps ›
Special app access. The gate re-checks the grant when you come back to it, Home and Settings re-check it
whenever they return to the foreground, and Settings › Notification access opens the same gate.

For capture to work, WhatsApp must be allowed to post notifications and the chat must not be muted. WhatsApp
posts no notification for the chat that is open on screen, so nothing is captured for it while you are in
it. Some manufacturers' battery optimisation stops notification listeners in the background; exclude
OpenChat from it (Settings › Apps › OpenChat › Battery) if capture stops after a while.

**Media / storage**, deleted-media tool only. Asked inline: the Deleted media screen shows an "Allow media
access" button while it is missing. Android 13+: `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`,
`READ_MEDIA_AUDIO`; Android 10–12: `READ_EXTERNAL_STORAGE` (with `requestLegacyExternalStorage` on Android
10); Android 7–9: `READ_EXTERNAL_STORAGE` and `WRITE_EXTERNAL_STORAGE`. Save to gallery writes through
MediaStore on Android 10+ and into the public folders below. If you denied the permission permanently the
button does nothing visible; grant it under Settings › Apps › OpenChat › Permissions.

**Notifications** (`POST_NOTIFICATIONS`, Android 13+). Asked when you tap "Scan QR" on the Second account
screen. It only decides whether the "Second account linked" notification is visible; the foreground service
runs either way.

`INTERNET` is declared for the WhatsApp Web WebView and for opening links; nothing else uses it.

## Testing each tool by hand

In every case: WhatsApp (and WhatsApp Business or Telegram if you want to test them) installed and logged in
on the test phone, plus a second phone with a WhatsApp account that can message the test phone's number.

### Open a chat with any number

- Needs: a number that is on WhatsApp and one that is not.
- Do: paste `+62 812-3456-7890` with the Paste pill. Expect the country chip to switch to Indonesia, the
  field to read `812 3456 7890` and the toast "Pasted · country set to Indonesia". Type a message, tap Send.
  Expect the toast "Opening WhatsApp…", WhatsApp opening the chat with the text prefilled, and, back in
  OpenChat, the number at the top of Recent with the app's glyph. Open the app selector (right segment of
  Send) and check that only installed apps are listed and the tick follows your last choice.
- Not on WhatsApp: send to a number without an account. WhatsApp shows its own message and returns you to
  OpenChat; when that happens within 7 s, OpenChat shows the "This number is not on WhatsApp" sheet with
  "Edit number" and "Try Telegram". "Try Telegram" sends the same number and message to Telegram (or opens
  `t.me` in the browser when Telegram is not installed) and records the recent as Telegram. Coming back to
  OpenChat within 7 s after a normal send shows the sheet too; that is the heuristic's known false positive
  (see below).
- Failure looks like: Send enabled with no digits; a wrong country after paste; nothing opening (toast "No
  app can open this number") although WhatsApp is installed; the recent missing after a successful send;
  Telegram not opening the number's chat.

### Read without blue ticks

- Needs: Notification Access granted; the chat closed in WhatsApp on the test phone.
- Do: from the second phone send two or three messages. Do not open WhatsApp. Open OpenChat › Read without
  blue ticks. Expect the conversation (contact name, or the number for unsaved senders) with an unread badge
  and the last message as preview, the Home row reading "N unread, seen by no one", and every message in
  the conversation view. On the second phone the ticks stay grey (delivered); they turn blue only once the
  chat is opened in WhatsApp, for example with the conversation's "Open chat in WhatsApp" button.
- Failure looks like: nothing listed (check the grant, that WhatsApp notifications are on and the chat is
  not muted, that recovery is not paused and the chat is not excluded); blue ticks on the second phone
  without WhatsApp having been opened. Messages that arrived while the chat was open in WhatsApp are
  missing by design: no notification was posted for them.

### Recover deleted messages

- Needs: the same, plus the second phone deleting a message "for everyone" within WhatsApp's time limit.
- Do: from the second phone send a message, wait for the notification on the test phone, then on the second
  phone long-press the message › Delete › Delete for everyone, all without opening the chat in WhatsApp on
  the test phone. Expect the WhatsApp notification to turn into "This message was deleted", OpenChat ›
  Recover deleted messages (the "Deleted only" filter) to list the conversation with the amber dashed icon
  and the original text, the conversation view to show the message under the "DELETED BY SENDER" label, and
  the Home row to read "N messages the sender deleted". A photo works the same way; when the notification
  carried the image, the bubble shows it and opens the copied file.
- Failure looks like: the message present but never marked (the chat was opened in WhatsApp before the
  deletion, so the placeholder never reached a notification, or WhatsApp's language is not one the app
  knows, see below); a different message marked (the notification carried no per-message times, so lines
  were matched by position).

### Recover deleted media

- Needs: Notification Access (the watcher runs with the listener), "Allow media access" granted on the
  Deleted media screen, WhatsApp's auto-download on for photos, and the second phone.
- Do: from the second phone send a photo and wait until it is downloaded on the test phone (the file lands in
  WhatsApp Images). Give the watcher a few seconds: it copies 1.5–10 s after the file appears, after the next
  WhatsApp notification, or when the Deleted media screen is opened. Then delete the photo for everyone from
  the second phone; WhatsApp removes the file. Expect Deleted media › Photos to show the item under "Today ·
  N" with its time chip. The detail shows the preview and "From <sender> · Today HH:MM · Deleted HH:MM" (or
  "Received …" when the copy came from the folder watcher rather than a notification); Save to gallery puts a
  copy in Pictures/OpenChat (toast "Saved to gallery"), Share opens the share sheet, Delete asks for
  confirmation and removes the copy. Videos save to Movies/OpenChat, audio to Music/OpenChat, documents to
  Download/OpenChat.
- Failure looks like: nothing listed while the original still exists in WhatsApp (by design: only files whose
  original disappeared are shown); nothing after the deletion (the file was deleted before a copy pass ran,
  the permission is missing, the media folder is not one of the watched roots, or the file sat in a skipped
  folder, see below); a document not recovered on Android 11+ (best effort only).

### Second account

- Needs: internet on the test phone; the second phone with the account to link.
- Do: Open a second account › Scan QR (Android 13+ asks for the notification permission first). Expect
  WhatsApp Web's desktop page with a QR code and "Linking…" in the bar. On the second phone: WhatsApp › ⋮ ›
  Linked devices › Link a device, scan the code. Expect the chip "Linked", the toast "Linked", the "Second
  account linked" notification, and the Home row reading "Linked". Leave the screen and come back: the chats
  are still there, without a reload. Reload reloads the page (toast "Reloading…"). Log out › Log out clears
  cookies and web storage, ends the service and returns to the entry screen (toast "Logged out"); the second
  phone keeps working.
- Failure looks like: no QR code after a minute (no internet, or WhatsApp Web refusing the user agent); the
  page reloading from scratch when you return to the screen; the notification staying after logout; the chip
  stuck on "Linked" after unlinking from the other phone (known: a QR code shown after a confirmed link is
  treated as transient until you Reload or Log out).

## Screenshots

The branch https://github.com/Crushguard/OpenChat/tree/screenshots holds one PNG per screen and state under
`screenshots/<name>.png` with an index README; the same set is the `screenshots` artifact of each CI run. The
inventory (file → composable and state) is §6 of [docs/design-map.md](docs/design-map.md).

They are Paparazzi screenshot tests (`app/src/test/java/com/piptechnologies/openchat/screenshots/`), rendered
without an emulator by `./gradlew recordPaparazziDebug` on a 1170×2532 px device config (390×844 dp at 3x,
the frame the design was drawn in). Paparazzi scales its output to 1000 px on the long side, so the files
are 462×1000. The CI `screenshots` job renames `<package>_<Class>ScreenshotTests_<method>.png` to
`<method>.png`, writes the index and force-pushes the result to `screenshots` on every push to `main`, so
that branch has no history worth keeping.

Every screenshot uses fixed fake data (`Fakes.kt`: five conversations, four recents, a media grid, and a
clock pinned to 24 Sep 2026 21:13 UTC), never a real phone's content. Sheets and dialogs are drawn in a
static frame, the second-account WebView is replaced by a hatched placeholder, and the notification-access
states are forced.

## Design decisions and known limitations

The rulings where the brief and the design disagreed (R1–R17 in [docs/design-map.md](docs/design-map.md))
and the main implementation decisions, in plain words.

General

- The design's brand text "Direct Chat" renders as **OpenChat** everywhere (R1). The ad banner slot and the
  "More apps" row are not rendered (R2, R3). The launch, onboarding, language, contact and rating screens the
  design shows are implemented (R4).
- Flags are emoji (R8); Hanken Grotesk, JetBrains Mono and the Lucide icons are bundled (R9). No WhatsApp or
  Telegram logos ship: the app selector shows the installed apps' own launcher icons, other places use neutral
  line glyphs (R10).
- Contact us and rating feedback open the system email composer (`mailto:` to the support address in
  `strings.xml`, body = your text plus the app and Android versions) because there is no backend (R11); with
  no email app installed they say so and send nothing. "Rate on Google Play" opens
  `market://details?id=com.piptechnologies.openchat` (R12); as the app is not on Play, the store reports it
  missing. The privacy-policy URL and the support email in `strings.xml` are placeholders (R16).
- Language sets the per-app locale through AppCompat (`setApplicationLocales`, stored by
  `AppLocalesMetadataHolderService`), which works on every supported API level, and persists the choice. Only
  English strings ship, so the other seven languages fall back to English while the row shows the choice
  (R13). The number field uses the phone keyboard, not the design's mock keypad (R14).

Open a chat

- **Not-on-WhatsApp heuristic (R6).** WhatsApp never reports back. If OpenChat resumes within 7 s (a
  half-open window: 0 ≤ elapsed < 7000 ms) of launching WhatsApp or WhatsApp Business for a send, the sheet is
  shown. Returning quickly for any other reason triggers it too; the browser fallback and Telegram never do.
- Telegram receives the number and the message as `tg://resolve?phone=<digits>&text=<message>`; the browser
  fallback `https://t.me/+<digits>` carries no text. Whether Telegram prefills the text depends on the
  installed Telegram version.
- When none of the three apps is installed, all three are listed and Send opens the web link. The stored
  choice is used only while that app is installed; otherwise WhatsApp, else the first installed app.

Messages (Unseen, deleted)

- Unseen and Recover deleted messages are one screen with "All" and "Deleted only" filters, reached from the
  two Home rows (R5).
- Only what arrives as a notification after the grant is captured: no import of history, and notifications
  already on screen when the listener connects are not re-read. Group summaries, progress, call and "checking
  for new messages" notifications are ignored.
- A deletion is detected only when WhatsApp posts the placeholder. The placeholder is matched against the
  conversation's window: the messages captured since the chat was last opened in WhatsApp (or its
  notification otherwise withdrawn by WhatsApp), at most the latest 25. A notification you swipe away keeps
  its window. Timestamped placeholders match within ±2 s; untimestamped ones (plain-text notifications) match
  by position. Consequences: a message deleted after you opened the chat in WhatsApp is not marked, and two
  identical texts posted in the same second are stored once.
- Placeholders are recognised in English, Indonesian, Portuguese, Spanish, Turkish, Hindi and Urdu (plus
  "deleted by admin"); a WhatsApp interface in another language is not detected.
- Excluded chats (R15) are skipped by the listener from the moment they are excluded; messages captured
  earlier stay. Pause stops both message and media capture.
- Photos arrive as "📷 Photo" lines, with the image copied when the notification carries it. Voice notes,
  documents and stickers show by name only.

Media

- The grid lists only copies whose original file is gone (R7). Copies of files that still exist are kept
  privately for up to 14 days and then dropped, and a file already older than 14 days when first seen is never
  copied, so a deletion more than 14 days after receipt is not recoverable.
- Watched roots: `Android/media/com.whatsapp/WhatsApp/Media`, `WhatsApp/Media` and the WhatsApp Business
  equivalents, two folder levels deep. Skipped: `.Statuses`, `Sent`, `.Shared`, `.Links`, `.Thumbs`, `.trash`,
  `.Private`, "WhatsApp Profile Photos", "WallPaper", any folder named "Backup Excluded", and dot-files.
  Statuses and your own sent media are therefore never recovered.
- Copy passes are debounced (1.5 s after the last event, at most 10 s after the first), skip files modified
  in the last second, discard a copy whose original changed while it was read, keep a 32 MB free-space
  margin, and mark a copy as deleted only when a complete folder listing plus a direct check prove the
  original is gone. On Android 10+ the folder walk is merged with MediaStore rows; documents on Android 11+
  are best effort. Exclusions do not apply to folder-based media, which carry no chat information.
- The media permission is asked inline on the Deleted media screen; a permanent denial has no shortcut to the
  system settings yet (R17).

Second account

- The WebView lives for the whole process on the application context, so leaving the screen or rotating never
  reloads the session. Trade-off: there is no file chooser, so attaching files from the second account is not
  possible, and some drop-down dialogs may not open. Downloads from the page are not handled; links to other
  sites open in the phone's browser.
- "Linked" is decided by probing the page every 3 s for WhatsApp Web's chat list. Once linked, a QR code is
  treated as transient, so an unlink made on the other phone is only noticed after Reload or Log out. The
  foreground service is not sticky: if Android kills the process the notification disappears; reopening the
  screen probes again and restarts it.
- If the WebView's renderer is killed the app survives; the screen may stay blank until you leave and return.

Rating

- The rating sheet opens from Settings › Rate us and once after the third successful send. 4–5 stars lead to
  the Play link, 1–3 to the feedback form, which sends by email like Contact us.

## Licenses

- Hanken Grotesk: SIL Open Font License 1.1,
  [docs/licenses/OFL-HankenGrotesk.txt](docs/licenses/OFL-HankenGrotesk.txt). Font files are in
  `app/src/main/res/font/`.
- JetBrains Mono: SIL Open Font License 1.1,
  [docs/licenses/OFL-JetBrainsMono.txt](docs/licenses/OFL-JetBrainsMono.txt).
- Lucide icons: ISC, [docs/licenses/LICENSE-lucide.txt](docs/licenses/LICENSE-lucide.txt);
  `ui/icons/LucideIcons.kt` is generated from the SVGs by `tools/lucide_gen.py`.
- Country names and dial codes: `core/phone/DialCountries.kt` is generated from
  [mledoze/countries](https://github.com/mledoze/countries) (ODbL). The attribution is the header of that file;
  the repository carries no copy of the ODbL text.

This repository has no license file of its own yet.
