package dev.sitar.kmail.smtp.agent.transports.server

interface SmtpServerTransportClient {
    fun bind(): SmtpServerTransportConnection
}