# OpenChat — the brief (verbatim)

## Objective
Build "OpenChat", a native Android WhatsApp companion app, package com.piptechnologies.openchat, from the attached design and the spec below. Map the design, plan with the /superpowers plugin, then build without stopping until the app is functional, CI is green on main, and screenshots of every screen are on the screenshots branch. No interview, no plan approval, no other approvals. If an input is missing, pick the best option, note it in the final report, and continue.

## Workflow
Superpowers: writing-plans for the plan, executing-plans with subagent-driven-development for the build. You decide agent split and sequencing. Verification: assembleDebug passes in CI, one unit test per tool passes, screenshot job passes. No lint gates, no coverage.

Loop: push to main, GitHub Actions builds, tests, renders screenshots, read the result, fix, repeat until green.

## Context
- Free app. No ads, no IAP, no login, no backend, no Firebase. Everything on device.
- Screens (design attached, follow it exactly): Home (Direct Chat form, empty / filled / with recents), Country picker, Send app selector, "Not on WhatsApp" dialog, Notification Access gate (before / after grant), Recover Messages list + conversation, Recover Media grid + item detail, Unseen (read without blue tick) list + conversation, Second Account entry + linked WebView, Settings. Implement every state the design shows.

## Features
1. OpenChat (Home). Country code picker (auto-detect from SIM/locale), number field with inline paste that strips spaces, dashes, plus and leading zero, optional message, split "Send Message" button with app selector: WhatsApp, WhatsApp Business, Telegram, only installed apps shown, last choice remembered. Send opens the chosen app's chat with number + prefilled text via its intent/URL scheme. If WhatsApp reports the number is not on WhatsApp, show the dialog with "Try Telegram" and "Edit number". Recents: last 5 numbers in Room, tap to refill, long press to delete.
2. Recover Deleted Messages. NotificationListenerService on WhatsApp and WhatsApp Business notifications, store every message in Room, mark the ones whose notification was cancelled after a "This message was deleted" pattern or removed before being read. Grouped by sender, conversation view, "Deleted by sender" label. Pause/resume, clear all.
3. Recover Deleted Media. Same listener plus a FileObserver/MediaStore watcher on the WhatsApp media folders; copy new media to app storage, keep the copy when the original disappears. Tabs: Photos, Videos, Audio, Documents, Stickers. Item detail: preview, Save to gallery, Share, Delete. Use scoped storage correctly per API level (24 to 35).
4. Unseen. Show messages captured by the listener without opening WhatsApp, so no read receipt is sent. Same data as feature 2, presented as live conversations; mark as seen locally only.
5. Second Account. WhatsApp Web in a WebView with desktop user agent, QR link flow, persistent session (cookies + storage), foreground service with notification while linked, top bar with back, reload, logout.
One Notification Access grant serves features 2, 3 and 4. Gate screen explains it, opens system settings, reflects the granted state.

## Stack (locked)
Kotlin, Jetpack Compose, Material 3 themed from the attached design tokens, single Gradle module, MVVM with one ViewModel per screen, Hilt, Room, Coil, Coroutines + Flow, DataStore for settings, minSdk 24, targetSdk 35, Gradle version catalog. GitHub Actions on ubuntu-latest with the preinstalled Android SDK.

## Screenshots (locked)
Paparazzi screenshot tests, one per screen and state listed above, with fake data. CI job "screenshots" runs recordPaparazziDebug and force-pushes the PNGs to branch screenshots under screenshots/<screen>_<state>.png with an index README listing them. No emulator.

## Target State
- CI on main green: assembleDebug, unit tests, screenshots job.
- app-debug.apk downloadable from the CI run.
- Every screen and state in the design implemented and reachable.
- screenshots branch holds one PNG per screen and state.
- README: build, install the APK, grant Notification Access, what each tool needs on the phone to be tested.

## Scope
- Push to main only. NEVER publish to Play, NEVER create or reference a release keystore. Debug builds only.
- Only what is in this brief and the attached design. No ads, no dark mode, no onboarding carousel unless the design shows it.
- Do not ship any WhatsApp branding, logos or trademarked names in app resources beyond what the design shows as text.

## Stop Conditions
None. Run to completion.

Design: https://claude.ai/design/p/d67a24d8-850a-4e36-bc74-2b6349fffe29?file=Prototype.dc.html&via=share
