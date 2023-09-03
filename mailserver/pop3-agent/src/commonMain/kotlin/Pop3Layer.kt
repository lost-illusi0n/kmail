package dev.sitar.kmail.agents.pop3

interface Pop3Layer {
    suspend fun userExists(user: String): Boolean
    suspend fun login(user: String, password: String): Boolean
    suspend fun maildrop(user: String): Pop3Maildrop
}

interface Pop3Maildrop {
    suspend fun messages(): List<Pop3Message>

    fun commit()
}

suspend fun Pop3Maildrop.messageCount(): Int = messages().size

suspend fun Pop3Maildrop.dropSize(): Long = messages().sumOf { it.size }

interface Pop3Message {
    val uniqueIdentifier: String

    val size: Long

    val deleted: Boolean

    suspend fun getContent(): String

    fun delete()
}