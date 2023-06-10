package dev.sitar.kmail.message

import dev.sitar.kio.buffers.asBuffer
import dev.sitar.kmail.message.headers.Header
import dev.sitar.kmail.message.headers.Headers

data class Message(
    val headers: Headers,
    val body: String?
) {
    companion object {
        fun fromText(raw: String): Message {
            val buffer = raw.encodeToByteArray().asBuffer()
            println(raw)
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

            return Message(Headers(headers), body)
        }
    }

    fun asText(): String {
        val textHeaders = headers.joinToString("\r\n") { it.asText() } + "\r\n"
        return textHeaders + if (body != null) "\r\n$body" else ""
    }

    override fun toString(): String {
        return "Message(headers=$headers, body=${body?.replace("\r\n", "\\r\\n")})"
    }
}

class MessageBuilder {
    class HeadersBuilder: MutableSet<Header> by mutableSetOf() {
        operator fun Header.unaryPlus() {
            add(this)
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
        return Message(Headers(headers.toSet()), body)
    }
}

fun message(builder: MessageBuilder.() -> Unit): Message {
    return MessageBuilder().apply(builder).build()
}