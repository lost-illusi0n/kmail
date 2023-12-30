package dev.sitar.kmail.agents.smtp

import dev.sitar.kmail.agents.smtp.transports.IMPLICIT_SMTP_PORT
import dev.sitar.kmail.agents.smtp.transports.SMTP_SUBMISSION_PORT
import dev.sitar.kmail.agents.smtp.transports.SMTP_TRANSFER_PORT
import dev.sitar.kmail.agents.smtp.transports.client.SmtpClientTransport
import dev.sitar.kmail.utils.connection.ConnectionFactory
import kotlinx.coroutines.withTimeoutOrNull
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

interface SmtpServerConnector {
    val connectionFactory: ConnectionFactory
    val ports: Set<Int>
    val timeout: Long

    /**
     * Attempts to connect to any SMTP server given a [host].
     */
    suspend fun connect(host: String): SmtpClientTransport? {
        for (port in ports) {
            val connection = withTimeoutOrNull(timeout) {
                logger.debug { "Attempting to connect to $host using $connectionFactory." }
                connectionFactory.connect(host, port)
            }

            if (connection != null) {
                logger.debug { "Connected to $host." }
                return SmtpClientTransport(connection)
            }
        }

        return null
    }
}

class DefaultTransferSessionSmtpConnector(
    override val connectionFactory: ConnectionFactory,
    override val timeout: Long = 2000
) : SmtpServerConnector {
    companion object {
        val STANDARD_PORTS: Set<Int> = setOf(
            SMTP_TRANSFER_PORT,
            IMPLICIT_SMTP_PORT
        )
    }

    override val ports: Set<Int> =  STANDARD_PORTS
}