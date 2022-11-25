package dev.sitar.kmail.smtp.agent.transports.client

interface SmtpTransportClient {
    val name: String

    suspend fun connect(server: String): SmtpTransportConnection
}