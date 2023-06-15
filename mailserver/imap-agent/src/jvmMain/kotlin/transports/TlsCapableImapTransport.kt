package dev.sitar.kmail.imap.agent.transports

import dev.sitar.kio.async.readers.toAsyncReader
import dev.sitar.kmail.imap.agent.io.asImapServerReader
import dev.sitar.kmail.imap.agent.io.asImapServerWriter
import dev.sitar.kmail.imap.frames.command.TaggedImapCommand
import dev.sitar.kmail.imap.frames.response.TaggedImapResponse
import dev.sitar.kmail.utils.io.toAsyncWriterStream
import io.ktor.util.network.*
import mu.KotlinLogging
import java.net.Socket
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket

private val logger = KotlinLogging.logger { }

actual class TlsCapableImapTransport(
    val ssl: SSLContext,
    val socket: Socket,
    override val isUpgraded: Boolean
) : ImapTransport {
    override val remote: String = "${socket.remoteSocketAddress.hostname}:${socket.remoteSocketAddress.port}"

    override val isClosed: Boolean
        get() = socket.isClosed

    private val reader = socket.inputStream.toAsyncReader().asImapServerReader()
    private val writer = socket.outputStream.toAsyncWriterStream().asImapServerWriter()

    override suspend fun upgrade() : ImapTransport {
        if (isUpgraded) return this

        val secureSocket = ssl.socketFactory.createSocket(socket, socket.inputStream, true)

        secureSocket as SSLSocket
        secureSocket.startHandshake()

        return TlsCapableImapTransport(ssl, secureSocket, isUpgraded = true)
    }

    override suspend fun send(response: TaggedImapResponse) {
        logger.trace { "IMAP($remote) >>> $response" }
        writer.writeResponse(response)
    }

    override suspend fun recv(): TaggedImapCommand {
        val command = reader.readCommand()
        logger.trace { "IMAP($remote) <<< $command" }
        return command
    }

    override fun close() {
        socket.close()
    }
}