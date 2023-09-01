package dev.sitar.kmail.agents.smtp.submission

import dev.sitar.kmail.agents.smtp.connections.ServerConnection
import dev.sitar.kmail.agents.smtp.connections.ServerExtension
import dev.sitar.kmail.agents.smtp.connections.StartTlsExtension
import dev.sitar.kmail.agents.smtp.transports.server.SmtpCommandContext
import dev.sitar.kmail.agents.smtp.transports.server.SmtpCommandPipeline
import dev.sitar.kmail.agents.smtp.transports.server.SmtpServerTransport
import dev.sitar.kmail.smtp.AuthenticationCommand
import dev.sitar.kmail.smtp.Domain
import dev.sitar.kmail.smtp.MailCommand
import dev.sitar.kmail.smtp.SmtpCommand
import dev.sitar.kmail.smtp.frames.replies.SmtpReply

class SubmissionAgent(
    transport: SmtpServerTransport,
    config: SubmissionConfig,
): ServerConnection(transport, config.domain) {
    override val extensions: Set<ServerExtension> = setOf(
        StartTlsExtension(this),
        AuthenticationExtension(this, config.authenticationManager)
    )
}

data class SubmissionConfig(
    val domain: Domain,
    val requiresEncryption: Boolean,
    val authenticationManager: SubmissionAuthenticationManager<*>?
)

class AuthenticationExtension(override val server: ServerConnection, private val authenticationManager: SubmissionAuthenticationManager<*>?): ServerExtension {
    var isAuthenticated: Boolean = false
        private set

    var user: SmtpAuthenticatedUser? = null

    override fun apply() {
        if (authenticationManager == null) return

        server.transport.commandPipeline.filter(SmtpCommandPipeline.Process) {
            require(this is SmtpCommandContext.Known)

            if (command.requiresAuthentication && user == null) {
                continuePropagation = false

                server.transport.send(SmtpReply.PermanentNegative.Default(530, listOf("authentication is required.")))
                TODO("command requires authentication")
            }

            if (command !is AuthenticationCommand) return@filter

            require(server.transport.isSecure)

            if (command.response == null) TODO("null response")

            user = authenticationManager.authenticate(command.response!!)

            if (user == null) TODO("not authenticated")

            isAuthenticated = true

            server.transport.send(SmtpReply.PositiveCompletion.Default(235, listOf("Authorized.")))
        }
    }

    override fun capabilities(): Set<String> {
        return when {
            isAuthenticated -> emptySet()
            server.transport.isSecure && authenticationManager != null -> setOf("AUTH PLAIN")
            else -> emptySet()
        }
    }
}

private val SmtpCommand.requiresAuthentication: Boolean
    get() = when(this) {
        is MailCommand -> true
        else -> false
    }

//TODO: THE DOMAIN SHOULD POINT TO ADDRESS LITERAL OR A DOMAIN THAT RESOLVES TO A RR
private const val GREET = "localhost ESMTP the revolutionary kmail :-)"


// email client -> submission server connection
//class SubmissionAgent(
//    private val config: SubmissionConfig,
//    private val authenticationManager: SubmissionAuthenticationManager<*>?,
//    private var transport: SmtpServerTransport,
//    private val scope: CoroutineScope
//) {
//    private var isAuthenticated: Boolean = false
//    private var state: SubmissionState = SubmissionState.Initial
//
//    private var user: SmtpAuthenticatedUser? = null
//
//    private val capabilities: List<String> get() = buildList {
//        if (!transport.isSecure) add(Capabilities.STARTTLS)
//        else if (!isAuthenticated && authenticationManager != null) add(Capabilities.AUTH_PLAIN)
//    }
//
//    suspend fun handle(incoming: MutableSharedFlow<InternetMessage>) {
//        transport.send(GreetCompletion(GREET))
//
//        while (scope.isActive) {
//            when (state) {
//                SubmissionState.Initial -> initial()
//                SubmissionState.Initiated -> initiated()
//                SubmissionState.Mail -> incoming.emit(mail())
//            }
//        }
//    }
//
//    private suspend fun initial() {
//        when (val command = transport.recv()) {
//            is EhloCommand -> {
//                transport.send(EhloCompletion(config.domain, GREET, capabilities))
//                state = SubmissionState.Initiated
//            }
//            else -> {}
//        }
//    }
//
//    private suspend fun initiated() {
//        if (authenticationManager != null) {
//            if (!transport.isSecure) {
//                transport.recv() as StartTlsCommand
//                transport.send(ReadyToStartTlsCompletion("start tls."))
//
//                transport = transport.secure()
//
//                state = SubmissionState.Initial
//                return
//            }
//
//            val auth = transport.recv() as AuthenticationCommand
//
//            if (auth.response == null) TODO("handle null inital response")
//
//            user = authenticationManager.authenticate(auth.response!!)
//
//            if (user == null) {
//                transport.send(SmtpReply.PermanentNegative.Default(535, listOf("unauthorized")))
//                TODO("stop")
//            }
//
//            isAuthenticated = true
//
//            transport.send(SmtpReply.PositiveCompletion.Default(235, listOf("Authorized.")))
//        }
//
//        state = SubmissionState.Mail
//    }
//
//    private suspend fun mail(): InternetMessage {
//        val mail = transport.recv() as MailCommand
//
//        if (!user.canSend(mail)) TODO("not authenticated")
//
//        transport.send(OkCompletion("Ok."))
//
//        val rcpts = mutableListOf<Path>()
//        val message: Message
//
//        while (true) {
//            when (val command = transport.recv()) {
//                is RecipientCommand -> {
//                    transport.send(OkCompletion("Ok."))
//
//                    rcpts.add(command.to)
//                }
//                is DataCommand -> {
//                    transport.send(StartMailInputIntermediary("End message with <CR><LF>.<CR><LF>"))
//
//                    message = transport.recvMail()
//
//                    break
//                }
//                else -> error("unexpected command: $command")
//            }
//        }
//
//        val internetMessage = InternetMessage(Envelope(mail.from, rcpts), message)
//
//        transport.send(OkCompletion("Queued as ${internetMessage.queueId}"))
//
//        // TODO: make this not so happy go lucky
//        transport.recv() as QuitCommand
//        transport.send(SmtpReply.PositiveCompletion.Default(code = 221))
//
//        println("important")
//        return internetMessage
//    }
//
//    private fun SmtpAuthenticatedUser?.canSend(mail: MailCommand): Boolean = user.let {
//        if (it == null) return true
//
//        (authenticationManager as SubmissionAuthenticationManager<SmtpAuthenticatedUser>).canSend(it, mail.from)
//    }
//}