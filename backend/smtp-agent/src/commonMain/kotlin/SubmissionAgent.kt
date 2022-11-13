package dev.sitar.kmail.smtp.agent

import dev.sitar.kmail.smtp.*
import dev.sitar.kmail.smtp.io.smtp.reader.AsyncSmtpServerReader
import dev.sitar.kmail.smtp.io.smtp.reader.asAsyncSmtpServerReader
import dev.sitar.kmail.smtp.io.smtp.writer.AsyncSmtpServerWriter
import dev.sitar.kmail.smtp.io.smtp.writer.asAsyncSmtpServerWriter
import dev.sitar.kmail.smtp.io.toAsyncByteChannelWriter
import dev.sitar.kmail.smtp.io.toAsyncByteReadChannelReader
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

// TODO: authentication
// TODO: transport encryption
class SubmissionAgent private constructor(
    val data: SmtpServerData,
    val socket: ServerSocket,
    coroutineContext: CoroutineContext
) {
    companion object {
        // TODO: we should take some extra information to better process incoming messages as a msu
        suspend fun withHostname(host: String): SubmissionAgent {
            val submissionSocket = aSocket(SelectorManager(Dispatchers.Default)).tcp().bind(port = 587)
            println("SUBMISSION: STARTED LISTENING ON PORT 587")

            return SubmissionAgent(SmtpServerData(host), submissionSocket, coroutineContext)
        }
    }

    private val coroutineScope = CoroutineScope(coroutineContext + SupervisorJob() + CoroutineName("SUBMISSION-AGENT"))

    private val _incomingMail: Channel<Message> = Channel()

    val incomingMail: Flow<Message> = _incomingMail.consumeAsFlow()
    suspend fun start() = coroutineScope {
        while (isActive) {
            val socket = socket.accept()

            launch { listen(socket) }
        }
    }

    private data class SubmissionSession(
        val remoteAddress: SocketAddress,
        val reader: AsyncSmtpServerReader,
        val writer: AsyncSmtpServerWriter,
    ) {
        fun println(message: String) {
            kotlin.io.println("SUBMISSION(${remoteAddress}): $message")
        }

        suspend inline fun <reified T: SmtpReply> send(status: Int, reply: T) {
            kotlin.io.println("SUBMISSION(${remoteAddress}) >>> $reply")

            writer.writeReply(status, reply)
        }

        suspend inline fun <reified T: SmtpCommand> recv(): T {
            val resp = reader.readSmtpCommand<T>()

            kotlin.io.println("SUBMISSION(${remoteAddress}) >>> $resp")

            return resp
        }
    }

    private suspend fun listen(socket: Socket) {
        println("ACCEPTED A CONNECTION FROM ${socket.remoteAddress}")

        val reader = socket.openReadChannel().toAsyncByteReadChannelReader().asAsyncSmtpServerReader()
        val writer = socket.openWriteChannel().toAsyncByteChannelWriter().asAsyncSmtpServerWriter()

        val session = SubmissionSession(socket.remoteAddress, reader, writer)

        with (session) {
            send(220, GreetReply("Hello, I am Kmail!"))

            val ehlo = recv<EhloCommand>()
            send(250, EhloReply(data.host, "Hello, I am Kmail!", emptyMap()))

            val mail = recv<MailCommand>()
            send(221, OkReply("Ok."))

            val rcpt = recv<RecipientCommand>()
            send(221, OkReply("Ok."))

            recv<DataCommand>()
            send(354, StartMailInputReply)

            val messageCommand = recv<MessageCommand>()

            val message = Message(ehlo, mail, rcpt, messageCommand)
            _incomingMail.send(message)

            println("QUEUED MESSAGE ${message.queueId}")

            send(221, OkReply("Queued as ${message.queueId}"))

            // TODO: this should be a state machine and the client should be able to make a new mail or quit.
            recv<QuitCommand>()
            send(221, OkReply("Goodbye."))

            socket.close()
        }
    }
}