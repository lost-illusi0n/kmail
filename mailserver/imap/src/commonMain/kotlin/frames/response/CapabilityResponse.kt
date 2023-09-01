package dev.sitar.kmail.imap.frames.response

import dev.sitar.kio.async.writers.AsyncWriter
import dev.sitar.kmail.imap.Capability
import dev.sitar.kmail.utils.io.writeStringUtf8

data class CapabilityResponse(val capabilities: List<Capability>): ImapResponse {
    override suspend fun serialize(output: AsyncWriter) {
        output.writeStringUtf8("CAPABILITY ${capabilities.joinToString(" ") { it.value }}")
    }
}
