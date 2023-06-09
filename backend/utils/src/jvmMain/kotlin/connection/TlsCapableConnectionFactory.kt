package dev.sitar.kmail.utils.connection

import javax.net.SocketFactory
import javax.net.ssl.SSLContext

actual class TlsCapableConnectionFactory(val sslContext: SSLContext) : ConnectionFactory {
    override suspend fun connect(host: String, port: Int): Connection {
        val socket = SocketFactory.getDefault().createSocket(host, port)
        return TlsCapableConnection(socket, sslContext, isSecure = false)
    }
}