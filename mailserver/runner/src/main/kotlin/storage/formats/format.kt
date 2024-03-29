package dev.sitar.kmail.runner.storage.formats

import dev.sitar.kmail.imap.agent.Flag
import dev.sitar.kmail.message.Message
import dev.sitar.kmail.runner.storage.Attributable
import kotlinx.coroutines.flow.Flow

interface Mailbox: Attributable {
    val inbox: MailboxFolder

    suspend fun init()

    suspend fun folders(): List<String>

    fun folder(name: String): MailboxFolder

    suspend fun createFolder(name: String)

    suspend fun store(message: String)
}

interface MailboxFolder: Attributable {
    val name: String

    var onMessageStore: (suspend (Message) -> Unit)?

    suspend fun totalMessages(): Int

    suspend fun newMessages(): Int

    suspend fun messages(): List<MailboxMessage>

    suspend fun message(index: Int): MailboxMessage

    suspend fun messageByUid(uid: Int): MailboxMessage?

    suspend fun store(flags: Set<Flag>, message: String)
}

interface MailboxMessage {
    val name: String

    val length: Long
    val flags: Set<Flag>

    suspend fun updateFlags(flags: Set<Flag>)

    suspend fun getMessage(): Message
}