package dev.sitar.kmail.smtp.agent.transports.client

import dev.sitar.kmail.smtp.agent.transports.SslContext

expect class TlsCapableSmtpTransferTransportClient(sslContext: SslContext) : SmtpTransportClient {
    fun fromSocket(socket: TlsCapableSocket, isClient: Boolean): TlsCapableSmtpTransportConnection
}