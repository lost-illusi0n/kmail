package dev.sitar.kmail.agents.smtp.transfer

import dev.sitar.kmail.agents.smtp.queueId
import dev.sitar.kmail.smtp.InternetMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import mu.KotlinLogging
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private val logger = KotlinLogging.logger { }

class TransferServer(
    val config: TransferConfig,
    val outgoingMail: Flow<InternetMessage>
) {
    suspend fun handle() {
        supervisorScope {
            outgoingMail.collect {
                logger.info { "Beginning transfer of ${it.queueId}: $it" }

                launch {
                    TransferSendAgent(config, it).transfer()
                }
            }
        }
    }
}