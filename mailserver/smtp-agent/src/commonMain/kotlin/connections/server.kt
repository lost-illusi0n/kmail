package dev.sitar.kmail.agents.smtp.connections

import dev.sitar.kmail.agents.smtp.queueId
import dev.sitar.kmail.agents.smtp.transports.server.SmtpCommandContext
import dev.sitar.kmail.agents.smtp.transports.server.SmtpCommandPipeline
import dev.sitar.kmail.agents.smtp.transports.server.SmtpServerTransport
import dev.sitar.kmail.message.Message
import dev.sitar.kmail.smtp.*
import dev.sitar.kmail.smtp.frames.replies.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import mu.KotlinLogging
import java.lang.Exception

private val logger = KotlinLogging.logger { }

fun interface ServerSink {
    suspend fun send(message: InternetMessage)
}

abstract class ServerConnection(val transport: SmtpServerTransport, private val domain: Domain, addresses: List<String>? = null) {
    abstract val extensions: Set<ServerExtension>

    private val mail by lazy { MailExtension(this, addresses) }

    val incoming by lazy { mail.incoming.consumeAsFlow() }

    val allExtensions by lazy { extensions + setOf(EhloExtension(this, domain), QuitExtension(this), mail) }

    suspend fun handleAndPipeTo(sink: ServerSink) = coroutineScope {
        launch { transport.startPipeline() }

        // send incoming email to the sink
        incoming.onEach { sink.send(it) }.launchIn(this)

        transport.commandPipeline {
            filter(SmtpCommandPipeline.Logging) {
                logger.trace { "SMTP (port: ) <<< $command" }
            }

            filter(SmtpCommandPipeline.After) {

            }
        }

        allExtensions.forEach { it.apply(this) }

        transport.send(GreetCompletion("${domain.asString()} ESMTP the revolutionary kmail :-)"))
    }
}

interface ServerExtension {
    val server: ServerConnection

    fun capabilities(): Set<String> = emptySet()

    fun apply(scope: CoroutineScope)
}

class QuitExtension(override val server: ServerConnection): ServerExtension {
    override fun apply(scope: CoroutineScope) {
        server.transport.commandPipeline {
            filter(SmtpCommandPipeline.Processing) {
                if (command is QuitCommand) {
                    wasProcessed = true

                    try {
                        server.transport.send(SmtpReply.PositiveCompletion.Default(code = 221, lines = listOf("goodbye, thanks for mail.")))
                    } catch (_: Exception) { }

                    scope.cancel()
                    server.transport.connection.close()
                    awaitCancellation()
                }
            }
        }
    }
}

class EhloExtension(override val server: ServerConnection, val domain: Domain): ServerExtension {
    override fun apply(scope: CoroutineScope) {
        server.transport.commandPipeline.filter(SmtpCommandPipeline.Processing) {
            if (command !is EhloCommand) return@filter

            wasProcessed = true

            val capabilities = server.allExtensions.flatMap { it.capabilities() }

            server.transport.send(EhloCompletion(domain, "${domain.asString()} ESMTP the revolutionary kmail :-)", capabilities))
        }
    }
}

class StartTlsExtension(override val server: ServerConnection): ServerExtension {
    override fun apply(scope: CoroutineScope) {
        server.transport.commandPipeline {
            filter(SmtpCommandPipeline.Processing) {
                if (command !is StartTlsCommand) return@filter

                wasProcessed = true

                require(!server.transport.isSecure)
                server.transport.send(ReadyToStartTlsCompletion("start tls."))

                logger.debug { "Starting TLS negotiations. ${System.currentTimeMillis()}" }

                server.transport.secure()

                logger.debug { "Upgraded connection to TLS. ${System.currentTimeMillis()}"}
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

    override fun apply(scope: CoroutineScope) {
        scope.coroutineContext.job.invokeOnCompletion { incoming.cancel() }

        // TODO: spam filtering???
        server.transport.commandPipeline {
            filter(SmtpCommandPipeline.Processing) {
                if (command !is MailCommand) return@filter

                wasProcessed = true

                state = State(command)

                server.transport.send(OkCompletion("Ok."))
            }

            filter(SmtpCommandPipeline.Processing) {
                if (command !is RecipientCommand) return@filter

                wasProcessed = true

                if (addresses?.contains(command.to.mailbox.asText()) == false) {
                    server.transport.send(SmtpReply.PermanentNegative.Default(550, listOf("unknown user: ${command.to.mailbox.asText()}")))
                    return@filter
                }

                state!!.rcpts.add(command.to)

                server.transport.send(OkCompletion("Ok."))
            }

            filter(SmtpCommandPipeline.Processing) {
                if (command !is DataCommand) return@filter

                wasProcessed = true

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
