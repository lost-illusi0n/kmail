package dev.sitar.kmail.smtp.agent.transports.client

import dev.sitar.kmail.smtp.agent.transports.SslContext
import javax.net.SocketFactory

actual class TlsCapableSmtpSubmissionTransportClient actual constructor(val context: SslContext) : SmtpTransportClient {
    override val name: String = "Java Submission Socket"

    actual fun fromSocket(socket: TlsCapableSocket, isClient: Boolean): TlsCapableSmtpTransportConnection {
        return TlsCapableSmtpTransportConnection(context, socket, isClient)
    }

    override suspend fun connect(server: String): SmtpTransportConnection {
        val socket = SocketFactory.getDefault().createSocket(server, SMTP_SUBMISSION_PORT)
        return fromSocket(socket, true)
    }
}