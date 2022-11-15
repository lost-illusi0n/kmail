package dev.sitar.kmail.smtp.agent.transports.server

import dev.sitar.kmail.smtp.agent.transports.client.SmtpSocketTransportClient
import dev.sitar.kmail.smtp.agent.transports.client.SmtpTransportConnection
import io.ktor.network.sockets.*

class SmtpSocketServerTransportConnection(
    val serverSocket: ServerSocket,
    override val client: SmtpSocketTransportClient
) : SmtpServerTransportConnection {
    override suspend fun accept(): SmtpTransportConnection {
        val socket = serverSocket.accept()
        return client.fromConnectedSocket(socket)
    }

    override fun close() {
        serverSocket.close()
    }
}