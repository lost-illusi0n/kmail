package dev.sitar.kmail.imap

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.utils.io.readUtf8StringUntil

data class SequenceSet(val start: Position, val end: Position, val mode: Mode) {
    enum class Mode {
        SequenceNumber,
        Uid
    }

    sealed interface Position {
        class Actual(val pos: Int): Position
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
        suspend fun deserialize(mode: Mode, input: AsyncReader): SequenceSet {
            val start = input.readUtf8StringUntil { it == ':' }
            val end = input.readUtf8StringUntil { it == ' ' }

            return SequenceSet(Position.fromString(start), Position.fromString(end), mode)
        }
    }
}