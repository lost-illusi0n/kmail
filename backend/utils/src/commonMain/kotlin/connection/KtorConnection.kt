package dev.sitar.kmail.utils.connection

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.utils.io.AsyncWriterStream
import dev.sitar.kmail.utils.io.toAsyncReader
import dev.sitar.kmail.utils.io.toAsyncWriterStream
import io.ktor.network.sockets.*
import io.ktor.network.tls.*
import kotlin.coroutines.coroutineContext

class KtorConnection(val socket: Socket, override val isSecure: Boolean) : Connection {
    // raw ktor r/w channels
    private val writeChannel = socket.openWriteChannel()
    private val readChannel = socket.openReadChannel()

    override val remote: String
        get() = socket.remoteAddress.toString()

    override val reader: AsyncReader = readChannel.toAsyncReader()
    override val writer: AsyncWriterStream = writeChannel.toAsyncWriterStream()

    override suspend fun secureAsClient(): Connection {
        if (isSecure) return this

        return KtorConnection(Connection(socket, readChannel, writeChannel).tls(coroutineContext), isSecure = true)
    }

    override suspend fun secureAsServer(): Connection {
        error("not possible with ktor yet")
    }

    override fun close() {
        socket.close()
    }
}