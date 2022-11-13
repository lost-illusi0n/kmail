package dev.sitar.kmail.smtp.io

import dev.sitar.kio.Pool
import dev.sitar.kio.Slice
import dev.sitar.kio.async.writers.AsyncWriter
import dev.sitar.kio.buffers.Buffer
import dev.sitar.kio.buffers.DefaultBufferPool
import io.ktor.utils.io.*

public class AsyncByteWriteChannelWriter(channel: ByteWriteChannel) : AsyncWriter, ByteWriteChannel by channel {
    override val bufferPool: Pool<Buffer> = DefaultBufferPool
    override val openForWrite: Boolean get() = !isClosedForWrite

    override suspend fun writeBytes(slice: Slice): Int {
        val (arr, off, len) = slice
        return writeAvailable(arr, off, len)
    }
}

public fun ByteWriteChannel.toAsyncByteChannelWriter(): AsyncByteWriteChannelWriter {
    return AsyncByteWriteChannelWriter(this)
}