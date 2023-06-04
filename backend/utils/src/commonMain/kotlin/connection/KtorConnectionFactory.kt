package dev.sitar.kmail.utils.connection

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers

class KtorConnectionFactory(val port: Int): ConnectionFactory {
    override suspend fun connect(host: String): Connection {
        val socket = aSocket(SelectorManager(Dispatchers.Default)).tcp().connect(host, port)
        return KtorConnection(socket, isSecure = false)
    }
}