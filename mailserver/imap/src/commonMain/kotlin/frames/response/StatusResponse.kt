package dev.sitar.kmail.imap.frames.response

import dev.sitar.kio.async.writers.AsyncWriter
import dev.sitar.kmail.imap.frames.command.StatusDataItem
import dev.sitar.kmail.utils.io.writeStringUtf8

data class StatusResponse(val mailbox: String, val items: Map<StatusDataItem, Any>): ImapResponse {
    override suspend fun serialize(output: AsyncWriter) {
        output.writeStringUtf8("STATUS $mailbox (${items.toList().joinToString(" ") { (item, value) -> "$item $value" }})")
    }
}