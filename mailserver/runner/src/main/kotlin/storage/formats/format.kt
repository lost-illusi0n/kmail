package dev.sitar.kmail.runner.storage.formats

import dev.sitar.kmail.message.Message
import dev.sitar.kmail.runner.storage.Attributable

interface Mailbox: Attributable {
    val inbox: MailboxFolder

    suspend fun folders(): List<String>

    fun folder(name: String): MailboxFolder

    suspend fun createFolder(name: String)

    suspend fun store(message: Message)
}

interface MailboxFolder: Attributable {
    val name: String

    var onMessageStore: (suspend (Message) -> Unit)?

    suspend fun totalMessages(): Int

    suspend fun newMessages(): Int

    suspend fun messages(): List<MailboxMessage>

    suspend fun message(index: Int): MailboxMessage
}

interface MailboxMessage {
    val name: String

    val length: Long

    suspend fun getMessage(): Message
}