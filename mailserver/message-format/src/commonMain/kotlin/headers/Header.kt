package dev.sitar.kmail.message.headers

data class Header(val fieldName: String, val fieldBody: String) {
    companion object {
        fun fromText(header: String): Header {
            val separator = header.indexOf(':')
            if (separator == -1) error("invalid header: $header")

            val name = header.substring(0, separator)
            val body = header.substring(separator + 1).trim()

            return Header(name, body)
        }
    }

    fun asText(): String {
        return "$fieldName: $fieldBody"
    }
}
