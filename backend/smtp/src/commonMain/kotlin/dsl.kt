package dev.sitar.kmail.smtp

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.format.TextStyle
import java.util.*

// TODO: MISSPLACED HEADERS???
public class MessageBuilder(
    public var originalDate: Instant,
    public var from: String,
    public var messageId: String
) {
    public var subject: String? = null
    public var comments: String? = null
    public var keywords: MutableList<String> = mutableListOf()
    public var to: String? = null

    public var content: String? = null

    public fun toMessage(): MessageCommand {
        return MessageCommand(
            "Message-ID:$messageId\r\n" +
                    "Date:${originalDate.toSmtpDate()}\r\n" +
                    (if (to != null) "To:$to\r\n" else "") +
                    "From:$from\r\n" +
                    (if (subject != null) "Subject:$subject\r\n" else "") +
                    (if (comments != null) "Comments:$comments\r\n" else "") +
                    (if (keywords.isNotEmpty()) "Keywords:${keywords.joinToString(",")}\r\n" else "") +
                    (content ?: "") +
                    "\r\n"
        )
    }
}

public fun message(
    originalDate: Instant = Clock.System.now(),
    from: String,
    host: String,
    builder: MessageBuilder.() -> Unit
): MessageCommand {
    Clock.System.now()
    Instant
    return MessageBuilder(originalDate, from, "<${Clock.System.now().epochSeconds}@$host>").apply(builder).toMessage()
}

private fun Instant.toSmtpDate(): String {
    with(toLocalDateTime(TimeZone.UTC)) {
        return "${dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)}, $dayOfMonth ${month.getDisplayName(
            TextStyle.SHORT, Locale.ENGLISH)} $year ${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}:${second.toString().padStart(2, '0')} -0000"
    }
}