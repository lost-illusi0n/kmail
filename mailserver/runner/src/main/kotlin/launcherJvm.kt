package dev.sitar.kmail.runner

import dev.sitar.kmail.utils.connection.TlsCapableConnectionFactory
import dev.sitar.kmail.utils.server.TlsCapableServerSocketFactory
import kotlinx.coroutines.coroutineScope

suspend fun main(): Unit = coroutineScope {
    dns()

//    System.setProperty("javax.net.debug", "all")

    val (ssl, keystore) = ssl()
    val socketFactory = TlsCapableServerSocketFactory(ssl)
    // TODO: i cant get ktor connection factory to work
    val connectionFactory = TlsCapableConnectionFactory(ssl)
//    val connectionFactory = KtorConnectionFactory(TLSConfigBuilder().apply { addKeyStore(keystore, null, "private") }.build())
    run(socketFactory, connectionFactory)
}