package dev.sitar.kmail.imap.frames.command

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.imap.readList
import dev.sitar.kmail.imap.readValue
import dev.sitar.kmail.utils.todo

sealed interface NotifyCommand: ImapCommand {
    override val identifier: ImapCommand.Identifier get() = ImapCommand.Identifier.Notify

    class Events(val status: Boolean, val events: List<String>): NotifyCommand
    object None: NotifyCommand

    companion object: ImapCommandSerializer<NotifyCommand> {
        override suspend fun deserialize(input: AsyncReader): NotifyCommand {
            when (input.readChar().lowercase()) {
                "s" -> { // set
                    require(input.readValue(isEnd = false).contentEquals("et", ignoreCase = true))

                    var status: Boolean = false
                    var events: List<String>

                    when (input.readChar().lowercase()) {
                        "(" -> events = input.readList()
                        "s" -> { // status
                            require(input.readValue(isEnd = false).contentEquals("tatus", ignoreCase = true))
                            status = true
                            events = input.readList(checkFirst = false)
                        }
                        else -> todo()
                    }

                    return Events(status, events)
                }
                "n" -> { // none
                    require(input.readValue(isEnd = true).contentEquals("one", ignoreCase = true))

                    return None
                }
                else -> todo()
            }
        }
    }
}