package dev.sitar.kmail.imap.frames.command

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.imap.Sequence
import dev.sitar.kmail.imap.frames.DataItem
import dev.sitar.kmail.utils.io.readUtf8StringUntil
import dev.sitar.kmail.utils.io.readUtf8UntilLineEnd

data class FetchCommand(val sequence: Sequence, val dataItems: List<DataItem.Fetch>) : ImapCommand {
    override val identifier: ImapCommand.Identifier = ImapCommand.Identifier.Fetch

    companion object : ImapCommandSerializer<FetchCommand> {
        suspend fun deserialize(mode: Sequence.Mode, input: AsyncReader): FetchCommand {
            val sequence = Sequence.deserialize(mode, input)

            val char = input.read()

            val items = if (char != '('.code.toByte()) {
                val identifier = input.readUtf8StringUntil { it == ' ' || it == '[' }

                val typed = DataItem.Identifier.from("${char.toInt().toChar()}$identifier") ?: TODO("unknown data item: $identifier")

                listOf(typed.fetchSerializer.deserialize(input))
            } else buildList {
                var isFinal = false

                while (!isFinal) {
                    val identifier = input.readUtf8StringUntil {
                        isFinal = it == ')'
                        isFinal || it == ' ' || it == '['
                    }

                    if (identifier == "") break

                    val typed = DataItem.Identifier.from(identifier) ?: TODO("unknown data item: $identifier")

                    add(typed.fetchSerializer.deserialize(input))
                }
            }

            if (input.readUtf8UntilLineEnd() != "") TODO("syntax error")

            return FetchCommand(sequence, items)
        }

        override suspend fun deserialize(input: AsyncReader): FetchCommand =
            deserialize(mode = Sequence.Mode.SequenceNumber, input)
    }
}
