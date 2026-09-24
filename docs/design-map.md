# OpenChat — design map (the spec)

Source of truth: the Claude Design project "Direct Chat" (share link in the brief), three pages —
`Design System.dc.html` (tokens + components), `Screens.dc.html` (25 frames), `Prototype.dc.html`
(the interactive prototype; a copy is committed at `docs/design/Prototype.dc.html`). This document
maps every token, component, screen and state of that design onto the Android implementation, and
records the rulings where the brief and the design disagree. The brief is the product spec; the design
is the visual spec. Where this file says "design says", the value is copied verbatim.

## 0. Rulings (brief vs design)

| # | Topic | Ruling |
|---|---|---|
| R1 | App name | The design's brand text is "Direct Chat". The brief names the product **OpenChat** (package `com.piptechnologies.openchat`). Every place the design shows "Direct Chat" renders **"OpenChat"** (brand bar, splash, onboarding, settings callout, rating sheet). |
| R2 | Ad banner slot | The design reserves a dashed "AD BANNER SLOT · 320×50" at the bottom of tool screens. The brief says no ads. **Not rendered**; the screens keep the 14 dp bottom padding instead. |
| R3 | "More apps (AD)" settings row | Cross-promotion is an ad. **Omitted.** |
| R4 | Screens beyond the brief's list | The brief says "implement every state the design shows" and "no onboarding carousel unless the design shows it". The design shows Launch, Onboarding (2 slides), Language, Contact us and the Rating sheet, so they are **implemented**. |
| R5 | Unseen vs Recover Messages | The brief lists them as two tools; the design merges them into one "Messages" screen with the filter chips **All** / **Deleted only**. One screen and one ViewModel with a `mode` argument, reached from two Home rows (Read without blue ticks → All, Recover deleted messages → Deleted only). Both routes exist. |
| R6 | "Not on WhatsApp" detection | WhatsApp never reports back to the caller. Heuristic: if the app resumes within 7 s of launching WhatsApp / WhatsApp Business for a send, the dialog is shown. Documented in the README. |
| R7 | Media grid contents | "Recover deleted media" shows items whose original file disappeared after we copied it. Copies of files that still exist are kept privately (so they can be recovered later) but not listed. |
| R8 | Flags | The design loads PNG flags from flagcdn.com. Flags render as **emoji** (regional indicator pairs), 26×18 slot in the chip, 30×20 in the picker rows. |
| R9 | Fonts / icons | Hanken Grotesk and JetBrains Mono are bundled (OFL). Lucide icons are bundled as generated `ImageVector`s at stroke 1.75 (the design's default) — see `ui/icons`. |
| R10 | Third-party marks | No WhatsApp/Telegram logos in resources. The design's line glyphs (phone-in-bubble, B-in-bubble, paper plane) are drawn in our colours; where an app is installed, its own launcher icon from PackageManager is shown in the selector (design system note). |
| R11 | Contact us / feedback "Send" | No backend. Send opens the system email composer (`mailto:` to `support_email` in strings.xml, subject "OpenChat feedback", body = text + app/Android version). Rating feedback does the same. |
| R12 | Rate on Google Play | Opens `market://details?id=<package>` (falls back to the Play web URL). Never publishes anything. |
| R13 | Language | The Language screen sets the per-app locale (AppCompat `setApplicationLocales`) and persists it. Only English strings ship; other locales fall back to English. The row still shows the chosen language. |
| R14 | Keyboard | The design's numeric keypad is a mock of the system IME. The number field uses the phone keyboard (`KeyboardType.Phone`). Paste is the inline pill (and the system clipboard suggestion). |
| R15 | Exclude chats | Tool settings row "Exclude chats" opens a sheet listing known conversations with a switch each; excluded conversations are skipped by the listener. Count shown on the row. |
| R16 | Privacy policy | Opens `privacy_policy_url` from strings.xml (placeholder https://piptechnologies.com/openchat/privacy). |
| R17 | Media permission | Notification Access is the one gate for tools 2, 3 and 4 (as the brief says). Copying media additionally needs the storage/media runtime permission; the Deleted media screen asks for it inline (empty state button "Allow media access") the first time. |

## 1. Tokens

Colours (light only, no dark mode). Code: `ui/theme/Color.kt` (`OcColors`, `OcTheme.colors`).

| Token | Hex | Use |
|---|---|---|
| canvas | #FAFBFC | screen background |
| surface | #FFFFFF | cards, fields, sheets |
| subtle | #F3F5F8 | chips, secondary buttons, icon boxes |
| subtle2 | #F6F7F9 | info callouts |
| ink | #1E2128 | primary text |
| inkStrong | #171A20 | display titles |
| ink2 | #626873 | secondary text |
| inkSoft | #3D4550 | top-bar icons, menu glyphs |
| inkMuted | #565C67 | body on gate/entry, row glyphs |
| muted | #8B929D | meta, eyebrows, timestamps |
| hint | #99A0AC | one-line hints, honesty line |
| placeholder | #A2A9B4 | field placeholders |
| chevron | #B4BAC4 | row chevrons, empty-state icons |
| border | #E7EAEF | 1 px borders |
| borderSoft | #EEF0F4 | card borders |
| borderStrong | #E1E5EB | sheet handle |
| calloutBorder | #EBEEF2 | info callout border |
| hairline | #F2F4F7 | row dividers |
| switchOff | #D8DCE3 | switch off, empty stars |
| green | #0A7E3A | primary, active |
| greenTint / greenTintBorder | #DDFAE4 / #ACE8BE | selected chips, split trailing segment |
| greenSoft | #4CB98A | toast check |
| successTint / successFg | #E2F3EA / #2E9E6B | rating thank-you box |
| amber | #B8792A | attention: off, paused, deleted-by-sender |
| amberTint / amberTintBorder | #FEFBF5 / #F0DFC4 | Off chip, not-on-WhatsApp icon box |
| destructive | #C4553D | delete, clear, log out |
| destructiveTint / destructiveBorder | #FBEFEC / #EEDAD3 | destructive icon boxes, swipe reveal |
| gold | #E0A64B | rating stars |
| scrim | rgba(20,22,28,.42) | behind sheets/dialogs |
| tool tints | ticks #D4F1D8/#1C8742 · messages #D2EBFF/#1F74BF · media #ECE2FF/#7F5BB6 · second #C6F2F4/#008892 · orange #FFDFD0/#B2511E | icon boxes, avatars |

Type. Hanken Grotesk 400/500/600/700/800, JetBrains Mono 400/500/600/700. Design px = sp 1:1.
Code: `ui/theme/Type.kt` (`OcTypography`, `OcTheme.type`). Key styles: display26 (onboarding), display24
(splash), display22 (gate/entry, −.02em), title19 (brand bar, 800), title18 (dialog), title17 (settings
bar, empty-state title), title16 (top bars), title15 (sheet titles), label16 (primary button), label15
(dialog buttons), label14_5 (row titles, secondary button), label13 (chips), label12 (small chips),
body15, body14_5 (bubbles), body13_5, body13, body12_5, body12, body11_5 (hints), mono22 (number),
mono16 (dial chip), mono14_5 (recent number), mono14 (picker dial), mono12 (version, counter),
mono11_5 (recent time, conversation subtitle), mono11 (row time), mono10_5 (bubble time), eyebrow11
(section labels, 700, .08em, uppercase), eyebrow10 ("Send with"), eyebrow9_5 ("Deleted by sender", date
pill uses mono10 600 .06em), badge10_5.

Radii: 6 (split inner) · 10 (chips/menu rows) · 12 (icon buttons, search, tool icon box) · 13
(secondary/dialog buttons) · 14 (fields, primary button, callouts, tiles) · 16 (cards) · 18 (settings
access card) · 20 (gate icon box, media preview) · 24 (bottom sheet top) · 26 (confirmation sheet) ·
pill. Code: `ui/theme/Shape.kt` (`OcRadius`).

Spacing: 4-based. Screen horizontal padding 20 (settings 18). Section gap 20. Card row padding 12 14
(settings rows 15). Icons Lucide at stroke 1.75 unless noted.

## 2. Components (code in `ui/components`)

| Component | Spec | Code |
|---|---|---|
| Primary button | 52 / 14, green, white 16/600; disabled = whole button at 45 % opacity; optional leading icon, 8 gap | `PrimaryButton` |
| Secondary button | 48 / 13, subtle bg, 1 px border, ink 14.5/600 | `SecondaryButton` |
| Ghost | text only ink2 14/600, 42 high (46 in sheets) | `GhostButton` |
| Destructive outline | 42 / 11, destructive border + text | `DestructiveOutlineButton` |
| Dialog buttons | two equal 50 / 13: cancel subtle+border ink 15/600, confirm coloured white | `DialogButtonRow` |
| Icon square | 52 / 14, secondary (subtle) or destructive (white + destructive border), icon 20 | `IconSquareButton` |
| Top-bar icon button | 40 / 12, icon 20–22 | `TopBarIconButton` |
| Top bar | 52 (56 with subtitle), padding 0 12, back arrow-left 22 ink, title 16/700 | `OcTopBar` |
| State chip | Off (amber dot 8) / Active (check 16 sw2.4) at 32, 13/600; small 28 variant 12/600 with Paused (pause 12) and Linked (dot 7) | `StateChip` |
| Filter pill | 36, padding 0 15, 13/600; selected greenTint + green border + green text | `FilterPill` |
| Count badge | 20 pill green, white 10.5/700 | `CountBadge` |
| Section eyebrow | mono 11/700 .08em uppercase muted | `SectionEyebrow` |
| Card | white, 16 radius, 1 px borderSoft, hairline dividers | `CardColumn`, `HairlineDivider` |
| Icon box | tinted rounded square (40/12 tool rows, 44/13 dialogs, 46/14 settings, 64/20 gate & entry) | `IconBox` |
| Info callout | subtle2 bg, calloutBorder, 14 radius, info 14 muted + 12/1.5 ink2 | `InfoCallout` |
| Honesty callout | greenTint, 14 radius, heart-handshake 16 green + 12.5 inkMuted | `HonestyCallout` |
| Honesty line | heart-handshake 12 + 12/400 hint centred | `HonestyLine` |
| Hint line | 11.5 hint centred | `HintLine` |
| Empty state | icon box 56/16 subtle, icon 28 chevron colour, title 17/700, body 13.5 muted 240 wide | `EmptyState` |
| Switch | 48×28 pill, green/switchOff, 22 white knob | `OcSwitch` |
| Brand mark | green rounded square + send-horizontal (sw 2) white | `BrandMark` |
| Bottom sheet | M3 `ModalBottomSheet`, 24 top radius, handle 36×4 borderStrong; content composables are stateless so screenshots render them in a fake sheet frame | task-level |
| Confirmation sheet | bottom-anchored, 26 radius, padding 24 22 22, icon box 44/13, title 18/700, body 13.5 ink2, `DialogButtonRow` | `ConfirmSheet` (task) |
| Dark toast | Snackbar-style: ink bg, white 13.5/600, 12 16 padding, 13 radius, check 16 sw2.4 greenSoft, 90 dp from bottom, 2.2 s | `DarkToastHost` (task) |

## 3. Navigation

Single activity, Navigation Compose. Routes:

`splash` → (first run) `onboarding` → `home`; else `home`.
`home` → `country` (sheet, on Home), `sendWith` (dropdown, on Home), `notOnWhatsApp` (confirmation sheet, on Home)
`home` → `gate?tool={messagesUnseen|messagesDeleted|media}` → target tool (or Continue when already granted)
`home` → `messages?mode=all` / `messages?mode=deleted` → `conversation/{conversationId}?mode=` → `mediaDetail/{mediaId}` (from a deleted photo bubble)
`home` → `media` → `mediaDetail/{mediaId}`
`home` → `second` (entry or linked, one route; the ViewModel decides)
`home` → `settings` → `language`, `contact`, `gate?tool=settings`

Back always pops. The gate is skipped when access is already granted. Tool screens re-check access on
resume and show the gate if it was revoked.

## 4. Screens and states

Every frame is 390×844 dp. Status bar is the system's. "Bar" = `OcTopBar` unless noted.

### 4.1 Launch (`splash`) — screenshot `splash`
Centered: brand mark 88 / 26 with glyph 40; "OpenChat" display24 inkStrong, 20 below; a 22 dp green
spinner 14 below (M3 `CircularProgressIndicator`, stroke 2). Bottom: honesty line "Free · No ads · No account",
34 dp above the bottom. Advances after 1.2 s to onboarding (first run) or home.

### 4.2 Onboarding (`onboarding`) — screenshots `onboarding_1`, `onboarding_2`
Padding 4 24 26. Top row 40 with ghost "Skip" (13.5/600 ink2) right-aligned, slide 1 only.
Centre column (gap 26):
* Slide 1 visual (342 wide, gap 10): the phone field (60/14, white, border, shadow) with the country chip
  (flag + "+62" + chevron) and "812 3456 7890" mono22; the split button "Send Message" with the WhatsApp
  glyph; check line (check 16 sw2.4 green + "Not added to your contacts" 12.5 ink2). Title display26
  "Chat with any number"; body body15 inkMuted "Type or paste a number, tap Send. It opens in WhatsApp,
  WhatsApp Business or Telegram. Nothing is saved to your contacts."
* Slide 2 visual: 300-wide card with the four tool rows (icon box 40/12, title 14.5/600, chevron), no
  status lines. Title "Four tools, one permission"; body "Read without blue ticks, recover what senders
  delete, run a second account. Everything stays on this phone."
Dot pager: 8 high pills, active 22 wide green, inactive 8 wide switchOff, gap 6, margin 10 0 22.
Primary "Next" (slide 1) / "Get started" (slide 2). Honesty line 14 below. Both Skip and Get started
mark onboarding done and open Home with the number field focused.

### 4.3 Home (`home`) — screenshots `home_empty`, `home_filled`, `home_recents`
Brand bar 52: padding 0 10 0 20; mark 28 / 9 glyph 15 + "OpenChat" title19 inkStrong (gap 9); gear
(settings 22 inkSoft) in a 44 / 12 button → Settings.
Scrollable column, padding 6 20 20, gap 12:
1. **Phone field** 60 / 14 white, 1 px border (focused: 1.5 px green ring), padding 0 8, gap 10.
   Country chip 44 / 10 subtle: flag (26×18) + dial mono16 ink + chevron-down 16 sw2 ink2, padding 0 8, gap 7
   → opens the country sheet. Text: placeholder "Phone number" body17 placeholder colour; filled: mono22
   ink, grouped as typed (3 4 rest: "812 3456 7890"). Trailing: empty → **Paste** pill 36 (greenTint,
   greenTintBorder, green 13/600, clipboard 14 sw2, padding 0 12 0 10, gap 6); filled → **clear** 36
   circle subtle with x 18 ink2. Keyboard type phone.
2. **Message field** min 56 / 14 white border, padding 6 14; body15 (line 1.4) ink; placeholder
   "Message (optional)"; grows with content.
3. **Split button** 52: leading segment radius 14 6 6 14 green "Send Message" label16 white (45 %
   opacity while there are no digits; still shows); 2 dp gap; trailing 62 wide radius 6 14 14 6 greenTint
   + greenTintBorder, green: chosen app glyph 22 + chevron-down 14 sw2.2 (gap 3). Trailing stays live
   while Send is disabled. Tapping it opens the **Send with** menu (4.5).
4. First run only (no recents): hint "Opens the chat in the app you pick. Nothing is saved to your contacts."
5. **Recent** (when any): eyebrow "Recent"; card rows 52 (padding 0 14, gap 12): app glyph 20
   inkMuted, number mono14_5 ink ("+62 812 3456 7890"), time mono11_5 muted right ("Now", "2h",
   "Yesterday", "Mon"). Tap refills country, number and app and focuses the field. Long-press or
   swipe (end→start) reveals the delete state: row slides 96 dp left over a destructiveTint panel with
   trash 20 + "Delete" 13/600 destructive at the right (padding-right 18); tapping Delete removes the
   row, tapping the row slides it back. Max 5 rows, newest first.
6. **Tools** card rows 64 (padding 12 14, gap 13): icon box 40/12, title label14_5 ink, status body12
   muted (ellipsis), chevron-right 18. Rows and statuses:
   * "Read without blue ticks" — check-check sw2, ticks tint — granted: "N unread, seen by no one"
     (or "Paused"); not granted: "Read here. WhatsApp shows nothing".
   * "Recover deleted messages" — archive-restore, messages tint — granted: "N messages the sender
     deleted"; else "Keeps what the sender deleted".
   * "Recover deleted media" — image, media tint — granted: "N items recovered this week"; else
     "Photos, videos, voice notes, documents".
   * "Open a second account" — qr-code, second tint — linked: "Linked"; else "A second WhatsApp inside
     this app".
States: `home_empty` = first run, field focused, no recents, tools in set-up state. `home_filled` =
"+62 812-3456-7890" pasted → country Indonesia, "812 3456 7890", message "Hi! Is the blue one still
available?", clear button, Send enabled. `home_recents` = four recents (+62 812 3456 7890 WhatsApp 2h,
+62 813 9922 0417 WhatsApp Yesterday, +62 878 1201 5566 Telegram Yesterday, +60 12 345 6789 WhatsApp
Business Mon), third row mid-swipe, tools with live status "7 unread, seen by no one", "4 messages the
sender deleted", "9 items recovered this week", "Linked".

### 4.4 Country picker (sheet on Home) — screenshot `country_picker`
ModalBottomSheet 86 % tall, 24 top radius, padding 10 20 0. Handle. Row: "Country" title18 + close x 18
ink2 in a 40 button. Search 44 / 12, 1 px green border (focused look), search 18 muted + input body15
ink, placeholder "Search country or code". Eyebrow "All countries" / "N results" (margin 16 2 8). Card
rows 54 (padding 0 14, gap 12): flag 30×20, name label14_5 ink, dial mono14 ink2, check 18 sw2.2 green
on the current one. Footer 12 hint centred: "Detected from your SIM: Indonesia · +62" (uses the real
SIM/locale detection; falls back to "Detected from your locale: …"). Filter: name starts-with or dial
starts-with (case-insensitive, plus ignored). Picking closes the sheet.

### 4.5 Send app selector (dropdown on Home) — screenshot `send_app_selector`
DropdownMenu anchored under the trailing segment, right-aligned, 8 below, 238 wide, white, border,
14 radius, padding 6, shadow. Eyebrow "Send with" (eyebrow10 muted, padding 8 10 6). Rows 46 (gap 11,
padding 0 10, radius 10): glyph 20 inkSoft (installed app's launcher icon when available), label
label14_5 ink, check 18 sw2.2 green on the remembered choice. Only installed apps among WhatsApp,
WhatsApp Business, Telegram are listed; if none is installed all three are listed and Send opens the web
link. Tap outside closes. Choice persisted (DataStore).

### 4.6 Not on WhatsApp (confirmation sheet on Home) — screenshot `not_on_whatsapp`
Icon box 44/13 amberTint with message-circle-off 22 amber. Title "This number is not on WhatsApp".
Body "+62 812 3456 7890 has no WhatsApp account. Check the digits, or send the same message on Telegram."
Buttons: "Edit number" (cancel: closes, refocuses the field) / "Try Telegram" (green: sends the same
number + message to Telegram, records the recent as Telegram). Shown per ruling R6.

### 4.7 Notification access gate (`gate`) — screenshots `gate_before`, `gate_after`
Bar title = the tool the user came from ("Messages" or "Deleted media"; "Notification access" from
Settings). Centre (padding 0 28 20): icon box 64/20 messages tint, bell 30; title display22 "Allow
notification access" (18 below); body body15 inkMuted max 300 "Deleted messages and media are kept from
WhatsApp notifications. Nothing leaves your phone."; 18 below: StateChip Off (before) / Active (after).
Bottom (padding 12 20 4): before → primary "Open settings" with external-link 18 leading, hint "One grant
covers Message Recovery and Media Recovery."; while waiting for Android → spinner 16 + "Waiting for
Android…"; after → primary "Continue", hint "Turn it off anytime in Settings." Opening settings launches
`ACTION_NOTIFICATION_LISTENER_SETTINGS` (detail settings on API 30+); the state re-checks on resume.
Continue opens the pending tool.

### 4.8 Messages (`messages`) — screenshots `unseen_list` (All), `recover_messages_list` (Deleted only), `messages_empty`
Bar "Messages" + small StateChip Active/Paused + sliders-horizontal 20 → tool settings sheet.
Mode chips row (padding 2 20 12, gap 8): FilterPill "All · N unread", FilterPill "Deleted only · N".
List (padding 0 20 12, gap 12): card rows (padding 12 14, gap 12): avatar 42 circle tinted by conversation
(initial 15/700; "#" when the name is a number); name label14_5 ink (ellipsis); time mono11 muted;
second line (gap 8): in Deleted only, message-square-dashed 12 sw2 amber first; preview body13 ink2
ellipsis; CountBadge (unread count in All, deleted count in Deleted only; hidden at 0). Below the card an
info callout: All → "Read here and WhatsApp shows no blue ticks. Photos appear when the notification
carries a preview; voice notes and files show by name."; Deleted only → "Only messages the sender
deleted are kept. Excluded chats are skipped entirely." Empty state: message-square-dashed 28, "Nothing
here yet", "New messages land here as they arrive. Deleted ones stay in "Deleted only"." In All the
preview is the last message; in Deleted only it is the last deleted message and only conversations with
deleted messages are listed. Tapping a row opens the conversation and marks its messages seen locally.

### 4.9 Conversation (`conversation`) — screenshots `unseen_conversation`, `recover_messages_conversation`
Bar 56 two-line: name title16, subtitle mono11_5 muted "+62 812 3456 7890 · seen by no one" (All) or
"+62 812 3456 7890 · 2 deleted" (Deleted only). List padding 6 20 12, gap 10: date pill centred
("Today" mono10/600 .06em uppercase muted on subtle, padding 4 9, pill); incoming bubbles max 80 %,
start-aligned, gap 5: optional label row (message-square-dashed 12 + "DELETED BY SENDER" eyebrow9_5
amber, padding-left 4); bubble white, 1 px border, radius 16 16 16 6, padding 10 12 7: text body14_5
ink, or a photo 200×130 radius 10 (thumbnail; tap opens Media detail), then time mono10_5 muted right
(5 above). In Deleted only, only deleted messages are listed. Footer (white, top hairline borderSoft,
padding 12 20 4): SecondaryButton "Open chat in WhatsApp" with the WhatsApp glyph 18; hint: All →
"Opening the chat in WhatsApp sends read receipts."; Deleted only → "Read-only here. Replies happen in
WhatsApp." Opening uses `https://wa.me/<digits>` when the conversation name is a number, otherwise the
app's launch intent.

### 4.10 Tool settings sheet (on Messages and Deleted media) — screenshot `tool_settings_sheet`
Sheet padding 14 12 20. Title title15 ("Messages" / "Deleted media", padding 4 12 10). Rows 52 (padding
0 12, gap 13, icon 20 inkMuted): "Pause recovery" + OcSwitch (on = paused); "Exclude chats" + count 13.5
muted + chevron (opens 4.11); destructive row trash 20 + "Clear all kept messages" / "Delete all recovered
media" label14_5 destructive → confirmation sheet "Clear all recovered messages?" / "Delete all recovered
media?", body "Deletes this app's copies. WhatsApp is not affected.", Keep / Clear (destructive). Toasts:
"Recovery paused" / "Recovery resumed", "Cleared".

### 4.11 Exclude chats sheet
Same sheet grammar: title "Exclude chats", rows 52 per known conversation (avatar 30, name, OcSwitch);
excluded conversations are skipped by the listener from then on. Count feeds the tool-settings row.

### 4.12 Deleted media (`media`) — screenshots `recover_media_grid`, `media_empty`
Bar "Deleted media" + sliders. FilterPills: Photos, Videos, Audio, Documents, Stickers (padding 2 20 12,
gap 8, horizontally scrollable). Grid (padding 0 20 12) grouped by day: eyebrow "Today · N" (margin 2 2 8),
"Yesterday · N", then dates "Mon 22 Sep · N" (margin 16 2 8); LazyVerticalGrid 3 columns, 8 gap, square
tiles radius 14, 1 px border, thumbnail (Coil) over a hatched placeholder; videos show play 20 filled
inkMuted centred; time chip bottom-left (left 8, bottom 7, tileTime9_5 ink2 on 85 % white, padding 2 5,
radius 5). Audio/Documents/Stickers: same tiles with a type icon (music / file-text / sticker 24 inkMuted)
and the file name 10.5 mono under the icon. Empty state: image 28; title "Nothing recovered yet" (Photos,
Videos) or "No audio yet" / "No documents yet" / "No stickers yet"; body "Files someone deletes for
everyone are kept here as long as they were received first." When storage/media permission is missing
the empty state adds a primary "Allow media access" button (ruling R17).

### 4.13 Media detail (`mediaDetail`) — screenshot `recover_media_detail`
Bar title "Photo" / "Video" / "Audio" / "Document" / "Sticker" + counter "1 / 3" mono12 muted (padding-
right 8). Preview: fills, margin 0 20, radius 20, background ink; photos/stickers zoomable image (Coil),
video plays inline (VideoView/ExoPlayer-free: `VideoView` in `AndroidView`), audio: play/pause + name,
documents: file icon + name + size. Meta row (padding 14 20 0, gap 8): message-square-dashed 12 amber +
"From Ayu Lestari · Today 14:25 · Deleted 14:26" body12_5 ink2 (sender when known, else "Received Today
14:25 · Deleted 14:26"). Actions (padding 12 20 4, gap 8): primary "Save to gallery" with download 18
(MediaStore insert; toast "Saved to gallery"); IconSquareButton share-2 (ACTION_SEND via FileProvider;
toast "Opening share sheet…"); IconSquareButton trash destructive → confirmation "Delete this photo?",
"It is removed from Media Recovery. If you saved it to the gallery, that copy stays.", Keep / Delete →
toast "Deleted", back.

### 4.14 Second account (`second`) — screenshots `second_account_entry`, `second_account_linked`
Entry: bar "Second account". Centre (padding 0 28 12): icon box 64/20 second tint qr-code 30; title
display22 "A second WhatsApp, in here"; body body15 inkMuted max 300 "Links a WhatsApp Web session inside
this app. Use it for a second account, or the same account on a second phone."; callout (smartphone 14,
margin-top 22, left-aligned) "You need the other phone. A QR code appears here; scan it there under
WhatsApp › Linked devices › Link a device." Bottom: primary "Scan QR" with qr-code 18 → opens the
WebView state (the QR is rendered by WhatsApp Web); hint "A notification stays visible while linked.
Android requires it."
Linked / WebView: bar on white with bottom hairline: back, "Second account", small StateChip Linked
(or "Linking…" plain mono11_5 muted text while the QR is still showing), rotate-cw 20 (reload; toast
"Reloading…"), log-out 20 → confirmation "Log out of the linked account?", "The session ends on this
phone. The other phone keeps working.", Stay / Log out (destructive) → clears cookies + web storage,
stops the foreground service, toast "Logged out", back to entry. WebView: desktop user agent, JS, DOM
storage, database, persistent cookies; probes the DOM every 3 s (`#pane-side` present → linked). While
linked a foreground service shows "Second account linked · OpenChat" with an Open action. Screenshots
replace the WebView with a hatched box labelled "WHATSAPP WEB · WEBVIEW · FULL HEIGHT".

### 4.15 Settings (`settings`) — screenshot `settings`
Bar 56 (back radius 11, title title17). Column padding 2 18 18, gap 20:
* Access card (white, 1 px border, radius 18, padding 18, gap 13): icon box 46/14 bell 22 — messages
  tint when granted, amberTint/amber otherwise; "Notification access" title16; sub body13 ink2 "Active ·
  messages and media tools" / "Off · needed for the messages and media tools"; chevron. Opens the gate.
* Eyebrow "Preferences"; card rows (padding 15, gap 13, glyph 20 inkMuted, title label14_5, value 13.5
  muted, chevron 18): "Default app" (current app glyph; value "WhatsApp") → default-app sheet; "Language"
  (languages) value "English" → Language; "Clear recent numbers" (trash) value count → confirmation
  "Clear recent numbers?", "Removes the N numbers on Home. Nothing changes in WhatsApp.", Keep / Clear
  (destructive), toast "Recent numbers cleared".
* Eyebrow "About"; card: "Rate us" (star) → rating sheet; "Contact us" (mail) → Contact; "Share app"
  (share-2) → ACTION_SEND text; "Privacy policy" (shield) with arrow-up-right 18 → browser; "Version"
  (info) value "1.0.0 (1)" mono12 muted, not tappable.
* Honesty callout "OpenChat is free. No ads, no account, no tracking. Numbers, messages and media never
  leave your phone."

### 4.16 Default app sheet (on Settings) — screenshot `default_app_sheet`
Title "Default app"; rows 50 radius 12 (padding 0 12, gap 13): glyph 20 inkSoft, label, check 18 green on
the current one; picking closes with toast "Default app: WhatsApp".

### 4.17 Language (`language`) — screenshot `language`
Bar "Language". Intro body13_5 muted "Changes the app. Messages you send are typed by you, in any
language." Card rows 58 (padding 0 15, gap 12): native name label14_5 ink over English name body11_5
hint; "RTL" MonoTag for Urdu; check 20 sw2.2 green on the current. Languages in order: English/English,
Bahasa Indonesia/Indonesian, Português (Brasil)/Portuguese, اردو/Urdu (RTL), हिन्दी/Hindi, Türkçe/Turkish,
Español (México)/Spanish, isiZulu/Zulu. Toast "Language: Indonesian".

### 4.18 Contact us (`contact`) — screenshot `contact`
Bar "Contact us". Column padding 4 20 12, gap 14: intro body14 ink2 "A number that will not open, a
message that did not recover, an idea. It goes straight to the team."; message card (white, border,
radius 14, padding 12 14) with a 6-line field body15 ink placeholder "What happened, and on which
screen?"; label label12_5 ink2 "Email · optional, only if you want a reply" + input 50 / 12 placeholder
"you@example.com"; callout "Sent with the app version and Android version. Never your numbers or
messages." Footer white with top hairline, padding 12 20 20: primary "Send", disabled until there is
text → email composer (R11), toast "Sent. Thank you.", back.

### 4.19 Rating sheet (on Settings and after the third successful send) — screenshots `rating_stars`, `rating_store`, `rating_feedback`, `rating_thanks`
Sheet padding 10 22 24, handle 38×5 border, margin 2 auto 16.
* Stars: icon box 64/19 greenTint star 32 green; "Enjoying OpenChat?" title20; body body13_5 ink2 max
  290 "Your honest rating helps other people find an app that opens any number in one tap."; five star
  38 buttons (gap 8, margin 22 0 6): filled gold up to the rating, else switchOff outline; hint 11.5 muted
  (min-height 16): "Tap a star" or by rating 1 "Sorry to hear that", 2 "We can do better", 3 "Thanks, tell
  us more", 4 "Glad it works", 5 "Thank you!"; ghost "Maybe later" 46. 350 ms after a tap: 4–5 → store,
  1–3 → feedback.
* Store: icon box successTint party-popper 30 successFg; five gold stars 22 (gap 5); "Thank you!";
  "Would you share that on Google Play? It helps a small app grow."; primary 54 / 15 "Rate on Google
  Play" with external-link 18 (R12; toast "Opening Google Play…"); ghost "Not now".
* Feedback: icon box 52/15 greenTint message-square-text 24 green; "How can we do better?" 19/700;
  "Tell us what fell short. It goes straight to the team, not a public review, and we read every one.";
  field 104 high radius 14 border placeholder "What would have made this a 5-star app?"; primary "Send
  feedback" with send 16, disabled until text (R11 → thanks); ghost "Cancel".
* Thanks: icon box successTint check 32 sw2.4 successFg; "Thank you, we hear you"; "Your note is on its
  way to the team. It shapes what we build next."; primary "Done".
Trigger: Rate us row, and once after the third successful Send (1.8 s later), never again after shown.

### 4.20 Confirmation sheets — screenshots `dialog_clear_recents`, `dialog_clear_all`, `dialog_delete_media`, `dialog_logout`
Spec in §2. Destructive ones use trash 22 (log-out 22 for logout) in destructiveTint/destructive and a
destructive confirm button.

### 4.21 Dark toast
Shown for 2.2 s, 90 dp above the bottom, centred, over any screen. Text per action (see screens).

## 5. Behaviour specs

### 5.1 Number entry and Send
* Digits only in the field; grouping for display: first 3, next 4, rest ("812 3456 7890"); max 15 digits.
* Paste normalisation (`PhoneNumberNormalizer.normalizePaste(raw, current: DialCountry)`): keep digits;
  drop spaces, dashes, dots, parentheses and a leading "+"; "00" prefix = international prefix and is
  dropped; if the raw text started with "+" or "00", the longest matching dial code becomes the country
  and is stripped; otherwise the current country stays; then one leading "0" (trunk prefix) is dropped.
  Examples: "+62 812-3456-7890" → ID, "81234567890"; "0812 3456 7890" with ID → "81234567890";
  "(202) 555-0123" with US → "2025550123"; "+1 684 555 0123" → AS, "5550123"; "+44 (0)20 7946 0958" →
  GB, "2079460958".
* E.164 digits for sending = dialCode + national digits.
* Links (`SendLinkBuilder`): WhatsApp `https://wa.me/<digits>?text=<url-encoded>` with package
  `com.whatsapp`; WhatsApp Business same URL with package `com.whatsapp.w4b`; Telegram
  `tg://resolve?phone=<digits>&text=<url-encoded>` with package `org.telegram.messenger`, web fallback
  `https://t.me/+<digits>`. Empty message → no `text` parameter. If the chosen app is not installed the
  URL opens in the browser (wa.me / t.me).
* Installed apps: `PackageManager.getLaunchIntentForPackage` (manifest `<queries>` lists the three).
  Last choice in DataStore (`send_app`), default WhatsApp when installed, else the first installed.
* Recents (Room `recent_numbers`): upsert by (dialCode, nationalNumber), keep newest 5; row shows the
  app used; time labels: < 60 s "Now", < 60 min "Nm", < 24 h "Nh", yesterday "Yesterday", < 7 d weekday
  ("Mon"), else "d MMM".
* Send flow: launch → toast "Opening WhatsApp…" → record recent → count successful sends (rating after
  the third). Resume within 7 s after a WhatsApp/Business launch → Not on WhatsApp sheet (R6).

### 5.2 Notification listener (tools 2 and 4)
* `WaNotificationListenerService` bound for `com.whatsapp` and `com.whatsapp.w4b`. For every posted
  notification: parse `MessagingStyle` (conversation title, each message's text, timestamp, sender);
  fall back to EXTRA_TITLE / EXTRA_TEXT / EXTRA_BIG_TEXT / EXTRA_TEXT_LINES. Skip group summaries
  (FLAG_GROUP_SUMMARY) and excluded chats. Skip system notifications before storing anything, whatever
  the phone's language (`NotificationText.isSystemNotification`, fed by the parser with the category and
  flags): category call, missed call, progress, service, status, transport, system, alarm or error;
  ongoing or foreground-service notifications ("Checking for new messages", a call in progress); progress
  bars (backup, restore); and plain-text (non-MessagingStyle) notifications titled "WhatsApp" / "WhatsApp
  Business" (a chat's notification carries the chat's name). The lines of plain-text notifications and of
  notifications titled with the app label also go through the noise rules (`isSummaryOrNoise`): the English
  rules ("N messages from M chats", "Checking for new messages", "backup", calls, …) plus WhatsApp's status
  lines in every app language (checking for / may have new messages, "N new messages", backup and restore,
  incoming / ongoing / missed / group calls, calling, ringing, "WhatsApp Web is active"; 2021 wording
  outside English), each a whole line give or take a leading 📹/☎ and trailing "…" or count, and the
  bundle summary in either argument order: two numbers, one next to the language's message word and one
  next to its chat word ("12 messages from 2 chats", "2 sohbetten 12 mesaj", "来自 2 个对话的 12 条消息").
  A conversation's own MessagingStyle lines never go through the noise rules. Conversation key =
  normalized title. Each new message (by conversation + timestamp + text hash) is stored once in Room
  `messages` with `seenLocally = false`. Message kinds: TEXT,
  PHOTO ("📷 Photo" text or a data URI image), VOICE ("🎤 Voice message (0:12)"), DOCUMENT ("📄 name"),
  STICKER, VIDEO — derived from the text prefix; the text is kept verbatim for display.
* Images: a MessagingStyle message with an image `dataUri` (WhatsApp content provider) or EXTRA_PICTURE
  is copied to app storage at once (`files/notif-media/`), linked to the message (`mediaId`) and also
  listed in Deleted media when the message is later deleted.
* Deleted detection (`DeletedMessageDetector`, pure Kotlin): a message line whose text matches a
  deleted placeholder (`NotificationText.isDeletedPattern`, strings in `WhatsAppStrings`; table below)
  replaces the original in the notification. The match is exact after normalizing both sides: bidi marks
  and isolates dropped, NFC, an optional leading 🚫 (notifications carry none; the chat bubble's is a
  drawable), trailing "." "।" "۔" "。" and spaces trimmed, any case, Arabic yeh/kaf read as the
  Persian/Urdu letters (ي→ی, ك→ک), typographic apostrophe and space runs folded. Admin variants are
  templates: the admin's name (1–80 characters) sits where the language puts it and the rest must match
  the whole line, e.g. "This message was deleted by an admin", "This message was deleted by admin
  Zeeshan", "Un admin., Lucía, eliminó este mensaje.", "Diese Nachricht wurde von Admin Max gelöscht.",
  "Bu mesaj Ayşe adlı yönetici tarafından silindi.", "این پیام را مدیر (رضا) حذف کرد." (Android English
  high; the other languages from WhatsApp iOS strings and the Help Center, low–medium, medium for es, de,
  fa). Old and new wordings are all kept, since the WhatsApp versions in use vary. Sources: WhatsApp
  Android string resources (2017/2021), WhatsApp iOS strings, real Android chat exports, WhatsApp Help
  Center.

  | Language | Placeholder variants (newest first) | Confidence |
  |---|---|---|
  | en | This message was deleted | high |
  | id | Pesan ini dihapus · Pesan ini telah dihapus (2017–2021) | medium · high |
  | pt-BR | Mensagem apagada · Essa mensagem foi apagada (2021) · Esta mensagem foi apagada (2017) | medium · high · high |
  | pt | Esta mensagem foi apagada · Esta mensagem foi apagada pelo remetente. (2017) | high · high |
  | ur | یہ میسج حذف کر دیا گیا ہے۔ (iOS wording) · یہ پیغام حذف کیا گیا (2017/2021) · یہ پیغام حذف کر دیا گیا (in no WhatsApp source; kept) | low–medium · high · low |
  | hi | यह मैसेज डिलीट कर दिया गया है. (iOS wording) · यह मैसेज मिटाया गया (2021) · यह संदेश मिटाया गया (2017) · यह मैसेज हटा दिया गया (in no WhatsApp source; kept) | medium–low · high · high · low |
  | tr | Bu mesaj silindi | high |
  | es | Se eliminó este mensaje. · Este mensaje fue eliminado (2017–2021) | medium · high |
  | ar | تم حذف هذه الرسالة | high |
  | fa | این پیام حذف شده است. (iOS wording) · این پیام حذف شد (2017/2021) | low–medium · high |
  | ps | none of its own: WhatsApp for Android has no Pashto UI, the phone shows English | medium (inferred) |
  | he | הודעה זו נמחקה | high |
  | fr | Ce message a été supprimé. | high |
  | de | Diese Nachricht wurde gelöscht. | high |
  | it | Questo messaggio è stato eliminato | high |
  | ru | Данное сообщение удалено | high |
  | zh | 这条消息已被删除。 (iOS wording) · 消息已删除 (2021) · 信息已删除 (2017) | low–medium · high · high |
  | ha | An goge wannan saƙon (unverified guess: WhatsApp's Hausa strings could not be read; older builds show English) | low |
  | my | none of its own: no Burmese UI in WhatsApp for Android, the phone shows English | medium (inferred) |

  Rule: for the same conversation, a deleted-pattern line with timestamp T marks the stored message
  with timestamp T (±2 s) as deleted; when timestamps
  are absent, the stored unseen message at the same position from the end is marked. Also: when the
  notification for a conversation is removed with REASON_APP_CANCEL and the last posted content for it
  contained a deleted-pattern line, any still-unmarked stored message that no longer appears in the last
  content is marked deleted. Deleted messages get `deletedAt = now` and the label "Deleted by sender".
* Pause (DataStore `recovery_paused`): the listener ignores everything while paused; the chips read
  Paused. Clear all: deletes the `messages` rows (and notif-media copies).
* Unseen semantics: `seenLocally` is set when a conversation is opened in the app. Nothing is ever sent
  to WhatsApp; the app never cancels or acts on WhatsApp notifications.

### 5.3 Media watcher (tool 3)
* Roots: `<ext>/Android/media/com.whatsapp/WhatsApp/Media`, `<ext>/WhatsApp/Media`, and the Business
  equivalents (`com.whatsapp.w4b/WhatsApp Business/Media`, `WhatsApp Business/Media`). Subfolders:
  WhatsApp Images, WhatsApp Video, WhatsApp Audio, WhatsApp Voice Notes, WhatsApp Documents, WhatsApp
  Stickers, WhatsApp Animated Gifs. `.Statuses` and `Sent` subfolders are skipped.
* Permission by API: 24–28 READ/WRITE_EXTERNAL_STORAGE; 29 READ_EXTERNAL_STORAGE with
  `requestLegacyExternalStorage`; 30–32 READ_EXTERNAL_STORAGE; 33+ READ_MEDIA_IMAGES/VIDEO/AUDIO.
  Documents on 30+ are best-effort (MediaStore Files rows in `Android/media/com.whatsapp/…`).
* Watching: a `FileObserver` per root/subfolder (recursive list constructor on API 29+, one observer per
  folder below) plus a `ContentObserver` on `MediaStore.Files` (API 29+) both trigger a debounced
  `reconcile()`; the listener service also triggers it after each WhatsApp notification, and the media
  screen on open. `reconcile()` lists the current files, copies unknown ones to `files/media/<id>.<ext>`
  (Room `media`, `deletedAt = null`), and marks known copies whose original is gone with `deletedAt =
  now`. Copies of still-present originals older than 14 days are dropped to bound storage.
* Category (`MediaClassifier`, pure Kotlin) from MIME/extension/folder: PHOTO (jpg/jpeg/png/webp in
  Images or Animated Gifs — gif is VIDEO-like but shown under Photos), VIDEO (mp4/3gp/mkv/mov), AUDIO
  (opus/m4a/mp3/aac/amr/ogg/wav; Voice Notes and Audio folders), DOCUMENT (pdf/doc/docx/xls/xlsx/ppt/
  pptx/txt/csv/zip/apk and anything under Documents), STICKER (webp under Stickers).
* Detail actions: Save to gallery (MediaStore insert into Pictures/OpenChat, Movies/OpenChat,
  Music/OpenChat, Download/OpenChat; pre-29 direct file copy), Share (FileProvider), Delete (removes the
  copy and the row).

### 5.4 Second account (tool 5)
* URL `https://web.whatsapp.com/`; user agent "Mozilla/5.0 (Windows NT 10.0; Win64; x64)
  AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"; settings: JavaScript, DOM
  storage, database, wide viewport, overview mode, mixed content compatibility off, media playback
  without gesture, cookies accepted (persistent by default). `WebView` kept in the ViewModel-scoped
  holder so rotation/navigation does not reload the session.
* Linked probe (`LinkedStateProbe`): JS `!!document.querySelector('#pane-side')` every 3 s → linked;
  QR visible → not linked. DataStore `second_linked` mirrors the last known state so the tool row and
  the entry/linked route are right at launch.
* Foreground service `WebSessionService` (type specialUse) while linked: notification channel
  "Second account", low importance, "Second account linked" / "Tap to open", stopped on logout.
* Logout: `CookieManager.removeAllCookies`, `WebStorage.deleteAllData`, `webView.clearCache`,
  `clearFormData`, then load the URL again and return to the entry state.

### 5.5 Settings and misc
* DataStore keys: `onboarding_done`, `send_app`, `recovery_paused`, `second_linked`, `language`,
  `send_count`, `rating_shown`, `notif_access_seen`.
* Notification access check: `NotificationManagerCompat.getEnabledListenerPackages(context)` contains
  the package.
* SIM/locale country: `TelephonyManager.simCountryIso` → `networkCountryIso` → `Locale.getDefault().country`
  → "US".

## 6. Screenshot inventory (Paparazzi, 390×844 @3x, fake data)

| File | Composable + state |
|---|---|
| splash.png | SplashScreen |
| onboarding_1.png / onboarding_2.png | OnboardingScreen(slide 0 / 1) |
| home_empty.png | HomeScreen(first run, focused) |
| home_filled.png | HomeScreen(ID, 81234567890, message, WhatsApp) |
| home_recents.png | HomeScreen(4 recents, 3rd swiped, live statuses) |
| country_picker.png | Home + CountryPickerSheetContent in a sheet frame |
| send_app_selector.png | Home filled + SendWithMenuContent under the trailing segment |
| not_on_whatsapp.png | Home filled + ConfirmSheet notOnWhatsApp |
| gate_before.png / gate_after.png | GateScreen(granted = false / true) |
| unseen_list.png | MessagesScreen(All, 5 conversations) |
| recover_messages_list.png | MessagesScreen(Deleted only) |
| messages_empty.png | MessagesScreen(Deleted only, empty) |
| unseen_conversation.png | ConversationScreen(All, 4 messages incl. 2 deleted, 1 photo) |
| recover_messages_conversation.png | ConversationScreen(Deleted only) |
| tool_settings_sheet.png | Messages + ToolSettingsSheetContent |
| recover_media_grid.png | MediaScreen(Photos, 2 today + 5 yesterday) |
| media_empty.png | MediaScreen(Audio, empty) |
| recover_media_detail.png | MediaDetailScreen(photo 1/3) |
| second_account_entry.png / second_account_linked.png | SecondAccountScreen(entry / linked with placeholder web area) |
| settings.png | SettingsScreen(granted, WhatsApp, English, 4 recents) |
| default_app_sheet.png | Settings + DefaultAppSheetContent |
| language.png | LanguageScreen(English) |
| contact.png | ContactScreen(empty) |
| rating_stars.png / rating_store.png / rating_feedback.png / rating_thanks.png | RatingSheetContent stages |
| dialog_clear_recents.png / dialog_clear_all.png / dialog_delete_media.png / dialog_logout.png | ConfirmSheet variants over their screens |

Fake data (from the prototype): conversations Ayu Lestari (+62 812 3456 7890, last "Sorry, wrong chat",
deleted "I can do 300k if you ship today", 14:26, 3 unread, 2 deleted, blue), +62 813 9922 0417 (last
"Ok deal, 350k transfer tonight", 11:02, 1 unread, 1 deleted, green), Budi · Tokopedia (+62 811 700 2210,
"Order is on the way", Yesterday, 2 unread, 0 deleted, orange), Rani (+62 856 4410 9087, "Voice note ·
0:12", Yesterday, 1 unread, 1 deleted, purple), Toko Sinar Jaya (+62 821 5567 3390, "Can you send the
address again?", Mon, 0 unread, teal). Conversation messages: "Hi kak, is the blue one still available?"
14:20; "I can do 300k if you ship today" 14:24 deleted; photo 14:25 deleted; "Sorry, wrong chat" 14:26.
Media tiles: today 14:25, 09:41, 08:12 (video); yesterday 21:07, 19:33 (video), 18:50, 16:02, 12:44, 10:19.
