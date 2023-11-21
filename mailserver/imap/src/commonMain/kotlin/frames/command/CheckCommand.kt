package dev.sitar.kmail.imap.frames.command

import dev.sitar.kio.async.readers.AsyncReader

object CheckCommand : ImapCommand, ImapCommandSerializer<CheckCommand> {
    override val identifier = ImapCommand.Identifier.Check

    override suspend fun deserialize(input: AsyncReader): CheckCommand {
        return CheckCommand
    }
}