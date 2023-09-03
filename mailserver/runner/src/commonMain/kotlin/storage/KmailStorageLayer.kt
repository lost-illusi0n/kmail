package dev.sitar.kmail.runner.storage

import dev.sitar.kmail.runner.Config
import dev.sitar.kmail.runner.KmailConfig
import dev.sitar.kmail.runner.storage.filesystems.FileSystem
import dev.sitar.kmail.runner.storage.formats.Mailbox
import dev.sitar.kmail.runner.storage.formats.Maildir

class KmailStorageLayer(override val fs: FileSystem) : StorageLayer {
    override suspend fun user(username: String): Mailbox {
        val user = fs.folder(username)

        return when (Config.mailbox.format) {
            KmailConfig.Mailbox.Format.Maildir -> Maildir(user)
        }
    }
}