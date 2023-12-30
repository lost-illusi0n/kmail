package dev.sitar.kmail.imap.frames.command

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.imap.Sequence
import dev.sitar.kmail.utils.io.readUtf8StringUntil

data class UidCommand(val command: ImapCommand): ImapCommand {
    override val identifier: ImapCommand.Identifier = ImapCommand.Identifier.Uid

    companion object: ImapCommandSerializer<UidCommand> {
        private suspend fun AsyncReader.readCommandIdentifier(): String {
            var lastChar = '\u0000'

            val tag = readUtf8StringUntil {
                val t = it == ' ' || (lastChar == '\r' && it == '\n')
                lastChar = it
                t
            }

            return if (lastChar == '\n') tag.dropLast(1) else tag
        }

        override suspend fun deserialize(input: AsyncReader): UidCommand {
            val identifier = input.readCommandIdentifier()

            val command = when (ImapCommand.Identifier.findByIdentifier(identifier)) {
                ImapCommand.Identifier.Fetch -> FetchCommand.deserialize(Sequence.Mode.Uid, input)
                ImapCommand.Identifier.Store -> StoreCommand.deserialize(Sequence.Mode.Uid, input)
                ImapCommand.Identifier.Copy -> CopyCommand.deserialize(Sequence.Mode.Uid, input)
                // ImapCommand.Identifier.Search
                else -> TODO("got $identifier")
            }

            return UidCommand(command)
        }
    }
}