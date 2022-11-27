package dev.sitar.kmail.imap.frames.response

import dev.sitar.kio.async.writers.AsyncWriter
import dev.sitar.kmail.utils.io.writeStringUtf8

data class FetchResponse(val dataItems: Map<String, String>): ImapResponse {
    override suspend fun serialize(output: AsyncWriter) {
        output.writeStringUtf8("FETCH (${dataItems.map { (a, b) -> "$a $b" }.joinToString(" ")})")
    }
}