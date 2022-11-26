package dev.sitar.kmail.imap.frames.response

import dev.sitar.kio.async.writers.AsyncWriter

interface ImapResponse {
    suspend fun serialize(output: AsyncWriter)
}