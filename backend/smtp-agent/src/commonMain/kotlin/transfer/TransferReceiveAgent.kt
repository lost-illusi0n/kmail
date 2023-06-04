package dev.sitar.kmail.agents.smtp.transfer

import dev.sitar.kmail.agents.smtp.rewrite.ServerConnection
import dev.sitar.kmail.agents.smtp.rewrite.ServerExtension
import dev.sitar.kmail.agents.smtp.rewrite.StartTlsExtension
import dev.sitar.kmail.agents.smtp.transports.server.SmtpServerTransport
import dev.sitar.kmail.smtp.Domain

//TODO: THE DOMAIN SHOULD POINT TO ADDRESS LITERAL OR A DOMAIN THAT RESOLVES TO A RR
private const val GREET = "localhost ESMTP the revolutionary kmail :-)"

class TransferReceiveAgent(
    transport: SmtpServerTransport,
    config: TransferReceiveConfig
): ServerConnection(transport, config.domain) {
    override val extensions: Set<ServerExtension> = setOf(
        StartTlsExtension(this)
    )
}
data class TransferReceiveConfig(
    val domain: Domain,
    val requiresEncryption: Boolean
)

//class TransferReceiveAgent(
//    private val config: TransferReceiveConfig,
//    private var transport: SmtpServerTransport,
//    private val scope: CoroutineScope
//) {
//    private var state: SubmissionState = SubmissionState.Initial
//
//    private val capabilities: List<String> get() = buildList {
//        if (!transport.isSecure) add(Capabilities.STARTTLS)
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
//        if (!transport.isSecure) {
//            transport.recv() as StartTlsCommand
//            transport.send(ReadyToStartTlsCompletion("start tls."))
//
//            transport.secure()
//
//            state = SubmissionState.Initial
//            return
//        }
//
//        state = SubmissionState.Mail
//    }
//
//    private suspend fun mail(): InternetMessage {
//        val mail = transport.recv() as MailCommand
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
//        return internetMessage
//    }
//}