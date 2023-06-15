package dev.sitar.kmail.agents.smtp.submission

import dev.sitar.kmail.agents.smtp.transports.server.SmtpServerTransport
import dev.sitar.kmail.smtp.InternetMessage
import dev.sitar.kmail.utils.server.ServerSocket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import mu.KotlinLogging
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private val logger = KotlinLogging.logger { }

class SubmissionServer(
    val socket: ServerSocket,
    val config: SubmissionConfig,
    val incoming: MutableSharedFlow<InternetMessage>
) {
    suspend fun listen() {
        supervisorScope {
            while (isActive) {
                val transport = SmtpServerTransport(socket.accept())

                logger.debug { "Accepted a connection from ${transport.remote}." }

                launch {
                    val agent = SubmissionAgent(transport, config)
                    agent.handle()
                    agent.incoming.collect { incoming.emit(it) }
                }
            }
        }
    }
}