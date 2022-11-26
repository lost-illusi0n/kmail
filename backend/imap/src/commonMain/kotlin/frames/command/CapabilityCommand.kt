package dev.sitar.kmail.imap.frames.command

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.utils.io.readUtf8UntilLineEnd

object CapabilityCommand : ImapCommand, ImapCommandSerializer<CapabilityCommand> {
    override val identifier = ImapCommand.Identifier.Capability

    override suspend fun deserialize(input: AsyncReader): CapabilityCommand {
        return CapabilityCommand
    }
}