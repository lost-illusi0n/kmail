package dev.sitar.kmail.imap.agent.transports

interface ImapServerTransport {
    suspend fun accept(): ImapTransport
}