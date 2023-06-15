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
class SmtpServerTransport(connection: Connection, coroutineContext: CoroutineContext = EmptyCoroutineContext) {
    val connection = MutableStateFlow(connection)

    val remote: String get() = connection.value.remote

    val isSecure: Boolean get() = connection.value.isSecure

    val commandPipeline = SmtpCommandPipeline()

    private var reader: AsyncSmtpServerReader = connection.reader.asAsyncSmtpServerReader()
    private var writer: AsyncSmtpServerWriter = connection.writer.asAsyncSmtpServerWriter()
    val scope = CoroutineScope(Job() + coroutineContext)

    private val lock = Mutex(true)

    init {
        this.connection.onEach {
            reader = it.reader.asAsyncSmtpServerReader()
            writer = it.writer.asAsyncSmtpServerWriter()
            lock.unlock()
        }.launchIn(scope)

        scope.launch {
            while (isActive) {
                val command = lock.withLock { reader.readSmtpCommand() }

                val context = SmtpCommandContext(command, true)

                commandPipeline.process(context)
            }
        }
    }

    suspend fun send(reply: SmtpReply<*>) {
        logger.trace { "TO ${connection.value.remote}: $reply" }
        writer.writeReply(reply)
    }

    suspend fun recvMail(): Message {
        val mail = reader.readMailInput()
        logger.trace { "FROM ${connection.value.remote}: $mail" }
        return mail
    }

    suspend fun secure() {
        if (isSecure) return

        lock.lock()
        connection.emit(connection.value.secureAsServer())
    }

    fun close() {
        connection.value.close()
        scope.cancel()
    }
}