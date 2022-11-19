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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

class TransferAgent private constructor(
    val data: SmtpServerData,
    val connector: SmtpServerConnector,
    val outgoingMessages: Flow<InternetMessage>,
    coroutineContext: CoroutineContext
) {
    companion object {
        private val GOOGLE_DNS = listOf("8.8.8.8", "8.8.4.4").map { DnsServer(it) } // Google's Public DNS

        suspend fun fromOutgoingMessages(hostname: String, outgoingMessages: Flow<InternetMessage>, connector: SmtpServerConnector = DefaultTransferSmtpConnector()): TransferAgent {
            return TransferAgent(SmtpServerData(hostname), connector, outgoingMessages, coroutineContext)
        }
    }

    private val resolver = dnsResolver()
    private val coroutineScope = CoroutineScope(coroutineContext + SupervisorJob() + CoroutineName("TRANSFER-AGENT"))

    init {
        println("TRANSFER: WAITING ON QUEUE")
        outgoingMessages.onEach { coroutineScope.launch { transfer(it) } }.launchIn(coroutineScope)
    }

    private data class TransferSession(
        val message: InternetMessage,
        var exchange: String?,
    ) {
        lateinit var connection: SmtpTransportConnection

        lateinit var reader: AsyncSmtpClientReader
        lateinit var writer: AsyncSmtpClientWriter

        fun updateChannels() {
            reader = connection.reader.asAsyncSmtpClientReader()
            writer = connection.writer.asAsyncSmtpClientWriter()
        }

        fun println(content: String) {
            kotlin.io.println("TRANSFER (${message.queueId}/$exchange): $content")
        }

        suspend inline fun <reified T : SmtpCommand> send(command: T) {
            kotlin.io.println("TRANSFER (${message.queueId}/$exchange) >>> $command")

            writer.writeCommand(command)
        }

        suspend inline fun <reified C: SmtpReply<C>, reified T: C> recv(): SmtpReply<*> {
            var reply = reader.readSmtpReply()

            if (reply is C) {
                reply = reply.tryAs<C, T>() as SmtpReply<*>
            }

            kotlin.io.println("TRANSFER (${message.queueId}/$exchange) <<< $reply")

            return reply
        }

        suspend inline fun <reified C: SmtpReply<C>, reified T: C> recvCoerced(): StepProgression {
            var reply = reader.readSmtpReply()

            if (reply is C) {
                (reply.tryAs<C, T>() as? SmtpReply<*>)?.let { reply = it }
            }

            kotlin.io.println("TRANSFER (${message.queueId}/$exchange) <<< $reply")

            return reply.coerceToStepProgression()
        }
    }

    // TODO: STARTTLS
    // TODO: error handling. e.g. incorrect host/message recipient syntax
    private suspend fun transfer(mail: InternetMessage) {
        val session = TransferSession(mail, null)

        with (session) {
            println("MESSAGE (${mail.queueId}) READY FOR TRANSFER")

            // TODO: make this robust
            val host = mail.envelope.recipientAddress.split('@')[1].removeSuffix(">")

            println("RESOLVING HOST $host")

            val response = resolver.resolveRecursively(host, GOOGLE_DNS) {
                qType = ResourceType.MX
            }

            println("RESOLVED ${response.orEmpty().size} MX RECORDS")
            println(response.toString())

            response.orEmpty()
                .filterIsInstance<MXResourceRecord>()
                .sortedBy { it.data.preference }
                .firstNotNullOfOrNull { connector.connect(it.data.exchange)?.run { it.data.exchange to this } }
                ?.let { (exchange, connection) ->
                    this.exchange = exchange
                    this.connection = connection
                } ?: TODO("could not connect to any exchange servers")

            connection = connector.connect(exchange!!) ?: error("no mx records work")
            updateChannels()

            var isEncrypted = false

            machine {
                step {
                    recvCoerced<SmtpReply.PositiveCompletion, GreetCompletion>()
                }

                step {
                    send(EhloCommand(data.host))

                    val ehlo = recv<SmtpReply.PositiveCompletion, EhloCompletion>()

                    if (ehlo !is EhloCompletion) return@step StepProgression.Abort("EXPECTED EHLO REPLY. GOT: $ehlo")

                    if (connection.isImplicitlyEncrypted) return@step StepProgression.Continue

                    if (isEncrypted || !ehlo.capabilities.containsKey(STARTTLS)) return@step StepProgression.Continue

                    send(StartTlsCommand)

                    when (val resp = recv<SmtpReply.PositiveCompletion, ReadyToStartTlsCompletion>()) {
                        is SmtpReply.PermanentNegative -> return@step StepProgression.Abort("RECEIVED NO TO STARTTLS: $resp")
                        is SmtpReply.TransientNegative -> TODO("figure out what we should do")
                        else -> {}
                    }

                    connection.upgradeToTls()
                    isEncrypted = true
                    updateChannels()

                    println("SUCCESSFULLY UPGRADED TO TLS")

                    StepProgression.Retry
                }

                // TODO: implement pipelining
                step {
                    send(MailCommand(mail.envelope.originatorAddress))
                    recvCoerced<SmtpReply.PositiveCompletion, OkCompletion>()
                }

                step {
                    send(RecipientCommand(mail.envelope.recipientAddress))
                    recvCoerced<SmtpReply.PositiveCompletion, OkCompletion>()
                }

                step {
                    send(DataCommand)
                    recvCoerced<SmtpReply.PositiveIntermediate, StartMailInputIntermediary>()
                }

                step {
                    send(MailInputCommand(mail.message))
                    recvCoerced<SmtpReply.PositiveCompletion, OkCompletion>()
                }

                stop {
                    if (it is StopReason.Abrupt) println("STOPPING TRANSFER SESSION DUE TO: ${it.reason}")

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
}