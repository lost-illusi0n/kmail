package dev.sitar.kmail.imap.frames.command

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.imap.readValue
import dev.sitar.kmail.utils.io.readUtf8UntilLineEnd

data class UnsubscribeCommand(val mailbox: String): ImapCommand {
    override val identifier: ImapCommand.Identifier = ImapCommand.Identifier.Unsubscribe

    companion object: ImapCommandSerializer<UnsubscribeCommand> {
        override suspend fun deserialize(input: AsyncReader): UnsubscribeCommand {
            return UnsubscribeCommand(input.readValue(isEnd = true))
        }
    }
}