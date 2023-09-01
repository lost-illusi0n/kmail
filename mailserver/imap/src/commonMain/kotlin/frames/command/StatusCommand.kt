package dev.sitar.kmail.imap.frames.command

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.imap.readList
import dev.sitar.kmail.imap.readValue
import dev.sitar.kmail.utils.io.readUtf8UntilLineEnd

class StatusCommand(val mailbox: String, val items: Set<StatusDataItem>) : ImapCommand {
    override val identifier: ImapCommand.Identifier = ImapCommand.Identifier.Status

    companion object: ImapCommandSerializer<StatusCommand> {
        override suspend fun deserialize(input: AsyncReader): StatusCommand {
            val name = input.readValue(isEnd = false)
            val items = input.readList()

            input.readUtf8UntilLineEnd()

            return StatusCommand(name, items.map { StatusDataItem.fromName(it) }.toSet())
        }
    }
}

enum class StatusDataItem {
    Messages,
    Recent,
    UidNext,
    UidValidity,
    Unseen;

    companion object {
        fun fromName(name: String): StatusDataItem {
            return values().find { it.name.contentEquals(name, ignoreCase = true) }!!
        }
    }
}