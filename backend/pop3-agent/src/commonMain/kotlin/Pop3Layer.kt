package dev.sitar.kmail.agents.pop3

interface Pop3Layer {
    suspend fun userExists(user: String): Boolean
    suspend fun login(user: String, password: String): Boolean
    suspend fun maildrop(user: String): Pop3Maildrop
}

interface Pop3Maildrop {
    val messageCount: Int
    val dropSize: Int

    //TODO: replace this with just a collection or smth with a modeled message struct
    fun getMessageSize(index: Int): Int
    fun getMessage(index: Int): String
    fun deleteMessage(index: Int)

    fun commit()
}