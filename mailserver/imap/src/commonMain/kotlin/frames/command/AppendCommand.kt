package dev.sitar.kmail.imap.frames.command

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.imap.frames.DataItem
import dev.sitar.kmail.imap.readList
import dev.sitar.kmail.imap.readValue
import dev.sitar.kmail.message.Message
import dev.sitar.kmail.utils.io.readStringUtf8
import dev.sitar.kmail.utils.io.readUtf8UntilLineEnd

data class AppendCommand(val mailbox: String, val flags: List<String>? = null, val messageSize: Int): ImapCommand {
    override val identifier: ImapCommand.Identifier = ImapCommand.Identifier.Append

    companion object: ImapCommandSerializer<AppendCommand> {
        override suspend fun deserialize(input: AsyncReader): AppendCommand {
            val mailboxName = input.readValue(isEnd = false)
            var flags: List<String>? = null


            if (input.readStringUtf8(1).contentEquals("(")) {
                flags = input.readList(checkFirst = false)
                input.discard(2)
            }

            val size = input.readUtf8UntilLineEnd().dropLast(1).toInt()

            return AppendCommand(mailboxName, flags, size)
        }
    }
}