package dev.sitar.kmail.imap.frames.response

import dev.sitar.kio.async.writers.AsyncWriter
import dev.sitar.kmail.utils.io.writeStringUtf8

data class ByeResponse(val responseCode: ResponseCode? = null, val text: String) : ImapResponse {
    override suspend fun serialize(output: AsyncWriter) {
        output.writeStringUtf8("BYE ")
        responseCode?.serialize(output)
        output.writeStringUtf8(text)
    }
}