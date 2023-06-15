package dev.sitar.kmail.utils.server

import dev.sitar.kmail.utils.connection.Connection
import dev.sitar.kmail.utils.connection.TlsCapableConnection
import javax.net.ssl.SSLContext

actual class TlsCapableServerSocket(val socket: java.net.ServerSocket, val sslContext: SSLContext) : ServerSocket {
    override suspend fun accept(): Connection {
        return TlsCapableConnection(socket.accept(), sslContext, isSecure = false)
    }
}