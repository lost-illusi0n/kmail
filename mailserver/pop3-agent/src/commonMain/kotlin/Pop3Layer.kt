package dev.sitar.kmail.agents.pop3

interface Pop3Layer {
    suspend fun userExists(user: String): Boolean
    suspend fun login(user: String, password: String): Boolean
    suspend fun maildrop(user: String): Pop3Maildrop
}

interface Pop3Maildrop {
    val messages: List<Pop3Message>

    fun commit()
}

val Pop3Maildrop.messageCount: Int get() = messages.size
val Pop3Maildrop.dropSize: Int get() = messages.sumOf { it.size }

interface Pop3Message {
    val uniqueIdentifier: String

    val size: Int

    val deleted: Boolean

    fun getContent(): String

    fun delete()
}