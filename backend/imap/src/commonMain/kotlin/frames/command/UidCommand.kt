package dev.sitar.kmail.imap.frames.command

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.utils.io.readUtf8UntilLineEnd

data class UidCommand(val data: String): ImapCommand {
    override val identifier: ImapCommand.Identifier = ImapCommand.Identifier.Uid

    companion object: ImapCommandSerializer<UidCommand> {
        override suspend fun deserialize(input: AsyncReader): UidCommand {
            return UidCommand(input.readUtf8UntilLineEnd())
        }
    }
}