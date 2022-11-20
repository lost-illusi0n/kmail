package dev.sitar.kmail.smtp.agent.transports.client

import dev.sitar.kmail.smtp.agent.transports.SslContext

expect class TlsCapableSocket
expect class TlsCapableSmtpTransportConnection(sslContext: SslContext, socket: TlsCapableSocket, isClient: Boolean) :
    SmtpTransportConnection