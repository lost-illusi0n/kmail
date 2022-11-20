package dev.sitar.kmail.smtp.agent.transports.client

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.network.tls.*
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.coroutineContext

object ImplicitTlsSmtpTransportClient : SmtpSocketTransportClient {
    override suspend fun fromConnectedSocket(socket: Socket): SmtpTransportConnection {
        return SmtpSocketTransportConnection(socket.tls(coroutineContext), isImplicitlyEncrypted = true)
    }

    override suspend fun connect(server: String): SmtpTransportConnection {
        val socket = aSocket(SelectorManager(Dispatchers.Default)).tcp().connect(server, IMPLICIT_SMTP_PORT)

        return fromConnectedSocket(socket)
    }
}