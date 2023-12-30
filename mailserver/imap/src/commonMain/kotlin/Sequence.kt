package dev.sitar.kmail.imap

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.utils.io.readUtf8StringUntil

sealed class Sequence {
    abstract val mode: Mode

    data class Single(val pos: Position, override val mode: Mode): Sequence()

    data class Set(val start: Position, val end: Position, override val mode: Mode): Sequence()

    enum class Mode {
        Sequence,
        Uid
    }

    sealed interface Position {
        @JvmInline
        value class Actual(val pos: Int): Position
        object Any: Position

        companion object {
            fun fromString(string: String): Position {
                return when (string) {
                    "*" -> Any
                    else -> Actual(string.toInt())
                }
            }
        }
    }

    companion object {
        suspend fun deserialize(mode: Mode, input: AsyncReader): Sequence {
            val set = input.readUtf8StringUntil { it == ' ' }

            return when (val sep = set.indexOf(':')) {
                -1 -> Single(Position.fromString(set), mode)
                else -> Set(Position.fromString(set.substring(0, sep)), Position.fromString(set.substring(sep + 1)), mode)
            }
        }
    }
}