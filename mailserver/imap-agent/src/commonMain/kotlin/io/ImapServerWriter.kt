package dev.sitar.kmail.imap.agent.io

import dev.sitar.kio.buffers.DefaultBufferPool
import dev.sitar.kio.use
import dev.sitar.kmail.imap.frames.response.TaggedImapResponse
import dev.sitar.kmail.utils.io.AsyncWriterStream
import dev.sitar.kmail.utils.io.asAsyncWriterStream
import dev.sitar.kmail.utils.io.writeLineEnd

class ImapServerWriter(val writer: AsyncWriterStream) : AsyncWriterStream by writer {
    suspend fun writeResponse(taggedResponse: TaggedImapResponse) {
        DefaultBufferPool.use(20) {
            val writer = ImapServerWriter(it.asAsyncWriterStream())

            taggedResponse.tag.serialize(writer)
            taggedResponse.response.serialize(writer)
            writer.writeLineEnd()

            writer.flush()

            println(it.fullSlice().joinToString("") {
                it.toInt().toChar().toString()
                    .replace("\r", "\\r")
                    .replace("\n", "\\n\n")
            })
        }

        taggedResponse.tag.serialize(this)
        taggedResponse.response.serialize(this)
        writeLineEnd()

        flush()
    }
}

fun AsyncWriterStream.asImapServerWriter(): ImapServerWriter {
    return ImapServerWriter(this)
}