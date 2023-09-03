package dev.sitar.kmail.runner.storage

import dev.sitar.kmail.runner.storage.filesystems.FileSystem
import dev.sitar.kmail.runner.storage.formats.Mailbox

interface StorageLayer {
    val fs: FileSystem

    suspend fun user(username: String): Mailbox
}
