package dev.sitar.kmail.runner

import dev.sitar.kmail.agents.smtp.SmtpServerConnector
import dev.sitar.kmail.utils.server.ServerSocketFactory
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

suspend fun run(
//    imapServerClient: ImapServerTransportClient,
//    smtpServerClient: SmtpServerTransportClient,
    connector: SmtpServerConnector,
    serverSocketFactory: ServerSocketFactory,
//    submissionServerFactory: TlsCapableSmtpServerSocketFactory,
//    transferServerFactory: TlsCapableSmtpServerSocketFactory,
) = withContext(SupervisorJob()) {
    println(KMAIL_ASCII)
    logger.info("Kmail is starting.")

//    val imapServer = imapServer(imapServerClient)

    val submissionServer = submission(serverSocketFactory)

    val receiveServer = transfer(submissionServer.incomingMail, connector, serverSocketFactory)

    val pop3Server = pop3Server(serverSocketFactory, KmailPop3Layer(receiveServer.incomingMail))

    coroutineContext.job.join()
}