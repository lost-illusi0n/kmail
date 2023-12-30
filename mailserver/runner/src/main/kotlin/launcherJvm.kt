package dev.sitar.kmail.runner

import dev.sitar.kio.async.readers.readBytes
import dev.sitar.kio.buffers.buffer
import dev.sitar.kio.buffers.contentEquals
import dev.sitar.kio.buffers.writeBytes
import dev.sitar.kmail.utils.connection.KtorConnectionFactory
import dev.sitar.kmail.utils.connection.TlsCapableConnectionFactory
import dev.sitar.kmail.utils.io.readStringUtf8
import dev.sitar.kmail.utils.io.writeStringUtf8
import dev.sitar.kmail.utils.server.TlsCapableServerSocketFactory
import io.ktor.network.sockets.*
import io.ktor.network.tls.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.debug.DebugProbes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.util.*
import kotlin.random.Random
import kotlin.random.nextUBytes
import kotlin.system.measureTimeMillis

private val logger = KotlinLogging.logger { }

suspend fun main() {
//    DebugProbes.enableCreationStackTraces = true
//    DebugProbes.sanitizeStackTraces = true
//    DebugProbes.install()

    logger.info { "Using JVM version: ${Runtime.version()}" }

    dns()

//    System.setProperty("javax.net.debug", "all")

    val (ssl, keystore) = ssl()
    val socketFactory = TlsCapableServerSocketFactory(ssl)
    // TODO: i cant get ktor connection factory to work
    val connectionFactory = TlsCapableConnectionFactory(ssl)
//    val connectionFactory = KtorConnectionFactory(TLSConfigBuilder().apply { addKeyStore(keystore, Config.security.password.toCharArray()) }.build())

    run(socketFactory, connectionFactory)
}