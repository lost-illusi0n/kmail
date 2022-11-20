package dev.sitar.kmail.smtp.agent

import dev.sitar.kmail.smtp.agent.transports.client.*
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

interface SmtpServerConnector {
    suspend fun connect(server: String): SmtpTransportConnection?
}

class DefaultTransferSessionSmtpConnector(val timeout: Long = 500, vararg val additionalClients: SmtpTransportClient) :
    SmtpServerConnector {
    companion object {
        val STANDARD_SERVICES = listOf(
            ImplicitTlsSmtpTransportClient,
            PlainTextSmtpTransferTransportClient
        )
    }

    override suspend fun connect(server: String): SmtpTransportConnection? {
        for (service in additionalClients.toList() + STANDARD_SERVICES) {
            val connection = withTimeoutOrNull(timeout) {
                println("SMTP SERVER CONNECTOR: ATTEMPTING CONNECTION TO $server USING $service")
                service.connect(server)
            }

            if (connection != null) return connection
        }

        return null
    }
}

class DefaultSubmissionSessionSmtpConnector(
    val timeout: Long = 500,
    vararg val additionalClients: SmtpTransportClient
) : SmtpServerConnector {
    companion object {
        val STANDARD_SERVICES = listOf(
            ImplicitTlsSmtpTransportClient,
            PlainTextSmtpSubmissionTransportClient,
        )
    }

    override suspend fun connect(server: String): SmtpTransportConnection? {
        for (service in additionalClients.toList() + STANDARD_SERVICES) {
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