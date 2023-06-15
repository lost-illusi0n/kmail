package dev.sitar.kmail.utils.connection

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kio.async.readers.toAsyncReader
import dev.sitar.kmail.utils.io.AsyncWriterStream
import dev.sitar.kmail.utils.io.toAsyncWriterStream
import io.ktor.util.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Socket
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket

actual class TlsCapableConnection(
    val socket: Socket,
    val sslContext: SSLContext,
    override val isSecure: Boolean
) : Connection {
    override val remote: String get() = socket.remoteSocketAddress.toString()

    override val reader: AsyncReader = socket.inputStream.toAsyncReader()
    override val writer: AsyncWriterStream = socket.outputStream.toAsyncWriterStream()

    override suspend fun secureAsClient(): Connection {
        if (isSecure) return this

        return withContext(Dispatchers.IO) {
            val secureSocket = sslContext.socketFactory.createSocket(
                socket,
                socket.remoteSocketAddress.hostname,
                socket.remoteSocketAddress.port,
                true
            ) as SSLSocket

            secureSocket.startHandshake()

            TlsCapableConnection(secureSocket, sslContext, true)
        }
    }

    override suspend fun secureAsServer(): Connection {
        if (isSecure) return this

        return withContext(Dispatchers.IO) {
            val secureSocket = sslContext.socketFactory.createSocket(
                socket,
                socket.inputStream,
                true
            ) as SSLSocket

            TlsCapableConnection(secureSocket, sslContext, true)
        }
    }

    override fun close() {
        socket.close()
    }
}