package com.piptechnologies.openchat.core.phone

/**
 * The words [RelativeTime] puts into its labels: [now] (under a minute), [today] and [yesterday] (day
 * labels), and the compact same-day ages [minutesAgo] ("5m") and [hoursAgo] ("2h"). Weekday and month
 * names come from the locale instead. [ENGLISH] is the design's copy and the default of every
 * RelativeTime function; the UI builds one from string resources (ui/components/TimeFormat.kt).
 */
data class TimeWords(
    val now: String,
    val today: String,
    val yesterday: String,
    val minutesAgo: (Long) -> String,
    val hoursAgo: (Long) -> String,
) {
    companion object {
        /** "Now", "Today", "Yesterday", "5m", "2h". */
        val ENGLISH = TimeWords("Now", "Today", "Yesterday", { "${it}m" }, { "${it}h" })
    }
}
