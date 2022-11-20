package dev.sitar.kmail.smtp.agent.transports.client

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.smtp.io.AsyncWriterStream

interface SmtpTransportConnection {
    val remote: String

    val isImplicitlyEncrypted: Boolean
        get() = false

    suspend fun upgradeToTls()

    val reader: AsyncReader
    val writer: AsyncWriterStream

    fun close()
}