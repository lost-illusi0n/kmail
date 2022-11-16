package dev.sitar.kmail.smtp.agent

import dev.sitar.dns.dnsResolver
import dev.sitar.dns.records.MXResourceRecord
import dev.sitar.dns.records.ResourceType
import dev.sitar.dns.transports.DnsServer
import dev.sitar.kmail.smtp.*
import dev.sitar.kmail.smtp.agent.transports.client.SmtpTransportConnection
import dev.sitar.kmail.smtp.io.smtp.reader.AsyncSmtpClientReader
import dev.sitar.kmail.smtp.io.smtp.reader.asAsyncSmtpClientReader
import dev.sitar.kmail.smtp.io.smtp.writer.AsyncSmtpClientWriter
import dev.sitar.kmail.smtp.io.smtp.writer.asAsyncSmtpClientWriter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

        val reader: AsyncSmtpClientReader by lazy { connection.reader.asAsyncSmtpClientReader() }
        val writer: AsyncSmtpClientWriter by lazy { connection.writer.asAsyncSmtpClientWriter() }

        fun println(content: String) {
            kotlin.io.println("TRANSFER (${message.queueId}/$exchange): $content")
        }

        suspend inline fun <reified T: SmtpCommand> send(command: T) {
            kotlin.io.println("TRANSFER (${message.queueId}/$exchange) >>> $command")

            writer.writeCommand(command)
        }

        suspend inline fun <reified T: SmtpReply> recv(): T {
            val reply = reader.readSmtpReply<T>()

            kotlin.io.println("TRANSFER (${message.queueId}/$exchange) <<< $reply")

            return reply
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

            recv<GreetReply>()

            send(EhloCommand(data.host))
            recv<EhloReply>()

            send(MailCommand(mail.envelope.originatorAddress))
            recv<OkReply>()

            send(RecipientCommand(mail.envelope.recipientAddress))
            recv<OkReply>()

            send(DataCommand)
            recv<StartMailInputReply>()

            send(MailInputCommand(mail.message))
            recv<OkReply>()

            send(QuitCommand)
            recv<OkReply>()
        }
    }
}