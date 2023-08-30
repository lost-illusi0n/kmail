package dev.sitar.kmail.runner.storage

import dev.sitar.kmail.message.Message
import kotlinx.datetime.Clock
import java.io.File
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

interface Mailbox {
    val inbox: MailboxFolder

    fun folders(): List<String>

    fun folder(name: String): MailboxFolder

    fun store(message: Message)
}

data class MailboxMessage(val uniqueIdentifier: String, val message: Message)

interface MailboxFolder {
    val name: String

    fun messages(): List<MailboxMessage>

    fun message(index: Int): MailboxMessage
}

class Maildir(val user: File) : Mailbox {
    override val inbox: MaildirInbox = MaildirInbox(user)

    override fun folders(): List<String> {
        return user.list()?.asList()?.minus(arrayOf("new", "cur")) ?: emptyList()
    }

    override fun folder(name: String): MailboxFolder {
        println("folder: $name")
        require(name in folders())
        return MaildirFolder(user.resolve(name))
    }

    override fun store(message: Message) {
        inbox.store(message)
    }

//    val tmp: MaildirDirectory = MaildirDirectory(dir.resolve("tmp"))
//    val new: MaildirDirectory = MaildirDirectory(dir.resolve("new"))
//    val cur: MaildirDirectory = MaildirDirectory(dir.resolve("cur"))
//
//    fun new(name: MaildirUniqueName, message: Message) {
//        tmp.write(name, message)
//        tmp.moveAtomic(name, new)
//    }
//
//    fun readFromNew(name: MaildirUniqueName) {
//        new.moveAtomic(name, cur)
//    }
}

class MaildirInbox(val user: File) : MailboxFolder {
    val tmp: MaildirFolder = MaildirFolder(user.resolve("tmp"))
    val new: MaildirFolder = MaildirFolder(user.resolve("new"))
    val cur: MaildirFolder = MaildirFolder(user.resolve("cur"))

    override val name: String
        get() = "INBOX"

    override fun messages(): List<MailboxMessage> {
        return new.messages() + cur.messages()
    }

    override fun message(index: Int): MailboxMessage {
        return messages()[index]
    }

    fun store(message: Message) {
        val name = tmp.store(message)
        tmp.moveAtomic(name, new)
    }
}

// TODO: replace this with atomicfu
private val deliveries: AtomicInteger = AtomicInteger(0)

private fun File.toMailboxMessage(): MailboxMessage {
    return MailboxMessage(name, Message.fromText(readText()))
}

class MaildirFolder(val folder: File) : MailboxFolder {
    override val name: String = folder.name

    init {
        folder.mkdir()
    }

    override fun messages(): List<MailboxMessage> {
        return folder.listFiles()?.map(File::toMailboxMessage) ?: emptyList()
    }

    override fun message(index: Int): MailboxMessage {
        return messages()[index]
    }

    fun store(message: Message): MaildirUniqueName {
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
        val file = folder.resolve(name.value)
        file.createNewFile()
        file.writeText(message.asText())
        return name
    }

    fun moveAtomic(name: MaildirUniqueName, to: MaildirFolder) {
        folder.resolve(name.value).renameTo(to.folder.resolve(name.value))
    }
}

class MaildirDirectory(val dir: File) {
    val size: Int = dir.listFiles()!!.size
    val names: Sequence<MaildirUniqueName> get() = dir.listFiles()!!.asSequence().map { MaildirUniqueName(it.name) }

    fun read(name: MaildirUniqueName): Message {
        return Message.fromText(dir.resolve(name.value).readText())
    }

    fun write(name: MaildirUniqueName, message: Message) {
        val file = dir.resolve(name.value)
        file.createNewFile()
        file.writeText(message.asText())
    }

    fun moveAtomic(name: MaildirUniqueName, to: MaildirDirectory) {
        dir.resolve(name.value).renameTo(to.dir.resolve(name.value))
    }
}