package dev.sitar.kmail.utils.connection

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.network.tls.*
import kotlinx.coroutines.Dispatchers

class KtorConnectionFactory(val tlsConfig: TLSConfig = TLSConfigBuilder().build()): ConnectionFactory {
    override suspend fun connect(host: String, port: Int): Connection {
        val socket = aSocket(SelectorManager(Dispatchers.Default)).tcp().connect(host, port)
        return KtorConnection(socket, tlsConfig, isSecure = false)
    }
}