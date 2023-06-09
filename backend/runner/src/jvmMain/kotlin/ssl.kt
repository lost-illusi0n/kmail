package dev.sitar.kmail.runner

import mu.KotlinLogging
import java.io.FileInputStream
import java.io.InputStream
import java.security.KeyFactory
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory


private val logger = KotlinLogging.logger { }

private fun InputStream.load(): Array<Certificate> {
    logger.debug { "Loading certificates from input stream." }
    val factory = CertificateFactory.getInstance("X.509")

    return buildList {
        while (available() > 0) {
            val cert = factory.generateCertificate(this@load)
            add(cert)
            logger.debug { "Generated a certificate." }
            logger.trace { cert }
        }
    }.toTypedArray()
}

fun ssl(): Pair<SSLContext, KeyStore> {
    val keyStore = KeyStore.getInstance("PKCS12")
    keyStore.load(null, null)

    Config.security.certificatePaths.forEachIndexed { i, certPath ->
        logger.debug { "Loading certificates for $certPath." }

        val keyPath = Config.security.certificateKeys[i]

        FileInputStream(certPath).use { certStream ->
            certStream.load().apply {
                forEachIndexed { i, cert ->
                    keyStore.setCertificateEntry(certPath, cert)
                }

                keyStore.setKeyEntry("private",
                    KeyFactory.getInstance("RSA")
                        .generatePrivate(PKCS8EncodedKeySpec(FileInputStream(keyPath).readAllBytes())),
                    null,
                    this
                )
            }
        }
    }

    logger.debug { "Generating SSL context." }
    val ssl = SSLContext.getInstance("TLS")

    val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(keyStore, null)

    val trustManagerFactory = TrustManagerFactory.getInstance("SunX509")
    trustManagerFactory.init(keyStore)

    ssl.init(keyManagerFactory.keyManagers, trustManagerFactory.trustManagers, null)

    logger.debug { "Generated SSL context." }

    return ssl to keyStore
}
