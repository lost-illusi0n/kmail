package dev.sitar.kmail.runner.storage.formats

import dev.sitar.kmail.message.Message
import dev.sitar.kmail.runner.storage.Attributable
import dev.sitar.kmail.runner.storage.filesystems.FsFile
import dev.sitar.kmail.runner.storage.filesystems.FsFolder
import io.ktor.util.*
import kotlinx.datetime.Clock
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

@JvmInline
value class MaildirUniqueName(val value: String) {
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
        deliveries: Int? = null
    ): this("$timestamp.#${sequenceNumber}X${bootNumber}R${random}.$hostname")

    val timestamp: Long get() = 0
    val unique: String get() = ""
    val hostname: String get() = ""
}

class Maildir(val user: FsFolder) : Mailbox, Attributable by user {
    override val inbox: MaildirInbox = MaildirInbox(user)

    override suspend fun folders(): List<String> {
        return user.listFolders().map { it.name }.minus(arrayOf("new", "cur", "tmp")).plus("INBOX")
    }

    override fun folder(name: String): MailboxFolder {
        return if (name == "INBOX") inbox else MaildirFolder(user.folder(name))
    }

    override suspend fun createFolder(name: String) {
        user.createFolder(name)
    }

    override suspend fun store(message: Message) {
        inbox.store(message)
    }
}

class MaildirInbox(val user: FsFolder) : MailboxFolder, Attributable by user {
    val tmp: MaildirFolder = MaildirFolder(user.folder("tmp"))
    val new: MaildirFolder = MaildirFolder(user.folder("new"))
    val cur: MaildirFolder = MaildirFolder(user.folder("cur"))

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

    suspend fun store(message: Message) {
        val name = tmp.store(message)
        tmp.moveFile(name, new)
        onMessageStore?.invoke(message)
    }
}

class MaildirFolder(val folder: FsFolder) : MailboxFolder, Attributable by folder {
    override val name: String = folder.name

    override var onMessageStore: (suspend (Message) -> Unit)? = null

    override suspend fun totalMessages(): Int = folder.listFiles().size

    override suspend fun newMessages(): Int = if (folder.name == "new") totalMessages() else 0

    override suspend fun messages(): List<MailboxMessage> {
        return folder.listFiles().map { MaildirMessage(it) }
    }

    override suspend fun message(index: Int): MailboxMessage {
        return messages()[index]
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
        onMessageStore?.invoke(message)
        return name
    }

    suspend fun moveFile(name: MaildirUniqueName, to: MaildirFolder) {
        return folder.move(name.value, to.folder)
    }
}


// TODO: replace this with atomicfu
private val deliveries: AtomicInteger = AtomicInteger(0)

class MaildirMessage(val file: FsFile): MailboxMessage {
    override val name: String = file.name

    override val length: Long = file.size

    override suspend fun getMessage(): Message {
        return Message.fromText(file.readContent().decodeToString())
    }
}