package com.piptechnologies.openchat.ui.second

/**
 * Where the second account stands (design map §4.14): the entry screen, the WebView showing WhatsApp
 * Web's QR code, or a linked session.
 */
enum class SecondPhase { ENTRY, LINKING, LINKED }

/**
 * What the Second account screen shows: its [phase], and whether the "Log out of the linked account?"
 * confirmation sheet is open ([confirmLogout]).
 */
data class SecondUiState(val phase: SecondPhase, val confirmLogout: Boolean)
