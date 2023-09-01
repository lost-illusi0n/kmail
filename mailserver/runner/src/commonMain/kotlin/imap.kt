package dev.sitar.kmail.runner

import dev.sitar.kmail.imap.PartSpecifier
import dev.sitar.kmail.imap.Sequence
import dev.sitar.kmail.imap.agent.*
import dev.sitar.kmail.imap.frames.DataItem
import dev.sitar.kmail.message.Message
import dev.sitar.kmail.runner.storage.*
import dev.sitar.kmail.sasl.SaslChallenge
import dev.sitar.kmail.utils.server.ServerSocketFactory
import io.ktor.util.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger { }

suspend fun imap(socket: ServerSocketFactory, layer: ImapLayer): ImapServer = coroutineScope {
    logger.info("Starting IMAP server.")

    val server = ImapServer(socket.bind(IMAP_SERVER), layer)
    launch { server.listen() }

    logger.info("Started IMAP server.")

    server
}

class KmailImapMessage(val folder: KmailImapFolder, override val sequenceNumber: Int, override val typed: Message) :
    ImapMessage {
    override val uniqueIdentifier: Int = sequenceNumber

    override val size: Int = typed.size
}

// TODO: use abstracted FS
class KmailImapFolder(val folder: MailboxFolder, val root: File) : ImapFolder {
    override val name: String = folder.name

    override val attributes: Set<String> = setOf("HasNoChildren")

    override val flags: Set<String> = emptySet()

    override val exists: Int get() = folder.totalMessages

    override val recent: Int get() = folder.newMessages

    override var uidValidity: Int
        get() = root.resolve("UIDVALIDITY").also { it.createNewFile() }.readText().toIntOrNull() ?: run {
            uidValidity = Clock.System.now().epochSeconds.toInt(); uidValidity
        }
        set(value) = root.resolve("UIDVALIDITY").also { it.createNewFile() }.writeText(value.toString())

    // TODO: this can break if a message is deleted...
    override val uidNext: Int get() = exists + 1

    override val messages: List<ImapMessage> =
        folder.messages().mapIndexed { index, message -> KmailImapMessage(this, index + 1, message.message) }
}

// TODO: use abstracted FS
class KmailImapMailbox(val mailbox: Mailbox, val root: File) : ImapMailbox {
    override fun folders(): List<LightImapFolder> {
        return mailbox.folders().map { LightImapFolder(setOf("HasNoChildren"), it) }
    }

    override fun folder(name: String): ImapFolder? {
        return KmailImapFolder(mailbox.folder(name), if (name == "INBOX") root else root.resolve(name)) // TODO: this is purely dependent on maildir code... any other format will probably break
    }

    override fun createFolder(name: String) {
        mailbox.createFolder(name)
    }

    override fun subscriptions(): List<String> {
        val subscriptions = root.resolve("subscriptions")
        if (!subscriptions.exists()) subscriptions.createNewFile()
        return subscriptions.readLines()
    }

    override fun subscribe(folder: String) {
        val subscriptions = root.resolve("subscriptions")
        if (!subscriptions.exists()) subscriptions.createNewFile()
        subscriptions.writer().appendLine("$folder\n")
    }
}

class KmailImapLayer(val storage: StorageLayer): ImapLayer {
    override suspend fun create(username: String, folder: String) {
        storage.user(username).folder(folder)
        println("creating a mailbox called $folder")
    }

    override suspend fun authenticate(challenge: SaslChallenge): String? {
        require(challenge is SaslChallenge.Plain)

        return Config.accounts.firstOrNull {
            it.username.contentEquals(challenge.authenticationIdentity)
                    && it.password.contentEquals(challenge.password)
        }?.username
    }

    override suspend fun mailbox(username: String): ImapMailbox {
        // TODO: fs
        return KmailImapMailbox(
            storage.user(username),
            File("${(Config.mailbox.filesystem as KmailConfig.Mailbox.Filesystem.Local).dir}/$username")
        )
    }
}

private val Message.size: Int
    get() = headers.fold(0) { a, b -> a + b.fieldBody.length + 2 + b.fieldBody.length + 2 } + (body?.length ?: 0)