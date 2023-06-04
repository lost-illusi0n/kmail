package dev.sitar.kmail.runner

import dev.sitar.kmail.agents.smtp.SmtpServerConnector
import dev.sitar.kmail.agents.smtp.transfer.TransferConfig
import dev.sitar.kmail.agents.smtp.transfer.TransferReceiveConfig
import dev.sitar.kmail.agents.smtp.transfer.TransferReceiveServer
import dev.sitar.kmail.agents.smtp.transfer.TransferServer
import dev.sitar.kmail.agents.smtp.transports.SMTP_TRANSFER_PORT
import dev.sitar.kmail.smtp.InternetMessage
import dev.sitar.kmail.utils.server.ServerSocketFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

suspend fun CoroutineScope.transfer(outgoingMessages: Flow<InternetMessage>, connector: SmtpServerConnector, factory: ServerSocketFactory): TransferReceiveServer {
    logger.info("SMTP transfer server is starting.")

    val server = TransferServer(TransferConfig(CONFIGURATION.domain, requireEncryption = true, connector), coroutineContext = coroutineContext)
    server.handle(outgoingMessages)

    logger.info("SMTP transfer server has started.")

    logger.info("SMTP receive server is starting.")

    val socket = factory.bind(SMTP_TRANSFER_PORT)
    val receiveServer = TransferReceiveServer(socket, TransferReceiveConfig(CONFIGURATION.domain, requiresEncryption = true))

    launch {
        receiveServer.listen()
    }

    logger.info("SMTP receive server has started.")

    return receiveServer
}