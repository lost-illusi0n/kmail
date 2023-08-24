package dev.sitar.kmail.agents.pop3

import dev.sitar.kmail.agents.pop3.transports.Pop3ServerTransport
import dev.sitar.kmail.pop3.commands.*
import dev.sitar.kmail.pop3.replies.Pop3Reply
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

class Pop3Agent(
    val transport: Pop3ServerTransport,
    val layer: Pop3Layer
) {
    var state: State = State.Authorization(this)

    suspend fun handle() = coroutineScope {
        launch { transport.startPipeline() }

        transport.sendReply(Pop3Reply.OkReply("kmail pop3 service"))

        transport.commandPipeline {
            filter(Pop3CommandPipeline.Logging) {
                logger.trace { "<<< $command" }
            }

            filter(Pop3CommandPipeline.Before) {
                if (command is UnknownCommand) {
                    wasProcessed = true

                    transport.sendReply(Pop3Reply.ErrReply("unknown command"))
                }
            }

            filter(Pop3CommandPipeline.Processing) {
                state.handle(this)
            }

            filter(Pop3CommandPipeline.After) {
                if (wasProcessed) return@filter

                if (command is QuitCommand) {
                    cancel()
                    transport.connection.close()
                    awaitCancellation()
                }

                // bad command in state (not processed)
            }
        }
    }
}

sealed interface State {
    class Authorization(val agent: Pop3Agent) : State {
        var user: String? = null

        override suspend fun handle(context: Pop3CommandContext) {
            context.wasProcessed = true

            when (context.command) {
                is UserCommand -> {
                    require(user == null)
                    require(agent.layer.userExists(context.command.name))

                    user = context.command.name
                    agent.transport.sendReply(Pop3Reply.OkReply("oki doki"))
                }
                is PassCommand -> {
                    require(user != null)

                    require(agent.layer.login(user!!, context.command.password))

                    val maildrop = agent.layer.maildrop(user!!)

                    agent.state = Transaction(agent, maildrop)

                    agent.transport.sendReply(Pop3Reply.OkReply("nice reply"))
                }
                else -> context.wasProcessed = false
            }
        }
    }

    class Transaction(val agent: Pop3Agent, val maildrop: Pop3Maildrop) : State {
        override suspend fun handle(context: Pop3CommandContext) {
            context.wasProcessed = true

            when (context.command) {
                is StatCommand -> {
                    agent.transport.sendReply(Pop3Reply.OkReply("${maildrop.messageCount} ${maildrop.dropSize}"))
                }
                is ListCommand -> {
                    val message = context.command.messageNumber

                    if (message == null) {
                        agent.transport.sendReply(Pop3Reply.OkReply("${maildrop.messageCount} messages (${maildrop.dropSize} octets)"))

                        repeat(maildrop.messageCount) {
                            agent.transport.sendReply(Pop3Reply.DataReply("${it + 1} ${maildrop.messages[it].size}"))
                        }

                        agent.transport.sendReply(Pop3Reply.DataReply("."))
                    } else {
                        agent.transport.sendReply(Pop3Reply.OkReply("$message ${maildrop.messages[message - 1].size}"))
                    }
                }
                is RetrCommand -> {
                    val message = maildrop.messages[context.command.messageNumber - 1]
                    agent.transport.sendReply(Pop3Reply.OkReply("${message.size} octets"))
                    agent.transport.sendReply(Pop3Reply.DataReply(message.getContent()))
                    agent.transport.sendReply(Pop3Reply.DataReply("."))
                }
                else -> context.wasProcessed = false
            }
        }
    }
    class Update(val agent: Pop3Agent) : State {
        override suspend fun handle(context: Pop3CommandContext) {
            TODO("Not yet implemented")
        }
    }

    suspend fun handle(context: Pop3CommandContext)
}

interface Pop3Extension {
    val agent: Pop3Agent

    fun handle()
}