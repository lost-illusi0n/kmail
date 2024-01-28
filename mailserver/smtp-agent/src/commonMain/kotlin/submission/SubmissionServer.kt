package dev.sitar.kmail.agents.smtp.submission

import dev.sitar.kmail.agents.smtp.transfer.OutgoingMessageQueue
import dev.sitar.kmail.agents.smtp.transports.server.SmtpServerTransport
import dev.sitar.kmail.smtp.InternetMessage
import dev.sitar.kmail.utils.server.ServerSocket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

class SubmissionServer(
    val socket: ServerSocket,
    val config: SubmissionConfig,
    val outgoing: OutgoingMessageQueue
) {
    suspend fun listen() = supervisorScope {
        logger.debug { "SMTP submission server is listening." }

        while (isActive) {
            val transport = SmtpServerTransport(socket.accept())

            launch(Dispatchers.IO) {
                logger.debug { "Accepted a connection from ${transport.remote}." }

                try {
                    SubmissionAgent(transport, config).handleAndPipeTo { outgoing.send(it) }
                } catch (e: Exception) {
                    logger.error(e) { "SMTP session (${transport.connection.remote} encountered an exception." }
                }

                logger.debug { "SMTP session (${transport.connection.remote} completed." }
            }
        }
    }
}