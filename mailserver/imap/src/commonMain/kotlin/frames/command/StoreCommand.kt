package dev.sitar.kmail.imap.frames.command

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.imap.Sequence
import dev.sitar.kmail.imap.readList
import dev.sitar.kmail.utils.io.readUtf8StringUntil
import java.lang.Exception

data class StoreCommand(val sequence: Sequence, val item: StoreDataItem): ImapCommand {
    override val identifier: ImapCommand.Identifier = ImapCommand.Identifier.Append

    companion object: ImapCommandSerializer<StoreCommand> {
        suspend fun deserialize(mode: Sequence.Mode, input: AsyncReader): StoreCommand {
            val sequence = Sequence.deserialize(mode, input)

            val rawName = input.readUtf8StringUntil { it == ' ' }
            val flags = input.readList()

            val parts = rawName.split('.')

            val mode: StoreMode = when (parts[0]) {
                "FLAGS" -> StoreMode.Set
                "+FLAGS" -> StoreMode.Add
                "-FLGS" -> StoreMode.Remove
                else -> throw Exception("could not parse store mode ${parts[0]}")
            }

            val silent = parts.getOrNull(1)?.lowercase()?.toBooleanStrictOrNull() ?: false

            return StoreCommand(sequence, StoreDataItem(mode, silent, flags))
        }

        override suspend fun deserialize(input: AsyncReader): StoreCommand =
            deserialize(mode = Sequence.Mode.Sequence, input)
    }
}

enum class StoreMode {
    Set,
    Add,
    Remove;
}

data class StoreDataItem(val mode: StoreMode, var silent: Boolean, val flags: List<String>)
