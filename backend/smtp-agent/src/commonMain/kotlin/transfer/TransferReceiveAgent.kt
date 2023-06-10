package dev.sitar.kmail.agents.smtp.transfer

import dev.sitar.kmail.agents.smtp.rewrite.ServerConnection
import dev.sitar.kmail.agents.smtp.rewrite.ServerExtension
import dev.sitar.kmail.agents.smtp.rewrite.StartTlsExtension
import dev.sitar.kmail.agents.smtp.transports.server.SmtpServerTransport
import dev.sitar.kmail.smtp.Domain

//TODO: THE DOMAIN SHOULD POINT TO ADDRESS LITERAL OR A DOMAIN THAT RESOLVES TO A RR
private const val GREET = "localhost ESMTP the revolutionary kmail :-)"

class TransferReceiveAgent(
    transport: SmtpServerTransport,
    config: TransferReceiveConfig
): ServerConnection(transport, config.domain) {
    override val extensions: Set<ServerExtension> = setOf(
        StartTlsExtension(this)
    )
}
data class TransferReceiveConfig(
    val domain: Domain,
    val requiresEncryption: Boolean
)