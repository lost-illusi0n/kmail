package dev.sitar.kmail.runner.storage

import dev.sitar.kmail.runner.Config
import dev.sitar.kmail.runner.KmailConfig
import dev.sitar.kmail.runner.filterSpam
import dev.sitar.kmail.smtp.InternetMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

fun CoroutineScope.mailbox(incoming: Flow<InternetMessage>): StorageLayer {
    require(Config.mailbox.filesystem.type.contentEquals("local"))
    val storage = KmailFileSystemStorageLayer(Config.mailbox.filesystem.dir)
//    val mailbox = when (Config.mailbox.format) {
//        KmailConfig.Mailbox.Format.Maildir -> Maildir(File(Config.mailbox.dir).also { it.mkdir() })
//    }

    launch {
        incoming.filterSpam().mapToAccount().onEach { (account, message) ->
            storage.user(account.username).store(message.message)
        }.launchIn(this)
    }

    return storage
}

private fun Flow<InternetMessage>.mapToAccount(): Flow<Pair<KmailConfig.Account, InternetMessage>> = map {
    // find the user that an email associated with any of the rcpts of the message
    Config.accounts.find { acc ->
        it.envelope.recipientAddresses.any { addr ->
            addr.asText().drop(1).dropLast(1).contentEquals(acc.email)
        }
    }!! to it
}