package dev.sitar.kmail.smtp.agent

import dev.sitar.dns.dnsResolver
import dev.sitar.dns.records.MXResourceRecord
import dev.sitar.dns.records.ResourceType
import dev.sitar.dns.transports.DnsServer
import dev.sitar.kmail.smtp.*
import dev.sitar.kmail.smtp.io.smtp.reader.AsyncSmtpClientReader
import dev.sitar.kmail.smtp.io.smtp.reader.asAsyncSmtpClientReader
import dev.sitar.kmail.smtp.io.smtp.writer.AsyncSmtpClientWriter
import dev.sitar.kmail.smtp.io.smtp.writer.asAsyncSmtpClientWriter
import dev.sitar.kmail.smtp.io.toAsyncByteChannelWriter
import dev.sitar.kmail.smtp.io.toAsyncByteReadChannelReader
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

class TransferAgent private constructor(
    val data: SmtpServerData,
    val outgoingMessages: Flow<Message>,
    coroutineContext: CoroutineContext
) {
    companion object {
        private val GOOGLE_DNS = listOf("8.8.8.8", "8.8.4.4").map { DnsServer(it) } // Google's Public DNS

        suspend fun fromOutgoingMessages(hostname: String, outgoingMessages: Flow<Message>): TransferAgent {
            return TransferAgent(SmtpServerData(hostname), outgoingMessages, coroutineContext)
        }
    }

    private val resolver = dnsResolver()
    private val coroutineScope = CoroutineScope(coroutineContext + SupervisorJob() + CoroutineName("TRANSFER-AGENT"))

    init {
        println("TRANSFER: WAITING ON QUEUE")
        outgoingMessages.onEach { coroutineScope.launch { transfer(it) } }.launchIn(coroutineScope)
    }

    private data class TransferSession(
        val message: Message,
        var exchange: String?,
    ) {
        lateinit var reader: AsyncSmtpClientReader
        lateinit var writer: AsyncSmtpClientWriter

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

    // TODO: error handling. e.g. incorrect host/message recipient syntax
    private suspend fun transfer(message: Message) {
        val session = TransferSession(message, null)

        with (session) {
            println("MESSAGE (${message.queueId}) READY FOR TRANSFER")

            val host = message.rcpt.to.split('@')[1].removeSuffix(">")

            println("RESOLVING HOST $host")

            val response = resolver.resolveRecursively(host, GOOGLE_DNS) {
                qType = ResourceType.MX
            }

            println("RESOLVED ${response.orEmpty().size} MX RECORDS")

            exchange = response.orEmpty().filterIsInstance<MXResourceRecord>().minBy { it.data.preference }.data.exchange

            val transferSocket = aSocket(SelectorManager(Dispatchers.Default)).tcp().connect(exchange!!, port = 25)
            reader = transferSocket.openReadChannel().toAsyncByteReadChannelReader().asAsyncSmtpClientReader()
            writer = transferSocket.openWriteChannel().toAsyncByteChannelWriter().asAsyncSmtpClientWriter()

            recv<GreetReply>()

            send(EhloCommand(data.host))
            recv<EhloReply>()

            send(message.mail)
            recv<OkReply>()

            send(message.rcpt)
            recv<OkReply>()

            send(DataCommand)
            recv<StartMailInputReply>()

            send(message.message)
            recv<OkReply>()

            send(QuitCommand)
            recv<OkReply>()
        }
    }
}