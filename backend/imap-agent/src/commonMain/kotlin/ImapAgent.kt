package dev.sitar.kmail.imap.agent

import dev.sitar.kmail.imap.Capabilities
import dev.sitar.kmail.imap.agent.transports.ImapTransport
import dev.sitar.kmail.imap.frames.Tag
import dev.sitar.kmail.imap.frames.command.*
import dev.sitar.kmail.imap.frames.response.*
import io.ktor.util.logging.*
import kotlinx.coroutines.*
import mu.KotlinLogging
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private val logger = KotlinLogging.logger { }

class ImapAgent(
    private var transport: ImapTransport,
    private val queryAgent: ImapQueryAgent,
    coroutineContext: CoroutineContext = EmptyCoroutineContext
) {
    private var mailbox: String? = null
    private var state = ImapState.NotAuthenticated
    private val scope = CoroutineScope(CoroutineName("imap-agent") + Job() + coroutineContext)

    init {
        scope.launch { handle() }
    }

    private val capabilities: List<String>
        get() = buildList {
            add(Capabilities.Imap4Rev1)

            if (state == ImapState.NotAuthenticated) {
                add(Capabilities.Login)
//                if (transport.isUpgraded) add(Capabilities.Login)
//                else add(Capabilities.LoginDisabled)
            }

//            if (!transport.isUpgraded) {
//                add(Capabilities.StartTls)
//            }
        }

    private suspend fun CoroutineScope.handle() {
        transport.send(Tag.Untagged + OkResponse(text = "Greetings from Kmail!"))

        while (isActive) {
            when (state) {
                ImapState.NotAuthenticated -> notAuthenticated()
                ImapState.Authenticated -> authenticated()
                ImapState.Selected -> selected()
                ImapState.Logout -> break
            }
        }

        logger.info { "IMAP: FINISHED A SESSION." }
    }

    private suspend fun notAuthenticated() {
        val taggedCommand = transport.recv()

        when (taggedCommand.command) {
            StartTlsCommand -> {
                transport.send(taggedCommand.tag + OkResponse(text = "Let the TLS negotiations begin."))
                transport = transport.upgrade()
            }

            is LoginCommand -> {
                // TODO: check auth
                transport.send(taggedCommand.tag + OkResponse(text = "LOGIN completed."))
                state = ImapState.Authenticated
            }
            // TODO: authenticate and login
            else -> universalHandler(taggedCommand)
        }
    }

    private suspend fun authenticated() {
        val taggedCommand = transport.recv()

        when (val command = taggedCommand.command) {
            is SelectCommand -> {
                transport.send(Tag.Untagged + FlagsResponse(flags = FlagsResponse.SYSTEM_FLAGS + "Sent"))
                transport.send(Tag.Untagged + ExistsResponse(n = 2))
                transport.send(Tag.Untagged + RecentResponse(n = 3))
                transport.send(Tag.Untagged + OkResponse(text = "[UIDVALIDITY 1]"))
                transport.send(Tag.Untagged + OkResponse(text = "[UIDNEXT 1]"))
                transport.send(taggedCommand.tag + OkResponse(text = "SELECT complete."))

                mailbox = command.mailboxName
                state = ImapState.Selected
            }

            is ListCommand -> {
                queryAgent.listFolders(command.referenceName, command.mailboxName).forEach {
                    transport.send(Tag.Untagged + ListResponse(it.attributes, ImapFolder.DELIM, it.name))
                }

                transport.send(taggedCommand.tag + OkResponse(text = "Here are your folders."))
            }

            is ListSubscriptionsCommand -> {
                queryAgent.listSubscribedFolders(command.referenceName, command.mailboxName).forEach {
                    transport.send(Tag.Untagged + ListSubscriptionsResponse(it.attributes, ImapFolder.DELIM, it.name))
                }

                transport.send(taggedCommand.tag + OkResponse(text = "Here are your subscribed folders."))
            }

            else -> universalHandler(taggedCommand)
        }
    }

    private suspend fun selected() {
        val taggedCommand = transport.recv()
        val command = taggedCommand.command

        when (command) {
            is UidCommand -> {
                val form = command.command
                when (form) {
                    is FetchCommand -> fetch(taggedCommand.tag, form)
                    else -> error("shouldnt happen, it wouldnt get deserialized.")
                }
            }
            is FetchCommand -> fetch(taggedCommand.tag, command)
            else -> universalHandler(taggedCommand)
        }
    }

    private suspend fun fetch(tag: Tag, command: FetchCommand) {
        queryAgent.fetch(mailbox!!, command.sequenceSet, command.dataItems).forEach {
            transport.send(Tag.Untagged + FetchResponse(it))
        }

        transport.send(tag + OkResponse(text = "Here is your mail."))
    }

    private suspend fun logout() {

    }

    private suspend fun universalHandler(taggedCommand: TaggedImapCommand) {
        when (taggedCommand.command) {
            CapabilityCommand -> {
                transport.send(Tag.Untagged + CapabilityResponse(capabilities))
                transport.send(taggedCommand.tag + OkResponse(text = "CAPABILITY completed."))
            }

            NoOpCommand -> {
                transport.send(taggedCommand.tag + OkResponse(text = "NOOP completed."))
            }

            LogoutCommand -> {
                try {
                    transport.send(Tag.Untagged + ByeResponse(text = "Bye!"))
                    transport.send(taggedCommand.tag + OkResponse(text = "Bye-bye!"))
                } catch (e: Exception) {
                    logger.error(e)
                }

                transport.close()
                scope.cancel()
                state = ImapState.Logout
            }
            else -> {
                logger.error("IMAP(${transport.remote}, $state): Could not handle command: $taggedCommand.")
                transport.send(taggedCommand.tag + BadResponse(text  = "Could not handle command in current state."))
            }
        }
    }
}