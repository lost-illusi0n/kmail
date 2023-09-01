package dev.sitar.kmail.imap.frames.command

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.utils.io.readUtf8UntilLineEnd

class LoginCommand(val username: String, val password: String): ImapCommand {
    override val identifier: ImapCommand.Identifier = ImapCommand.Identifier.Login

    companion object: ImapCommandSerializer<LoginCommand> {
        override suspend fun deserialize(input: AsyncReader): LoginCommand {
            val (username, password) = input.readUtf8UntilLineEnd().split(' ').takeIf { it.size == 2 } ?: TODO("incorrect syntax")

            return LoginCommand(username, password)
        }
    }

    override fun toString(): String {
        return "LoginCommand(username=$username, password=$password)"
    }
}