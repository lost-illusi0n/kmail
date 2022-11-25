package dev.sitar.kmail.smtp.agent.transports.client

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.smtp.io.*
import io.ktor.network.sockets.*
import io.ktor.network.tls.*
import io.ktor.utils.io.*
import kotlin.coroutines.coroutineContext

class SmtpSocketTransportConnection(private var socket: Socket, override val isImplicitlyEncrypted: Boolean = false) :
    SmtpTransportConnection {
    override val remote: String = socket.remoteAddress.toString()

    override val supportsClientTls: Boolean = true
    override val supportsServerTls: Boolean = false

    private var _readChannel: ByteReadChannel = socket.openReadChannel()
        set(value) {
            field = value
            _reader = field.toAsyncReader()
        }
    private var _writeChannel: ByteWriteChannel = socket.openWriteChannel()
        set(value) {
            field = value
            _writer = field.toAsyncWriterStream()
        }

    private var _reader = _readChannel.toAsyncReader()
    private var _writer = _writeChannel.toAsyncWriterStream()

    private var isUpgraded: Boolean = false

    override val reader: AsyncReader
        get() = _reader

    override val writer: AsyncWriterStream
        get() = _writer

    override suspend fun upgradeToTls() {
        if (isUpgraded) return

        socket = Connection(socket, _readChannel, _writeChannel).tls(coroutineContext)

        _readChannel = socket.openReadChannel()
        _writeChannel = socket.openWriteChannel()

        isUpgraded = true
    }

    override fun close() {
        socket.close()
    }
}