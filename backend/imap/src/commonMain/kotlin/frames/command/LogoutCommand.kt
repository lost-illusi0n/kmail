package dev.sitar.kmail.imap.frames.command

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.utils.io.readUtf8UntilLineEnd

object LogoutCommand : ImapCommand, ImapCommandSerializer<LogoutCommand> {
    override val identifier = ImapCommand.Identifier.Logout

    override suspend fun deserialize(input: AsyncReader): LogoutCommand {
        return LogoutCommand
    }
}