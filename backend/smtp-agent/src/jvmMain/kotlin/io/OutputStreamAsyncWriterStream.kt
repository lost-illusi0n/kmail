package dev.sitar.kmail.smtp.agent.io

import dev.sitar.kio.Pool
import dev.sitar.kio.Slice
import dev.sitar.kio.buffers.Buffer
import dev.sitar.kio.buffers.DefaultBufferPool
import dev.sitar.kmail.utils.io.AsyncWriterStream
import java.io.OutputStream

class OutputStreamAsyncWriterStream(val outputStream: OutputStream) : AsyncWriterStream {
    override fun flush() {
        outputStream.flush()
    }

    override val bufferPool: Pool<Buffer>
        get() = DefaultBufferPool

    override val openForWrite: Boolean
        get() = true

    override suspend fun writeBytes(slice: Slice): Int {
        val (arr, off, len) = slice

        outputStream.write(arr, off, len)

        return len
    }
}

fun OutputStream.toAsyncWriterStream(): OutputStreamAsyncWriterStream {
    return OutputStreamAsyncWriterStream(this)
}