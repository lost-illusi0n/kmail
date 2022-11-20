package dev.sitar.kmail.smtp.agent

import dev.sitar.kmail.smtp.SaslMechanism

interface SmtpAuthenticatedUser

interface SubmissionAuthenticationManager<User: SmtpAuthenticatedUser> {
    fun authenticate(mechanism: SaslMechanism): User?

    fun canSend(user: User, from: String): Boolean
}