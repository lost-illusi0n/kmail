package dev.sitar.kmail.agents.smtp.transports.client

import dev.sitar.kmail.message.Message
import dev.sitar.kmail.smtp.SmtpCommand
import dev.sitar.kmail.smtp.frames.reply.SmtpReply
import dev.sitar.kmail.smtp.io.smtp.reader.asAsyncSmtpClientReader
import dev.sitar.kmail.smtp.io.smtp.writer.asAsyncSmtpClientWriter
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
class SmtpClientTransport(connection: Connection, coroutineContext: CoroutineContext = EmptyCoroutineContext) {
    val connection = MutableStateFlow(connection)

    val scope = CoroutineScope(Job() + coroutineContext)

    private val lock = Mutex(true)

    init {
        this.connection.onEach {
            reader = it.reader.asAsyncSmtpClientReader()
            writer = it.writer.asAsyncSmtpClientWriter()
            lock.unlock()
        }.launchIn(scope)
    }

    val isSecure: Boolean get() = connection.value.isSecure

    private var writer = connection.writer.asAsyncSmtpClientWriter()
    private var reader = connection.reader.asAsyncSmtpClientReader()

    suspend fun send(command: SmtpCommand) = lock.withLock {
        logger.trace { "TO ${connection.value.remote}: $command" }
        writer.writeCommand(command)
    }

    suspend fun sendMessage(message: Message) {
        logger.trace { "TO ${connection.value.remote}: $message" }
        writer.writeStringUtf8(message.asText())
        writer.write('.'.code.toByte())
        writer.writeLineEnd()

        writer.flush()
    }

    suspend fun recv(): SmtpReply.Raw {
        val reply = lock.withLock { reader.readSmtpReply() }
        logger.trace { "FROM ${connection.value.remote}: $reply" }
        return reply
    }

    suspend fun secure() {
        if (isSecure) return

        lock.lock()

        try {
            connection.emit(connection.value.secureAsClient())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun close() {
        connection.value.close()
        scope.cancel()
    }
}