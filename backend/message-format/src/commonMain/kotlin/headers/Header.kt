package dev.sitar.kmail.message.headers

import dev.sitar.kio.buffers.SequentialReader
import dev.sitar.kmail.message.*
import dev.sitar.kmail.message.readUtf8StringUntil

public data class Header(val fieldName: String, val fieldBody: String) {
    public companion object {
        public fun fromText(header: String): Header {
            val separator = header.indexOf(':')
            if (separator == -1) error("invalid header")

            val name = header.substring(0, separator)
            val body = header.substring(separator + 1).trim()

            return Header(name, body)
        }
    }

    fun asText(): String {
        return "$fieldName: $fieldBody"
    }
}
