package dev.sitar.kmail.runner.storage

import dev.sitar.kmail.message.Message
import dev.sitar.kmail.smtp.InternetMessage
import io.ktor.util.date.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancel
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.FileFilter

// TODO: replace this with a mpp solution
class KmailFileSystemStorageLayer(rootDir: String) : StorageLayer {
    private val root = File(rootDir)

    init {
        root.mkdir()
    }

    override suspend fun user(username: String): UserStorageLayer {
        val user = root.resolve(username)
        user.mkdir()
        return KmailFileSystemUserStorageLayer(user)
    }
}

class KmailFileSystemUserStorageLayer(val user: File) : UserStorageLayer {
    override suspend fun store(message: Message) {
        val email = user.resolve("${getTimeMillis()}.eml")
        email.createNewFile()
        email.writeText(message.asText())
    }

    // TODO: cache this or something
    override fun messages(): List<Message> = buildList {
        user.listFiles(FileFilter { it.extension.contentEquals("eml") })?.forEach {
            add(Message.fromText(it.readText()))
        }
    }
}