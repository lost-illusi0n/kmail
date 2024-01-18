package dev.sitar.kmail.imap

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.utils.io.readUtf8StringUntil

sealed class Sequence {
    data class Set(val selections: List<Sequence>, val mode: Mode)

    data class Single(val pos: Position): Sequence()

    data class Range(val start: Position, val end: Position): Sequence()

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
        suspend fun deserialize(mode: Mode, input: AsyncReader): Set {
            val sequence = input.readUtf8StringUntil { it == ' ' }

            val sections = buildList {
                sequence.split(',').forEach { section ->
                    when (val sep = section.indexOf(':')) {
                        -1 -> add(Single(Position.fromString(section)))
                        else -> add(Range(Position.fromString(section.substring(0, sep)), Position.fromString(section.substring(sep + 1))))
                    }
                }
            }

            return Set(sections, mode)
        }
    }
}