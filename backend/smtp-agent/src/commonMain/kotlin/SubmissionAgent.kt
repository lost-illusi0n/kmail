package dev.sitar.kmail.smtp.agent

import dev.sitar.kmail.message.Message
import dev.sitar.kmail.message.headers.Headers
import dev.sitar.kmail.message.headers.messageId
import dev.sitar.kmail.smtp.*
import dev.sitar.kmail.smtp.agent.transports.client.SmtpTransportConnection
import dev.sitar.kmail.smtp.agent.transports.server.SmtpServerTransportConnection
import dev.sitar.kmail.smtp.frames.replies.*
import dev.sitar.kmail.smtp.io.smtp.reader.asAsyncSmtpServerReader
import dev.sitar.kmail.smtp.io.smtp.writer.asAsyncSmtpServerWriter
import io.ktor.util.date.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.*
import mu.KotlinLogging
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private val logger = KotlinLogging.logger { }

data class SubmissionConfig(
    val domain: Domain,
    val requiresEncryption: Boolean
)

class SubmissionAgent<User : SmtpAuthenticatedUser>(
    val config: SubmissionConfig,
    val serverTransport: SmtpServerTransportConnection,
    val authenticationManager: SubmissionAuthenticationManager<User>,
    coroutineContext: CoroutineContext = EmptyCoroutineContext
) {
    private val scope = CoroutineScope(coroutineContext + Job() + CoroutineName("submission-agent"))

    private val rawMail: Channel<InternetMessage> = Channel()

    val incomingMail: SharedFlow<InternetMessage> = rawMail
        .consumeAsFlow()
        .map { InternetMessage(it.envelope, validateMail(it.message)) }
        .shareIn(scope, SharingStarted.Eagerly)

    fun launch() {
        scope.launch {
            while (isActive) {
                val socket = serverTransport.accept()
                logger.debug("Accepted a connection from ${socket.remote}.")

                launch { listen(socket) }
            }
        }
    }

    private data class SubmissionSession(
        val connection: SmtpTransportConnection
    ) {
        var reader = connection.reader.asAsyncSmtpServerReader()
        var writer = connection.writer.asAsyncSmtpServerWriter()

        suspend fun send(reply: SmtpReply<*>) {
            logger.trace { "SUBMISSION(${connection.remote}) >>> $reply" }

            writer.writeReply(reply)
        }

        suspend inline fun recv(): SmtpCommand {
            val resp = reader.readSmtpCommand()

            logger.trace { "SUBMISSION(${connection.remote}) <<< $resp" }

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
            logger.error(reason)
            TODO(reason)
        }
    }

    sealed interface State {
        object Established : State

        object Initiate : State

        object Initiated : State

        object Authorized : State

        data class Recipient(
            val from: Path,
            val recipients: List<Path>
        ) : State {
            fun progress(message: Message) = GotMail(from, recipients, message)
        }

        data class GotMail(val from: Path, val recipients: List<Path>, val message: Message) : State
    }

    private suspend fun listen(transport: SmtpTransportConnection) {
        logger.info { "Accepted submission from ${transport.remote}." }

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
                            if ((!transport.isImplicitlyEncrypted || !isUpgraded) && transport.supportsServerTls) {
                                put("STARTTLS", null)
                                if (!isAuthenticated) put("AUTH", "PLAIN")
                            } else if (config.requiresEncryption) {
                                logger.error("Server requires encryption but it cannot provide it!")
                                TODO("handle no encryption.")
                            }
                        }

                        send(EhloCompletion(config.domain, "Hello, I am Kmail!", capabilities))
                        state = State.Initiated
                    }

                    State.Initiated -> {
                        // if an auth manager is present, require auth.
                        if (authenticationManager != null) {
                            if (transport.isImplicitlyEncrypted || isUpgraded) {
                                val auth = recvExpected<AuthenticationCommand>() ?: break

                                if (auth.response == null) todo("handle null initial response")

                                authenticatedUser = authenticationManager.authenticate(auth.response!!)

                                if (authenticatedUser == null) {
                                    send(SmtpReply.PermanentNegative.Default(535, listOf("Unauthorized.")))
                                    continue
                                }

                                isAuthenticated = true

                                send(SmtpReply.PositiveCompletion.Default(235, listOf("Authorized.")))

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
                        logger.info("Submission has received email as ${message.queueId}.\n{}", message)
                        send(OkCompletion("Queued as ${message.queueId}"))

                        rawMail.send(message)

                        state = State.Authorized
                    }
                }

                logger.debug { "Submission from ${transport.remote} is now $state." }
            }

            logger.info { "Submission from ${transport.remote} has been closed." }
        }
    }

    fun close() {
        serverTransport.close()
        scope.cancel()
    }
}

private fun SubmissionAgent<*>.validateMail(mail: Message): Message {
    val headers = mail.headers.toMutableSet()

    if (Headers.MessageId !in mail.headers) {
        headers += messageId("<${getTimeMillis()}@${config.domain.asString()}>")
    }

    return Message(Headers(headers), mail.body)
}