package dev.sitar.kmail.runner

import dev.sitar.kmail.smtp.InternetMessage
import dev.sitar.kmail.smtp.agent.TransferAgent
import dev.sitar.kmail.smtp.agent.TransferConfig
import kotlinx.coroutines.flow.Flow
import mu.KotlinLogging
import kotlin.coroutines.coroutineContext

private val logger = KotlinLogging.logger { }

suspend fun transfer(outgoingMessages: Flow<InternetMessage>): TransferAgent {
    logger.info("SMTP transfer agent is starting.")

    val agent = TransferAgent(TransferConfig(CONFIGURATION.domain, requireEncryption = true), outgoingMessages, coroutineContext = coroutineContext)

    logger.info("SMTP transfer agent has started.")

    return agent
}