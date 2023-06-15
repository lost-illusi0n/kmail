package dev.sitar.kmail.agents.pop3.transports

import dev.sitar.kmail.agents.pop3.Pop3CommandContext
import dev.sitar.kmail.agents.pop3.Pop3CommandPipeline
import dev.sitar.kmail.pop3.io.asPop3ServerReader
import dev.sitar.kmail.pop3.io.asPop3ServerWriter
import dev.sitar.kmail.pop3.replies.Pop3Reply
import dev.sitar.kmail.utils.connection.Connection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

class Pop3ServerTransport(val connection: Connection) {
    val remote: String get() = connection.remote

    private val reader = connection.reader.asPop3ServerReader()
    private val writer = connection.writer.asPop3ServerWriter()

    val commandPipeline = Pop3CommandPipeline()

    suspend fun startPipeline() = coroutineScope {
        while (isActive && reader.openForRead) {
            val command = reader.readCommand()
            val context = Pop3CommandContext(command, false)
            commandPipeline.process(context)
        }
    }

    suspend fun sendReply(reply: Pop3Reply) {
        logger.debug { ">>> $reply" }
        writer.writeReply(reply)
    }
}