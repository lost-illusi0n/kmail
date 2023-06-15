package dev.sitar.kmail.pop3.io

import dev.sitar.kmail.pop3.replies.Pop3Reply
import dev.sitar.kmail.utils.io.AsyncWriterStream
import dev.sitar.kmail.utils.io.writeLineEnd
import dev.sitar.kmail.utils.io.writeStringUtf8

class AsyncPop3ServerWriter(writer: AsyncWriterStream): AsyncWriterStream by writer {
    suspend fun writeReply(reply: Pop3Reply) {
        when (reply) {
            is Pop3Reply.ErrReply -> writeStringUtf8("-ERR ")
            is Pop3Reply.OkReply -> writeStringUtf8("+OK ")
            is Pop3Reply.DataReply -> { }
        }

        writeStringUtf8(reply.message)
        writeLineEnd()
        flush()
    }
}

fun AsyncWriterStream.asPop3ServerWriter(): AsyncPop3ServerWriter {
    return AsyncPop3ServerWriter(this)
}