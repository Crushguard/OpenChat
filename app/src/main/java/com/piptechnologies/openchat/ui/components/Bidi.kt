package com.piptechnologies.openchat.ui.components

/** U+2066 LEFT-TO-RIGHT ISOLATE. */
private const val LRI = '\u2066'

/** U+2068 FIRST STRONG ISOLATE. */
private const val FSI = '\u2068'

/** U+2069 POP DIRECTIONAL ISOLATE. */
private const val PDI = '\u2069'

/**
 * [text] as a left-to-right isolate (LRI … PDI), for phone numbers, dial codes and other
 * left-to-right tokens shown in right-to-left text or in a right-to-left layout.
 *
 * Without it the Unicode bidi algorithm reorders them in an RTL paragraph: the "+" of "+62" is a
 * neutral that takes the Arabic direction and ends up on the far side ("62+"), and the digit groups of
 * "+62 812 3456 7890" are separated by neutral spaces, so they come out in reverse order
 * ("7890 3456 812 62+"). The isolate keeps the token in its own left-to-right order and keeps it from
 * reordering the translated text around it. Returns an empty [text] unchanged.
 */
fun ltr(text: String): String = if (text.isEmpty()) text else "$LRI$text$PDI"

/**
 * [text] as a first-strong isolate (FSI … PDI), for text the user or a contact wrote (names, file names,
 * message previews) when it is placed inside a translated sentence: a Latin name in an Arabic sentence, or
 * an Arabic name in an English one, keeps its own direction and does not drag the separators and numbers
 * around it to the wrong side. Its direction comes from its first strong character. Returns an empty
 * [text] unchanged. Display only: never store the result or put it in an intent.
 */
fun isolate(text: String): String = if (text.isEmpty()) text else "$FSI$text$PDI"
