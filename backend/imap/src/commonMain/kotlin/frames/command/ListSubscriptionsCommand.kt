package dev.sitar.kmail.imap.frames.command

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.imap.readValue

data class ListSubscriptionsCommand(val referenceName: String, val mailboxName: String): ImapCommand {
    override val identifier: ImapCommand.Identifier = ImapCommand.Identifier.Lsub

    companion object: ImapCommandSerializer<ListSubscriptionsCommand> {
        override suspend fun deserialize(input: AsyncReader): ListSubscriptionsCommand {
            val referenceName = input.readValue(isEnd = false)
            val mailboxName = input.readValue(isEnd = true)

            return ListSubscriptionsCommand(referenceName, mailboxName)
        }
    }
}