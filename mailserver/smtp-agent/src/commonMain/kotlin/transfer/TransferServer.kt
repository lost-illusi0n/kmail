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
    val queue: OutgoingMessageQueue
) {
    suspend fun handle() = supervisorScope {
        logger.info("SMTP transfer server is listening.")

        queue.collect(this) {
            TransferSendAgent(config, it).transfer()
        }
    }
}