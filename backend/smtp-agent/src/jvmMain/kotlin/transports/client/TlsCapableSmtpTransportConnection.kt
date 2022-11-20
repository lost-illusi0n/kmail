package dev.sitar.kmail.smtp.agent.transports.client

import dev.sitar.kio.async.readers.AsyncInputStreamReader
import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kio.async.readers.toAsyncReader
import dev.sitar.kmail.smtp.agent.io.OutputStreamAsyncWriterStream
import dev.sitar.kmail.smtp.agent.io.toAsyncWriterStream
import dev.sitar.kmail.smtp.io.AsyncWriterStream
import io.ktor.util.network.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import io.ktor.utils.io.pool.*
import kotlinx.coroutines.*
import kotlinx.coroutines.CancellationException
import java.net.Socket
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import kotlin.coroutines.EmptyCoroutineContext

actual typealias TlsCapableSocket = Socket

actual class TlsCapableSmtpTransportConnection actual constructor(
    private val sslContext: SSLContext,
    private var socket: TlsCapableSocket,
    private val isClient: Boolean
) : SmtpTransportConnection {
    private var isUpgraded = false

    override val remote: String
        get() = socket.remoteSocketAddress.toString()

    private var _reader: AsyncInputStreamReader = socket.inputStream.toAsyncReader()
    
    private var _writer: OutputStreamAsyncWriterStream = socket.outputStream.toAsyncWriterStream()

    override val reader: AsyncReader get() = _reader
    override val writer: AsyncWriterStream get() = _writer

    override suspend fun upgradeToTls() {
        if (isUpgraded) return

        val secureSocket = when (isClient) {
            true -> sslContext.socketFactory.createSocket(
                socket,
                socket.remoteSocketAddress.hostname,
                socket.remoteSocketAddress.port,
                true
            )

            false -> sslContext.socketFactory.createSocket(socket, _reader.inputStream, true)
        }

        secureSocket as SSLSocket
        secureSocket.startHandshake()

        socket = secureSocket
        _reader = socket.inputStream.toAsyncReader()
        _writer = socket.outputStream.toAsyncWriterStream()

        isUpgraded = true
    }

    override fun close() {
        socket.close()
    }
}