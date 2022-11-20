package dev.sitar.kmail.smtp.agent

import dev.sitar.kmail.smtp.*
import dev.sitar.kmail.smtp.agent.transports.client.SmtpTransportConnection
import dev.sitar.kmail.smtp.agent.transports.server.SmtpServerTransportClient
import dev.sitar.kmail.smtp.agent.transports.server.SmtpServerTransportConnection
import dev.sitar.kmail.smtp.frames.replies.*
import dev.sitar.kmail.smtp.io.smtp.reader.asAsyncSmtpServerReader
import dev.sitar.kmail.smtp.io.smtp.writer.asAsyncSmtpServerWriter
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

// TODO: transport encryption
class SubmissionAgent private constructor(
    val data: SmtpServerData,
    val server: SmtpServerTransportConnection,
    val authenticationManager: SubmissionAuthenticationManager<out SmtpAuthenticatedUser>?,
    coroutineContext: CoroutineContext
) {
    companion object {
        // TODO: we should take some extra information to better process incoming messages as a msu
        suspend fun withHostname(
            host: String,
            authenticationManager: SubmissionAuthenticationManager<*>? = null,
            client: SmtpServerTransportClient
        ): SubmissionAgent {
            val connection = client.bind()
            println("SUBMISSION: STARTED LISTENING")

            return SubmissionAgent(SmtpServerData(host), connection, authenticationManager, coroutineContext)
        }
    }

    private val coroutineScope = CoroutineScope(coroutineContext + SupervisorJob() + CoroutineName("SUBMISSION-AGENT"))

    private val _incomingMail: Channel<InternetMessage> = Channel()

    val incomingMail: Flow<InternetMessage> = _incomingMail.consumeAsFlow()
    suspend fun start() = coroutineScope {
        while (isActive) {
            val socket = server.accept()

            launch { listen(socket) }
        }
    }

    private data class SubmissionSession(
        val connection: SmtpTransportConnection
    ) {
        var reader = connection.reader.asAsyncSmtpServerReader()
        var writer = connection.writer.asAsyncSmtpServerWriter()

        fun println(message: String) {
            kotlin.io.println("SUBMISSION(${connection.remote}): $message")
        }

        suspend inline fun <reified T : SmtpReply<*>> send(reply: T) {
            kotlin.io.println("SUBMISSION(${connection.remote}) >>> $reply")

            writer.writeReply(reply)
        }

        suspend inline fun <reified T: SmtpCommand> recv(): T {
            val resp = reader.readSmtpCommand<T>()

            kotlin.io.println("SUBMISSION(${connection.remote}) <<< $resp")

            return resp
        }
    }

    // TODO!!: this should be a state machine
    private suspend fun listen(transport: SmtpTransportConnection) {
        println("ACCEPTED A CONNECTION FROM ${transport.remote}")

        val session = SubmissionSession(transport)

        with (session) {
            send(GreetCompletion("Hello, I am Kmail!"))

            recv<EhloCommand>()
            send(EhloCompletion(data.host, "Hello, I am Kmail!", mapOf("STARTTLS" to null)))

            recv<StartTlsCommand>()
            send(ReadyToStartTlsCompletion("Go ahead."))
            transport.upgradeToTls()
            reader = transport.reader.asAsyncSmtpServerReader()
            writer = transport.writer.asAsyncSmtpServerWriter()
            println("UPGRADED")

            val ehlo = recv<EhloCommand>()
            send(EhloCompletion(data.host, "Hello, I am Kmail!", mapOf("AUTH" to "PLAIN")))

            val auth = recv<AuthenticationCommand>()
            requireNotNull(authenticationManager)

            val user = authenticationManager.authenticate(auth.response!!)
            requireNotNull(user)
            send(OkCompletion("Authenticated."))

            val mail = recv<MailCommand>()
            send(OkCompletion("Ok."))

            require(
                (authenticationManager as SubmissionAuthenticationManager<in SmtpAuthenticatedUser>).canSend(
                    user,
                    mail.from
                )
            )

            val rcpt = recv<RecipientCommand>()
            send(OkCompletion("Ok."))

            recv<DataCommand>()
            send(StartMailInputIntermediary("spill the tea."))

            val mailInput = recv<MailInputCommand>()
            val internetMessage = InternetMessage(Envelope(mail.from, rcpt.to), mailInput.message)
            _incomingMail.send(internetMessage)

            println("QUEUED MESSAGE ${internetMessage.queueId}")

            send(OkCompletion("Queued as ${internetMessage.queueId}"))

            recv<QuitCommand>()
            send(OkCompletion("Goodbye."))

            connection.close()
        }
    }
}