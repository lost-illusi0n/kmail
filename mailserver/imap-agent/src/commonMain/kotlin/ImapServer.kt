package dev.sitar.kmail.imap.agent

import dev.sitar.kmail.imap.agent.transports.ImapServerTransport
import dev.sitar.kmail.utils.server.ServerSocket
import kotlinx.coroutines.*
import mu.KotlinLogging
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private val logger = KotlinLogging.logger { }

class ImapServer(
    val socket: ServerSocket,
    val layer: ImapLayer
) {
    suspend fun listen() = supervisorScope {
        while (isActive) {
            val transport = ImapServerTransport(socket.accept())

            launch {
                logger.debug { "Accepted a connection from ${transport.remote}" }

                ImapAgent(transport, layer)
            }
        }
    }
}