package dev.sitar.kmail.runner

import dev.sitar.kmail.agents.smtp.DefaultTransferSessionSmtpConnector
import dev.sitar.kmail.agents.smtp.transfer.*
import dev.sitar.kmail.smtp.InternetMessage
import dev.sitar.kmail.utils.connection.ConnectionFactory
import dev.sitar.kmail.utils.server.ServerSocketFactory
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

suspend fun transfer(
    serverFactory: ServerSocketFactory,
    connectionFactory: ConnectionFactory,
    outgoingMessages: Flow<InternetMessage>,
    incomingMessages: MutableSharedFlow<InternetMessage>
): TransferReceiveServer = coroutineScope {
    logger.info("SMTP transfer server is starting.")

    val server = TransferServer(
        TransferConfig(
            Config.domains.first(),
            requireEncryption = Config.smtp.transfer.encryption,
            proxy = Config.proxy?.intoSmtpProxy(),
            connector = DefaultTransferSessionSmtpConnector(connectionFactory)
        ), outgoingMessages
    )
    launch { server.handle() }

    logger.info("SMTP transfer server has started.")

    logger.info("SMTP receive server is starting.")

    val receiveServer = TransferReceiveServer(
        serverFactory.bind(Config.smtp.transfer.port),
        TransferReceiveConfig(Config.domains.first(), requiresEncryption = true),
        incomingMessages
    )

    launch { receiveServer.listen() }

    logger.info("SMTP receive server has started.")

    receiveServer
}