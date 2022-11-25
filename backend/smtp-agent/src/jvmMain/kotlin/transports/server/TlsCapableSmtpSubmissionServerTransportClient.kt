package dev.sitar.kmail.smtp.agent.transports.server

import dev.sitar.kmail.smtp.agent.transports.SslContext
import mu.KotlinLogging
import javax.net.ServerSocketFactory

private val logger = KotlinLogging.logger { }

actual class TlsCapableSmtpSubmissionServerTransportClient actual constructor(val sslContext: SslContext) : SmtpServerTransportClient {
    override fun bind(): SmtpServerTransportConnection {
        val serverSocket = ServerSocketFactory.getDefault().createServerSocket(SMTP_SUBMISSION_PORT)
        logger.info("Listening on localhost:$SMTP_SUBMISSION_PORT.")
        return TlsCapableSmtpServerTransportConnection(sslContext, serverSocket)
    }
}