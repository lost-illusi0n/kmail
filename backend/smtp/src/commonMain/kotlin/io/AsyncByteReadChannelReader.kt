package dev.sitar.kmail.smtp.io

import dev.sitar.kio.Pool
import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kio.buffers.Buffer
import dev.sitar.kio.buffers.DefaultBufferPool
import io.ktor.utils.io.*
import io.ktor.utils.io.bits.*

internal class AsyncByteReadChannelReader(val channel: ByteReadChannel) : AsyncReader, ByteReadChannel by channel {
    override val bufferPool: Pool<Buffer> = DefaultBufferPool
    override val openForRead: Boolean get() = !channel.isClosedForRead

    override suspend fun discard(n: Int): Int {
        return channel.discard(n.toLong()).toInt()
    }

    override suspend fun readBytes(n: Int, dest: Buffer): Int {
        return channel.read { source, start, endExclusive ->
            val len = minOf(n, (endExclusive - start).toInt())

            dest.resize(len)

            val (arr, off, _) = dest[dest.writeIndex..dest.writeIndex + len]
            source.copyTo(arr, start, len, off)

            len
        }
    }
}

internal fun ByteReadChannel.toAsyncByteReadChannelReader(): AsyncByteReadChannelReader {
    return AsyncByteReadChannelReader(this)
}