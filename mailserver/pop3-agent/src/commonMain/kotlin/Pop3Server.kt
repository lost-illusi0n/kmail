package dev.sitar.kmail.agents.pop3

import dev.sitar.kmail.agents.pop3.transports.Pop3ServerTransport
import dev.sitar.kmail.utils.server.ServerSocket
import kotlinx.coroutines.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

class Pop3Server(
    val socket: ServerSocket,
    val layer: Pop3Layer,
) {
    suspend fun listen() = supervisorScope {
        logger.debug { "POP3 server is listening." }

        while (isActive) {
            val transport = Pop3ServerTransport(socket.accept())
            logger.debug { "Accepted a connection from ${transport.remote}." }

            launch(Dispatchers.IO) {
                val agent = Pop3Agent(transport, layer)
                agent.handle()
            }
        }
    }
}