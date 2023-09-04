package dev.sitar.kmail.imap.frames.command

import dev.sitar.kio.async.readers.AsyncReader

object IdleCommand : ImapCommand, ImapCommandSerializer<IdleCommand> {
    override val identifier: ImapCommand.Identifier = ImapCommand.Identifier.Idle

    override suspend fun deserialize(input: AsyncReader): IdleCommand {
        return IdleCommand
    }
}