package dev.sitar.kmail.imap.agent

import dev.sitar.kmail.imap.Capability
import dev.sitar.kmail.imap.agent.transports.ImapServerTransport
import dev.sitar.kmail.imap.frames.Tag
import dev.sitar.kmail.imap.frames.command.*
import dev.sitar.kmail.imap.frames.response.*
import dev.sitar.kmail.sasl.SaslChallenge
import dev.sitar.kmail.sasl.SaslMechanism
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
//
//    private suspend fun notAuthenticated() {
//        val taggedCommand = transport.recv()
//
//        when (taggedCommand.command) {
//            StartTlsCommand -> {
//                transport.send(taggedCommand.tag + OkResponse(text = "Let the TLS negotiations begin."))
//                transport = transport.upgrade()
//            }
//
//            is LoginCommand -> {
//                // TODO: check auth
//                transport.send(taggedCommand.tag + OkResponse(text = "LOGIN completed."))
//                state = ImapState.Authenticated
//            }
//            // TODO: authenticate and login
//            else -> universalHandler(taggedCommand)
//        }
//    }
//
//    private suspend fun authenticated() {
//        val taggedCommand = transport.recv()
//
//        val tag = taggedCommand.tag
//
//        when (val command = taggedCommand.command) {
//            is SelectCommand -> {
//                val selectedMailbox = layer.select(command.mailboxName)
//
//                transport.send(Tag.Untagged + FlagsResponse(flags = selectedMailbox.flags))
//                transport.send(Tag.Untagged + ExistsResponse(n = selectedMailbox.exists))
//                transport.send(Tag.Untagged + RecentResponse(n = selectedMailbox.recent))
//                transport.send(Tag.Untagged + OkResponse(text = "[UIDVALIDITY ${selectedMailbox.uidValidity}]")) // TODO: this is a response code
//                transport.send(Tag.Untagged + OkResponse(text = "[UIDNEXT ${selectedMailbox.uidNext}]")) // TODO: this is a response code
//                transport.send(tag + OkResponse(text = "[READ-WRITE] SELECT complete."))
//
//                mailbox = command.mailboxName
//                state = ImapState.Selected
//            }
//
//            is ListCommand -> {
//                layer.listFolders(command.referenceName, command.mailboxName).forEach {
//                    transport.send(Tag.Untagged + ListResponse(it.attributes, ImapFolder.DELIM, it.name))
//                }
//
//                transport.send(tag + OkResponse(text = "Here are your folders."))
//            }
//
//            is ListSubscriptionsCommand -> {
//                layer.listSubscribedFolders(command.referenceName, command.mailboxName).forEach {
//                    transport.send(Tag.Untagged + ListSubscriptionsResponse(it.attributes, ImapFolder.DELIM, it.name))
//                }
//
//                transport.send(tag + OkResponse(text = "Here are your subscribed folders."))
//            }
//
//            is CreateCommand -> {
//                // TODO: check the result of this call
//                layer.create(command.mailboxName)
//
//                transport.send(tag + OkResponse(text = "Created a new mailbox."))
//            }
//
//            else -> universalHandler(taggedCommand)
//        }
//    }
//
//    private suspend fun selected() {
//        val taggedCommand = transport.recv()
//        val command = taggedCommand.command
//
//        when (command) {
//            is UidCommand -> {
//                val form = command.command
//                when (form) {
//                    is FetchCommand -> fetch(taggedCommand.tag, form)
//                    else -> error("shouldnt happen, it wouldnt get deserialized.")
//                }
//            }
//            is FetchCommand -> fetch(taggedCommand.tag, command)
//            else -> universalHandler(taggedCommand)
//        }
//    }
//
//    private suspend fun fetch(tag: Tag, command: FetchCommand) {
//        layer.fetch(mailbox!!, command.sequence, command.dataItems).forEach { (id, items) ->
//            println("got response $id $items")
//            transport.send(Tag.Untagged + FetchResponse(id, items))
//        }
//
//        transport.send(tag + OkResponse(text = "Here is your mail."))
//    }
//
//    private suspend fun logout() {
//
//    }
//
//    private suspend fun universalHandler(taggedCommand: TaggedImapCommand) {
//        when (taggedCommand.command) {
//            CapabilityCommand -> {
//                transport.send(Tag.Untagged + CapabilityResponse(capabilities))
//                transport.send(taggedCommand.tag + OkResponse(text = "CAPABILITY completed."))
//            }
//
//            NoOpCommand -> {
//                transport.send(taggedCommand.tag + OkResponse(text = "NOOP completed."))
//            }
//
//            LogoutCommand -> {
//                try {
//                    transport.send(Tag.Untagged + ByeResponse(text = "Bye!"))
//                    transport.send(taggedCommand.tag + OkResponse(text = "Bye-bye!"))
//                } catch (e: Exception) {
//                    logger.error(e)
//                }
//
//                transport.close()
//                scope.cancel()
//                state = ImapState.Logout
//            }
//            else -> {
//                logger.error("IMAP(${transport.remote}, $state): Could not handle command: $taggedCommand.")
//                transport.send(taggedCommand.tag + BadResponse(text  = "Could not handle command in current state."))
//            }
//        }
//    }
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
                        TODO("not authenticated")
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
                        agent.transport.send(Tag.Untagged + ExistsResponse(n = folder.exists))
                        agent.transport.send(Tag.Untagged + RecentResponse(n = folder.recent))
//                        agent.transport.send(Tag.Untagged + OkResponse(text = "[UNSEEN ${folder.unseen}]"))
                        agent.transport.send(Tag.Untagged + OkResponse(text = "[UIDVALIDITY ${folder.uidValidity}]")) // TODO: this is a response code
                        agent.transport.send(Tag.Untagged + OkResponse(text = "[UIDNEXT ${folder.uidNext}]")) // TODO: this is a response code
                        agent.transport.send(context.command.tag + OkResponse(text = "[READ-WRITE] SELECT complete."))

                        agent.state = Selected(agent, this, folder)
                    } else {
                        TODO("bad folder")
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
//                SUBSCRIBE,
//                UNSUBSCRIBE,
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
                is StatusCommand -> {
                   val mailbox = mailbox.folder(command.mailbox)!!

                   val responses = command.items.associateWith {
                       when (it) {
                           StatusDataItem.Messages -> mailbox.exists
                           StatusDataItem.Recent -> mailbox.recent
                           StatusDataItem.UidNext -> mailbox.uidNext
                           StatusDataItem.UidValidity -> mailbox.uidValidity
                           StatusDataItem.Unseen -> mailbox.recent // TODO: unseen
                       }
                   }

                    agent.transport.send(Tag.Untagged + StatusResponse(mailbox.name, responses))

                    agent.transport.send(context.command.tag + OkResponse(text = "status complete."))
                }
//                APPEND,
//                IDLE
                else -> context.wasProcessed = false
            }
        }
    }
    class Selected(val agent: ImapAgent, val authenticated: Authenticated, val folder: ImapFolder): State {
        override suspend fun handle(context: ImapCommandContext) {
            context.wasProcessed = true
            val command = context.command.command

            when (command) {
                is FetchCommand -> {
                    fetch(context.command.tag, command)
                }
                is UidCommand -> {
                    val form = command.command
                    when (form) {
                        is FetchCommand -> fetch(context.command.tag, form)
                        else -> error("shouldnt happen, it wouldnt get deserialized.")
                    }
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
    }
    class Logout(): State {
        override suspend fun handle(context: ImapCommandContext) {
            TODO("Not yet implemented")
        }
    }

    suspend fun handle(context: ImapCommandContext)
}