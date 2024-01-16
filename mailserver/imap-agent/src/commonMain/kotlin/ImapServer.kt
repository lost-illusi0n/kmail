package dev.sitar.kmail.imap.agent

import dev.sitar.kmail.imap.agent.transports.ImapServerTransport
import dev.sitar.kmail.utils.ExceptionLoggingCoroutineExceptionHandler
import dev.sitar.kmail.utils.server.ServerSocket
import kotlinx.coroutines.*
import mu.KotlinLogging
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private val logger = KotlinLogging.logger { }

data class ImapConfig(
    val allowInsecurePassword: Boolean
)

class ImapServer(
    val socket: ServerSocket,
    val layer: ImapLayer,
    val config: ImapConfig
) {
    suspend fun listen() = supervisorScope {
        logger.debug { "IMAP server is listening." }

        while (isActive) {
            val transport = ImapServerTransport(socket.accept())

            launch(Dispatchers.IO) {
                logger.debug { "Accepted a connection from ${transport.remote}" }

                try {
                    ImapAgent(transport, layer, config).handle()
                } catch (e: Exception) {
                    logger.error(e) { "IMAP session encountered an exception." }
                }

                logger.debug { "IMAP session completed." }
            }
        }
    }
}