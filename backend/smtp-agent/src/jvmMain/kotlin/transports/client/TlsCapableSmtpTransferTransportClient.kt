package dev.sitar.kmail.smtp.agent.transports.client

import dev.sitar.kmail.smtp.agent.transports.SslContext
import javax.net.SocketFactory

actual class TlsCapableSmtpTransferTransportClient actual constructor(val sslContext: SslContext) : SmtpTransportClient {
    actual fun fromSocket(socket: TlsCapableSocket, isClient: Boolean): TlsCapableSmtpTransportConnection {
        return TlsCapableSmtpTransportConnection(sslContext, socket, isClient)
    }

    override suspend fun connect(server: String): SmtpTransportConnection {
        val socket = SocketFactory.getDefault().createSocket(server, SMTP_TRANSFER_PORT)
        return fromSocket(socket, true)
    }
}