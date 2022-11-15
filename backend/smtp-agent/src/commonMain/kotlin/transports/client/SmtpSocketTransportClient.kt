package dev.sitar.kmail.smtp.agent.transports.client

import dev.sitar.kmail.smtp.agent.transports.client.SmtpTransportClient
import io.ktor.network.sockets.*

interface SmtpSocketTransportClient : SmtpTransportClient {
    suspend fun fromConnectedSocket(socket: Socket) : SmtpTransportConnection
}