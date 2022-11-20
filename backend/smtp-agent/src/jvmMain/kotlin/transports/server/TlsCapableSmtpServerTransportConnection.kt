package dev.sitar.kmail.smtp.agent.transports.server

import dev.sitar.kmail.smtp.agent.transports.SslContext
import dev.sitar.kmail.smtp.agent.transports.client.SmtpTransportClient
import dev.sitar.kmail.smtp.agent.transports.client.SmtpTransportConnection
import dev.sitar.kmail.smtp.agent.transports.client.TlsCapableSmtpSubmissionTransportClient
import dev.sitar.kmail.smtp.agent.transports.client.TlsCapableSmtpTransportConnection
import java.net.ServerSocket

actual typealias TlsCapableServerSocket = ServerSocket

actual class TlsCapableSmtpServerTransportConnection actual constructor(
    sslContext: SslContext,
    val serverSocket: TlsCapableServerSocket
) : SmtpServerTransportConnection {
    override val client: TlsCapableSmtpSubmissionTransportClient = TlsCapableSmtpSubmissionTransportClient(sslContext)

    override suspend fun accept(): SmtpTransportConnection {
        return client.fromSocket(serverSocket.accept(), isClient = false)
    }

    override fun close() {
        serverSocket.close()
    }
}