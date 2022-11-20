package dev.sitar.kmail.smtp.agent.transports.server

import dev.sitar.kmail.smtp.agent.transports.SslContext

expect class TlsCapableSmtpSubmissionServerTransportClient(sslContext: SslContext) : SmtpServerTransportClient