package dev.sitar.kmail.agents.smtp.submission

import dev.sitar.kmail.smtp.Path
import dev.sitar.kmail.smtp.SaslMechanism

interface SmtpAuthenticatedUser

interface SubmissionAuthenticationManager<User: SmtpAuthenticatedUser> {
    fun authenticate(mechanism: SaslMechanism): User?

    fun canSend(user: User, from: Path): Boolean
}