package dev.sitar.kmail.imap.frames.command

import dev.sitar.kio.async.readers.AsyncReader

object NoOpCommand : ImapCommand, ImapCommandSerializer<NoOpCommand> {
    override val identifier = ImapCommand.Identifier.Noop

    override suspend fun deserialize(input: AsyncReader): NoOpCommand {
        return NoOpCommand
    }
}