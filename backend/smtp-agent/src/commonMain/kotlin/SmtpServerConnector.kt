package dev.sitar.kmail.smtp.agent

import dev.sitar.kmail.smtp.agent.transports.client.ImplicitTlsSmtpTransportClient
import dev.sitar.kmail.smtp.agent.transports.client.PlainTextSmtpSubmissionTransportClient
import dev.sitar.kmail.smtp.agent.transports.client.PlainTextSmtpTransportClient
import dev.sitar.kmail.smtp.agent.transports.client.SmtpTransportConnection
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

interface SmtpServerConnector {
    suspend fun connect(server: String): SmtpTransportConnection?
}

class DefaultTransferSmtpConnector(val timeout: Long = 500) : SmtpServerConnector {
    companion object {
        val STANDARD_SERVICES = listOf(
            PlainTextSmtpTransportClient
        )
    }

    override suspend fun connect(server: String): SmtpTransportConnection? {
        for (service in STANDARD_SERVICES) {
            val connection = withTimeoutOrNull(timeout) {
                println("SMTP SERVER CONNECTOR: ATTEMPTING CONNECTION TO $server USING $service")
                service.connect(server)
            }

            if (connection != null) return connection
        }

        return null
    }
}

class DefaultSubmissionSmtpConnector(val timeout: Long = 500) : SmtpServerConnector {
    companion object {
        val STANDARD_SERVICES = listOf(
            ImplicitTlsSmtpTransportClient,
            PlainTextSmtpSubmissionTransportClient,
        )
    }

    override suspend fun connect(server: String): SmtpTransportConnection? {
        for (service in STANDARD_SERVICES) {
            try {
                return withTimeout(timeout) {
                    println("SMTP SERVER CONNECTOR: ATTEMPTING CONNECTION TO $server USING $service")
                    service.connect(server)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        return null
    }
}