package dev.sitar.kmail.smtp.agent.transports.server

import dev.sitar.kmail.smtp.agent.transports.client.SmtpTransportClient
import dev.sitar.kmail.smtp.agent.transports.client.SmtpTransportConnection

interface SmtpServerTransportConnection {
    val client: SmtpTransportClient

    suspend fun accept(): SmtpTransportConnection
    fun close()
}