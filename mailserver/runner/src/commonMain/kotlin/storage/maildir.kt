package dev.sitar.kmail.runner.storage

import dev.sitar.kmail.message.Message
import dev.sitar.kmail.smtp.InternetMessage
import java.io.File

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
    fun new(message: Message): String

    fun read(name: String): Message

    fun all()
}

class Maildir(val dir: File): Mailbox {
    val tmp: MaildirDirectory = MaildirDirectory(dir.resolve("tmp"))
    val new: MaildirDirectory = MaildirDirectory(dir.resolve("new"))
    val cur: MaildirDirectory = MaildirDirectory(dir.resolve("cur"))

    fun new(name: MaildirUniqueName, message: Message) {
        tmp.write(name, message)
        tmp.moveAtomic(name, new)
    }

    fun readFromNew(name: MaildirUniqueName) {
        new.moveAtomic(name, cur)
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