package dev.sitar.kmail.message

import dev.sitar.kio.buffers.asBuffer
import dev.sitar.kmail.message.headers.Header
import dev.sitar.kmail.message.headers.Headers
import kotlinx.datetime.FixedOffsetTimeZone
import kotlinx.datetime.Instant
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toLocalDateTime
import kotlin.math.absoluteValue
import kotlin.math.sign

data class Message(
    val headers: Headers,
    val body: String?,
    val text: String?
) {
    companion object {
        fun fromText(raw: String): Message {
            val buffer = raw.encodeToByteArray().asBuffer()
            val headers = mutableSetOf<Header>()
            var body: String? = null

            while (buffer.readIndex < buffer.writeIndex) {
                var line = buffer.readUtf8UntilMailEnding()

                // unfolding
                while (buffer.peek() == ' '.code.toByte() || buffer.peek() == '\t'.code.toByte()) {
                    line += "\r\n" + buffer.readUtf8UntilMailEnding()
                }

                if (line.isEmpty()) {
                    body = buffer.readStringUtf8(buffer.writeIndex - buffer.readIndex)
                } else {
                    headers += Header.fromText(line)
                }
            }

            return Message(Headers(headers), body, raw)
        }
    }

    fun asText(): String {
        if (text != null) return text
        val textHeaders = headers.joinToString("\r\n") { it.asText() } + "\r\n"
        return textHeaders + if (body != null) "\r\n$body" else ""
    }

    override fun toString(): String {
        return "Message(headers=$headers, body=${body?.replace("\r\n", "\\r\\n")})"
    }
}

class MessageBuilder {
    class HeadersBuilder: MutableSet<Header> by mutableSetOf() {
        fun header(name: String, body: String) {
            add(Header(name,  body))
        }

        fun from(from: String) {
            header(Headers.From, from)
        }

        fun to(rcpt: String) {
            header(Headers.To, rcpt)
        }

        fun subject(subject: String) {
            header(Headers.Subject, subject)
        }

        fun originationDate(date: Instant, zone: UtcOffset) {
            fun Instant.format(offset: UtcOffset): String {
                with(toLocalDateTime(FixedOffsetTimeZone(offset))) {
                    val offsetInHours = offset.totalSeconds / 60 / 60
                    val sign = if (offsetInHours.sign == 1) "+" else "-"
                    // FIXME?: why is zone 4 digits. can it account for minutes as well? if so do that
                    val zone = "$sign${offsetInHours.absoluteValue.toString().padStart(2, '0').padEnd(4, '0')}"
                    val dayOfWeek = dayOfWeek.name.take(3).lowercase().capitalize()
                    val month = month.name.take(3).lowercase().capitalize()
                    val hour = hour.toString().padStart(2, '0')
                    val minute = minute.toString().padStart(2, '0')
                    val second = second.toString().padStart(2, '0')

                    return "$dayOfWeek, $dayOfMonth $month $year $hour:$minute:$second $zone"
                }
            }

            header(Headers.OriginalDate, date.format(zone))
        }

        fun messageId(messageId: String) {
            header(Headers.MessageId, messageId)
        }
    }

    class BodyBuilder {
        private var body: StringBuilder = StringBuilder()

        fun line(content: String) {
            require(content.length < 998)

            body.append(content)
            body.append("\r\n")
        }

        fun raw(content: String) {
            body.append(content)
        }

        fun build(): String {
            return body.toString()
        }
    }

    val headers: HeadersBuilder = HeadersBuilder()
    var body: String? = null

    fun headers(builder: HeadersBuilder.() -> Unit) {
        headers.apply(builder)
    }

    fun body(builder: BodyBuilder.() -> Unit) {
        body = BodyBuilder().apply(builder).build()
    }

    fun build(): Message {
        return Message(Headers(headers.toSet()), body, null)
    }
}

fun message(builder: MessageBuilder.() -> Unit): Message {
    return MessageBuilder().apply(builder).build()
}