package dev.sitar.kmail.utils.server

import mu.KotlinLogging
import javax.net.ssl.SSLContext

private val logger = KotlinLogging.logger { }

actual class TlsCapableServerSocketFactory(val sslContext: SSLContext) : ServerSocketFactory {
    override suspend fun bind(port: Int): ServerSocket {
        logger.debug { "Creating a secure server socket on port $port." }
        val serverSocket = javax.net.ServerSocketFactory.getDefault().createServerSocket(port)
        logger.debug { "Created a secure server socket on $port." }
        return TlsCapableServerSocket(serverSocket, sslContext)
    }
}