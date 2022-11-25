package dev.sitar.kmail.runner

import dev.sitar.kmail.smtp.agent.transports.server.TlsCapableSmtpSubmissionServerTransportClient

expect fun tlsServerSocketClient(): TlsCapableSmtpSubmissionServerTransportClient