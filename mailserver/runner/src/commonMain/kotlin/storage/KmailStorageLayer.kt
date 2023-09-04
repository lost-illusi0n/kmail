package dev.sitar.kmail.runner.storage

import dev.sitar.kmail.runner.Config
import dev.sitar.kmail.runner.KmailConfig
import dev.sitar.kmail.runner.storage.filesystems.FileSystem
import dev.sitar.kmail.runner.storage.formats.Mailbox
import dev.sitar.kmail.runner.storage.formats.Maildir

class KmailStorageLayer(override val fs: FileSystem) : StorageLayer {
    private val activeUsers = mutableMapOf<String, Mailbox>()

    override suspend fun user(username: String): Mailbox {
        var user = activeUsers[username]
        if (user != null) return user

        val folder = fs.folder(username)

        return when (Config.mailbox.format) {
            KmailConfig.Mailbox.Format.Maildir -> Maildir(folder)
        }.also { activeUsers[username] = it }
    }
}