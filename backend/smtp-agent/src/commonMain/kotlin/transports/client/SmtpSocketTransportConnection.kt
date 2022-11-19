package dev.sitar.kmail.smtp.agent.transports.client

import dev.sitar.kmail.smtp.io.AsyncByteReadChannelReader
import dev.sitar.kmail.smtp.io.AsyncByteWriteChannelWriter
import dev.sitar.kmail.smtp.io.toAsyncByteChannelWriter
import dev.sitar.kmail.smtp.io.toAsyncByteReadChannelReader
import io.ktor.network.sockets.*
import io.ktor.network.tls.*
import io.ktor.utils.io.*
import kotlin.coroutines.coroutineContext

class SmtpSocketTransportConnection(private var socket: Socket, override val isImplicitlyEncrypted: Boolean = false) :
    SmtpTransportConnection {
    override val remote: String = socket.remoteAddress.toString()

    private var _readChannel: ByteReadChannel = socket.openReadChannel()
    private var _writeChannel: ByteWriteChannel = socket.openWriteChannel()
    private var _reader: AsyncByteReadChannelReader = _readChannel.toAsyncByteReadChannelReader()
    private var _writer: AsyncByteWriteChannelWriter = _writeChannel.toAsyncByteChannelWriter()

    private var isUpgraded: Boolean = false

    override val reader: AsyncByteReadChannelReader
        get() = _reader

    override val writer: AsyncByteWriteChannelWriter
        get() = _writer

    override suspend fun upgradeToTls() {
        if (isUpgraded) return

        socket = Connection(socket, _readChannel, _writeChannel).tls(coroutineContext)

        _readChannel = socket.openReadChannel()
        _writeChannel = socket.openWriteChannel()
        _reader = _readChannel.toAsyncByteReadChannelReader()
        _writer = _writeChannel.toAsyncByteChannelWriter()

        isUpgraded = true
    }

    override fun close() {
        socket.close()
    }
}