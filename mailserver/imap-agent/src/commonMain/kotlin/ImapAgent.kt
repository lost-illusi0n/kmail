package dev.sitar.kmail.imap.agent

import dev.sitar.kmail.imap.Capability
import dev.sitar.kmail.imap.agent.transports.ImapServerTransport
import dev.sitar.kmail.imap.frames.DataItem
import dev.sitar.kmail.imap.frames.Tag
import dev.sitar.kmail.imap.frames.command.*
import dev.sitar.kmail.imap.frames.response.*
import dev.sitar.kmail.sasl.SaslChallenge
import dev.sitar.kmail.sasl.SaslMechanism
import dev.sitar.kmail.utils.todo
import io.ktor.util.*
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

class ImapAgent(
    var transport: ImapServerTransport,
    val layer: ImapLayer,
) {
    var state: State = State.NotAuthenticated(this)

    suspend fun handle() = coroutineScope {
        launch { transport.startPipeline() }

        transport.send(Tag.Untagged + OkResponse(text = "kmail imap service"))

        transport.commandPipeline {
            filter(ImapCommandPipeline.Logging) {
                logger.trace { "<<< $command" }
            }

            filter(ImapCommandPipeline.Before) {

            }

            filter(ImapCommandPipeline.Processing) {
                state.handle(this)
            }

            filter(ImapCommandPipeline.After) {
                if (wasProcessed) return@filter

                if (command.command is CapabilityCommand) {
                    transport.send(Tag.Untagged + CapabilityResponse(capabilities))
                    transport.send(command.tag + OkResponse(text = "done."))
                }

                if (command.command is NoOpCommand) {
                    transport.send(command.tag + OkResponse(text = "noop"))
                }

                if (command.command is LogoutCommand) {
                    transport.send(Tag.Untagged + ByeResponse(text = "bye."))
                    cancel()
                    transport.connection.close()
                    awaitCancellation()
                }
            }
        }
    }

    private val capabilities: List<Capability>
        get() = buildList {
            add(Capability.Imap4Rev1)
            add(Capability.Idle)
            add(Capability.LoginDisabled)

            if (state is State.NotAuthenticated) {
                if (transport.isSecure) {
                    add(Capability.Auth(SaslMechanism.Plain))
                }
            }

            if (!transport.isSecure) {
                add(Capability.StartTls)
            }
        }
}

sealed interface State {
    class NotAuthenticated(val agent: ImapAgent): State {
        override suspend fun handle(context: ImapCommandContext) {
            context.wasProcessed = true
            val command = context.command.command
            val tag = context.command.tag

            when (command) {
                // TODO: thunderbird sends quoted username/password. why? this is not a problem with authenticate
//                is LoginCommand -> {
//                    if (agent.layer.login(command.username, command.password)) {
//                        agent.transport.send(tag + OkResponse(text = "logged in."))
//
//                        agent.state = Authenticated(agent, command.username)
//                    } else {
//                        TODO()
//                        // no response
////                        agent.transport.send(context.command.tag + )
//                    }
//                }
                StartTlsCommand -> {
                    agent.transport.send(tag + OkResponse(text = "Let the TLS negotiations begin."))
                    agent.transport.secure()
                    logger.debug { "secured ${agent.transport.commandPipeline}" }
                }
                is AuthenticateCommand -> {
                    require(command.mechanism is SaslMechanism.Plain)

                    agent.transport.send(Tag.None + ContinueDataResponse)

                    val challenge = SaslChallenge.Plain.fromString(agent.transport.readData().decodeBase64String())
                    val user = agent.layer.authenticate(challenge)

                    if (user != null) {
                        val mailbox = agent.layer.mailbox(user)

                        agent.transport.send(tag + OkResponse(text = "authenticated."))
                        agent.state = Authenticated(agent, mailbox)
                    } else {
                        logger.debug { "user ${challenge.authenticationIdentity} failed to authenticate."}
                        todo("not authenticated")
                    }
                }
                else -> context.wasProcessed = false
            }
        }
    }
    class Authenticated(val agent: ImapAgent, val mailbox: ImapMailbox): State {
        override suspend fun handle(context: ImapCommandContext) {
            context.wasProcessed = true
            val command = context.command.command

            when (command) {
//                is ENABLE,
                is SelectCommand -> {
                    val folder = mailbox.folder(command.mailboxName)

                    if (folder != null) {
                        agent.transport.send(Tag.Untagged + FlagsResponse(flags = folder.flags))
                        agent.transport.send(Tag.Untagged + ExistsResponse(n = folder.exists()))
                        agent.transport.send(Tag.Untagged + RecentResponse(n = folder.recent()))
//                        agent.transport.send(Tag.Untagged + OkResponse(text = "[UNSEEN ${folder.unseen}]"))
                        agent.transport.send(Tag.Untagged + OkResponse(text = "[UIDVALIDITY ${folder.uidValidity()}]")) // TODO: this is a response code
                        agent.transport.send(Tag.Untagged + OkResponse(text = "[UIDNEXT ${folder.uidNext()}]")) // TODO: this is a response code
                        agent.transport.send(context.command.tag + OkResponse(text = "[READ-WRITE] SELECT complete."))

                        agent.state = Selected(agent, this, folder)
                    } else {
                        todo("bad folder")
                    }
                }
//                EXAMINE,
//                NAMESPACE,
                is CreateCommand -> {
                    mailbox.createFolder(command.mailboxName)

                    agent.transport.send(context.command.tag + OkResponse(text = "created the mailbox."))
                }
//                DELETE,
//                RENAME,
                is ListCommand -> {
                    if (command.referenceName == "" && command.mailboxName == "*") {
                        mailbox.folders().forEach {
                            agent.transport.send(Tag.Untagged + ListResponse(it.attributes, ImapFolder.DELIM, it.name))
                        }

                        agent.transport.send(context.command.tag + OkResponse(text = "Here are your folders."))
                    } else {
                        // TODO: rest of list implementation
                        agent.transport.send(context.command.tag + OkResponse(text = "Here are your folders."))
                    }
                }
                is ListSubscriptionsCommand -> {
                    val subscriptions = mailbox.subscriptions()
                    mailbox.folders().filter { it.name in subscriptions }.forEach {
                        agent.transport.send(Tag.Untagged + ListResponse(it.attributes, ImapFolder.DELIM, it.name))
                    }

                    agent.transport.send(context.command.tag + OkResponse(text = "Here are your subscriptions."))
                }
                is SubscribeCommand -> {
                    mailbox.subscribe(command.mailbox)

                    agent.transport.send(context.command.tag + OkResponse(text = "subscribed."))
                }
                is UnsubscribeCommand -> {
                    mailbox.unsubscribe(command.mailbox)

                    agent.transport.send(context.command.tag + OkResponse(text = "unsubscribed."))
                }
                is StatusCommand -> {
                   val mailbox = mailbox.folder(command.mailbox)!!

                   val responses = command.items.associateWith {
                       when (it) {
                           StatusDataItem.Messages -> mailbox.exists()
                           StatusDataItem.Recent -> mailbox.recent()
                           StatusDataItem.UidNext -> mailbox.uidNext()
                           StatusDataItem.UidValidity -> mailbox.uidValidity()
                           StatusDataItem.Unseen -> mailbox.recent() // TODO: unseen
                       }
                   }

                    agent.transport.send(Tag.Untagged + StatusResponse(mailbox.name, responses))

                    agent.transport.send(context.command.tag + OkResponse(text = "status complete."))
                }
                is AppendCommand -> {
                    agent.transport.send(Tag.None + ContinueDataResponse)

                    val message = agent.transport.readMessage(command.messageSize)

                    val folder = mailbox.folder(command.mailbox) ?: todo("folder doesnt exist")

                    val flags = command.flags.orEmpty().map { Flag.fromValue(it) }
                        .filter {
                            if (it is Flag.Other) {
                                logger.warn { "unsupported flag: $it. ignoring" }
                                true
                            } else false
                        }.toSet()

                    folder.save(flags, message)

                    agent.transport.send(context.command.tag + OkResponse(text = "appended"))
                }
                else -> context.wasProcessed = false
            }
        }
    }
    class Selected(val agent: ImapAgent, val authenticated: Authenticated, val folder: ImapFolder): State {
        override suspend fun handle(context: ImapCommandContext) {
            context.wasProcessed = true
            val command = context.command.command

            when (command) {
                is IdleCommand -> {
                    agent.transport.send(Tag.None + ContinueDataResponse)

                    folder.onMessageStore {
                        agent.transport.send(Tag.Untagged + ExistsResponse(n = folder.exists()))
                    }

                    require(agent.transport.readData().lowercase() == "done")

                    folder.onMessageStore(null)

                    agent.transport.send(context.command.tag + OkResponse(text = "done idling"))
                }
                is FetchCommand -> {
                    fetch(context.command.tag, command)
                }
                is StoreCommand -> {
                    store(context.command.tag, command)
                }
                is CopyCommand -> {
                    copy(context.command.tag, command)
                }
                is UidCommand -> {
                    when (val form = command.command) {
                        is FetchCommand -> fetch(context.command.tag, form)
                        is StoreCommand -> store(context.command.tag, form)
                        is CopyCommand -> copy(context.command.tag, form)
                        else -> throw Exception("shouldnt happen, it wont get deserialized.")
                    }
                }
                is CheckCommand -> {
                    agent.transport.send(context.command.tag + OkResponse(text = "noop"))
                }
                else -> authenticated.handle(context) // authenticated commands are allowed here
            }
        }

        private suspend fun fetch(tag: Tag, command: FetchCommand) {
            folder.fetch(command.sequence, command.dataItems).forEach { (id, items) ->
                agent.transport.send(Tag.Untagged + FetchResponse(id, items))
            }

            agent.transport.send(tag + OkResponse(text = "Here is your mail."))
        }

        private suspend fun store(tag: Tag, command: StoreCommand) {
            val resp = folder.store(command.sequence, command.item.flags.map { Flag.fromValue(it) }.toSet(), command.item.mode)

            if (!command.item.silent) {
                resp.forEach {
                    agent.transport.send(Tag.Untagged + FetchResponse(it.key, setOf(DataItem.Response.Flags(it.value.map(Flag::value)))))
                }
            }

            agent.transport.send(tag + OkResponse(text = "Stored new flags."))
        }

        private suspend fun copy(tag: Tag, command: CopyCommand) {
            val messages = folder.sequenceToMessages(command.sequence)

            val copy = authenticated.mailbox.folder(command.mailbox)

            if (copy == null) {
                agent.transport.send(tag + BadResponse(text = "[TRYCREATE] dest doesn't exist."))
                return
            }

            messages.forEach {
                copy.save(it.flags, it.typedMessage().asText())
            }

            agent.transport.send(tag + OkResponse(text = "copy done."))
        }
    }
    class Logout(): State {
        override suspend fun handle(context: ImapCommandContext) {
            todo()
        }
    }

    suspend fun handle(context: ImapCommandContext)
}