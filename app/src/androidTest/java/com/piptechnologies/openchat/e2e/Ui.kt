package com.piptechnologies.openchat.e2e

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo

/** How long a screen may take to show what a test waits for. */
const val UI_TIMEOUT_MS = 15_000L

/** [text] without invisible format characters (the bidi isolates the app puts around numbers), with plain spaces. */
fun cleanText(text: String): String =
    text.filterNot { it.category == CharCategory.FORMAT }.replace(' ', ' ').replace(' ', ' ').trim()

/** The node's texts, including a text field's content, cleaned. */
fun SemanticsNode.texts(): List<String> = buildList {
    config.getOrNull(SemanticsProperties.Text)?.forEach { add(cleanText(it.text)) }
    config.getOrNull(SemanticsProperties.EditableText)?.let { add(cleanText(it.text)) }
}

fun SemanticsNode.descriptions(): List<String> =
    config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().map(::cleanText)

/** A node showing [expected] (one of its texts equal to it, or containing it with [substring]), bidi marks ignored. */
fun hasTextOf(expected: String, substring: Boolean = false, ignoreCase: Boolean = false): SemanticsMatcher {
    val want = cleanText(expected)
    return SemanticsMatcher("text ${if (substring) "containing" else "="} \"$want\"") { node ->
        node.texts().any { if (substring) it.contains(want, ignoreCase) else it.equals(want, ignoreCase) }
    }
}

fun hasDescriptionOf(expected: String): SemanticsMatcher {
    val want = cleanText(expected)
    return SemanticsMatcher("content description = \"$want\"") { node -> node.descriptions().any { it == want } }
}

/** A node with a text whose digits are exactly [digits]: "+62 812 3456 7890" for "6281234567890". */
fun hasDigits(digits: String): SemanticsMatcher =
    SemanticsMatcher("a text with the digits $digits") { node -> node.texts().any { text -> text.filter { it.isDigit() } == digits } }

/** The launch screen's spinner (the only indeterminate progress indicator at launch). */
val isIndeterminateProgress: SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate)

/** Whether a node matches now; never throws while no OpenChat screen is in front. */
fun ComposeTestRule.exists(matcher: SemanticsMatcher): Boolean =
    onAllNodes(matcher).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()

/** Every text and description on screen, for failure messages. */
fun ComposeTestRule.onScreenTexts(): List<String> = try {
    onAllNodes(SemanticsMatcher("any node") { true })
        .fetchSemanticsNodes(atLeastOneRootRequired = false)
        .flatMap { it.texts() + it.descriptions() }
        .filter { it.isNotEmpty() }
        .distinct()
} catch (e: Throwable) {
    listOf("<screen not readable: $e>")
}

/** Waits (pumping Compose frames) until a node matches, then returns the first match. */
fun ComposeTestRule.waitFor(matcher: SemanticsMatcher, timeoutMs: Long = UI_TIMEOUT_MS): SemanticsNodeInteraction {
    try {
        waitUntil(timeoutMillis = timeoutMs) { exists(matcher) }
    } catch (e: ComposeTimeoutException) {
        throw AssertionError("No node with ${matcher.description} after $timeoutMs ms. On screen: ${onScreenTexts()}", e)
    }
    return onAllNodes(matcher).onFirst()
}

fun ComposeTestRule.waitForGone(matcher: SemanticsMatcher, timeoutMs: Long = UI_TIMEOUT_MS) {
    try {
        waitUntil(timeoutMillis = timeoutMs) { !exists(matcher) }
    } catch (e: ComposeTimeoutException) {
        throw AssertionError("A node with ${matcher.description} is still there after $timeoutMs ms. On screen: ${onScreenTexts()}", e)
    }
}

/**
 * Waits for a tappable node matching [matcher], scrolls it into view when it sits in a scrolling column, taps it.
 * The scroll and the tap's handler run as queued coroutines (E2eTest's dispatcher): one frame of the clock after
 * each lets them run before the next step.
 */
fun ComposeTestRule.tap(matcher: SemanticsMatcher, timeoutMs: Long = UI_TIMEOUT_MS) {
    val node = waitFor(matcher and hasClickAction(), timeoutMs)
    if (runCatching { node.performScrollTo() }.isSuccess) settle() // no scrolling parent: nothing to scroll
    node.performClick()
    settle()
}

/**
 * One frame of the rule's clock, which also runs the coroutines due now. Needs no OpenChat screen in front (a tap
 * may just have opened another app), unlike waitForIdle.
 */
fun ComposeTestRule.settle() {
    mainClock.advanceTimeByFrame()
}
