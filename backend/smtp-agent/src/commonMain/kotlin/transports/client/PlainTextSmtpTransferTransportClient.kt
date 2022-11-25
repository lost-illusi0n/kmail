package dev.sitar.kmail.smtp.agent.transports.client

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers


object PlainTextSmtpTransferTransportClient : SmtpSocketTransportClient {
    override val name: String = "Ktor Transfer Socket"

    override suspend fun fromConnectedSocket(socket: Socket): SmtpTransportConnection {
        return SmtpSocketTransportConnection(socket)
    }

    override suspend fun connect(server: String): SmtpTransportConnection {
        val socket = aSocket(SelectorManager(Dispatchers.Default)).tcp().connect(server, SMTP_TRANSFER_PORT)

        return fromConnectedSocket(socket)
    }
}