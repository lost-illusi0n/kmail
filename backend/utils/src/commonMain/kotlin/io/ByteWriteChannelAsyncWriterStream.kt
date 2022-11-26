package dev.sitar.kmail.utils.io

import dev.sitar.kio.Pool
import dev.sitar.kio.Slice
import dev.sitar.kio.buffers.Buffer
import dev.sitar.kio.buffers.DefaultBufferPool
import io.ktor.utils.io.*

public class ByteWriteChannelAsyncWriterStream(public val channel: ByteWriteChannel) : AsyncWriterStream {
    override val bufferPool: Pool<Buffer>
        get() = DefaultBufferPool

    override val openForWrite: Boolean
        get() = !channel.isClosedForWrite

    override suspend fun writeBytes(slice: Slice): Int {
        val (arr, off, len) = slice

        return channel.writeAvailable(arr, off, len)
    }

    override fun flush() {
        channel.flush()
    }
}

public fun ByteWriteChannel.toAsyncWriterStream(): ByteWriteChannelAsyncWriterStream {
    return ByteWriteChannelAsyncWriterStream(this)
}