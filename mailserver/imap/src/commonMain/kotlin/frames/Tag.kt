package dev.sitar.kmail.imap.frames

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kio.async.writers.AsyncWriter
import dev.sitar.kmail.utils.io.readUtf8StringUntil
import dev.sitar.kmail.utils.io.writeStringUtf8

sealed interface Tag {
    object None: Tag {
        override suspend fun serialize(output: AsyncWriter) { }

        override fun toString(): String {
            return "None"
        }
    }

    object Untagged: Tag {
        override suspend fun serialize(output: AsyncWriter) {
            output.writeStringUtf8("* ")
        }

        override fun toString(): String {
            return "Untagged"
        }
    }

    @JvmInline
    value class Identifier(val identifier: String): Tag {
        override suspend fun serialize(output: AsyncWriter) {
            output.writeStringUtf8("$identifier ")
        }
    }

    suspend fun serialize(output: AsyncWriter)

    companion object {
        suspend fun deserialize(input: AsyncReader): Tag {
            return when (val tag = input.readUtf8StringUntil { it == ' ' }) {
                "*" -> Untagged
                else -> Identifier(tag)
            }
        }
    }
}