package dev.sitar.kmail.agents.smtp.transfer

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
            withContext(Dispatchers.IO) {
                val transport = SmtpServerTransport(socket.accept())

                logger.debug { "Accepted a connection from ${transport.remote}." }

                launch {
                    val agent = TransferReceiveAgent(transport, config)
                    agent.handle()
                    agent.incoming.collect { incoming.emit(it) }
                }
            }
        }
    }
}