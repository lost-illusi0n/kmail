package dev.sitar.kmail.smtp.agent

import dev.sitar.dns.dnsResolver
import dev.sitar.dns.records.MXResourceRecord
import dev.sitar.dns.records.ResourceType
import dev.sitar.dns.transports.DnsServer
import dev.sitar.kmail.smtp.*
import dev.sitar.kmail.smtp.agent.transports.client.SmtpTransportConnection
import dev.sitar.kmail.smtp.frames.replies.*
import dev.sitar.kmail.smtp.io.smtp.reader.AsyncSmtpClientReader
import dev.sitar.kmail.smtp.io.smtp.reader.asAsyncSmtpClientReader
import dev.sitar.kmail.smtp.io.smtp.writer.AsyncSmtpClientWriter
import dev.sitar.kmail.smtp.io.smtp.writer.asAsyncSmtpClientWriter
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import mu.KotlinLogging
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private val logger = KotlinLogging.logger { }

data class TransferConfig(
    val domain: Domain,
    val requireEncryption: Boolean,
)

class TransferAgent(
    val config: TransferConfig,
    val outgoingMessages: Flow<InternetMessage>,
    val connector: SmtpServerConnector = DefaultTransferSessionSmtpConnector(),
    coroutineContext: CoroutineContext = EmptyCoroutineContext
) {
    private companion object {
        private val GOOGLE_DNS = listOf("8.8.8.8", "8.8.4.4").map { DnsServer(it) } // Google's Public DNS
    }

    private val resolver = dnsResolver()
    private val coroutineScope = CoroutineScope(coroutineContext + Job() + CoroutineName("transfer-agent"))

    init {
        coroutineScope.launch {
            outgoingMessages.collect {
                launch {
                    transfer(it)
                }
            }
        }
    }

    private data class TransferSession(
        val id: String,
        var exchange: String?,
    ) {
        lateinit var connection: SmtpTransportConnection

        lateinit var reader: AsyncSmtpClientReader
        lateinit var writer: AsyncSmtpClientWriter

        fun updateChannels() {
            reader = connection.reader.asAsyncSmtpClientReader()
            writer = connection.writer.asAsyncSmtpClientWriter()
        }

        suspend inline fun <reified T : SmtpCommand> send(command: T) {
            logger.trace { "TRANSFER ($id/$exchange) >>> $command" }

            writer.writeCommand(command)
        }

        suspend inline fun <reified C: SmtpReply<C>, reified T: C> recv(): SmtpReply<*> {
            var reply = reader.readSmtpReply()

            logger.trace { "TRANSFER ($id/$exchange) <<< $reply" }

            if (reply is C) {
                reply = reply.tryAs<C, T>() as SmtpReply<*>
            }

            return reply
        }

        suspend inline fun <reified C: SmtpReply<C>, reified T: C> recvCoerced(): StepProgression {
            var reply = reader.readSmtpReply()

            if (reply is C) {
                (reply.tryAs<C, T>() as? SmtpReply<*>)?.let { reply = it }
            }

            logger.trace { "TRANSFER ($id/$exchange) <<< $reply" }

            return reply.coerceToStepProgression()
        }
    }

    private suspend fun transfer(mail: InternetMessage) {
        logger.info("Beginning transfer of the message ${mail.queueId}.")

        mail.envelope.recipientAddresses.forEach {
            logger.info("Beginning transfer of the message ${mail.queueId} to ${it}.")
            mail.sendTo(it)
        }

        logger.info("Finished transfer of the message ${mail.queueId}.")
    }

    private suspend fun InternetMessage.sendTo(rcpt: Path) = with(TransferSession(queueId, null)) {
        val host = when (val domain = rcpt.mailbox.domain) {
            is Domain.Actual -> domain.domain
            is Domain.AddressLiteral -> domain.networkAddress.toString()
        }

        logger.debug { "Attempting to resolve MX records for $host." }

        val mxRecords = resolver.resolveRecursively(host, GOOGLE_DNS) {
            qType = ResourceType.MX
        }?.filterIsInstance<MXResourceRecord>().orEmpty()

        logger.debug { "Resolved ${mxRecords.size} MX records.${mxRecords.joinToString(prefix = "\n", separator = "\n")}" }

        mxRecords
            .sortedBy { it.data.preference }
            .firstNotNullOfOrNull { connector.connect(it.data.exchange)?.run { it.data.exchange to this } }
            ?.let { (exchange, connection) ->
                this.exchange = exchange
                this.connection = connection
                updateChannels()
            } ?: TODO("could not connect to any exchange servers")

        var isEncrypted = false

        machine {
            step {
                recvCoerced<SmtpReply.PositiveCompletion, GreetCompletion>()
            }

            step {
                send(EhloCommand(config.domain))

                val ehlo = recv<SmtpReply.PositiveCompletion, EhloCompletion>()

                if (ehlo !is EhloCompletion) return@step StepProgression.Abort("Expected an EHLO reply, got $ehlo instead.")

                // negotiate TLS
                if (connection.isImplicitlyEncrypted || isEncrypted) return@step StepProgression.Continue

                if (!ehlo.capabilities.containsKey(STARTTLS) || !connection.supportsClientTls) {
                    if (config.requireEncryption) return@step StepProgression.Abort("Encryption is required however encryption could not be negotiated.")
                    else logger.debug { "Continuing the transfer of $queueId without any encryption!" }
                    return@step StepProgression.Continue
                }

                send(StartTlsCommand)

                when (val resp = recv<SmtpReply.PositiveCompletion, ReadyToStartTlsCompletion>()) {
                    is SmtpReply.PermanentNegative -> return@step StepProgression.Abort("STARTTLS was denied by the server!")
                    is SmtpReply.TransientNegative -> TODO("figure out what we should do")
                    else -> {}
                }

                logger.debug { "Starting TLS negotiations." }
                connection.upgradeToTls()
                isEncrypted = true
                updateChannels()
                logger.debug { "Upgraded connection to TLS."}

                StepProgression.Retry
            }

            // TODO: implement pipelining
            step {
                send(MailCommand(envelope.originatorAddress))
                recvCoerced<SmtpReply.PositiveCompletion, OkCompletion>()
            }

            step {
                send(RecipientCommand(rcpt))
                recvCoerced<SmtpReply.PositiveCompletion, OkCompletion>()
            }

            step {
                send(DataCommand)
                recvCoerced<SmtpReply.PositiveIntermediate, StartMailInputIntermediary>()
            }

            step {
                writer.writeMessageData(message)
                recvCoerced<SmtpReply.PositiveCompletion, OkCompletion>()
            }

            stop {
                if (it is StopReason.Abrupt) {
                    logger.warn { "The transfer of $queueId to $rcpt was abruptly stopped because of: ${it.reason}" }
                } else {
                    logger.info { "The transfer of $queueId to $rcpt was successful." }
                }

                send(QuitCommand)

                try {
                    recvCoerced<SmtpReply.PositiveCompletion, OkCompletion>()
                } catch (_: Throwable) {
                }

                connection.close()
            }
        }
    }
}