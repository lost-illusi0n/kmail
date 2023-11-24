package dev.sitar.kmail.runner.storage

import dev.sitar.kmail.runner.Account
import dev.sitar.kmail.runner.Config
import dev.sitar.kmail.runner.KmailConfig
import dev.sitar.kmail.runner.filterSpam
import dev.sitar.kmail.runner.storage.filesystems.CachedFileSystem
import dev.sitar.kmail.runner.storage.filesystems.InMemoryFileSystem
import dev.sitar.kmail.runner.storage.filesystems.LocalFileSystem
import dev.sitar.kmail.runner.storage.filesystems.S3FileSystem
import dev.sitar.kmail.smtp.InternetMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

suspend fun CoroutineScope.mailbox(incoming: Flow<InternetMessage>): StorageLayer {
    logger.info { "initiating filesystem" }

    val filesystem = when (Config.mailbox.filesystem) {
        is KmailConfig.Mailbox.Filesystem.InMemory -> InMemoryFileSystem()
        is KmailConfig.Mailbox.Filesystem.Local -> LocalFileSystem(Config.mailbox.filesystem.dir)
        is KmailConfig.Mailbox.Filesystem.S3 -> S3FileSystem(Config.mailbox.filesystem)
    }

    logger.info { "detected a ${Config.mailbox.filesystem} filesystem" }

    filesystem.init()

    logger.info { "initiated filesystem" }

    val storage = KmailStorageLayer(CachedFileSystem(filesystem))
    storage.init()

    launch {
        incoming.filterSpam().mapToAccount().onEach { (account, message) ->
            logger.debug { "received email for ${account.email}" }
            storage.user(account.email).store(message.message.asText())
        }.launchIn(this)
    }

    return storage
}

private fun Flow<InternetMessage>.mapToAccount(): Flow<Pair<Account, InternetMessage>> = map {
    // find the user that an email associated with any of the rcpts of the message
    Config.accounts.find { acc ->
        it.envelope.recipientAddresses.any { addr ->
            addr.asText().drop(1).dropLast(1).contentEquals(acc.email)
        }
    }!! to it
}