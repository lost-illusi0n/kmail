package dev.sitar.kmail.runner

import dev.sitar.kmail.agents.smtp.submission.SmtpAuthenticatedUser
import dev.sitar.kmail.agents.smtp.submission.SubmissionAuthenticationManager
import dev.sitar.kmail.agents.smtp.submission.SubmissionConfig
import dev.sitar.kmail.agents.smtp.submission.SubmissionServer
import dev.sitar.kmail.agents.smtp.transports.SMTP_SUBMISSION_PORT
import dev.sitar.kmail.smtp.InternetMessage
import dev.sitar.kmail.smtp.Path
import dev.sitar.kmail.smtp.PlainSaslMechanism
import dev.sitar.kmail.smtp.SaslMechanism
import dev.sitar.kmail.utils.server.ServerSocketFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

suspend fun submission(factory: ServerSocketFactory, outgoing: MutableSharedFlow<InternetMessage>): SubmissionServer = coroutineScope {
    logger.info("SMTP submission agent is starting.")

    val server = SubmissionServer(
        factory.bind(Config.smtp.submission.port),
        SubmissionConfig(Config.domains.first(), requiresEncryption = true, KmailAuthenticationManager),
        outgoing
    )
    launch { server.listen() }

    logger.info("SMTP submission agent has started.")

    server
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