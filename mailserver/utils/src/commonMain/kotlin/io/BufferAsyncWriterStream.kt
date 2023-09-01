package dev.sitar.kmail.utils.io

import dev.sitar.kio.Pool
import dev.sitar.kio.Slice
import dev.sitar.kio.buffers.Buffer
import dev.sitar.kio.buffers.DefaultBufferPool

/**
 * not concurrent
 */
class BufferAsyncWriterStream(val buffer: Buffer): AsyncWriterStream {
    override val bufferPool: Pool<Buffer> = DefaultBufferPool

    override val openForWrite: Boolean = true

    private var flushBuffer = DefaultBufferPool.acquire(32)

    override fun flush() {
        buffer.writeBytes(flushBuffer.fullSlice())
        DefaultBufferPool.recycle(flushBuffer)
        flushBuffer = DefaultBufferPool.acquire(32)
    }

    override suspend fun writeBytes(slice: Slice): Int {
        flushBuffer.writeBytes(slice)
        return slice.length
    }
}

fun Buffer.asAsyncWriterStream(): BufferAsyncWriterStream {
    return BufferAsyncWriterStream(this)
}