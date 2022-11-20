package dev.sitar.kmail.smtp.agent.transports.server

import dev.sitar.kmail.smtp.agent.transports.client.PlainTextSmtpTransferTransportClient
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers

object PlainTextSmtpServerTransportClient : SmtpServerTransportClient {
    override fun bind(): SmtpServerTransportConnection {
        val transport = aSocket(SelectorManager(Dispatchers.Default)).tcp().bind(port = SMTP_SUBMISSION_PORT)
        return SmtpSocketServerTransportConnection(transport, PlainTextSmtpTransferTransportClient)
    }
}