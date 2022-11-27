package dev.sitar.kmail.imap.agent

import dev.sitar.kmail.imap.agent.transports.ImapServerTransportClient
import dev.sitar.kmail.imap.agent.transports.ImapServerTransport
import kotlinx.coroutines.*
import mu.KotlinLogging
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext

private val logger = KotlinLogging.logger { }

class ImapServer(
    val transport: ImapServerTransport,
    val imapQueryAgent: ImapQueryAgent,
    coroutineContext: CoroutineContext = EmptyCoroutineContext
) {
    private val scope = CoroutineScope(CoroutineName("imap-server") + SupervisorJob() + coroutineContext)

    fun listen() {
        scope.launch {
            while (isActive) {
                val clientTransport = transport.accept()
                logger.debug { "Accepted a connection from ${clientTransport.remote}" }
                launch {
                    ImapAgent(clientTransport, imapQueryAgent, coroutineContext)
                }
            }
        }
    }
}