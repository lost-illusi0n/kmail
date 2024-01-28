package dev.sitar.kmail.agents.smtp.transfer

import dev.sitar.kmail.agents.smtp.connections.ServerSink
import dev.sitar.kmail.agents.smtp.transports.server.SmtpServerTransport
import dev.sitar.kmail.smtp.InternetMessage
import dev.sitar.kmail.utils.server.ServerSocket
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableSharedFlow
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

class TransferReceiveServer(
    val socket: ServerSocket,
    val config: TransferReceiveConfig,
    val incoming: MutableSharedFlow<InternetMessage>
) {
    suspend fun listen() = supervisorScope {
        logger.info("SMTP receive server is listening.")

        while (isActive) {
            val transport = SmtpServerTransport(socket.accept())

            launch(Dispatchers.IO) {
                logger.debug { "Accepted a connection from ${transport.remote}." }

                try {
                    TransferReceiveAgent(transport, config).handleAndPipeTo { incoming.emit(it) }
                } catch (e: Exception) {
                    logger.error(e) { "SMTP session (${transport.connection.remote} encountered an exception." }
                }

                logger.debug { "SMTP session (${transport.connection.remote} completed." }
            }
        }
    }
}