package dev.sitar.kmail.utils.io

import dev.sitar.kio.Pool
import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kio.buffers.Buffer
import dev.sitar.kio.buffers.DefaultBufferPool
import io.ktor.utils.io.*
import io.ktor.utils.io.bits.*

public class AsyncByteReadChannelReader(private val channel: ByteReadChannel) : AsyncReader {
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

public fun ByteReadChannel.toAsyncReader(): AsyncByteReadChannelReader {
    return AsyncByteReadChannelReader(this)
}