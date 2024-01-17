package dev.sitar.kmail.runner

import mu.KotlinLogging
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.KeyFactory
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import javax.net.ssl.*


private val logger = KotlinLogging.logger { }

fun ssl(security: KmailConfig.Security): Pair<SSLContext, KeyStore> {
    val keyStore = KeyStore.getInstance("PKCS12")
    keyStore.load(FileInputStream(security.keystore), security.password.toCharArray())

    logger.debug { "Generating SSL context." }
    val ssl = SSLContext.getInstance("TLSv1.3")

    val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
    keyManagerFactory.init(keyStore, security.password.toCharArray())

    keyStore.aliases().iterator().forEach {
        logger.debug { "got certificate\n${keyStore.getCertificate(it)}"}
    }

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
