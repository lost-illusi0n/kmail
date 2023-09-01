package dev.sitar.kmail.imap.frames.command

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.imap.readValue
import dev.sitar.kmail.utils.io.readUtf8UntilLineEnd

data class SubscribeCommand(val mailbox: String): ImapCommand {
    override val identifier: ImapCommand.Identifier = ImapCommand.Identifier.Subscribe

    companion object: ImapCommandSerializer<SubscribeCommand> {
        override suspend fun deserialize(input: AsyncReader): SubscribeCommand {
            return SubscribeCommand(input.readValue(isEnd = true))
        }
    }
}