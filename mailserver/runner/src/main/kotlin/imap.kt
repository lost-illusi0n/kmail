package dev.sitar.kmail.runner

import dev.sitar.kmail.imap.Sequence
import dev.sitar.kmail.imap.agent.*
import dev.sitar.kmail.message.Message
import dev.sitar.kmail.runner.storage.StorageLayer
import dev.sitar.kmail.runner.storage.formats.Mailbox
import dev.sitar.kmail.runner.storage.formats.MailboxFolder
import dev.sitar.kmail.runner.storage.formats.MailboxMessage
import dev.sitar.kmail.sasl.SaslChallenge
import dev.sitar.kmail.utils.server.ServerSocketFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import mu.KotlinLogging
import kotlin.math.max
import kotlin.math.min

private val logger = KotlinLogging.logger { }

suspend fun imap(socket: ServerSocketFactory, layer: ImapLayer): ImapServer = coroutineScope {
    logger.info("Starting IMAP server.")

    val server = ImapServer(socket.bind(IMAP_SERVER), layer)
    launch { server.listen() }

    logger.info("Started IMAP server.")

    server
}

// TODO: this is horrible
class KmailImapMessage(
    override val flags: Set<Flag>,
    override val sequenceNumber: Int,
    override val size: Long,
    private val message: suspend () -> Message,
) : ImapMessage {
    override val uniqueIdentifier: Int = sequenceNumber

    override suspend fun typedMessage(): Message {
        return message()
    }
}

class KmailImapFolder(val folder: MailboxFolder) : ImapFolder {
    override val name: String = folder.name

    override val attributes: Set<String> = setOf("HasNoChildren")

    override val flags: Set<String> = emptySet()

    override suspend fun exists(): Int = folder.totalMessages()

    override suspend fun recent(): Int = folder.newMessages()

    override suspend fun uidValidity(): Int {
        var value = folder.attributes.get("UIDVALIDITY")?.toIntOrNull()

        if (value == null) {
            value = Clock.System.now().epochSeconds.toInt()
            setUidValidity(value)
        }

        return value
    }

    override suspend fun setUidValidity(value: Int) {
        folder.attributes.set("UIDVALIDITY", value.toString())
    }

    // TODO: this can break if a message is deleted...
    override suspend fun uidNext(): Int {
        return exists() + 1
    }

    override suspend fun messages(): List<ImapMessage> {
        return folder.messages().mapIndexed { index, message -> KmailImapMessage(
            message.flags,
            index + 1,
            message.length,
            message::getMessage
        ) }
    }

    override suspend fun save(flags: Set<Flag>, message: String) {
        folder.store(flags, message)
    }

    // TODO: mix-matched nullability
    suspend fun get(pos: Int, mode: Sequence.Mode): MailboxMessage {
        return when (mode) {
            Sequence.Mode.SequenceNumber -> folder.message(exists() - pos)
            Sequence.Mode.Uid -> folder.messageByUid(pos)!!
        }
    }

    override suspend fun update(pos: Int, mode: Sequence.Mode, flags: Set<Flag>) {
        val message = get(pos, mode)

        message.updateFlags(flags)
    }

    override suspend fun onMessageStore(handler: (suspend (ImapMessage) -> Unit)?) {
        folder.onMessageStore = { handler?.invoke(KmailImapMessage(setOf(Flag.Recent), exists(), it.size.toLong(), { it })) }
    }
}

// TODO: use abstracted FS
class KmailImapMailbox(val mailbox: Mailbox) : ImapMailbox {
    override suspend fun folders(): List<LightImapFolder> {
        return mailbox.folders().map { LightImapFolder(setOf("HasNoChildren"), it) }
    }

    override fun folder(name: String): ImapFolder? {
        return KmailImapFolder(mailbox.folder(name))
    }

    override suspend fun createFolder(name: String) {
        mailbox.createFolder(name)
    }

    override suspend fun subscriptions(): List<String> {
        return mailbox.attributes.get("SUBSCRIPTIONS")?.lines().orEmpty()
    }

    override suspend fun subscribe(folder: String) {
        mailbox.attributes.append("SUBSCRIPTIONS", folder)
    }

    override suspend fun unsubscribe(folder: String) {
        mailbox.attributes.remove("SUBSCRIPTIONS", folder)
    }
}

class KmailImapLayer(val storage: StorageLayer): ImapLayer {
    override suspend fun create(username: String, mailbox: String) {
        storage.user(username).folder(mailbox)
        println("creating a mailbox called $mailbox")
    }

    override suspend fun authenticate(challenge: SaslChallenge): String? {
        require(challenge is SaslChallenge.Plain)

        if (!challenge.authorizationIdentity.isNullOrEmpty())
            logger.warn { "authentication request from ${challenge.authenticationIdentity} is attempting to authorize as ${challenge.authorizationIdentity}. this is not supported and will be ignored." }

        return Config.accounts.firstOrNull {
            it.email.contentEquals(challenge.authenticationIdentity) && passVerify(challenge.password, it.passwordHash)
        }?.email
    }

    override suspend fun mailbox(username: String): ImapMailbox {
        return KmailImapMailbox(storage.user(username))
    }
}

private val Message.size: Int
    get() = headers.fold(0) { a, b -> a + b.fieldBody.length + 2 + b.fieldBody.length + 2 } + (body?.length ?: 0)