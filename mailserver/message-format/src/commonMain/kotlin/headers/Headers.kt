package dev.sitar.kmail.message.headers

@JvmInline
value class Headers(private val headers: Set<Header>): Set<Header> by headers {
    companion object {
        const val From = "From"
        const val To = "To"
        const val MessageId = "Message-ID"
        const val OriginalDate = "Date"
        const val Subject = "Subject"
    }

    operator fun contains(name: String): Boolean {
        return headers.any { it.fieldName.equals(name, ignoreCase = true) }
    }

    operator fun get(name: String): Header? {
        return headers.find { it.fieldName.equals(name, ignoreCase = true) }
    }
}