package dev.sitar.kmail.agents.smtp.transfer

import dev.sitar.kmail.agents.smtp.rewrite.ServerConnection
import dev.sitar.kmail.agents.smtp.rewrite.ServerExtension
import dev.sitar.kmail.agents.smtp.rewrite.StartTlsExtension
import dev.sitar.kmail.agents.smtp.transports.server.SmtpServerTransport
import dev.sitar.kmail.smtp.Domain

class TransferReceiveAgent(
    transport: SmtpServerTransport,
    config: TransferReceiveConfig
): ServerConnection(transport, config.domains, config.address) {
    override val extensions: Set<ServerExtension> = setOf(
        StartTlsExtension(this)
    )
}
data class TransferReceiveConfig(
    val domains: Domain,
    val requiresEncryption: Boolean,
    val address: List<String>
)