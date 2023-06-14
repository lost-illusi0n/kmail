package dev.sitar.kmail.runner.storage

import dev.sitar.kmail.message.Message

interface UserStorageLayer {
    suspend fun store(message: Message)

    fun messages(): List<Message>
}