package dev.sitar.kmail.imap.agent.io

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.imap.frames.Tag
import dev.sitar.kmail.imap.frames.command.ImapCommand
import dev.sitar.kmail.imap.frames.command.TaggedImapCommand
import dev.sitar.kmail.imap.frames.command.plus
import dev.sitar.kmail.utils.io.readUtf8StringUntil

class ImapServerReader(val reader: AsyncReader) : AsyncReader by reader {
    private suspend fun readCommandIdentifier(): String {
        var lastChar = '\u0000'

        val tag = readUtf8StringUntil {
            val t = it == ' ' || (lastChar == '\r' && it == '\n')
            lastChar = it
            t
        }

        return if (lastChar == '\n') tag.dropLast(1) else tag
    }

    suspend fun readCommand(): TaggedImapCommand {
        val tag = Tag.deserialize(this)

        val identRaw = readCommandIdentifier()
        val identifier = ImapCommand.Identifier.findByIdentifier(identRaw) ?: throw UnknownCommandException(identRaw)

        return tag + identifier.serializer.deserialize(this)
    }
}

class UnknownCommandException(val identifier: String) : Exception("Unable to deserialize the command $identifier.")

fun AsyncReader.asImapServerReader(): ImapServerReader {
    return ImapServerReader(this)
}