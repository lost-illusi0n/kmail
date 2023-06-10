package dev.sitar.kmail.runner

import dev.sitar.kmail.smtp.InternetMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

suspend fun storage(storage: StorageLayer, incoming: Flow<InternetMessage>): Unit = coroutineScope {
    incoming.filterSpam().mapToAccount().onEach { (account, message) ->
        storage.user(account.username).store(message)
    }.launchIn(this)
}

private fun Flow<InternetMessage>.mapToAccount(): Flow<Pair<KmailConfig.Account, InternetMessage>> = map {
    // find the user that an email associated with any of the rcpts of the message
    Config.accounts.find { acc ->
        it.envelope.recipientAddresses.any { addr ->
            addr.asText().drop(1).dropLast(1).contentEquals(acc.email)
        }
    }!! to it
}

interface StorageLayer {
    suspend fun user(username: String): UserStorageLayer
}

interface UserStorageLayer {
    val messages: ReceiveChannel<InternetMessage>

    suspend fun store(message: InternetMessage)
}

class KmailInMemoryStorageLayer : StorageLayer {
    private val userStorages = mutableMapOf<String, UserStorageLayer>()

    override suspend fun user(username: String): UserStorageLayer {
        if (Config.accounts.none { it.username.contentEquals(username) }) throw Exception("User does not exist.")

        return userStorages.getOrPut(username) { KmailInMemoryUserStorageLayer() }
    }
}

class KmailInMemoryUserStorageLayer : UserStorageLayer {
    override val messages: Channel<InternetMessage> = Channel()

    override suspend fun store(message: InternetMessage) {
        messages.send(message)
    }
}