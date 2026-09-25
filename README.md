# OpenChat

Native Android companion app for WhatsApp, package `com.piptechnologies.openchat`. Kotlin, Jetpack
Compose, Material 3, Hilt, Room, DataStore, Coil; one Gradle module; minSdk 24 (Android 7.0), targetSdk 35
(Android 15).

Free, no ads, no in-app purchases, no account or login, no backend, no analytics or crash reporting.
Everything the app keeps (numbers, messages, media copies, settings) lives in its private storage on the
phone. The only thing that uses the network is the WhatsApp Web session of the second-account tool; the app
makes no requests of its own (Coil, which bundles OkHttp, is used only for local files). Debug builds only:
nothing is published on Google Play and no release keystore exists.

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
Contact us screen that opens your email app. The UI ships in 19 languages (see [Languages](#languages)).

## Build

Requirements: JDK 17 and an Android SDK with platform 35. The Gradle wrapper (8.10.2) fetches the rest: AGP
8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01, Paparazzi 1.3.5.

```
./gradlew assembleDebug           # APK at app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest       # JVM unit tests (number normalisation, links, deletion detection, media planning,
                                  # translation completeness …)
./gradlew recordPaparazziDebug    # screenshots (34 English + 612 translated) into app/src/test/snapshots/images/
./gradlew verifyPaparazziDebug    # renders them again and compares with the images recorded above
python3 tools/i18n/check_translations.py   # translations against the English strings (or only some: ar iw)
```

Nothing under `app/src/test/snapshots/` is committed, so `verifyPaparazziDebug` compares with your own earlier
`recordPaparazziDebug`. See [Verification](#verification) for what each check covers.

Without an Android SDK, `tools/jvmcheck` (`cd tools/jvmcheck && gradle test`) compiles the `core` package
and runs its tests on a plain JVM. It pins a JDK 21 toolchain (the app build itself uses JDK 17).

Or take the APK from CI: every push to `main` runs [the CI workflow](.github/workflows/ci.yml) at
https://github.com/Crushguard/OpenChat/actions. The `build` job runs `assembleDebug` and the unit tests and
uploads the `app-debug` artifact (and `unit-test-reports`); the `screenshots` job renders the screenshots
(see below). Download `app-debug` from the latest green run on `main` and unzip it. A second workflow,
[Device](.github/workflows/device.yml), runs the emulator end-to-end suite (see [Verification](#verification)).

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
whenever they return to the foreground, the Messages, conversation and Deleted media screens re-check it on
every resume and hand back to the gate when it was revoked, and Settings › Notification access opens the same
gate.

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

## Languages

English plus 18 translations, the same set as the company's All Recovery and Status Saver apps. The list is
declared in `ui/settings/Languages.kt`, `res/xml/locales_config.xml` (the manifest's `android:localeConfig`,
so Android 13+ lists them under Settings › Apps › OpenChat › Language) and `resourceConfigurations` in
`app/build.gradle.kts`; `LocalesConfigTest` fails if they drift.

| Language | English name | Tag | Folder | Direction |
|---|---|---|---|---|
| English | English | en | `values` | LTR |
| Bahasa Indonesia | Indonesian | id | `values-in` | LTR |
| Português (Brasil) | Portuguese (Brazil) | pt-BR | `values-pt-rBR` | LTR |
| Português | Portuguese | pt | `values-pt` | LTR |
| اردو | Urdu | ur | `values-ur` | RTL |
| हिन्दी | Hindi | hi | `values-hi` | LTR |
| Türkçe | Turkish | tr | `values-tr` | LTR |
| Español | Spanish | es | `values-es` | LTR |
| العربية | Arabic | ar | `values-ar` | RTL |
| فارسی | Persian | fa | `values-fa` | RTL |
| پښتو | Pashto | ps | `values-ps` | RTL |
| עברית | Hebrew | he | `values-iw` | RTL |
| Français | French | fr | `values-fr` | LTR |
| Deutsch | German | de | `values-de` | LTR |
| Italiano | Italian | it | `values-it` | LTR |
| Русский | Russian | ru | `values-ru` | LTR |
| 中文 | Chinese (Simplified) | zh | `values-zh` | LTR |
| Hausa | Hausa | ha | `values-ha` | LTR |
| မြန်မာ | Burmese | my | `values-my` | LTR |

`values-pt` is European Portuguese. `resourceConfigurations` holds these 19 qualifiers plus `zh-rCN` and
`pt-rPT`, which OpenChat does not ship but AndroidX and Material do (their own strings, such as TalkBack's
"Selected", would be stripped otherwise). Every other library translation is dropped from the APK.

How the language is chosen:

- By default the app follows the phone's language list: the first entry that matches one of the 19, exactly
  or by language (pt-PT and pt-AO give Português, es-MX gives Español, en-GB gives English). A phone language
  outside the list gives English, and so does Traditional Chinese (zh-TW, zh-HK, zh-Hant), since `values-zh`
  is Simplified (`Languages.match`).
- Settings › Language sets the per-app locale through AppCompat (`setApplicationLocales`). The screen is
  recreated and the whole UI switches: text, plurals, dates and times, digits, country names in the picker,
  and a mirrored right-to-left layout for Arabic, Persian, Urdu, Pashto and Hebrew. Phone numbers and dial
  codes stay left to right. Formatting follows the language actually shown, so a phone in an unsupported
  language gets English dates as well (`ui/components/UiLocale.kt`).
- AppCompat stores the choice (itself below Android 13, the system from 13). The in-app list has no "phone
  language" row: after picking one, only Android 13+'s system setting can go back to the system default.
- Text you type, WhatsApp's content and the support email's app and Android version lines are not translated.

Caveats:

- The translations were machine-assisted, then reviewed per language group (European; Asian and Hausa;
  right-to-left and Turkish). A native-speaker pass is recommended, above all for Hausa, Burmese and Pashto.
- In Hausa, WhatsApp's menu names ("Linked devices › Link a device", "Delete for everyone") are unconfirmed
  guesses. Burmese keeps those names, and "blue ticks", in English because WhatsApp's Burmese wording could not
  be confirmed. Pashto keeps them in English because WhatsApp has no Pashto UI.
- The second-account button says "Show QR code" in every translation (the phone shows the code, the other
  phone scans it); the English keeps the design's "Scan QR".
- Digits and calendars come from Android's locale data: Arabic may show Arabic-Indic or Latin digits depending
  on the Android version, and Persian and Pashto dates use the Gregorian calendar.

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
  staying on "Linked" for more than about 10 s after unlinking from the other phone (three QR probes in a row,
  about 9 s, end the link; a "Linked" restored after a restart that the page no longer confirms is ended at the
  first probe).

### Language

- Needs: nothing beyond the app; a few recents and captured messages make dates and counts visible.
- Do: Settings › Language › العربية. Expect the screen to come back in Arabic with the toast for العربية, the
  layout mirrored (back arrow on the right, rows and chevrons flipped) on every screen, phone numbers still left
  to right, and Home's recents, the Messages list and Deleted media's day headings showing Arabic day and month
  names and counts. Pick Deutsch and check the same in German, then go back to English. On Android 13+,
  Settings › Apps › OpenChat › Language lists the same 19 languages and changes the app too.
- Failure looks like: English text left on a translated screen; an Arabic screen laid out left to right; a
  phone number reversed; English month names or "Yesterday" in another language; the check mark on a language
  other than the one shown.

## Screenshots

The branch https://github.com/Crushguard/OpenChat/tree/screenshots holds one PNG per screen and state under
`screenshots/<name>.png` (34 in English) and the same 34 in each of the 18 translations under
`screenshots/locales/<tag>/<name>.png` (612, tags as in [Languages](#languages)). Its index README has the
English gallery, a language table and a grid linking every screen in every language; each
`screenshots/locales/<tag>/README.md` is that language's gallery. The same set is the `screenshots` artifact
of each CI run. The inventory (file → composable and state) is §6 of [docs/design-map.md](docs/design-map.md).

They are Paparazzi screenshot tests (`app/src/test/java/com/piptechnologies/openchat/screenshots/`), rendered
without an emulator by `./gradlew recordPaparazziDebug` on a 1170×2532 px device config (390×844 dp at 3x,
the frame the design was drawn in). Paparazzi scales its output to 1000 px on the long side, so the files
are 462×1000. The translated set comes from `LocaleScreenshotTests`, which renders every scene of `Scenes.kt`
with a language's strings (right to left for the five RTL languages); it runs only under
`recordPaparazziDebug` and `verifyPaparazziDebug`, not under a plain `testDebugUnitTest`. The CI
`screenshots` job renames `<package>_<Class>ScreenshotTests_<method>.png` to `<method>.png` and
`<package>_LocaleScreenshotTests_everyScene[<tag>]_<scene>.png` to `locales/<tag>/<scene>.png`, fails unless
there are exactly 34 English files and the same 34 names in each of the 18 language folders, writes the
indexes and force-pushes the result to `screenshots` on every push to `main`, so that branch has no history
worth keeping.

Every screenshot uses fixed fake data (`Fakes.kt`: five conversations, four recents, a media grid, and a
clock pinned to 24 Sep 2026 21:13 UTC), never a real phone's content. Sheets and dialogs are drawn in a
static frame, the second-account WebView is replaced by a hatched placeholder, and the notification-access
states are forced.

## Verification

**Translation checker.** `python3 tools/i18n/check_translations.py [qualifier …]` compares each
`values-<qualifier>/strings*.xml` with the English files: every translatable key present and no extra or
non-translatable one, no empty value, the same format placeholders, every CLDR plural category the language
needs, apostrophes escaped and no unescaped leading `@` or `?`, links kept verbatim. Values identical to
English are reported as warnings. It exits with 1 on any error; with no argument it checks all 18.

**Unit tests.** `./gradlew testDebugUnitTest` includes `TranslationCompletenessTest` (the checker's errors,
one case per language) and `LocalesConfigTest` (`Languages.all`, `locales_config.xml`, the manifest's
`android:localeConfig`, `resourceConfigurations` and the `values-*` folders agree; the RTL set; how phone
locales map to a language). The CI `build` job runs them on every push to `main`, so a missing or broken
translation fails the build; reports are in the `unit-test-reports` artifact.

**Screenshots.** Paparazzi renders the 34 English screens and the 612 translated ones as described above;
`./gradlew recordPaparazziDebug` locally, then `verifyPaparazziDebug` after a change. CI publishes them to the
`screenshots` branch.

**Emulator suite.** [The Device workflow](.github/workflows/device.yml) runs the instrumented tests in
`app/src/androidTest/java/com/piptechnologies/openchat/e2e/` on emulators at API 30 and 34, on every push to
`main`. The real app is driven against `e2e-fixture`, a CI-only stand-in with the package name `com.whatsapp`
that posts WhatsApp-like chat and system notifications, writes and deletes files in WhatsApp's media folder
and receives the chat links. Each test starts from cleared app data. Covered: first run and onboarding;
Send with a pasted number (the `wa.me` link, the not-on-WhatsApp sheet, deleting a recent); the
notification-access gate and system settings; captured messages, unread counts, the conversation, a deleted
message kept under "Deleted by sender" and a system notification ignored; a deleted photo recovered, saved
and deleted; the second-account WebView opening in "Linking…"; the default-app sheet, clearing recents, the
rating sheet and Contact us; and every one of the 19 languages picked through the Language screen, with the
title translated, the back arrow on the right for RTL, and Settings, Language, Home, Messages, Deleted media,
the gate and Second account captured. Artifacts per API level: `device-screens-api<N>` (folders
`api<N>/<tag>/` for the language sweep, `api<N>/flows/` for the tool flows, `api<N>/failures/` for the screen
when a test failed), `device-test-reports-api<N>` and `device-logcat-api<N>`. Locally, on an emulator
without the real WhatsApp (the stand-in takes its package name):

```
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :e2e-fixture:assembleDebug
adb install -r -g e2e-fixture/build/outputs/apk/debug/e2e-fixture-debug.apk
./gradlew :app:connectedDebugAndroidTest
adb pull /sdcard/Download/openchat-e2e e2e-screens
```

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
- Language (R13): 19 languages ship, English and 18 translations (see [Languages](#languages)); the design's
  isiZulu and "Español (México)" rows are replaced by that set. The choice is the per-app locale set through
  AppCompat (`setApplicationLocales`, stored by `AppLocalesMetadataHolderService` below Android 13), which works
  on every supported API level; without one the app follows the phone's language, else English. The number
  field uses the phone keyboard, not the design's mock keypad (R14).

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
  already on screen when the listener connects are not re-read.
- A deletion is detected only when WhatsApp posts the placeholder. The placeholder is matched against the
  conversation's window: the messages captured since the chat was last opened in WhatsApp (or its
  notification otherwise withdrawn by WhatsApp), at most the latest 25. A notification you swipe away keeps
  its window. Timestamped placeholders match within ±2 s; untimestamped ones (plain-text notifications) match
  by position. Consequences: a message deleted after you opened the chat in WhatsApp is not marked, and two
  identical texts posted in the same second are stored once.
- The "This message was deleted" placeholder is recognised in WhatsApp's wording for 17 of the app's
  languages (31 wordings in `core/messages/WhatsAppStrings.kt`, old and new, since the WhatsApp versions in use
  vary; the Hausa one is an unverified guess). Pashto and Burmese need none: WhatsApp has no UI in them and
  shows English. The "deleted by admin" variants, with the admin's name, are known in 16 languages (not
  Hausa). Every language is checked on every notification, whatever the phone's or the app's language; a
  WhatsApp interface in a language outside the list is not detected.
- Group summaries and system notifications (calls, backup and restore, progress, "Checking for new
  messages") are ignored. They are recognised by their category and flags, which works in any language
  (`NotificationText.isSystemNotification`); status and summary lines of plain-text notifications are also
  matched against WhatsApp's wording in 16 languages.
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
- "Linked" is decided by probing the page every 3 s for WhatsApp Web's chat list. Once linked, one or two QR
  results in a row are treated as transient (the page reloading); the third ends the link, stops the service
  and clears the stored state. A link restored after a restart that the first probe cannot confirm is ended at
  once. The foreground service is not sticky: if Android kills the process the notification disappears;
  reopening the screen probes at once and restarts it.
- If the WebView's renderer is killed the app survives; the view is rebuilt and re-attached within one probe
  interval (3 s) while linking or linked.

Rating

- The rating sheet opens from Settings › Rate us and once, 1.8 s after you return to Home from the chat that
  the third successful send opened (never again after that). 4–5 stars lead to the Play link, 1–3 to the
  feedback form, which sends by email like Contact us.

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
