package dev.sitar.kmail.runner

import dev.sitar.kmail.agents.smtp.DefaultTransferSessionSmtpConnector
import dev.sitar.kmail.agents.smtp.transfer.*
import dev.sitar.kmail.smtp.InternetMessage
import dev.sitar.kmail.utils.connection.ConnectionFactory
import dev.sitar.kmail.utils.server.ServerSocketFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

fun CoroutineScope.transfer(
    serverFactory: ServerSocketFactory,
    connectionFactory: ConnectionFactory,
    outgoingMessages: OutgoingMessageQueue,
    incomingMessages: MutableSharedFlow<InternetMessage>
) {
    logger.info("SMTP transfer server is starting.")

    launch {
        val server = TransferServer(
            TransferConfig(
                Config.domains.first(),
                requireEncryption = Config.smtp.transfer.encryption,
                proxy = Config.proxy?.intoSmtpProxy(),
                connector = DefaultTransferSessionSmtpConnector(connectionFactory)
            ), outgoingMessages
        )

        server.handle()
    }

    logger.info("SMTP receive server is starting.")

    launch {
        val receiveServer = TransferReceiveServer(
            serverFactory.bind(Config.smtp.transfer.port),
            TransferReceiveConfig(Config.domains.first(), requiresEncryption = true, Config.accounts.map { it.email }),
            incomingMessages
        )

        receiveServer.listen()
    }
}