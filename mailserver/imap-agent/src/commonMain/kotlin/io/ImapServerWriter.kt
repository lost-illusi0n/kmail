package dev.sitar.kmail.imap.agent.io

import dev.sitar.kmail.imap.frames.response.TaggedImapResponse
import dev.sitar.kmail.utils.io.AsyncWriterStream
import dev.sitar.kmail.utils.io.writeLineEnd

class ImapServerWriter(private val writer: AsyncWriterStream) : AsyncWriterStream by writer {
    suspend fun writeResponse(taggedResponse: TaggedImapResponse) {
        taggedResponse.tag.serialize(this)
        taggedResponse.response.serialize(this)
        writeLineEnd()

        flush()
    }
}

fun AsyncWriterStream.asImapServerWriter(): ImapServerWriter {
    return ImapServerWriter(this)
}