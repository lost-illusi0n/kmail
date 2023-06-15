package dev.sitar.kmail.imap.agent.transports

import dev.sitar.kmail.imap.frames.command.TaggedImapCommand
import dev.sitar.kmail.imap.frames.response.TaggedImapResponse

interface ImapTransport {
    val remote: String

    val isClosed: Boolean
    val isUpgraded: Boolean

    suspend fun upgrade(): ImapTransport

    suspend fun send(response: TaggedImapResponse)

    suspend fun recv(): TaggedImapCommand

    fun close()
}