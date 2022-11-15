package dev.sitar.kmail.smtp.agent.transports.client

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers

object PlainTextSmtpTransportClient : SmtpSocketTransportClient {
    const val SMTP_PORT = 25

    override suspend fun fromConnectedSocket(socket: Socket): SmtpTransportConnection {
        return SmtpSocketTransportConnection(socket)
    }

    override suspend fun connect(server: String): SmtpTransportConnection {
        val socket = aSocket(SelectorManager(Dispatchers.Default)).tcp().connect(server, SMTP_PORT)

        return fromConnectedSocket(socket)
    }
}