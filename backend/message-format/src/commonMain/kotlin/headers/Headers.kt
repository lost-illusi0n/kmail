package dev.sitar.kmail.message.headers

@JvmInline
public value class Headers(private val headers: Set<Header>): Set<Header> by headers {
    public companion object {
        public const val From = "From"
        public const val To = "To"
        public const val MessageId = "Message-ID"
        public const val OriginalDate = "Date"
        public const val Subject = "Subject"
    }

    public operator fun contains(name: String): Boolean {
        return headers.any { it.fieldName.equals(name, ignoreCase = true) }
    }

    public operator fun get(name: String): Header? {
        return headers.find { it.fieldName.equals(name, ignoreCase = true) }
    }
}