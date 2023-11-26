package dev.sitar.kmail.utils.server

import dev.sitar.kmail.utils.connection.Connection
import dev.sitar.kmail.utils.connection.TlsCapableConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.net.ssl.SSLContext

actual class TlsCapableServerSocket(val socket: java.net.ServerSocket, val sslContext: SSLContext) : ServerSocket {
    override suspend fun accept(): Connection {
        val socket = withContext(Dispatchers.IO) { socket.accept() }
        return TlsCapableConnection(socket, sslContext, isSecure = false)
    }
}