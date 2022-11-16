package dev.sitar.kmail.smtp.agent.transports.client

import dev.sitar.kmail.smtp.agent.transports.client.SmtpTransportConnection
import dev.sitar.kmail.smtp.io.AsyncByteReadChannelReader
import dev.sitar.kmail.smtp.io.AsyncByteWriteChannelWriter
import dev.sitar.kmail.smtp.io.toAsyncByteChannelWriter
import dev.sitar.kmail.smtp.io.toAsyncByteReadChannelReader
import io.ktor.network.sockets.*
import io.ktor.utils.io.*

class SmtpSocketTransportConnection(private val socket: Socket): SmtpTransportConnection {
    override val remote: String = socket.remoteAddress.toString()

    override val reader: AsyncByteReadChannelReader = socket.openReadChannel().toAsyncByteReadChannelReader()
    override val writer: AsyncByteWriteChannelWriter = socket.openWriteChannel().toAsyncByteChannelWriter()

    override fun close() {
        socket.close()
    }
}