package dev.sitar.kmail.utils.connection

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.utils.io.AsyncWriterStream

interface Connection {
    val remote: String

    val isSecure: Boolean

    val reader: AsyncReader
    val writer: AsyncWriterStream

    suspend fun secureAsClient(): Connection

    suspend fun secureAsServer(): Connection

    fun close()
}