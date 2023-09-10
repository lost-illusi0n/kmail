package dev.sitar.kmail.runner

import mu.KotlinLogging
import java.io.FileInputStream
import java.io.InputStream
import java.security.KeyFactory
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager


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
    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
    keyStore.load(null)

    Config.security.certificates.forEach { (certPath, key) ->
        FileInputStream(certPath).use { certStream ->
            certStream.load().apply {
                forEachIndexed { i, cert ->
                    keyStore.setCertificateEntry(certPath, cert)
                }

                keyStore.setKeyEntry("private",
                    KeyFactory.getInstance("RSA")
                        .generatePrivate(PKCS8EncodedKeySpec(FileInputStream(key).readAllBytes())),
                    null,
                    this
                )
            }
        }
    }

    logger.debug { "Generating SSL context." }
    val ssl = SSLContext.getInstance("TLS")

    val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
    keyManagerFactory.init(keyStore, null)

    val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    trustManagerFactory.init(keyStore)

    val mine = trustManagerFactory.trustManagers.filterIsInstance<X509TrustManager>().first()
    val default = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).also { it.init(null as KeyStore?) }.trustManagers.filterIsInstance<X509TrustManager>().first()

    val trustManager = object: X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            default.checkClientTrusted(chain, authType)
        }

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            try {
                mine.checkServerTrusted(chain, authType)
            } catch (e: CertificateException) {
                default.checkServerTrusted(chain, authType)
            }
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return default.acceptedIssuers
        }

    }

    ssl.init(keyManagerFactory.keyManagers, arrayOf(trustManager), null)

    logger.debug { "Generated SSL context." }

    return ssl to keyStore
}
