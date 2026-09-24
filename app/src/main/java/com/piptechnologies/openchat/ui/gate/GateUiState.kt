package com.piptechnologies.openchat.ui.gate

/**
 * What the notification access gate shows (design map §4.7): the bar [title] of the tool the user came
 * from, whether access is [granted] (chip Active and Continue) and whether the app is [waitingForSystem]
 * to come back from the system settings screen (spinner and "Waiting for Android…" on the button).
 */
data class GateUiState(val title: String, val granted: Boolean, val waitingForSystem: Boolean)
