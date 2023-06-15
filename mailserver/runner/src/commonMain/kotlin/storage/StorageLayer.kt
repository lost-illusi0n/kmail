package dev.sitar.kmail.runner.storage

interface StorageLayer {
    suspend fun user(username: String): UserStorageLayer
}
