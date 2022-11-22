package dev.sitar.kmail.smtp.agent

import dev.sitar.kmail.smtp.PlainSaslMechanism
import dev.sitar.kmail.smtp.SaslMechanism
import dev.sitar.kmail.smtp.agent.transports.client.TlsCapableSmtpTransferTransportClient
import dev.sitar.kmail.smtp.agent.transports.server.TlsCapableSmtpSubmissionServerTransportClient
import kotlinx.coroutines.coroutineScope
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

const val HOSTNAME = "linux.org"

// TODO: use kotlin-logging
// TODO: ktor doesnt support common tls server. implement it in jvm at least manually. it is important
suspend fun main(): Unit = coroutineScope {
    println("STARTING KMAIL SMTP AGENT.")

    val sslContext = SSLContext.getInstance("TLS")

    val keyStore = KeyStore.getInstance("JKS")
    keyStore.load(javaClass.getResourceAsStream("/example.keystore"), "example".toCharArray())

    val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(keyStore, "example".toCharArray())

    val trustManagerFactory = TrustManagerFactory.getInstance("SunX509")
    trustManagerFactory.init(keyStore)

    sslContext.init(keyManagerFactory.keyManagers, null, null)

    val smtpSubmissionAgent = SubmissionAgent.withHostname(
        HOSTNAME,
        authenticationManager = OurSmtpAuthenticationManager,
        client = TlsCapableSmtpSubmissionServerTransportClient(sslContext)
    )

    // TODO: mail from the submission agent should be verified, e.g. google requires a message id
    val transferAgent = TransferAgent.fromOutgoingMessages(
        HOSTNAME,
        smtpSubmissionAgent.incomingMail,
        connector = DefaultTransferSessionSmtpConnector(100, TlsCapableSmtpTransferTransportClient(SSLContext.getDefault()))
    )

    smtpSubmissionAgent.start()
}

data class User(val email: String?) : SmtpAuthenticatedUser

object OurSmtpAuthenticationManager: SubmissionAuthenticationManager<User> {
    override fun authenticate(mechanism: SaslMechanism): User? {
        if (mechanism !is PlainSaslMechanism) return null
        if (mechanism.authenticationIdentity != "example" && mechanism.password != "example") return null

        return User(mechanism.authorizationIdentity)
    }

    override fun canSend(user: User, from: String): Boolean {
        return (user.email?.takeIf { it.isNotEmpty() } ?: from) == from
    }
}