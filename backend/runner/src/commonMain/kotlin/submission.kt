package dev.sitar.kmail.runner

import dev.sitar.kmail.agents.smtp.submission.SmtpAuthenticatedUser
import dev.sitar.kmail.agents.smtp.submission.SubmissionAuthenticationManager
import dev.sitar.kmail.agents.smtp.submission.SubmissionConfig
import dev.sitar.kmail.agents.smtp.submission.SubmissionServer
import dev.sitar.kmail.agents.smtp.transports.SMTP_SUBMISSION_PORT
import dev.sitar.kmail.smtp.Path
import dev.sitar.kmail.smtp.PlainSaslMechanism
import dev.sitar.kmail.smtp.SaslMechanism
import dev.sitar.kmail.utils.server.ServerSocketFactory
import mu.KotlinLogging
import kotlin.coroutines.coroutineContext

private val logger = KotlinLogging.logger { }

suspend fun submission(factory: ServerSocketFactory): SubmissionServer {
    logger.info("SMTP submission agent is starting.")

    val socket = factory.bind(SMTP_SUBMISSION_PORT)
    val server = SubmissionServer(socket, SubmissionConfig(CONFIGURATION.domain, requiresEncryption = true, KmailAuthenticationManager), coroutineContext)
    server.listen()

    logger.info("SMTP submission agent has started.")

    return server
//    val agent = SubmissionAgentd(
//        SubmissionConfig(CONFIGURATION.domain, requiresEncryption = true),
//        client.bind(),
//        KmailAuthenticationManager,
//        coroutineContext
//    )

//    val agent = SubmissionAgent(
//        factory.bind()
//    )
//
//    agent.launch()
//
//    logger.info("SMTP submission agent has started.")
//    agent
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