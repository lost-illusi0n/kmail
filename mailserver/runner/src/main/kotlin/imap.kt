package dev.sitar.kmail.runner

import dev.sitar.kmail.imap.Sequence
import dev.sitar.kmail.imap.agent.*
import dev.sitar.kmail.imap.frames.command.StoreMode
import dev.sitar.kmail.message.Message
import dev.sitar.kmail.runner.storage.StorageLayer
import dev.sitar.kmail.runner.storage.formats.Mailbox
import dev.sitar.kmail.runner.storage.formats.MailboxFolder
import dev.sitar.kmail.runner.storage.formats.MailboxMessage
import dev.sitar.kmail.sasl.SaslChallenge
import dev.sitar.kmail.utils.server.ServerSocketFactory
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

suspend fun imap(socket: ServerSocketFactory, layer: ImapLayer) {
    logger.info("Starting IMAP server.")

    ImapServer(socket.bind(IMAP_SERVER), layer, Config.imap.toImapConfig()).listen()
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
        return folder.messages().mapIndexed { index, message -> ImapMessage(
            index + 1,
            index + 1,
            message.flags,
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
            Sequence.Mode.Sequence -> folder.message(exists() - pos)
            Sequence.Mode.Uid -> folder.messageByUid(pos)!!
        }
    }

    override suspend fun store(sequence: Sequence, flags: Set<Flag>, mode: StoreMode, messagesSnapshot: List<ImapMessage>?) : Map<Int, Set<Flag>> {
        val messages = sequenceToMessages(sequence, messagesSnapshot ?: messages())

        return messages.associate {
            val newFlags = when (mode) {
                StoreMode.Set -> flags
                StoreMode.Add -> it.flags + flags
                StoreMode.Remove -> it.flags - flags
            }

            val oldMessage = get(it.sequenceNumber, Sequence.Mode.Sequence)
            oldMessage.updateFlags(newFlags)

            when (sequence.mode) {
                Sequence.Mode.Sequence -> it.sequenceNumber
                Sequence.Mode.Uid -> it.uniqueIdentifier
            } to newFlags
        }
    }

    override suspend fun onMessageStore(handler: (suspend (ImapMessage) -> Unit)?) {
        folder.onMessageStore = { handler?.invoke(ImapMessage(exists(), exists(), setOf(Flag.Recent), it.size.toLong()) { it }) }
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
        logger.debug  { "creating a mailbox called $mailbox" }
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