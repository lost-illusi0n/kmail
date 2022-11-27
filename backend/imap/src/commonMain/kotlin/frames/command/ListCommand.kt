package dev.sitar.kmail.imap.frames.command

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.imap.readValue

data class ListCommand(val referenceName: String, val mailboxName: String): ImapCommand {
    override val identifier: ImapCommand.Identifier = ImapCommand.Identifier.List

    companion object: ImapCommandSerializer<ListCommand> {
        override suspend fun deserialize(input: AsyncReader): ListCommand {
            val referenceName = input.readValue(isEnd = false)
            val mailboxName = input.readValue(isEnd = true)

            return ListCommand(referenceName, mailboxName)
        }
    }
}