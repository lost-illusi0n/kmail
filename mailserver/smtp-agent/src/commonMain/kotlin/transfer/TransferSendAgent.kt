package dev.sitar.kmail.agents.smtp.transfer

import dev.sitar.dns.Dns
import dev.sitar.dns.records.MXResourceRecord
import dev.sitar.dns.records.ResourceType
import dev.sitar.dns.transports.DnsServer
import dev.sitar.kmail.agents.smtp.DefaultTransferSessionSmtpConnector
import dev.sitar.kmail.agents.smtp.SmtpServerConnector
import dev.sitar.kmail.agents.smtp.connections.ClientConnection
import dev.sitar.kmail.agents.smtp.connections.ClientObjective
import dev.sitar.kmail.agents.smtp.connections.MailObjective
import dev.sitar.kmail.agents.smtp.connections.SecureObjective
import dev.sitar.kmail.agents.smtp.transports.client.SmtpClientTransport
import dev.sitar.kmail.smtp.Domain
import dev.sitar.kmail.smtp.InternetMessage
import dev.sitar.kmail.smtp.Path
import dev.sitar.kmail.utils.connection.KtorConnectionFactory
import io.ktor.util.date.*
import kotlinx.coroutines.delay
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

private val GOOGLE_DNS = listOf("8.8.8.8", "8.8.4.4").map { DnsServer(it) } // Google's Public DNS
private val dns = Dns(defaultServers = GOOGLE_DNS)
//private val dns = Dns(defaultServers = listOf(DnsServer("192.168.1.1")))

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
    outgoing: OutgoingMessage,
): ClientConnection(transport, config.domain) {
    override val objectives: Set<ClientObjective> = setOf(
        SecureObjective(this, config.requireEncryption),
        MailObjective(this, outgoing.message, outgoing.message.envelope.recipientAddresses.filter { it.mailbox.domain == outgoing.domain })
    )
}

// transfer server -> rcpt smtp server connection
class TransferSendAgent(
    val config: TransferConfig,
    val outgoing: OutgoingMessage,
) {
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

    suspend fun transfer(): OutgoingMessage? {
        if (outgoing.time is When.Future) {
            logger.debug { "waiting for ${outgoing.time.timestamp}ms before transferring" }
            delay(outgoing.time.timestamp - getTimeMillis())
        }

        val transport = if (config.proxy != null) {
            logger.debug { "Using proxy for transfer." }

            SmtpClientTransport(config.connector.connectionFactory.connect(config.proxy.ip, config.proxy.port))
        } else {
            val mxRecords = resolve(outgoing.domain)

            logger.debug { "Resolved ${mxRecords.size} MX records.${mxRecords.joinToString(prefix = "\n", separator = "\n")}" }

            mxRecords.firstNotNullOfOrNull { config.connector.connect(it) }
                ?: TODO("could not connect to any exchange servers")
        }

        val client = TransferSendConnection(transport, config, outgoing)

        return when (client.start()) {
            ClientObjective.Result.Bad -> TODO()
            ClientObjective.Result.Okay -> {
                logger.debug { "successfully transferred $outgoing" }
                null
            }
            ClientObjective.Result.RetryLater -> {
                logger.debug { "asked to try again later, queueing with 2 minute delay $outgoing" }
                OutgoingMessage(When.Future(getTimeMillis() + 2 * 60 * 1000), outgoing.domain, outgoing.message)
            }
        }
    }
}