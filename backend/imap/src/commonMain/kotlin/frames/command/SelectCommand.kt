package dev.sitar.kmail.imap.frames.command

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.imap.readValue
import dev.sitar.kmail.utils.io.readUtf8UntilLineEnd

data class SelectCommand(val mailboxName: String): ImapCommand {
    override val identifier = ImapCommand.Identifier.Select

    companion object: ImapCommandSerializer<SelectCommand> {
        override suspend fun deserialize(input: AsyncReader): SelectCommand {
            return SelectCommand(input.readValue(isEnd = true))
        }
    }
}