package com.piptechnologies.openchat.core.messages

/**
 * One message line of a posted notification (MessagingStyle message or a plain text line).
 * [timestamp] is null for plain text lines, which carry no time of their own.
 */
data class NotificationLine(val text: String, val timestamp: Long?, val sender: String?)
