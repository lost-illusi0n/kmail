package dev.sitar.kmail.agents.smtp.transports.server

import dev.sitar.kmail.message.Message
import dev.sitar.kmail.smtp.frames.replies.SmtpReply
import dev.sitar.kmail.smtp.io.smtp.reader.AsyncSmtpServerReader
import dev.sitar.kmail.smtp.io.smtp.reader.asAsyncSmtpServerReader
import dev.sitar.kmail.smtp.io.smtp.writer.AsyncSmtpServerWriter
import dev.sitar.kmail.smtp.io.smtp.writer.asAsyncSmtpServerWriter
import dev.sitar.kmail.utils.connection.Connection
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private val logger = KotlinLogging.logger { }

// direct connection server -> client
class SmtpServerTransport(var connection: Connection) {
    val remote: String get() = connection.remote

    val isSecure: Boolean get() = connection.isSecure

    private var reader: AsyncSmtpServerReader = connection.reader.asAsyncSmtpServerReader()
    private var writer: AsyncSmtpServerWriter = connection.writer.asAsyncSmtpServerWriter()

    val commandPipeline = SmtpCommandPipeline()

    suspend fun startPipeline() = coroutineScope {
        while (isActive && reader.openForRead) {
            try {
                val command = reader.readSmtpCommand()
                val context = SmtpCommandContext(command, false)

                commandPipeline.process(context)
            } catch (e: Exception) {
                logger.error(e) { "SMTP transport stream encountered an exception." }

                cancel()
                break
            }
        }
    }

    suspend fun send(reply: SmtpReply<*>) {
        logger.trace { "SMTP ($remote) >>> $reply" }
        writer.writeReply(reply)
    }

    suspend fun recvMail(): Message {
        val mail = reader.readMailInput()
        logger.trace { "SMTP ($remote) <<< $mail" }
        return mail
    }

    suspend fun secure() {
        if (isSecure) return

        connection = connection.secureAsServer()
        updateReaderWriter()
    }

    private fun updateReaderWriter() {
        reader = connection.reader.asAsyncSmtpServerReader()
        writer = connection.writer.asAsyncSmtpServerWriter()
    }
}