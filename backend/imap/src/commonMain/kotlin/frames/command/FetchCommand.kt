package dev.sitar.kmail.imap.frames.command

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.imap.SequenceSet
import dev.sitar.kmail.imap.readList
import dev.sitar.kmail.utils.io.readUtf8UntilLineEnd

data class FetchCommand(val sequenceSet: SequenceSet, val dataItems: List<String>) : ImapCommand {
    override val identifier: ImapCommand.Identifier = ImapCommand.Identifier.Fetch

    companion object : ImapCommandSerializer<FetchCommand> {
        suspend fun deserialize(mode: SequenceSet.Mode, input: AsyncReader): FetchCommand {
            val sequence = SequenceSet.deserialize(mode, input)
            val dataItems = input.readList()
            input.readUtf8UntilLineEnd()
            return FetchCommand(sequence, dataItems)
        }

        override suspend fun deserialize(input: AsyncReader): FetchCommand =
            deserialize(mode = SequenceSet.Mode.SequenceNumber, input)
    }
}
