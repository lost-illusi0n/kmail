package dev.sitar.kmail.runner.storage.formats

import dev.sitar.kmail.imap.agent.Flag
import dev.sitar.kmail.message.Message
import dev.sitar.kmail.runner.storage.Attributable
import dev.sitar.kmail.runner.storage.filesystems.FsFile
import dev.sitar.kmail.runner.storage.filesystems.FsFolder
import kotlinx.datetime.Clock
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random


// TODO: this sucks
@JvmInline
value class MaildirUniqueName(val value: String) {
    constructor(name: String, flags: Set<Flag>? = null) : this(name + flags.orEmpty().joinToString(prefix = ":2,", separator = "") {
        if (it !is Flag.Other) it::class.simpleName!!.first().toString()
        else {
            logger.error { "encountered a non-standard flag. not yet supported" }
            ""
        }
    })

    constructor(
        timestamp: Long,
        hostname: String,
        sequenceNumber: Int? = null,
        bootNumber: Int? = null,
        random: Int? = null,
        inode: Int? = null,
        deviceNumber: Int? = null,
        microseconds: Int? = null,
        processId: Int? = null,
        deliveries: Int? = null,
        flags: Set<Flag>? = null
    ) : this("$timestamp.S${sequenceNumber}X${bootNumber}R${random}.$hostname", flags)

    val flags get() = value.split(':').last().run {
        if (startsWith("2,")) removePrefix("2,").map { it.toFlag() } else emptyList()
    }
}

private fun Char.toFlag(): Flag {
    return when (this) {
        'R' -> Flag.Replied
        'S' -> Flag.Seen
        'T' -> Flag.Trashed
        'D' -> Flag.Draft
        'F' -> Flag.Flagged
        else -> error("unexpected maildir flag: $this")
    }
}

class Maildir(val user: FsFolder) : Mailbox, Attributable by user {
    override val inbox: MaildirInbox = MaildirInbox(this, user)

    override suspend fun folders(): List<String> {
        return user.listFolders().map { it.name }.minus(arrayOf("new", "cur", "tmp")).plus("INBOX")
    }

    override fun folder(name: String): MailboxFolder {
        return if (name == "INBOX") inbox else MaildirFolder(this, user.folder(name))
    }

    override suspend fun createFolder(name: String) {
        user.createFolder(name)
    }

    override suspend fun store(message: Message) {
        inbox.store(message)
    }
}

class MaildirInbox(val mailbox: Maildir, val user: FsFolder) : MailboxFolder, Attributable by user {
    val tmp: MaildirFolder = MaildirFolder(mailbox, user.folder("tmp"))
    val new: MaildirFolder = MaildirFolder(mailbox, user.folder("new"))
    val cur: MaildirFolder = MaildirFolder(mailbox, user.folder("cur"))

    override val name: String
        get() = "INBOX"

    override var onMessageStore: (suspend (Message) -> Unit)? = null

    override suspend fun totalMessages(): Int = new.totalMessages() + cur.totalMessages()

    override suspend fun newMessages(): Int = new.totalMessages()

    override suspend fun messages(): List<MailboxMessage> {
        return new.messages() + cur.messages()
    }

    override suspend fun message(index: Int): MailboxMessage {
        return messages()[index]
    }

    override suspend fun messageByUid(uid: Int): MailboxMessage? {
        return cur.messageByUid(uid)
    }

    suspend fun store(message: Message) {
        val name = tmp.store(message)
        tmp.moveFile(name, new)
        onMessageStore?.invoke(message)
    }
}

class MaildirFolder(val mailbox: Maildir, val folder: FsFolder) : MailboxFolder, Attributable by folder {
    override val name: String = folder.name

    override var onMessageStore: (suspend (Message) -> Unit)? = null

    override suspend fun totalMessages(): Int = folder.listFiles().size

    override suspend fun newMessages(): Int = if (folder.name == "new") totalMessages() else 0

    override suspend fun messages(): List<MaildirMessage> {
        return folder.listFiles().map { MaildirMessage(it.name, it.size, folder, mailbox) }
    }

    override suspend fun message(index: Int): MaildirMessage {
        return messages()[index]
    }

    override suspend fun messageByUid(uid: Int): MailboxMessage? {
        return messages().find { it.name.contains(uid.toString()) }
    }

    suspend fun store(message: Message): MaildirUniqueName {
        // TODO: populate this
        val name = MaildirUniqueName(
            Clock.System.now().epochSeconds,
            "kmail",
            0,
            0,
            Random.nextInt(),
            0,
            0,
            0,
            0,
            deliveries.getAndIncrement()
        )

        folder.writeFile(name.value, message.asText().encodeToByteArray())
        return name
    }

    suspend fun moveFile(name: MaildirUniqueName, to: MaildirFolder) {
        return folder.move(name.value, to.folder)
    }
}


// TODO: replace this with atomicfu
private val deliveries: AtomicInteger = AtomicInteger(0)

private val logger = KotlinLogging.logger { }

class MaildirMessage(var fileName: String, override val length: Long, var folder: FsFolder, val mailbox: Maildir): MailboxMessage {
    override val name: String = fileName.split(':').first()

    override val flags: Set<Flag> = MaildirUniqueName(fileName).flags.toSet()

    override suspend fun updateFlags(flags: Set<Flag>) {
        if (Flag.Seen in flags) {
            if (folder.name == "new") {
                mailbox.inbox.new.moveFile(MaildirUniqueName(fileName), mailbox.inbox.cur)
                folder = mailbox.inbox.cur.folder
            }
        }

        val newName = MaildirUniqueName(name, flags).value
        folder.rename(fileName, newName)
        fileName = newName
    }

    override suspend fun getMessage(): Message {
        return Message.fromText(folder.readFile(fileName)!!.decodeToString())
    }
}