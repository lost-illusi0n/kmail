package dev.sitar.kmail.agents.smtp

import dev.sitar.kmail.agents.smtp.transports.IMPLICIT_SMTP_PORT
import dev.sitar.kmail.agents.smtp.transports.SMTP_SUBMISSION_PORT
import dev.sitar.kmail.agents.smtp.transports.SMTP_TRANSFER_PORT
import dev.sitar.kmail.agents.smtp.transports.client.SmtpClientTransport
import dev.sitar.kmail.utils.connection.ConnectionFactory
import dev.sitar.kmail.utils.connection.KtorConnectionFactory
import kotlinx.coroutines.withTimeoutOrNull
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

interface SmtpServerConnector {
    /**
     * Attempts to connect to any SMTP server given a [host].
     */
    suspend fun connect(host: String): SmtpClientTransport?
}

class DefaultTransferSessionSmtpConnector(val timeout: Long = 500, vararg additionalClients: ConnectionFactory) :
    SmtpServerConnector {
    companion object {
        val STANDARD_SERVICES: Set<ConnectionFactory> = setOf(
            KtorConnectionFactory(SMTP_TRANSFER_PORT),
            KtorConnectionFactory(IMPLICIT_SMTP_PORT)
        )
    }

    private val services = additionalClients.toList() + STANDARD_SERVICES

    override suspend fun connect(host: String): SmtpClientTransport? {
        for (service in services) {
            val connection = withTimeoutOrNull(timeout) {
                logger.debug { "Attempting to connect to $host using $service." }
                service.connect(host)
            }

            if (connection != null) {
                logger.debug { "Connected to $host." }
                return SmtpClientTransport(connection)
            }
        }

        return null
    }
}

class DefaultSubmissionSessionSmtpConnector(
    val timeout: Long = 500,
    vararg additionalClients: ConnectionFactory
) : SmtpServerConnector {
    companion object {
        val STANDARD_SERVICES: Set<ConnectionFactory> = setOf(
            KtorConnectionFactory(SMTP_SUBMISSION_PORT),
            KtorConnectionFactory(IMPLICIT_SMTP_PORT)
//            ImplicitTlsSmtpTransportClient,
//            PlainTextSmtpSubmissionTransportClient,
        )
    }

    private val services = additionalClients.toList() + STANDARD_SERVICES

    override suspend fun connect(host: String): SmtpClientTransport? {
        for (service in services) {
            val connection = withTimeoutOrNull(timeout) {
                logger.debug { "Attempting to connect to $host using $service." }
                service.connect(host)
            }

            if (connection != null) {
                logger.debug { "Connected to $host." }
                return SmtpClientTransport(connection)
            }
        }

        return null
    }
}