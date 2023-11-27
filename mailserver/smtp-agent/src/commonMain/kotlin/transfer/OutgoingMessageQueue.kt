package dev.sitar.kmail.agents.smtp.transfer

import dev.sitar.kmail.smtp.Domain
import dev.sitar.kmail.smtp.InternetMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

sealed interface When {
    object Now : When
    class Future(val timestamp: Long) : When

}

data class OutgoingMessage(val time: When, val domain: Domain, val message: InternetMessage)

class OutgoingMessageQueue {
    private val outgoing = MutableSharedFlow<OutgoingMessage>()

    suspend fun send(message: InternetMessage) {
        message.envelope
            .recipientAddresses.groupBy { it.mailbox.domain }
            .map { OutgoingMessage(When.Now, it.key, message) }
            .onEach { logger.debug { "queuing $it" } }
            .forEach { outgoing.emit(it) }
    }

    // TODO: we should store messages in transit (in case of failure/loss) until a new entry is received
    suspend fun collect(scope: CoroutineScope, block: suspend (OutgoingMessage) -> OutgoingMessage?) {
        outgoing
            .collect {
                logger.debug { "queue selected $it" }

                scope.launch {
                    when (val newEntry = block(it)) {
                        is OutgoingMessage -> {
                            logger.debug { "re-queueing $newEntry" }

                            outgoing.emit(newEntry)
                        }
                    }
                }
            }
    }




}