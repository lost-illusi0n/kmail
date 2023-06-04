package dev.sitar.kmail.agents.pop3

import dev.sitar.kmail.agents.pop3.transports.Pop3ServerTransport
import dev.sitar.kmail.utils.server.ServerSocket
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

class Pop3Server(
    val socket: ServerSocket,
    val layer: Pop3Layer,
) {
    suspend fun listen() {
        supervisorScope {
            while (isActive) {
                val transport = Pop3ServerTransport(socket.accept())

                launch {
                    logger.debug { "Accepted a connection from ${transport.remote}." }

                    val agent = Pop3Agent(transport, layer)
                    agent.handle()
                }
            }
        }
    }
}