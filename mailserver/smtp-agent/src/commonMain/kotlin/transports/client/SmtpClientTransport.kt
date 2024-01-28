package dev.sitar.kmail.agents.smtp.transports.client

import dev.sitar.kmail.message.Message
import dev.sitar.kmail.smtp.SmtpCommand
import dev.sitar.kmail.smtp.frames.reply.SmtpReply
import dev.sitar.kmail.smtp.io.smtp.reader.asAsyncSmtpClientReader
import dev.sitar.kmail.smtp.io.smtp.reader.asAsyncSmtpServerReader
import dev.sitar.kmail.smtp.io.smtp.writer.asAsyncSmtpClientWriter
import dev.sitar.kmail.smtp.io.smtp.writer.asAsyncSmtpServerWriter
import dev.sitar.kmail.utils.connection.Connection
import dev.sitar.kmail.utils.io.writeLineEnd
import dev.sitar.kmail.utils.io.writeStringUtf8
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private val logger = KotlinLogging.logger { }

// direct connection client -> server
class SmtpClientTransport(var connection: Connection) {
    val remote: String get() = connection.remote

    val isSecure: Boolean get() = connection.isSecure

    private var writer = connection.writer.asAsyncSmtpClientWriter()
    private var reader = connection.reader.asAsyncSmtpClientReader()

    suspend fun send(command: SmtpCommand) {
        logger.trace { "SMTP ($remote) >>> $command" }
        writer.writeCommand(command)
    }

    suspend fun sendMessage(message: Message) {
        logger.trace { "SMTP ($remote) >>> $message" }
        writer.writeStringUtf8(message.asText())
        writer.write('.'.code.toByte())
        writer.writeLineEnd()

        writer.flush()
    }

    suspend fun recv(): SmtpReply.Raw {
        val reply = reader.readSmtpReply()
        logger.trace { "SMTP ($remote) <<< $reply" }
        return reply
    }

    suspend fun secure() {
        if (isSecure) return

        connection = connection.secureAsClient()
        updateReaderWriter()
    }

    private fun updateReaderWriter() {
        writer = connection.writer.asAsyncSmtpClientWriter()
        reader = connection.reader.asAsyncSmtpClientReader()
    }
}