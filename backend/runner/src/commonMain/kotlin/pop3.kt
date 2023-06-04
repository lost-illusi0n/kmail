package dev.sitar.kmail.runner

import dev.sitar.kmail.agents.pop3.*
import dev.sitar.kmail.message.Message
import dev.sitar.kmail.smtp.InternetMessage
import dev.sitar.kmail.utils.server.ServerSocketFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

suspend fun CoroutineScope.pop3Server(socket: ServerSocketFactory, layer: Pop3Layer): Pop3Server {
    logger.info("Starting Pop3 server.")
    val server = Pop3Server(socket.bind(POP3_SERVER), layer)
    launch { server.listen() }
    logger.info("Started Pop3 server.")
    return server
}

class KmailPop3Layer(val incomingMail: ReceiveChannel<InternetMessage>): Pop3Layer {
    override suspend fun userExists(user: String): Boolean {
        return user.contentEquals("catlover69")
    }

    override suspend fun login(user: String, password: String): Boolean {
        return user.contentEquals("catlover69") && password.contentEquals("password1234")
    }

    override suspend fun maildrop(user: String): Pop3Maildrop {
        return KmailPop3Maildrop(incomingMail.asList())
    }
}

private suspend fun <T> ReceiveChannel<T>.asList(): MutableList<T> {
    return buildList {
        while (!isClosedForReceive) {
            add(tryReceive().getOrNull() ?: break)
        }
    }.toMutableList()
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

class KmailPop3Maildrop(private val incomingMail: MutableList<InternetMessage>): Pop3Maildrop {
    override val messages: List<KmailInMemoryPop3Message> = incomingMail.map { KmailInMemoryPop3Message(it.message) }

    override fun commit() {
        TODO()
    }
}