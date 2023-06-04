package dev.sitar.kmail.agents.smtp.transfer

import dev.sitar.kmail.agents.smtp.transports.server.SmtpServerTransport
import dev.sitar.kmail.smtp.InternetMessage
import dev.sitar.kmail.utils.server.ServerSocket
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

class TransferReceiveServer(
    val socket: ServerSocket,
    val config: TransferReceiveConfig,
) {
    private val mail = Channel<InternetMessage>()
    val incomingMail: ReceiveChannel<InternetMessage> = mail

    suspend fun listen() {
        supervisorScope {
            while (isActive) {
                val transport = SmtpServerTransport(socket.accept())

                logger.debug { "Accepted a connection from ${transport.remote}." }

                launch {
                    val agent = TransferReceiveAgent(transport, config)
                    agent.handle()
                    agent.incoming.collect { mail.send(it) }
                }
            }
        }
    }
}