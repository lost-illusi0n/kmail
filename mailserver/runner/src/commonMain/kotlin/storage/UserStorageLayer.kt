package dev.sitar.kmail.runner.storage

interface UserStorageLayer {
    suspend fun directory(name: String): UserDirectoryStorageLayer
}