package dev.sitar.kmail.smtp.frames.reply

import dev.sitar.kio.async.writers.AsyncWriter
import dev.sitar.kmail.utils.todo

public sealed interface SmtpReplyCode {
    public val code: Int

    @JvmInline
    public value class PositiveCompletion(override val code: Int): SmtpReplyCode {
        init {
            require(code in 200..299)
        }
    }

    @JvmInline
    public value class PositiveIntermediate(override val code: Int): SmtpReplyCode {
        init {
            require(code in 300..399)
        }
    }

    @JvmInline
    public value class TransientNegative(override val code: Int): SmtpReplyCode {
        init {
            require(code in 400..499)
        }
    }

    @JvmInline
    public value class PermanentNegative(override val code: Int): SmtpReplyCode {
        init {
            require(code in 500..599)
        }
    }

    public companion object {
        public fun from(code: Int): SmtpReplyCode {
            return when (code) {
                in 200..299 -> PositiveCompletion(code)
                in 300..399 -> PositiveIntermediate(code)
                in 400..499 -> TransientNegative(code)
                in 500..599 -> PermanentNegative(code)
                else -> error("code: $code")
            }
        }
    }
}

public sealed interface SmtpReply {
    public val code: SmtpReplyCode

    public suspend fun serialize(): Raw

    public data class Raw(override val code: SmtpReplyCode, val lines: List<String>): SmtpReply {
        public suspend fun serialize(output: AsyncWriter) {
            todo("Not yet implemented")
        }

        override suspend fun serialize(): Raw {
            return this
        }
    }
}

public sealed interface Either<out L, out R> {
    public data class Left<L>(public val left: L): Either<L, Nothing>

    public data class Right<R>(public val right: R): Either<Nothing, R>
}

public infix fun <T: SmtpReply> SmtpReply.Raw.deserializeAs(deserializer: SmtpReplyDeserializer<out T>): Either<SmtpReply, T> {
    return deserializer.deserialize(this)?.let { Either.Right(it) } ?: Either.Left(this)
}

public sealed interface SmtpReplyDeserializer<T: SmtpReply> {
    public fun deserialize(raw: SmtpReply.Raw): T?
}

internal fun String?.coerceAndSpace(): String {
    return this?.let { " $it" } ?: ""
}