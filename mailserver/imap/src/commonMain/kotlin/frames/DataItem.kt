package dev.sitar.kmail.imap.frames

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kio.async.writers.AsyncWriter
import dev.sitar.kio.async.writers.toAsyncWriter
import dev.sitar.kio.buffers.DefaultBufferPool
import dev.sitar.kio.use
import dev.sitar.kmail.imap.PartSpecifier
import dev.sitar.kmail.utils.io.readUtf8StringUntil
import dev.sitar.kmail.utils.io.writeStringUtf8
import dev.sitar.kmail.utils.todo

sealed interface DataItem {
    enum class Identifier(val raw: String, val fetchSerializer: Fetch.Serializer<out Fetch>, ) {
        Flags("Flags", Fetch.Flags),
        Uid("UID", Fetch.Uid),
        Rfc822Size("RFC822.SIZE", Fetch.Rfc822Size),
        BodyPeek("BODY.PEEK", Fetch.BodyPeek),
        Body("BODY", Fetch.Body);

        companion object {
            fun from(raw: String): Identifier? {
                return values().find { it.raw.contentEquals(raw, ignoreCase = true) }
            }
        }
    }

    sealed interface Fetch: DataItem {
        object Flags: Fetch, Serializer<Flags> {
            override suspend fun deserialize(input: AsyncReader): Flags {
                return Flags
            }
        }

        object Uid: Fetch, Serializer<Uid> {
            override suspend fun deserialize(input: AsyncReader): Uid {
                return Uid
            }
        }

        object Rfc822Size: Fetch, Serializer<Rfc822Size> {
            override suspend fun deserialize(input: AsyncReader): Rfc822Size {
                return Rfc822Size
            }
        }

        sealed interface BodyType: Fetch {
            val parts: List<PartSpecifier.Fetch>
        }

        data class Body(override val parts: List<PartSpecifier.Fetch>): Fetch, BodyType {
            companion object: Serializer<Body> {
                override suspend fun deserialize(input: AsyncReader): Body {
                    val parts = buildList {
                        while (true) {
                            val identifier = input.readUtf8StringUntil { it == ' ' || it == ']' }

                            if (identifier == "") break

                            val typed = PartSpecifier.Identifier.from(identifier) ?: todo("unknown identifier: $identifier")

                            add(typed.fetchSerializer.deserialize(input))
                        }
                    }

                    return Body(parts)
                }
            }
        }

        data class BodyPeek(val body: Body): Fetch, BodyType by body {
            companion object: Serializer<BodyPeek> {
                override suspend fun deserialize(input: AsyncReader): BodyPeek {
                    return BodyPeek(Body.deserialize(input))
                }
            }
        }

        interface Serializer<T: DataItem> {
            suspend fun deserialize(input: AsyncReader): T
        }
    }

    sealed interface Response: DataItem {
        data class Flags(val flags: List<String>): Response {
            override suspend fun serialize(output: AsyncWriter) {
                output.writeStringUtf8("FLAGS (${flags.joinToString(" ")})")
            }
        }

        data class Uid(val uid: String): Response {
            override suspend fun serialize(output: AsyncWriter) {
                output.writeStringUtf8("UID $uid")
            }
        }

        data class Rfc822Size(val size: Long): Response {
            override suspend fun serialize(output: AsyncWriter) {
                output.writeStringUtf8("RFC822.SIZE $size")
            }
        }

        data class Body(val part: PartSpecifier.Response): Response {
            override suspend fun serialize(output: AsyncWriter) {
                output.writeStringUtf8("BODY[")

                part.serializeInline(output)

                output.writeStringUtf8("]")

                DefaultBufferPool.use(32) {
                    // we first serialize into a temp buffer as to know the size of the part. we then use that as the origin octet
                    part.serializeBody(it.toAsyncWriter())

                    if (it.writeIndex > 0) {
                        output.writeStringUtf8(" {${it.writeIndex}}\r\n")
                        output.writeBytes(it.fullSlice())
                    }
                }
            }
        }

        suspend fun serialize(output: AsyncWriter)
    }
}