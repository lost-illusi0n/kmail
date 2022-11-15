package dev.sitar.kmail.smtp.agent.transports.client

interface SmtpTransportClient {
    suspend fun connect(server: String): SmtpTransportConnection
}