package dev.sitar.kmail.imap

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kio.async.writers.AsyncWriter
import dev.sitar.kmail.message.Message
import dev.sitar.kmail.message.headers.Header
import dev.sitar.kmail.utils.io.writeLineEnd
import dev.sitar.kmail.utils.io.writeStringUtf8

sealed interface PartSpecifier {
    val identifier: Identifier

    enum class Identifier(val raw: String, val fetchSerializer: Fetch.Serializer<out Fetch>) {
        HeaderFields("HEADER.FIELDS", Fetch.HeaderFields),
        Header("HEADER", Fetch.Header),
        Text("TEXT", Fetch.Text);

        companion object {
            fun from(raw: String): Identifier? {
                return values().find { it.raw.contentEquals(raw, ignoreCase = true) }
            }
        }
    }

    sealed interface Fetch: PartSpecifier {
        data class HeaderFields(val specifiedFields: List<String>): Fetch {
            override val identifier: Identifier = Identifier.HeaderFields

            companion object: Serializer<HeaderFields> {
                override suspend fun deserialize(input: AsyncReader): HeaderFields {
                    return HeaderFields(input.readList())
                }
            }
        }

        object Header: Fetch, Serializer<Header> {
            override val identifier: Identifier = Identifier.Header

            override suspend fun deserialize(input: AsyncReader): Header {
                return Header
            }
        }

        object Text: Fetch, Serializer<Text> {
            override val identifier: Identifier = Identifier.Text

            override suspend fun deserialize(input: AsyncReader): Text {
                return Text
            }
        }

//        object Header: Fetch, Serializer<Header> {
//            override val identifier: Identifier = Identifier.Header
//
//            override suspend fun deserialize(input: AsyncReader): Header {
//                return Header
//            }
//        }

        interface Serializer<T: Fetch> {
            suspend fun deserialize(input: AsyncReader): T
        }
    }

    sealed interface Response {
        val isInline: Boolean

        data class HeaderFields(val matchedHeaders: List<dev.sitar.kmail.message.headers.Header>): Response {
            override val isInline: Boolean = true

            override suspend fun serializeInline(output: AsyncWriter) {
                output.writeStringUtf8("HEADER.FIELDS (${matchedHeaders.joinToString(" ") { it.fieldName }})")
            }

            override suspend fun serializeBody(output: AsyncWriter) {
                matchedHeaders.forEachIndexed { i, header ->
                    output.writeStringUtf8(header.asText())
                    if (i != matchedHeaders.size - 1) output.writeLineEnd()
                }
            }
        }

        data class Header(val headers: List<dev.sitar.kmail.message.headers.Header>): Response {
            override val isInline: Boolean
                get() = true

            override suspend fun serializeInline(output: AsyncWriter) {
                output.writeStringUtf8("HEADER")
            }

            override suspend fun serializeBody(output: AsyncWriter) {
                headers.forEachIndexed { i, header ->
                    output.writeStringUtf8(header.asText())
                    output.writeLineEnd()
                    output.writeLineEnd()
                }
            }
        }

        data class Text(val text: String): Response {
            override val isInline: Boolean
                get() = false

            override suspend fun serializeInline(output: AsyncWriter) { }

            override suspend fun serializeBody(output: AsyncWriter) {
                output.writeStringUtf8(text)
            }
        }

        data class Body(val message: Message): Response {
            override val isInline: Boolean = false

            override suspend fun serializeInline(output: AsyncWriter) { }

            override suspend fun serializeBody(output: AsyncWriter) {
                output.writeStringUtf8(message.asText())
            }
        }

        suspend fun serializeInline(output: AsyncWriter)
        suspend fun serializeBody(output: AsyncWriter)
    }
}