package dev.sitar.kmail.imap.frames.response

import dev.sitar.kio.async.writers.AsyncWriter
import dev.sitar.kmail.imap.frames.DataItem
import dev.sitar.kmail.utils.io.writeStringUtf8

data class FetchResponse(val id: Int, val items: Set<DataItem.Response>): ImapResponse {
    override suspend fun serialize(output: AsyncWriter) {
        output.writeStringUtf8("$id FETCH (")

        items.forEachIndexed { index, item ->
            if (index != 0) output.writeStringUtf8(" ")

            item.serialize(output)
        }

        output.writeStringUtf8(")")
    }
}