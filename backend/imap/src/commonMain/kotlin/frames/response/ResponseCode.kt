package dev.sitar.kmail.imap.frames.response

import dev.sitar.kio.async.writers.AsyncWriter
import dev.sitar.kmail.utils.io.writeStringUtf8

sealed interface ResponseCode {
    object Alert: ResponseCode {
        override suspend fun serialize(output: AsyncWriter) {
            output.writeStringUtf8("[ALERT]")
        }
    }

    suspend fun serialize(output: AsyncWriter)
}
