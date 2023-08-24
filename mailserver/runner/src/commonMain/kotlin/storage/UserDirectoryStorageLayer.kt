package dev.sitar.kmail.runner.storage

import dev.sitar.kmail.message.Message

interface UserDirectoryStorageLayer {
    val name: String

    suspend fun store(message: Message)

    fun messages(): List<Message>
}