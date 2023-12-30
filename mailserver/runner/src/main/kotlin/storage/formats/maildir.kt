package dev.sitar.kmail.runner.storage.formats

import dev.sitar.kmail.imap.agent.Flag
import dev.sitar.kmail.message.Message
import dev.sitar.kmail.runner.Config
import dev.sitar.kmail.runner.storage.Attributable
import dev.sitar.kmail.runner.storage.filesystems.FileSystem
import dev.sitar.kmail.runner.storage.filesystems.FsFolder
import kotlinx.datetime.Clock
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

// TODO: this should change depending on platform. e.g., standard is : but windows doesnt support that, so it uses ; or - instead.
const val MAILDIR_SEPERATOR = ";"

// TODO: this sucks
@JvmInline
value class MaildirUniqueName(val value: String) {
    constructor(name: String, flags: Set<Flag>? = null) : this(name + flags.orEmpty().joinToString(prefix = "${MAILDIR_SEPERATOR}2,", separator = "") {
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

    val flags get() = value.split(MAILDIR_SEPERATOR).last().run {
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

    override suspend fun init() {
        if (attributes.get("SUBSCRIPTIONS")?.contains("INBOX") != true) attributes.append("SUBSCRIPTIONS", "INBOX")
    }

    override suspend fun folders(): List<String> {
        return user.listFolders().map { it.name }.minus(arrayOf("new", "cur", "tmp")).plus("INBOX")
    }

    override fun folder(name: String): MailboxFolder {
        return if (name == "INBOX") inbox else MaildirFolder(this, user.folder(name))
    }

    override suspend fun createFolder(name: String) {
        user.createFolder(name)
    }

    override suspend fun store(message: String) {
        inbox.store(emptySet(),  message)
    }
}

// TODO: non maildir-default folders should start with a .
private val DEFAULT_MAILDIR_USER_FOLDERS = arrayOf("new", "cur", "tmp", "Sent", "Drafts", "Trash")

suspend fun initMailDirStructure(fs: FileSystem) {
    Config.accounts.forEach {
        val user = fs.folder(it.email)

        for (folder in DEFAULT_MAILDIR_USER_FOLDERS) user.createFolder(folder)

        if(user.attributes.get("SUBSCRIPTIONS").isNullOrEmpty())
            user.attributes.set("SUBSCRIPTIONS", "INBOX\nSent\nDrafts\nTrash\n")
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

    override suspend fun store(flags: Set<Flag>, message: String) {
        val name = tmp.storeWithName(flags, message)
        tmp.moveFile(name, new)
        onMessageStore?.invoke(Message.fromText(message))
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

    suspend fun storeWithName(flags: Set<Flag>, message: String) : MaildirUniqueName {
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
            deliveries.getAndIncrement(),
            flags.takeIf { it.isNotEmpty() }
        )

        folder.writeFile(name.value, message.encodeToByteArray())
        return name
    }

    override suspend fun store(flags: Set<Flag>, message: String) {
        storeWithName(flags, message)
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

    override val flags: Set<Flag> get() = MaildirUniqueName(fileName).flags.toSet()

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