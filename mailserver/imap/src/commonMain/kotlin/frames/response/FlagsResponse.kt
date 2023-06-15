package dev.sitar.kmail.imap.frames.response

import dev.sitar.kio.async.writers.AsyncWriter
import dev.sitar.kmail.utils.io.writeStringUtf8

data class FlagsResponse(val flags: Set<String> = SYSTEM_FLAGS) : ImapResponse {
    companion object {
        val SYSTEM_FLAGS = setOf(
            "\\Seen",
            "\\Answered",
            "\\Flagged",
            "\\Deleted",
            "\\Draft",
            "\\Recent"
        )
    }
    override suspend fun serialize(output: AsyncWriter) {
        output.writeStringUtf8("FLAGS (${flags.joinToString(" ")})")
    }
}