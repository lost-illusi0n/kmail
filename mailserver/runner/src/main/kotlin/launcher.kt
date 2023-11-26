package dev.sitar.kmail.runner

import dev.sitar.kmail.imap.agent.ImapAgent
import dev.sitar.kmail.imap.agent.ImapLayer
import dev.sitar.kmail.imap.agent.ImapMailbox
import dev.sitar.kmail.imap.agent.ImapServer
import dev.sitar.kmail.imap.agent.transports.ImapServerTransport
import dev.sitar.kmail.runner.storage.mailbox
import dev.sitar.kmail.sasl.SaslChallenge
import dev.sitar.kmail.smtp.InternetMessage
import dev.sitar.kmail.utils.connection.ConnectionFactory
import dev.sitar.kmail.utils.server.ServerSocketFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

@OptIn(FlowPreview::class)
suspend fun run(serverSocketFactory: ServerSocketFactory, connectionFactory: ConnectionFactory) = supervisorScope {
    withContext(CoroutineExceptionHandler { coroutineContext, throwable -> logger.error(throwable) { } }) {
        println(KMAIL_ASCII)
        logger.info("Kmail is starting.")

        logger.debug { "Detected following configuration: $Config" }

        val incoming = MutableSharedFlow<InternetMessage>()
        val outgoing = MutableSharedFlow<InternetMessage>()

        val mailbox = mailbox(incoming)

        if (Config.imap.enabled) launch { imap(serverSocketFactory, KmailImapLayer(mailbox)) }
        if (Config.pop3.enabled) launch { pop3(serverSocketFactory, KmailPop3Layer(mailbox)) }
        if (Config.smtp.submission.enabled) launch { submission(serverSocketFactory, outgoing) }
        if (Config.smtp.transfer.enabled) launch { transfer(serverSocketFactory, connectionFactory, outgoing, incoming) }

        coroutineContext.job.join()
    }
}