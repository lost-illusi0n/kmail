package dev.sitar.kmail.runner

import dev.sitar.kmail.smtp.agent.transports.server.TlsCapableSmtpSubmissionServerTransportClient
import io.ktor.network.tls.certificates.*
import mu.KotlinLogging
import java.io.File
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

private val logger = KotlinLogging.logger { }

fun ssl(): SSLContext {
    val ssl = SSLContext.getInstance("TLS")

    val keyStore = if (File("./certs/root.keystore").exists()) {
        logger.debug { "Found an existing keystore!" }

        val keyStore = KeyStore.getInstance("JKS")

        keyStore.load(File("./certs/root.keystore").inputStream(), CONFIGURATION.keystorePassword!!.toCharArray())

        keyStore
    } else {
        logger.debug { "Generating a self-signed certificate." }
        val keyStore = generateCertificate(file = File("./certs/root.keystore"), keyAlias = "example", keyPassword = CONFIGURATION.keystorePassword!!)
        logger.debug { "Generated a self-signed certificate." }

        keyStore
    }


    val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(keyStore, CONFIGURATION.keystorePassword!!.toCharArray())

    val trustManagerFactory = TrustManagerFactory.getInstance("SunX509")
    trustManagerFactory.init(keyStore)

    ssl.init(keyManagerFactory.keyManagers, trustManagerFactory.trustManagers, null)

    logger.debug { "Generated SSL context." }

    return ssl
}
