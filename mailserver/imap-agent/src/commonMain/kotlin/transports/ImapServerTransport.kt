package dev.sitar.kmail.imap.agent.transports

import dev.sitar.kmail.imap.agent.ImapCommandContext
import dev.sitar.kmail.imap.agent.ImapCommandPipeline
import dev.sitar.kmail.imap.agent.io.asImapServerReader
import dev.sitar.kmail.imap.agent.io.asImapServerWriter
import dev.sitar.kmail.imap.frames.command.ImapCommand
import dev.sitar.kmail.imap.frames.response.ImapResponse
import dev.sitar.kmail.imap.frames.response.TaggedImapResponse
import dev.sitar.kmail.utils.connection.Connection
import dev.sitar.kmail.utils.io.readUtf8UntilLineEnd
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

class ImapServerTransport(var connection: Connection) {
    val remote: String get() = connection.remote
    val isSecure: Boolean get() = connection.isSecure
    
    private var reader = connection.reader.asImapServerReader()
    private var writer = connection.writer.asImapServerWriter()

    val commandPipeline = ImapCommandPipeline()

    suspend fun startPipeline() = coroutineScope {
        while (isActive && reader.openForRead) {
            val command = reader.readCommand()
            val context = ImapCommandContext(command, false)
            commandPipeline.process(context)
        }
    }

    suspend fun readData(): String {
        return reader.readUtf8UntilLineEnd()
    }

    suspend fun send(response: TaggedImapResponse) {
        logger.trace { ">>> $response"}
        writer.writeResponse(response)
    }

    suspend fun secure() {
        if (isSecure) return

        connection = connection.secureAsServer()
        updateReaderWriter()
    }

    private fun updateReaderWriter() {
        reader = connection.reader.asImapServerReader()
        writer = connection.writer.asImapServerWriter()
    }
}