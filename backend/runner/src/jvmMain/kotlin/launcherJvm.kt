package dev.sitar.kmail.runner

import dev.sitar.kmail.imap.agent.transports.TlsCapableImapServerTransportClient
import dev.sitar.kmail.smtp.agent.transports.server.TlsCapableSmtpSubmissionServerTransportClient

suspend fun main() {
    val ssl = ssl()
    val imapServerTransportClient = TlsCapableImapServerTransportClient(ssl)
    val smtpServerTransportClient = TlsCapableSmtpSubmissionServerTransportClient(ssl)

    run(imapServerTransportClient, smtpServerTransportClient)
}