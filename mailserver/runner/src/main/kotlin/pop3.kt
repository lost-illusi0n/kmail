package dev.sitar.kmail.runner

import dev.sitar.kmail.agents.pop3.Pop3Layer
import dev.sitar.kmail.agents.pop3.Pop3Maildrop
import dev.sitar.kmail.agents.pop3.Pop3Message
import dev.sitar.kmail.agents.pop3.Pop3Server
import dev.sitar.kmail.runner.storage.formats.Mailbox
import dev.sitar.kmail.runner.storage.formats.MailboxMessage
import dev.sitar.kmail.runner.storage.StorageLayer
import dev.sitar.kmail.utils.server.ServerSocketFactory
import dev.sitar.kmail.utils.todo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

suspend fun pop3(socket: ServerSocketFactory, layer: Pop3Layer) {
    logger.info("Starting Pop3 server.")

    Pop3Server(socket.bind(POP3_SERVER), layer).listen()
}

class KmailPop3Layer(val storage: StorageLayer): Pop3Layer {
    override suspend fun userExists(user: String): Boolean {
        return Config.accounts.any { it.email.contentEquals(user) }
    }

    override suspend fun login(user: String, password: String): Boolean {
        return Config.accounts.any { it.email.contentEquals(user) && passVerify(password, it.passwordHash) }
    }

    override suspend fun maildrop(user: String): Pop3Maildrop {
        return KmailPop3Maildrop(storage.user(user))
    }
}

class KmailPop3Message(val message: MailboxMessage) : Pop3Message {
    override val uniqueIdentifier: String = message.name

    override val size: Long = message.length

    override val deleted: Boolean
        get() = false

    override suspend fun getContent(): String = message.getMessage().asText()

    override fun delete() {
        todo()
    }
}

class KmailPop3Maildrop(private val mailbox: Mailbox) : Pop3Maildrop {
    private val folder = mailbox.inbox

    private lateinit var _messages: List<KmailPop3Message>

    override suspend fun messages(): List<KmailPop3Message> {
        if (this::_messages.isInitialized) return _messages

        _messages = folder.messages().map { KmailPop3Message(it) }

        return _messages
    }

    override fun commit() {
        todo()
    }
}