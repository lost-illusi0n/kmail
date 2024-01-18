package dev.sitar.kmail.imap.frames.command

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.imap.Sequence
import dev.sitar.kmail.imap.readValue

data class CopyCommand(val sequence: Sequence.Set, val mailbox: String): ImapCommand {
    override val identifier: ImapCommand.Identifier = ImapCommand.Identifier.Copy

    companion object: ImapCommandSerializer<CopyCommand> {
        suspend fun deserialize(mode: Sequence.Mode, input: AsyncReader): CopyCommand {
            return CopyCommand(Sequence.deserialize(mode, input), input.readValue(isEnd = true))
        }

        override suspend fun deserialize(input: AsyncReader): CopyCommand =
            deserialize(mode = Sequence.Mode.Sequence, input)
    }
}