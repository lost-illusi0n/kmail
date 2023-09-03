package dev.sitar.kmail.runner.storage.filesystems

import dev.sitar.kmail.runner.KmailConfig

expect class LocalFileSystem(config: KmailConfig.Mailbox.Filesystem.Local): FileSystem