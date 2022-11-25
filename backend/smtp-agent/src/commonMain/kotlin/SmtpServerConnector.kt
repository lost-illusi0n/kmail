package dev.sitar.kmail.smtp.agent

import dev.sitar.kmail.smtp.agent.transports.client.*
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

interface SmtpServerConnector {
    suspend fun connect(server: String): SmtpTransportConnection?
}

class DefaultTransferSessionSmtpConnector(val timeout: Long = 500, vararg val additionalClients: SmtpTransportClient) :
    SmtpServerConnector {
    companion object {
        val STANDARD_SERVICES = setOf(
            ImplicitTlsSmtpTransportClient,
            PlainTextSmtpTransferTransportClient
        )
    }

    override suspend fun connect(server: String): SmtpTransportConnection? {
        for (service in additionalClients.toList() + STANDARD_SERVICES) {
            val connection = withTimeoutOrNull(timeout) {
                logger.debug { "Attempting to connect to $server using ${service.name}." }
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
        val STANDARD_SERVICES = setOf(
            ImplicitTlsSmtpTransportClient,
            PlainTextSmtpSubmissionTransportClient,
        )
    }

    override suspend fun connect(server: String): SmtpTransportConnection? {
        for (service in additionalClients.toList() + STANDARD_SERVICES) {
            try {
                return withTimeout(timeout) {
                    logger.debug { "Attempting to connect to $server using ${service.name}." }
                    service.connect(server)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        return null
    }
}