package dev.sitar.kmail.runner

import dev.sitar.kmail.smtp.Path
import dev.sitar.kmail.smtp.PlainSaslMechanism
import dev.sitar.kmail.smtp.SaslMechanism
import dev.sitar.kmail.smtp.agent.SmtpAuthenticatedUser
import dev.sitar.kmail.smtp.agent.SubmissionAgent
import dev.sitar.kmail.smtp.agent.SubmissionAuthenticationManager
import dev.sitar.kmail.smtp.agent.SubmissionConfig
import dev.sitar.kmail.smtp.agent.transports.server.SmtpServerTransportClient
import kotlinx.coroutines.coroutineScope
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

suspend fun submission(client: SmtpServerTransportClient) = coroutineScope {
    logger.info("SMTP submission agent is starting.")

    val agent = SubmissionAgent(
        SubmissionConfig(CONFIGURATION.domain, requiresEncryption = true),
        client.bind(),
        KmailAuthenticationManager,
        coroutineContext
    )

    agent.launch()

    logger.info("SMTP submission agent has started.")
    agent
}

data class KmailAuthenticatedUser(val email: String) : SmtpAuthenticatedUser

object KmailAuthenticationManager : SubmissionAuthenticationManager<KmailAuthenticatedUser> {
    override fun authenticate(mechanism: SaslMechanism): KmailAuthenticatedUser? {
        // TODO: base authentication settings off of config
        when (mechanism) {
            is PlainSaslMechanism -> {
                // TODO: use some kind of db to store users
                if (mechanism.authenticationIdentity == "example" && mechanism.password == "example") {
                    return KmailAuthenticatedUser(mechanism.authorizationIdentity)
                }
            }
        }

        return null
    }

    override fun canSend(user: KmailAuthenticatedUser, from: Path): Boolean {
        return true
    }
}