package dev.sitar.kmail.runner

import dev.sitar.kmail.agents.pop3.*
import dev.sitar.kmail.message.Message
import dev.sitar.kmail.runner.storage.StorageLayer
import dev.sitar.kmail.runner.storage.UserStorageLayer
import dev.sitar.kmail.utils.server.ServerSocketFactory
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

suspend fun pop3(socket: ServerSocketFactory, layer: Pop3Layer): Pop3Server = coroutineScope {
    logger.info("Starting Pop3 server.")

    val server = Pop3Server(socket.bind(POP3_SERVER), layer)
    launch { server.listen() }

    logger.info("Started Pop3 server.")

    server
}

class KmailPop3Layer(val storage: StorageLayer): Pop3Layer {
    override suspend fun userExists(user: String): Boolean {
        return Config.accounts.any { it.username.contentEquals(user) }
    }

    override suspend fun login(user: String, password: String): Boolean {
        return Config.accounts.any { it.username.contentEquals(user) && it.password.contentEquals(password) }
    }

    override suspend fun maildrop(user: String): Pop3Maildrop {
        return KmailPop3Maildrop(storage.user(user))
    }
}

class KmailInMemoryPop3Message(val message: Message): Pop3Message {
    private val content = message.asText()

    override val size: Int = content.length

    override val deleted: Boolean
        get() = false

    override fun getContent(): String = content

    override fun delete() {
        TODO("Not yet implemented")
    }
}

class KmailPop3Maildrop(private val storage: UserStorageLayer): Pop3Maildrop {
    override val messages: List<KmailInMemoryPop3Message> = storage.messages().map { KmailInMemoryPop3Message(it) }

    override fun commit() {
        TODO()
    }
}