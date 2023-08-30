package dev.sitar.kmail.runner.storage

import dev.sitar.kmail.message.Message
import dev.sitar.kmail.runner.Config
import dev.sitar.kmail.runner.KmailConfig
import io.ktor.util.date.*
import java.io.File
import java.io.FileFilter

// TODO: replace this with a mpp solution
// TODO: replace this with the abstracted filesystem solution
class KmailFileSystemStorageLayer(rootDir: String) : StorageLayer {
    private val root = File(rootDir)

    init {
        root.mkdir()
    }

    override suspend fun user(username: String): Mailbox {
        val user = root.resolve(username)
        user.mkdir()

        return when (Config.mailbox.format) {
            KmailConfig.Mailbox.Format.Maildir -> Maildir(user)
        }
    }
}

class KmailFileSystemUserDirectoryStorageLayer(val mailbox: File) : UserDirectoryStorageLayer {
    override val name: String = mailbox.name

    init {
        mailbox.mkdir()
    }

    override suspend fun store(message: Message) {
        val email = mailbox.resolve("$name${getTimeMillis()}.eml")
        email.createNewFile()
        email.writeText(message.asText())
    }

    override fun messages(): List<Message> = buildList {
        mailbox.listFiles(FileFilter { it.extension.contentEquals("eml") })?.forEach {
            add(Message.fromText(it.readText()))
        }
    }
}

class KmailFileSystemUserStorageLayer(val user: File) : UserStorageLayer {
    override suspend fun directory(name: String): UserDirectoryStorageLayer {
        return KmailFileSystemUserDirectoryStorageLayer(user.resolve(name))
    }

//    override suspend fun directories(): List<UserDirectoryStorageLayer> {
//        return user.listFiles()!!.filter { it.isDirectory }.map { KmailFileSystemUserDirectoryStorageLayer(it) }
//    }
}