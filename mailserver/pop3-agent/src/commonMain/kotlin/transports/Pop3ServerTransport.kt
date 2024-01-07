package dev.sitar.kmail.agents.pop3.transports

import dev.sitar.kmail.agents.pop3.Pop3CommandContext
import dev.sitar.kmail.agents.pop3.Pop3CommandPipeline
import dev.sitar.kmail.pop3.io.asPop3ServerReader
import dev.sitar.kmail.pop3.io.asPop3ServerWriter
import dev.sitar.kmail.pop3.replies.Pop3Reply
import dev.sitar.kmail.utils.connection.Connection
import kotlinx.coroutines.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

class Pop3ServerTransport(val connection: Connection) {
    val remote: String get() = connection.remote

    private val reader = connection.reader.asPop3ServerReader()
    private val writer = connection.writer.asPop3ServerWriter()

    val commandPipeline = Pop3CommandPipeline()

    suspend fun startPipeline() = coroutineScope {
        while (isActive && reader.openForRead) {
            try {
                val command = reader.readCommand()
                val context = Pop3CommandContext(command, false)
                commandPipeline.process(context)
            } catch (e: Exception) {
                logger.error(e) { "pop3 transport stream encountered exception." }

                cancel()
            }
        }
    }

    suspend fun sendReply(reply: Pop3Reply) {
        logger.debug { ">>> $reply" }
        writer.writeReply(reply)
    }
}