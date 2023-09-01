package dev.sitar.kmail.imap.frames.response

import dev.sitar.kio.async.writers.AsyncWriter
import dev.sitar.kmail.utils.io.writeStringUtf8

data class ListResponse(val attributes: Set<String>, val delimiter: String, val name: String) : ImapResponse {
    override suspend fun serialize(output: AsyncWriter) {
        output.writeStringUtf8("LIST (${attributes.joinToString(" ")}) \"$delimiter\" $name")
    }
}