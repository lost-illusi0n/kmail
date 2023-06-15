package dev.sitar.kmail.imap.agent.transports

import java.net.ServerSocket
import javax.net.ssl.SSLContext

actual class TlsCapableImapServerTransport(val ssl: SSLContext, val serverSocket: ServerSocket) : ImapServerTransport {
    override suspend fun accept(): ImapTransport {
        return TlsCapableImapTransport(ssl, serverSocket.accept(), isUpgraded = false)
    }
}