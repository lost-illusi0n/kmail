package dev.sitar.kmail.smtp.agent

import dev.sitar.kmail.message.Message
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
class SubmissionAgent<User : SmtpAuthenticatedUser> private constructor(
    val data: SmtpServerData,
    val server: SmtpServerTransportConnection,
    val authenticationManager: SubmissionAuthenticationManager<User>,
    coroutineContext: CoroutineContext
) {
    companion object {
        // TODO: we should take some extra information to better process incoming messages as a msu
        suspend fun <User : SmtpAuthenticatedUser> withHostname(
            host: String,
            authenticationManager: SubmissionAuthenticationManager<User>,
            client: SmtpServerTransportClient
        ): SubmissionAgent<User> {
            val connection = client.bind()
            println("SUBMISSION: STARTED LISTENING")

            return SubmissionAgent(SmtpServerData(host), connection, authenticationManager, coroutineContext)
        }
    }

    private val coroutineScope = CoroutineScope(coroutineContext + CoroutineName("SUBMISSION-AGENT"))

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

        suspend inline fun recv(): SmtpCommand {
            val resp = reader.readSmtpCommand()

            kotlin.io.println("SUBMISSION(${connection.remote}) <<< $resp")

            return resp
        }

        suspend inline fun recvChecked(): SmtpCommand? {
            return when (val resp = recv()) {
                is QuitCommand -> {
                    send(OkCompletion("Goodbye."))
                    connection.close()
                    null
                }
                else -> resp
            }
        }

        suspend inline fun <reified T : SmtpCommand> recvExpected(): T? {
            return when (val resp = recvChecked()) {
                null -> null
                is T -> resp
                else -> todo("handle unexpected command")
            }
        }

        fun todo(reason: String): Nothing {
            println(reason)
            TODO(reason)
        }
    }

    sealed interface State {
        object Established : State

        object Initiate : State

        object Initiated : State

        object Authorized : State

        data class Recipient(
            val from: String,
            val recipients: List<String>
        ) : State {
            fun progress(message: Message) = GotMail(from, recipients, message)
        }

        data class GotMail(val from: String, val recipients: List<String>, val message: Message) : State
    }

    // TODO!!: this should be a state machine
    private suspend fun listen(transport: SmtpTransportConnection) {
        println("ACCEPTED A CONNECTION FROM ${transport.remote}")

        val session = SubmissionSession(transport)

        var state: State = State.Established

        // TODO: make isUpgraded part of the transport?
        var isUpgraded = false

        var isAuthenticated = false
        var authenticatedUser: User? = null

        with(session) {
            while (true) {
                when (state) {
                    State.Established -> {
                        send(GreetCompletion("Hello, I am Kmail!"))
                        state = State.Initiate
                    }

                    State.Initiate -> {
                        recvExpected<EhloCommand>() ?: break

                        val capabilities: Map<EhloKeyword, EhloParam> = buildMap {
                            if (!isUpgraded) put("STARTTLS", null)
                            else if (!isAuthenticated) put("AUTH", "PLAIN")
                        }

                        send(EhloCompletion(data.host, "Hello, I am Kmail!", capabilities))
                        state = State.Initiated
                    }

                    State.Initiated -> {
                        // if an auth manager is present, require auth.
                        if (authenticationManager != null) {
                            if (transport.isImplicitlyEncrypted || isUpgraded) {
                                val auth = recvExpected<AuthenticationCommand>() ?: break

                                if (auth.response == null) todo("handle null initial response")

                                authenticatedUser = authenticationManager.authenticate(auth.response!!)
                                    ?: todo("could not authenticate user")

                                isAuthenticated = true

                                send(OkCompletion("Authorized."))

                                state = State.Authorized
                                continue
                            }

                            // to require auth we first require tls

                            recvExpected<StartTlsCommand>() ?: break
                            send(ReadyToStartTlsCompletion("Go ahead."))

                            transport.upgradeToTls()

                            isUpgraded = true

                            reader = transport.reader.asAsyncSmtpServerReader()
                            writer = transport.writer.asAsyncSmtpServerWriter()

                            state = State.Initiate
                            continue
                        }

                        // there is no authentication required
                        isAuthenticated = true
                        state = State.Authorized
                    }

                    State.Authorized -> {
                        val mail = recvExpected<MailCommand>() ?: break

                        val canSend = authenticatedUser?.let { authenticationManager?.canSend(it, mail.from) } ?: true

                        if (!canSend) todo("authenticated user is not authorized to send as ${mail.from}")

                        send(OkCompletion("Ok."))

                        state = State.Recipient(mail.from, emptyList())
                    }

                    is State.Recipient -> {
                        state = when (val resp = recvChecked() ?: break) {
                            is RecipientCommand -> {
                                send(OkCompletion("Ok."))

                                (state as State.Recipient).let {
                                    it.copy(recipients = it.recipients + resp.to)
                                }
                            }

                            is DataCommand -> {
                                send(StartMailInputIntermediary("End message with <CR><LF>.<CR><LF>"))

                                val message = reader.readMailInput()

                                (state as State.Recipient).progress(message)
                            }

                            else -> todo("unexpected command")
                        }
                    }

                    is State.GotMail -> (state as State.GotMail).let {
                        val message = InternetMessage(Envelope(it.from, it.recipients), it.message)
                        send(OkCompletion("Queued as ${message.queueId}"))

                        _incomingMail.send(message)

                        state = State.Authorized
                    }
                }

                println(state)
            }
        }
    }
}