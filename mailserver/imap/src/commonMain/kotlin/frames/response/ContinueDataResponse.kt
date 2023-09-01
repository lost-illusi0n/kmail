package dev.sitar.kmail.imap.frames.response

import dev.sitar.kio.async.writers.AsyncWriter
import dev.sitar.kio.async.writers.writeBytes

object ContinueDataResponse: ImapResponse {
    override suspend fun serialize(output: AsyncWriter) {
        output.writeBytes(byteArrayOf('+'.code.toByte()))
    }
}