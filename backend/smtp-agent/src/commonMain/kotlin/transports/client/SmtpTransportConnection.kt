package dev.sitar.kmail.smtp.agent.transports.client

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.utils.io.AsyncWriterStream

interface SmtpTransportConnection {
    val remote: String

    val supportsClientTls: Boolean
    val supportsServerTls: Boolean

    val isImplicitlyEncrypted: Boolean
        get() = false

    suspend fun upgradeToTls()

    val reader: AsyncReader
    val writer: AsyncWriterStream

    fun close()
}