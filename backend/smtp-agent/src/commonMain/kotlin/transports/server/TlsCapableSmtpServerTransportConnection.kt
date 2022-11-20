package dev.sitar.kmail.smtp.agent.transports.server

import dev.sitar.kmail.smtp.agent.transports.SslContext

expect class TlsCapableServerSocket

expect class TlsCapableSmtpServerTransportConnection(sslContext: SslContext, serverSocket: TlsCapableServerSocket) :
    SmtpServerTransportConnection