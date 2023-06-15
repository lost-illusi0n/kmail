package dev.sitar.kmail.imap.frames.command

import dev.sitar.kmail.imap.frames.Tag

data class TaggedImapCommand(val tag: Tag, val command: ImapCommand)

operator fun Tag.plus(command: ImapCommand): TaggedImapCommand {
    return TaggedImapCommand(this, command)
}