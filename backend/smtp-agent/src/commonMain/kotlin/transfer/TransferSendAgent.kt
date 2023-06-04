package dev.sitar.kmail.agents.smtp.transfer

import dev.sitar.dns.dnsResolver
import dev.sitar.dns.records.MXResourceRecord
import dev.sitar.dns.records.ResourceType
import dev.sitar.dns.transports.DnsServer
import dev.sitar.kmail.agents.smtp.DefaultTransferSessionSmtpConnector
import dev.sitar.kmail.agents.smtp.SmtpServerConnector
import dev.sitar.kmail.agents.smtp.StepProgression
import dev.sitar.kmail.agents.smtp.rewrite.ClientConnection
import dev.sitar.kmail.agents.smtp.rewrite.ClientObjective
import dev.sitar.kmail.agents.smtp.rewrite.MailObjective
import dev.sitar.kmail.agents.smtp.rewrite.SecureObjective
import dev.sitar.kmail.agents.smtp.transports.client.SmtpClientTransport
import dev.sitar.kmail.smtp.Domain
import dev.sitar.kmail.smtp.InternetMessage
import dev.sitar.kmail.smtp.Path
import dev.sitar.kmail.smtp.frames.reply.SmtpReply
import dev.sitar.kmail.smtp.frames.reply.SmtpReplyCode
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

private val resolver = dnsResolver()

private val GOOGLE_DNS = listOf("8.8.8.8", "8.8.4.4").map { DnsServer(it) } // Google's Public DNS

data class TransferConfig(
    val domain: Domain,
    val requireEncryption: Boolean,
    val connector: SmtpServerConnector = DefaultTransferSessionSmtpConnector(),
)

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

                resolver.resolveRecursively(host, GOOGLE_DNS) { qType = ResourceType.MX }
                    ?.filterIsInstance<MXResourceRecord>()
                    ?.sortedBy { it.data.preference }
                    ?.map { it.data.exchange }
                    .orEmpty()
            }

            is Domain.AddressLiteral -> listOf(domain.networkAddress.toString())
        }
    }

    private suspend fun transferTo(rcpt: Path) {
        val mxRecords = resolve(rcpt.mailbox.domain)

        logger.debug { "Resolved ${mxRecords.size} MX records.${mxRecords.joinToString(prefix = "\n", separator = "\n")}" }

        val transport = mxRecords.firstNotNullOfOrNull { config.connector.connect(it) }
            ?: TODO("could not connect to any exchange servers")

        val client = TransferSendConnection(transport, config, message, rcpt)

        client.start()
//
//        machine {
//            step {
//                transport.recv().coerce()
//            }
//
//            step {
//                transport.send(EhloCommand(config.domain))
//
//                val ehlo: EhloReply = when (val resp = transport.recv() deserializeAs EhloReply) {
//                    is Either.Left -> return@step StepProgression.Abort("Expected an EHLO reply, got ${resp.left} instead.")
//                    is Either.Right -> resp.right
//                }
//
//                if (transport.isSecure) return@step StepProgression.Continue
//
//                if (Capabilities.STARTTLS !in ehlo.capabilities) {
//                    if (config.requireEncryption)  return@step StepProgression.Abort("Encryption is required however encryption could not be negotiated.")
//                    else logger.debug { "Continuing the transfer of ${message.queueId} without any encryption!" }
//
//                    return@step StepProgression.Continue
//                }
//
//                transport.send(StartTlsCommand)
//
//                if (transport.recv().code !is SmtpReplyCode.PositiveCompletion) TODO("no encryption")
//
//                logger.debug { "Starting TLS negotiations." }
//                transport = transport.secure()
//                logger.debug { "Upgraded connection to TLS."}
//
//                StepProgression.Retry
//            }
//
//            // TODO: implement pipelining
//            step {
//                transport.send(MailCommand(message.envelope.originatorAddress))
//                transport.recv().coerce()
//            }
//
//            step {
//                transport.send(RecipientCommand(rcpt))
//                transport.recv().coerce()
//            }
//
//            step {
//                transport.send(DataCommand)
//                transport.recv().coerce<SmtpReplyCode.PositiveIntermediate>()
//            }
//
//            step {
//                transport.sendMessage(message.message)
//                transport.recv().coerce()
//            }
//
//            stop {
//                if (it is StopReason.Abrupt) {
//                    logger.warn { "The transfer of ${message.queueId} to $rcpt was abruptly stopped because of: ${it.reason}" }
//                } else {
//                    logger.info { "The transfer of ${message.queueId} to $rcpt was successful." }
//                }
//
//                transport.send(QuitCommand)
//
//                try {
//                    transport.recv().coerce()
//                } catch (_: Throwable) {
//                }
//
//                transport.close()
//            }
//        }
    }
}

private fun SmtpReply.coerce(): StepProgression {
    return when (code) {
        is SmtpReplyCode.PositiveCompletion -> StepProgression.Continue
        else -> error("")
    }
}

@JvmName("coerceTyped")
private inline fun <reified T: SmtpReplyCode> SmtpReply.coerce(): StepProgression {
    return when (code) {
        is T -> StepProgression.Continue
        else -> error("")
    }
}