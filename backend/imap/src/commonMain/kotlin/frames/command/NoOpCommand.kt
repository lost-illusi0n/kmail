package dev.sitar.kmail.imap.frames.command

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.utils.io.readUtf8UntilLineEnd

object NoOpCommand : ImapCommand, ImapCommandSerializer<NoOpCommand> {
    override val identifier = ImapCommand.Identifier.Noop

    override suspend fun deserialize(input: AsyncReader): NoOpCommand {
        return NoOpCommand
    }
}