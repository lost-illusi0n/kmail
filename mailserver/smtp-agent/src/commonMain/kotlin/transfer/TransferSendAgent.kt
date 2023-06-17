package dev.sitar.kmail.agents.smtp.transfer

import dev.sitar.dns.Dns
import dev.sitar.dns.records.MXResourceRecord
import dev.sitar.dns.records.ResourceType
import dev.sitar.dns.transports.DnsServer
import dev.sitar.kmail.agents.smtp.DefaultTransferSessionSmtpConnector
import dev.sitar.kmail.agents.smtp.SmtpServerConnector
import dev.sitar.kmail.agents.smtp.rewrite.ClientConnection
import dev.sitar.kmail.agents.smtp.rewrite.ClientObjective
import dev.sitar.kmail.agents.smtp.rewrite.MailObjective
import dev.sitar.kmail.agents.smtp.rewrite.SecureObjective
import dev.sitar.kmail.agents.smtp.transports.client.SmtpClientTransport
import dev.sitar.kmail.smtp.Domain
import dev.sitar.kmail.smtp.InternetMessage
import dev.sitar.kmail.smtp.Path
import dev.sitar.kmail.utils.connection.KtorConnectionFactory
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

private val GOOGLE_DNS = listOf("8.8.8.8", "8.8.4.4").map { DnsServer(it) } // Google's Public DNS
private val dns = Dns(defaultServers = GOOGLE_DNS)

data class TransferConfig(
    val domain: Domain,
    val requireEncryption: Boolean,
    val proxy: Proxy? = null,
    val connector: SmtpServerConnector = DefaultTransferSessionSmtpConnector(KtorConnectionFactory()),
)

data class Proxy(val ip: String, val port: Int)

class TransferSendConnection(
    transport: SmtpClientTransport,
    config: TransferConfig,
    mail: InternetMessage,
    rcpt: Path
): ClientConnection(transport, config.domain) {
    override val objectives: Set<ClientObjective> = setOf(
        SecureObjective(this, config.requireEncryption),
        MailObjective(this, mail, rcpt)
    )
}

// transfer server -> rcpt smtp server connection
class TransferSendAgent(
    val config: TransferConfig,
    val message: InternetMessage,
) {
    suspend fun transfer() {
        message.envelope.recipientAddresses.forEach { transferTo(it) }
    }

    private suspend fun resolve(domain: Domain): List<String> {
        return when (domain) {
            is Domain.Actual -> {
                val host = domain.domain
                logger.debug { "Attempting to resolve MX records for $host." }

                // TODO: resolving lib?
                if (host.contentEquals("localhost")) return listOf("localhost")

                dns.resolveRecursively(host) { qType = ResourceType.MX }
                    ?.filterIsInstance<MXResourceRecord>()
                    ?.sortedBy { it.data.preference }
                    ?.map { it.data.exchange }
                    .orEmpty()
            }

            is Domain.AddressLiteral -> listOf(domain.networkAddress.toString())
        }
    }

    private suspend fun transferTo(rcpt: Path) {
        val transport = if (config.proxy != null) {
            logger.debug { "Using proxy for transfer." }

            SmtpClientTransport(config.connector.connectionFactory.connect(config.proxy.ip, config.proxy.port))
        } else {
            val mxRecords = resolve(rcpt.mailbox.domain)

            logger.debug { "Resolved ${mxRecords.size} MX records.${mxRecords.joinToString(prefix = "\n", separator = "\n")}" }

            mxRecords.firstNotNullOfOrNull { config.connector.connect(it) }
                ?: TODO("could not connect to any exchange servers")
        }

        val client = TransferSendConnection(transport, config, message, rcpt)

        client.start()
    }
}