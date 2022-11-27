package dev.sitar.kmail.runner

import dev.sitar.kmail.imap.agent.transports.ImapServerTransportClient
import dev.sitar.kmail.smtp.agent.transports.server.SmtpServerTransportClient
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.job
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

suspend fun run(
    imapServerClient: ImapServerTransportClient,
    smtpServerClient: SmtpServerTransportClient,
) = coroutineScope {
    println(KMAIL_ASCII)
    logger.info("Kmail is starting.")

    val imapServer = imapServer(imapServerClient)
    val submissionAgent = submission(smtpServerClient)
    val transferAgent = transfer(submissionAgent.incomingMail)

    coroutineContext.job.join()
}