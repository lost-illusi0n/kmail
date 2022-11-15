package dev.sitar.kmail.smtp.agent.transports.server

import dev.sitar.kmail.smtp.agent.transports.client.PlainTextSmtpTransportClient
import dev.sitar.kmail.smtp.agent.transports.client.SmtpSocketTransportClient
import dev.sitar.kmail.smtp.agent.transports.client.SmtpTransportClient
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers

object PlainTextSmtpServerTransportClient : SmtpServerTransportClient {
    const val SMTP_PORT = 25

    override fun bind(): SmtpServerTransportConnection {
        val transport = aSocket(SelectorManager(Dispatchers.Default)).tcp().bind(port = SMTP_PORT)
        return SmtpSocketServerTransportConnection(transport, PlainTextSmtpTransportClient)
    }
}