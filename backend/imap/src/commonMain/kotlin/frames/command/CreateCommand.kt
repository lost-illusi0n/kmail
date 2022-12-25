package dev.sitar.kmail.imap.frames.command

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.imap.readValue
import dev.sitar.kmail.utils.io.readUtf8UntilLineEnd

data class CreateCommand(val mailboxName: String) : ImapCommand {
    override val identifier: ImapCommand.Identifier = ImapCommand.Identifier.Create
    companion object: ImapCommandSerializer<CreateCommand> {
        override suspend fun deserialize(input: AsyncReader): CreateCommand {
            return CreateCommand(input.readValue(isEnd = true))
        }
    }
}