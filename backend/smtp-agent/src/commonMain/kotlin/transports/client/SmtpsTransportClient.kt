package dev.sitar.kmail.smtp.agent.transports.client

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.network.tls.*
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.coroutineContext

object SmtpsTransportClient : SmtpSocketTransportClient {
    const val IMPLICIT_SMTPS_PORT = 587

    override suspend fun fromConnectedSocket(socket: Socket): SmtpTransportConnection {
        return SmtpSocketTransportConnection(socket.tls(coroutineContext))
    }

    override suspend fun connect(server: String): SmtpTransportConnection {
        val socket = aSocket(SelectorManager(Dispatchers.Default)).tcp().connect(server, IMPLICIT_SMTPS_PORT)

        return fromConnectedSocket(socket)
    }
}