package dev.sitar.kmail.smtp.agent.transports.client

import dev.sitar.kmail.smtp.agent.transports.SslContext

expect class TlsCapableSmtpSubmissionTransportClient(context: SslContext) : SmtpTransportClient {
    fun fromSocket(socket: TlsCapableSocket, isClient: Boolean): TlsCapableSmtpTransportConnection
}