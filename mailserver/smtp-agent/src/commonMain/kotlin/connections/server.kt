package dev.sitar.kmail.agents.smtp.connections

import dev.sitar.kmail.agents.smtp.queueId
import dev.sitar.kmail.agents.smtp.transports.server.SmtpCommandContext
import dev.sitar.kmail.agents.smtp.transports.server.SmtpCommandPipeline
import dev.sitar.kmail.agents.smtp.transports.server.SmtpServerTransport
import dev.sitar.kmail.message.Message
import dev.sitar.kmail.smtp.*
import dev.sitar.kmail.smtp.frames.replies.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

abstract class ServerConnection(val transport: SmtpServerTransport, private val domain: Domain, addresses: List<String>? = null) {
    abstract val extensions: Set<ServerExtension>

    private val mail by lazy { MailExtension(this, addresses) }

    val incoming by lazy { mail.incoming.consumeAsFlow() }

    val allExtensions by lazy { extensions + setOf(EhloExtension(this, domain), QuitExtension(this), mail) }

    suspend fun handle() {
        transport.commandPipeline {
            filter(SmtpCommandPipeline.Logging) {
                when (this) {
                    is SmtpCommandContext.Known -> logger.trace { "FROM ${transport.remote}: $command" }
                    is SmtpCommandContext.Unknown -> {
                        logger.warn(cause) { "FROM ${transport.remote}: unknown!" }
                        continuePropagation = false
                        transport.send(SmtpReply.PermanentNegative.Default(code = 500))
                        close()
                    }
                }
            }
        }

        allExtensions.forEach(ServerExtension::apply)

        transport.send(GreetCompletion("${domain.asString()} ESMTP the revolutionary kmail :-)"))
    }

    fun close() {
        transport.close()
    }
}

interface ServerExtension {
    val server: ServerConnection

    fun capabilities(): Set<String> = emptySet()

    fun apply()
}

class QuitExtension(override val server: ServerConnection): ServerExtension {
    override fun apply() {
        server.transport.commandPipeline {
            filter(SmtpCommandPipeline.Process) {
                require(this is SmtpCommandContext.Known)

                if (command is QuitCommand) {
                    continuePropagation = false

                    server.transport.send(SmtpReply.PositiveCompletion.Default(code = 221, lines = listOf("goodbye, thanks for mail.")))

                    server.close()
                }
            }
        }
    }
}

class EhloExtension(override val server: ServerConnection, val domain: Domain): ServerExtension {
    override fun apply() {
        server.transport.commandPipeline.filter(SmtpCommandPipeline.Process) {
            require(this is SmtpCommandContext.Known)

            if (command is EhloCommand) {
                val capabilities = server.allExtensions.flatMap { it.capabilities() }

                server.transport.send(EhloCompletion(domain, "${domain.asString()} ESMTP the revolutionary kmail :-)", capabilities))
            }
        }
    }
}

class StartTlsExtension(override val server: ServerConnection): ServerExtension {
    override fun apply() {
        server.transport.commandPipeline {
            filter(SmtpCommandPipeline.Process) {
                require(this is SmtpCommandContext.Known)

                if (command is StartTlsCommand) {
                    require(!server.transport.isSecure)
                    server.transport.send(ReadyToStartTlsCompletion("start tls."))

                    logger.debug { "Starting TLS negotiations. ${System.currentTimeMillis()}" }

                    server.transport.secure()

                    logger.debug { "Upgraded connection to TLS. ${System.currentTimeMillis()}"}

                    continuePropagation = false
                }
            }
        }
    }

    override fun capabilities(): Set<String> {
        return if (server.transport.isSecure) emptySet() else setOf("STARTTLS")
    }
}

class MailExtension(override val server: ServerConnection, val addresses: List<String>?) : ServerExtension {
    val incoming: Channel<InternetMessage> = Channel()

    private class State(
        val mail: MailCommand,
        val rcpts: MutableList<Path> = mutableListOf(),
        var msg: Message? = null
    ) {
        fun toInternetMessage() = InternetMessage(Envelope(mail.from, rcpts), msg!!)
    }

    private var state: State? = null

    override fun apply() {
        server.transport.scope.coroutineContext[Job]!!.invokeOnCompletion {
            incoming.close(it)
        }

        // TODO: spam filtering???
        server.transport.commandPipeline {
            filter(SmtpCommandPipeline.Process) {
                require(this is SmtpCommandContext.Known)

                if (command !is MailCommand) return@filter

                state = State(command)

                server.transport.send(OkCompletion("Ok."))
            }

            filter(SmtpCommandPipeline.Process) {
                require(this is SmtpCommandContext.Known)

                if (command !is RecipientCommand) return@filter

                if (addresses?.contains(command.to.mailbox.asText()) == false) {
                    server.transport.send(SmtpReply.PermanentNegative.Default(550, listOf("unknown user: ${command.to.mailbox.asText()}")))
                    return@filter
                }
                state!!.rcpts.add(command.to)

                server.transport.send(OkCompletion("Ok."))
            }

            filter(SmtpCommandPipeline.Process) {
                require(this is SmtpCommandContext.Known)

                if (command !is DataCommand) return@filter

                server.transport.send(StartMailInputIntermediary("End message with <CR><LF>.<CR><LF>"))

                state!!.msg = server.transport.recvMail()

                val internetMessage = state!!.toInternetMessage()

                state = null

                server.transport.send(OkCompletion("Queued as ${internetMessage.queueId}"))

                incoming.send(internetMessage)
            }
        }
    }
}
