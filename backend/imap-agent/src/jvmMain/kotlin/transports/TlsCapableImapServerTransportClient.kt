package dev.sitar.kmail.imap.agent.transports

import mu.KotlinLogging
import javax.net.ServerSocketFactory
import javax.net.ssl.SSLContext

private val logger = KotlinLogging.logger { }

actual class TlsCapableImapServerTransportClient(val ssl: SSLContext) : ImapServerTransportClient {
    override fun bind(): ImapServerTransport {
        logger.debug { "IMAP Server binding on port $IMAP_TCP" }
        return TlsCapableImapServerTransport(ssl, ServerSocketFactory.getDefault().createServerSocket(IMAP_TCP))
    }
}