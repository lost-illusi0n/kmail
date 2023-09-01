package dev.sitar.kmail.imap.frames.command

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.sasl.SaslMechanism
import dev.sitar.kmail.utils.io.readUtf8UntilLineEnd

class AuthenticateCommand(val mechanism: SaslMechanism) : ImapCommand {
    override val identifier: ImapCommand.Identifier = ImapCommand.Identifier.Authenticate

    companion object: ImapCommandSerializer<AuthenticateCommand> {
        override suspend fun deserialize(input: AsyncReader): AuthenticateCommand {
            return AuthenticateCommand(SaslMechanism.byMechanism(input.readUtf8UntilLineEnd()))
        }
    }
}